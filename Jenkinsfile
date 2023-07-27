pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
    environment {
        SCANNER_HOME = tool 'SonarScanner'
    }
  
    stages {
        stage('CheckOut') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/firassBenNacib/appfor']]])
               
            }
        }
        
        stage("Test Cases") {
            steps {
                sh "mvn test"
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
    post {
        always {
            // This step will trigger the webhook after the SonarQube analysis is complete
            script {
                def response = httpRequest(
                    httpMode: 'POST',
                    url: 'http://localhost:8081/sonarqube-webhook/',
                    contentType: 'APPLICATION_JSON',
                    requestBody: '{}'
                )
                echo "SonarQube webhook response status: ${response.status}"
            }
        }
    }
}



        
        stage('Build docker image') {
            steps {
                script {
                    sh 'docker build -t myappspring-promethues:latest .'
                }
            }
        }
        
        stage('Push image to Hub') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'dockerhub-pwd', variable: 'dockerhubpwd')]) {
                        sh 'docker login -u firaskill12 -p ${dockerhubpwd}'
                    }
                    sh 'docker tag myappspring-promethues:latest firaskill12/kube-keda:latest'
                    sh 'docker push firaskill12/kube-keda:latest'
                }
            }
        }
    }
}