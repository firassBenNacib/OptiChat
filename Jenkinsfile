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
}
   stage("Quality gate") {
            steps {
                 timeout(time: 2, unit: 'MINUTES') {
                waitForQualityGate abortPipeline: true
			}
        }
   }


        
        stage('Build docker image') {
            steps {
                script {
                    sh 'docker build -t myappspring-prometheus:latest .'
                }
            }
        }
        
        stage('Push image to Hub') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'dockerhub-pwd', variable: 'dockerhubpwd')]) {
                        sh 'docker login -u firaskill12 -p ${dockerhubpwd}'
                    }
                    sh 'docker tag myappspring-prometheus:latest firaskill12/kube-keda:latest'
                    sh 'docker push firaskill12/kube-keda:latest'
                }
            }
        }
    }
}