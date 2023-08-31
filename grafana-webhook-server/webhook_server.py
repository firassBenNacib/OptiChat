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
    
    if state == "alerting":
        queue_size = get_queue_size_from_prometheus()
        n = (queue_size // 1000)

        desired_target_queue_size = int(queue_size // (n-1))
        print(f"New Queue Size: {desired_target_queue_size}")
        modify_scaled_object_target_queue_size(str(desired_target_queue_size))
    elif state == "ok":
        modify_scaled_object_target_queue_size("1000")
        
    return jsonify({"message": "Webhook received"}), 200

def get_queue_size_from_prometheus():
    prometheus_url = "http://10.110.96.5:9090/api/v1/query"
    query = {"query": "sum(pending_messages)"} 
    response = requests.get(prometheus_url, params=query)
    result = response.json()

    print(f"Prometheus Response: {result}")
    

    queue_size = int(float(result['data']['result'][0]['value'][1]))
    
    print(f"Extracted Queue Size: {queue_size}")

    return queue_size

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
