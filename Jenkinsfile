pipeline {
    agent any
    tools{
        maven 'Maven3'
    }
     triggers {
        pollSCM 'H * * * *'
    }
    stages{
        stage('Build Maven'){
            steps{
              checkout([$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/firassBenNacib/appfor']]])
                sh 'mvn clean install'
            }
        }
              stage('Build docker image'){
            steps{
                script{
                    sh 'docker build -t myappspring-promethues:latest .'
                }
            }
        }
            stage('Push image to Hub'){
            steps{
                script{
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