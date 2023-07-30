pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
     environment {
        SCANNER_HOME = tool 'SonarScanner'
        APP_NAME = 'kube-keda'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        HELM_CHART_REPO = 'https://github.com/firassBenNacib/appfor-helm.git' 
        HELM_CHART_PATH = 'helm'
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
            def appName = 'kube-keda' 
            def buildVersion = "${env.BUILD_NUMBER}"
            def imageTag = "${appName}:${buildVersion}"


            def previousBuildNumber = buildVersion.toInteger() - 1
            def previousImageTag = "${appName}:${previousBuildNumber}"

  
            try {
                sh "docker image inspect ${previousImageTag}"
                sh "docker image rm ${previousImageTag}"
            } catch (Exception e) {
                echo "Image ${previousImageTag} does not exist. Skipping removal."
            }

            sh "docker build -t ${imageTag} --build-arg APP_NAME=${appName} --build-arg BUILD_VERSION=${buildVersion} ."
        }
    }
}



        stage('Push image to Hub') {
            steps {
                script {
                    def appName = 'kube-keda' 
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
    environment {
        GIT_REPO_NAME = "appfor-helm" // Update with your Git repository name
        GIT_USER_NAME = "firassBenNacib" // Update with your Git username
    }
    steps {
        withCredentials([string(credentialsId: 'jenkins-github-token', variable: 'GITHUB_TOKEN')]) {
            sh '''
                git config user.email "firas.bennacib@esprit.tn" // Update with your email
                git config user.name "firassBenNacib" // Update with your name
                BUILD_NUMBER=${BUILD_NUMBER}
                sed -i "s/tag: latest/tag: ${BUILD_NUMBER}/g" helm/values.yaml // Use the correct relative path for values.yaml
                git add helm/values.yaml // Use the correct relative path for values.yaml
                git commit -m "Update values.yaml with build version ${BUILD_NUMBER}"
                git push https://${GITHUB_TOKEN}@github.com/${GIT_USER_NAME}/${GIT_REPO_NAME} HEAD:main
            '''
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