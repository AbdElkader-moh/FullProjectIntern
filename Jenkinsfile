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

                sh 'docker compose down'

                sh 'docker compose up -d'
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