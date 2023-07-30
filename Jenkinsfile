pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
    environment {
        SCANNER_HOME = tool 'SonarScanner'
        APP_NAME = 'kube-keda'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        HELM_CHART_PATH = './path/to/helm/chart' // Replace with the actual path to your Helm chart
    }

    stages {
        stage('CheckOut') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/firassBenNacib/appfor.git']]])
            }
        }

        stage("Test Cases") {
            steps {
                sh "mvn clean test jacoco:report"
            }
        }

        stage("Package") {
            steps {
                sh "mvn clean package"
            }
        }

        stage("Sonarqube Analysis") {
            steps {
                withSonarQubeEnv('Sonar-Server') {
                    sh """
                    ${SCANNER_HOME}/bin/sonar-scanner \
                    -Dsonar.projectKey=forapp-project \
                    -Dsonar.projectName='forapp-project' \
                    -Dsonar.java.binaries=target/classes
                    """
                }
            }
        }

        stage("Quality gate") {
            steps {
                timeout(time: 4, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

         stage('Build docker image') {
            steps {
                script {
                    def appName = 'my-app-name' // Replace with your app name
                    def buildVersion = "${env.BUILD_NUMBER}"
                    def imageTag = "${appName}:${buildVersion}"

                    // Check if the image with the same tag exists and remove it
                    try {
                        sh "docker image inspect ${imageTag}"
                        sh "docker image rm ${imageTag}"
                    } catch (Exception e) {
                        echo "Image ${imageTag} does not exist. Skipping removal."
                    }

                    // Build the new image with the specified appname and build version
                    sh "docker build -t ${imageTag} --build-arg APP_NAME=${appName} --build-arg BUILD_VERSION=${buildVersion} ."
                }
            }
        }


        stage('Push image to Hub') {
            steps {
                script {
                    def imageName = "${APP_NAME}:${BUILD_NUMBER}"

                    withCredentials([string(credentialsId: 'dockerhub-pwd', variable: 'dockerhubpwd')]) {
                        sh "docker login -u firaskill12 -p ${dockerhubpwd}"
                        sh "docker tag ${imageName} firaskill12/${imageName}"
                        sh "docker push firaskill12/${imageName}"
                    }
                }
            }
        }

          stage('Update Helm Chart') {
            steps {
                script {
                    def appName = 'my-app-name' // Replace with your app name
                    def buildVersion = "${env.BUILD_NUMBER}"
                    def valueYamlPath = "./path/to/helm/chart/value.yaml"

                    // Replace the placeholder with the build version in value.yaml
                    sh "sed -i 's/{{ .Values.appVersion }}/${buildVersion}/g' ${valueYamlPath}"
                }
            }
        }
    }


   post {
        success {
            script {
                slackSend(
                    color: 'good',
                    message: "Build successful! :white_check_mark:",
                    channel: '#jenkins',
                    tokenCredentialId: 'Slack-Token'
                )
                emailext body: "Build successful!", 
                         subject: "\$PROJECT_NAME - Build # \$BUILD_NUMBER - \$BUILD_STATUS!", 
                         to: "firas.bennacib@esprit.tn", 
                         mimeType: 'text/plain'
            }
        }
        failure {
            script {
                slackSend(
                    color: 'danger',
                    message: "Build failed! :x:",
                    channel: '#jenkins',
                    tokenCredentialId: 'Slack-Token'
                )
                emailext body: "Build failed!", 
                         subject: "\$PROJECT_NAME - Build # \$BUILD_NUMBER - \$BUILD_STATUS!", 
                         to: "firas.bennacib@esprit.tn", 
                         mimeType: 'text/plain'
            }
        }
    }
}
