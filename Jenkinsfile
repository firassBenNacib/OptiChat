pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
     environment {
        SCANNER_HOME = tool 'SonarScanner'
        APP_NAME = 'kube-keda'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
    
       
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
            def previousBuildNumber = buildVersion.toInteger() - 1
            def previousImageTag = "${appName}:${previousBuildNumber}"

            withCredentials([string(credentialsId: 'dockerhub-pwd', variable: 'dockerhubpwd')]) {
                sh "docker login -u firaskill12 -p ${dockerhubpwd}"
                sh "docker tag ${imageTag} firaskill12/${imageTag}"
                sh "docker push firaskill12/${imageTag}"

             
                try {
                    sh "docker image rm firaskill12/${previousImageTag}"
                } catch (Exception e) {
                    echo "Image firaskill12/${previousImageTag} does not exist. Skipping removal."
                }
            }
        }
    }
}

stage('Update Chart') {
    environment {
        GIT_USER_NAME = "firassBenNacib"
        GIT_REPO_URL = "github.com/firassBenNacib/appfor-helm.git"
    }
    steps {
        script {
            withCredentials([string(credentialsId: 'GITHUB_USERNAME', variable: 'GITHUB_USERNAME'),
                             string(credentialsId: 'GITHUB_TOKEN', variable: 'GITHUB_TOKEN')]) {
                // Clone the repository to the 'helm-repo' directory
                sh 'git clone https://' + GIT_REPO_URL + ' helm-repo'

                dir('helm-repo/helm') {
                    // Set the new tag value
                    def newTag = BUILD_NUMBER

                    // Read the current values.yaml content
                    def currentContent = readFile('values.yaml')

                    // Update the tag value
                    def updatedContent = currentContent.replaceAll(/(^tag:\s+).*/, "\$1${newTag}")

                    // Write back the updated content to values.yaml
                    writeFile(file: 'values.yaml', text: updatedContent)

                    // Check the git status
                    sh 'git status'

                    // Commit the changes
                    sh 'git config user.email "firas.bennacib@esprit.tn"'
                    sh "git config user.name $GIT_USER_NAME"
                    sh 'git add . --all'
                    sh "git commit -m 'Update values.yaml with build version ${newTag}'"

                    // Push the changes back to the repository
                    sh "git push https://$GITHUB_USERNAME:$GITHUB_TOKEN@$GIT_REPO_URL HEAD:main"
                }
            }
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