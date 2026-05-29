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
                // Stop and remove the old application containers (ignoring Jenkins)
                sh 'docker compose stop mysql user-service sensor-service frontend simulator || true'
                sh 'docker compose rm -f mysql user-service sensor-service frontend simulator || true'
                
                // Bring up the application containers on the same docker network without conflicts
                sh 'docker compose -p fullprojectintern up -d mysql user-service sensor-service frontend simulator'
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