pipeline {

    agent any

    environment {
        IMAGE_TAG = 'latest'
            MYSQL_PASSWORD = credentials('MYSQL_PASSWORD')
            SECRETSCLOUDINARY_CLOUD_NAME = credentials('SECRETSCLOUDINARY_CLOUD_NAME')
            CLOUDINARY_CLOUD_NAME = credentials('CLOUDINARY_CLOUD_NAME')
            CLOUDINARY_API_KEY = credentials('CLOUDINARY_API_KEY')
            CLOUDINARY_API_SECRET = credentials('CLOUDINARY_API_SECRET')
            MYSQL_USER = credentials('MYSQL_USER')
            MYSQL_ROOT_PASSWORD = credentials('MYSQL_ROOT_PASSWORD')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }
  stage('Prepare Secrets') {
    steps {
        sh '''
    mkdir -p secrets

    echo "$MYSQL_PASSWORD" > secrets/mysql_password.txt
    echo "$SECRETSCLOUDINARY_CLOUD_NAME" > secrets/secretscloudinary_api_key.txt
    echo "$CLOUDINARY_CLOUD_NAME" > secrets/cloudinary_cloud_name.txt
    echo "$CLOUDINARY_API_KEY" > secrets/cloudinary_api_key.txt
    echo "$CLOUDINARY_API_SECRET" > secrets/cloudinary_api_secret.txt
    echo "$MYSQL_USER" > secrets/mysql_user.txt
    echo "$MYSQL_ROOT_PASSWORD" > secrets/mysql_root_password.txt
'''
    }
    }
    stage('Verify Secrets') {
        steps {
            sh 'pwd'
            sh 'ls -la secrets' 
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