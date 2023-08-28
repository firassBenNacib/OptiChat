from flask import Flask, request, jsonify
from kubernetes import client, config

app = Flask(__name__)

# Configure the Kubernetes client
config.load_incluster_config()

@app.route("/grafana-webhook", methods=["POST"])
def grafana_webhook():
    data = request.json
    if not data:
        return jsonify({"message": "No data received"}), 400


    state = data.get("state", "")
    
    if state == "alerting":
  
        modify_scaled_object_target_queue_size("100000")
    elif state == "ok":

        modify_scaled_object_target_queue_size("1000")

    return jsonify({"message": "Webhook received"}), 200

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
