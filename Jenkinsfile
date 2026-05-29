pipeline {

    agent any

    environment {
        IMAGE_TAG = 'latest'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Images') {
            steps {
                sh 'docker compose build'
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {

                    sh '''
                        echo $DOCKER_PASS | docker login \
                        -u $DOCKER_USER \
                        --password-stdin
                    '''
                }
            }
        }

        stage('Push Images') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {

                    sh '''
                        docker push $DOCKER_USER/user-service:${IMAGE_TAG}
                    '''

                    sh '''
                        docker push $DOCKER_USER/sensor-service:${IMAGE_TAG}
                    '''

                    sh '''
                        docker push $DOCKER_USER/frontend-app:${IMAGE_TAG}
                    '''

                    sh '''
                        docker push $DOCKER_USER/simulator:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Stop and remove the old application containers (ignoring Jenkins)
                    sh 'docker compose stop mysql user-service sensor-service frontend simulator || true'
                    sh 'docker compose rm -f mysql user-service sensor-service frontend simulator || true'
                    
                    // Dynamically determine the host path of the Jenkins workspace so it is completely generic
                    def jenkinsMount = sh(script: "docker inspect -f '{{range .Mounts}}{{if eq .Destination \"/var/jenkins_home\"}}{{.Source}}{{end}}{{end}}' internship-jenkins", returnStdout: true).trim()
                    def hostProjectPath = "${jenkinsMount}/workspace/${env.JOB_NAME}"
                    
                    // Bring up the application containers using the dynamic host path
                    sh "HOST_PROJECT_PATH=\"${hostProjectPath}\" docker compose -p fullprojectintern up -d mysql user-service sensor-service frontend simulator"
                }
            }
        }
    }

    post {

        success {
            echo 'Pipeline completed successfully'
        }

        failure {
            echo 'Pipeline failed'
        }
    }
}