OptiChat
Overview
OptiChat is an messaging application designed to automatically scale according to user demand using KEDA (Kubernetes-based Event Driven Autoscaling). For instance, every thousand messages trigger an upscale to ensure smooth performance, while periods of inactivity result in a downscale to optimize resource use when no messages are being processed. This project incorporates a robust CI pipeline with Jenkins for continuous integration and employs Argo CD for continuous deployment, leveraging Helm for managing the manifest repository. Application responsiveness is monitored through Prometheus and Grafana, which also facilitates alert-based scaling. Additionally, message datasets are exported for analysis with Weka, enhancing its data mining capabilities.

Features
Automatic Scaling: Utilizes KEDA to adjust resources automatically based on the number of messages, ensuring efficient resource usage.
Continuous Integration and Deployment: Integrates a CI pipeline with Jenkins and CD with Argo CD, streamlined with Helm for manifest management.
Monitoring and Alerting: Employs Prometheus and Grafana for real-time monitoring and alert-based scaling, ensuring optimal application performance.
Data Mining: Exports message datasets for analysis with Weka, improving the application's data mining capabilities.
Getting Started
Prerequisites
Kubernetes cluster
Helm
Jenkins
Argo CD
Prometheus and Grafana
Weka for data analysis (optional)
