from flask import Flask, request, jsonify
from kubernetes import client, config
import requests

app = Flask(__name__)  # NOSONAR



config.load_incluster_config()

@app.route("/grafana-webhook", methods=["POST"])
def grafana_webhook():
    data = request.json
    if not data:
        return jsonify({"message": "No data received"}), 400

    state = data.get("state", "")
    queue_difference = get_queue_difference_from_prometheus()
    queue_size = get_queue_size_from_prometheus()


    if queue_size == 0:
        return jsonify({"message": "Queue size is 0. No action taken."}), 200
    
    if state == "alerting" and queue_difference < 200:
       
        n = (queue_size // 1000)
        
        if n == 1:
            desired_target_queue_size = 10000
        else:
            desired_target_queue_size = int((queue_size // (n-1)) - 100)
     
        print(f"New Queue Size: {desired_target_queue_size}")
        modify_scaled_object_target_queue_size(str(desired_target_queue_size))
    elif state == "ok" and queue_difference > 200:
        modify_scaled_object_target_queue_size("1000")
        
    return jsonify({"message": "Webhook received"}), 200

def get_queue_size_from_prometheus():
    prometheus_url = "http://10.110.96.5:9090/api/v1/query"   # NOSONAR
    query = {"query": "sum(pending_messages)"} 
    response = requests.get(prometheus_url, params=query)
    result = response.json()

    print(f"Prometheus Response: {result}")
    

    queue_size = int(float(result['data']['result'][0]['value'][1]))
    
    print(f"Extracted Queue Size: {queue_size}")

    return queue_size
    
def get_queue_difference_from_prometheus():
    prometheus_url = "http://10.110.96.5:9090/api/v1/query"   # NOSONAR
    query = {"query": "queue_difference_metric"}  
    response = requests.get(prometheus_url, params=query)
    result = response.json()

    print(f"Prometheus Response for Queue Difference: {result}")

    try:
        queue_difference = int(float(result['data']['result'][0]['value'][1]))
    except (KeyError, IndexError, TypeError, ValueError):
        print("No data in queue difference")
        return 0

    return queue_difference


def modify_scaled_object_target_queue_size(new_size):
    api_instance = client.CustomObjectsApi()

    group = 'keda.sh'
    version = 'v1alpha1'
    namespace = 'default' 
    plural = 'scaledobjects'
    name = 'myapp-scaledobject'

    current_scaled_object = api_instance.get_namespaced_custom_object(group, version, namespace, plural, name)
    triggers = current_scaled_object['spec']['triggers']

    for trigger in triggers:
        if trigger['type'] == 'activemq':
            trigger['metadata']['targetQueueSize'] = new_size
            break

    body = {
        "spec": {
            "triggers": triggers
        }
    }

    try:
        api_response = api_instance.patch_namespaced_custom_object(group, version, namespace, plural, name, body)
        print(f"ScaledObject modified: {api_response}")
    except client.ApiException as e:
        print(f"Exception when modifying ScaledObject: {e}")

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
