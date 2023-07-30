pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
     environment {
        SCANNER_HOME = tool 'SonarScanner'
        APP_NAME = 'kube-keda'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        HELM_CHART_REPO = 'https://github.com/firassBenNacib/appfor-helm.git' // Replace with your Helm chart repository URL
        HELM_CHART_PATH = 'helm' // Use 'helm' as the path to the Helm chart directory in the repository
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
                    def appName = 'kube-keda' // Replace with your app name
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
                    def appName = 'kube-keda' // Replace with your app name
                    def buildVersion = "${env.BUILD_NUMBER}"
                    def imageTag = "${appName}:${buildVersion}"

                    withCredentials([string(credentialsId: 'dockerhub-pwd', variable: 'dockerhubpwd')]) {
                        sh "docker login -u firaskill12 -p ${dockerhubpwd}"
                        sh "docker tag ${imageTag} firaskill12/${imageTag}"
                        sh "docker push firaskill12/${imageTag}"
                    }
                }
            }
        }
   stage('Update Helm Chart') {
        steps {
            script {
                def appName = 'kube-keda' // Replace with your app name
                def buildVersion = "${env.BUILD_NUMBER}"
                def helmChartRepo = "${HELM_CHART_REPO}"
                def helmChartPath = "${HELM_CHART_PATH}"

                // Check if the helm-repo directory exists
                def helmRepoDir = "helm-repo"
                def repoExists = fileExists(helmRepoDir)

                // Clone or pull the Helm chart repository based on its existence
                if (repoExists) {
                    dir(helmRepoDir) {
                        sh "git pull origin main"
                    }
                } else {
                    sh "git clone ${helmChartRepo} ${helmRepoDir}"
                }

                // Change working directory to the Helm chart directory
                dir("${helmRepoDir}/${helmChartPath}") {
                    // Debugging step: Display the content of values.yaml before the update
                    sh "cat values.yaml"
                    
                    // Replace the placeholder with the build version in values.yaml
                    sh "sed -i 's/{{ .Values.appVersion }}/${buildVersion}/g' values.yaml"
                    
                    // Debugging step: Display the content of values.yaml after the update
                    sh "cat values.yaml"
                }

                // Commit and push the changes back to the repository
                dir(helmRepoDir) {
                    sh "git config --global user.email 'firas.bennacib@esprit.tn'" // Set your email
                    sh "git config --global user.name 'firassBenNacib'" // Set your name
                    sh "git add ${helmChartPath}/values.yaml"
                    sh "git commit -m 'Update values.yaml with build version ${buildVersion}'"
                    sh "git push origin main"
                }
            }
        }
    }

    // ... Rest of the pipeline ...
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
