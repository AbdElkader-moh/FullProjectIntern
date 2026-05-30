pipeline {
<<<<<<< HEAD
    agent any

    environment {
        DOCKER_HUB_ID = 'docker-hub-credentials'
        DOCKER_HUB_USER = 'shahd22'
        IMAGE_USER_SERVICE = "${DOCKER_HUB_USER}/user-service"
        IMAGE_SENSOR_SERVICE = "${DOCKER_HUB_USER}/sensor-service"
        IMAGE_SIMULATOR = "${DOCKER_HUB_USER}/simulator"
        IMAGE_FRONTEND = "${DOCKER_HUB_USER}/frontend"
        TAG_SPRINT = 'sprint3'
        
        CLOUDINARY_CLOUD_NAME = credentials('cloudinary-cloud-name')
        CLOUDINARY_API_KEY = credentials('cloudinary-api-key')
        CLOUDINARY_API_SECRET = credentials('cloudinary-api-secret')
    }

    stages {
        stage('Initialization & Clean Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Backend Integration & Unit Tests') {
            steps {
                dir('backend/user') {
                    sh 'chmod +x mvnw || true'
                    sh './mvnw clean test'
                }
                dir('backend/sensor_data') {
                    sh 'chmod +x mvnw || true'
                    sh './mvnw clean test'
=======

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
>>>>>>> b485ec14d5e88360bd0794f0fa63bdb60e3edea4
                }
            }
        }

<<<<<<< HEAD
        stage('Build Stage') {
            steps {
                echo 'Building Docker images...'
                sh "docker build -t ${IMAGE_USER_SERVICE}:latest -t ${IMAGE_USER_SERVICE}:${TAG_SPRINT} ./backend/user"
                sh "docker build -t ${IMAGE_SENSOR_SERVICE}:latest -t ${IMAGE_SENSOR_SERVICE}:${TAG_SPRINT} ./backend/sensor_data"
                sh "docker build -t ${IMAGE_SIMULATOR}:latest -t ${IMAGE_SIMULATOR}:${TAG_SPRINT} ./simulator"
                sh "docker build -t ${IMAGE_FRONTEND}:latest -t ${IMAGE_FRONTEND}:${TAG_SPRINT} ./Frontend"
            }
        }

        stage('Registry Stage') {
            steps {
                echo 'Logging in to Docker Hub and pushing images...'
                withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_ID}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    
                    retry(3) {
                        // Push latest tags
                        sh "docker push ${IMAGE_USER_SERVICE}:latest"
                        sh "docker push ${IMAGE_SENSOR_SERVICE}:latest"
                        sh "docker push ${IMAGE_SIMULATOR}:latest"
                        sh "docker push ${IMAGE_FRONTEND}:latest"
                        
                        // Push sprint tags
                        sh "docker push ${IMAGE_USER_SERVICE}:${TAG_SPRINT}"
                        sh "docker push ${IMAGE_SENSOR_SERVICE}:${TAG_SPRINT}"
                        sh "docker push ${IMAGE_SIMULATOR}:${TAG_SPRINT}"
                        sh "docker push ${IMAGE_FRONTEND}:${TAG_SPRINT}"
                    }
                }
            }
        }

        stage('Deployment Stage') {
            steps {
                echo 'Deploying infrastructure using Docker Compose...'
                sh 'docker compose down || true'
                sh 'docker compose up -d --build'
                sh 'docker image prune -f'
            }
        }

        stage('Validation Stage') {
            steps {
                echo 'Waiting for services to initialize...'
                sleep time: 20, unit: 'SECONDS'
                echo 'Verifying running containers...'
                sh 'docker compose ps'
            }
        }
    }

    post {
        always {
            script {
                try {
                    echo 'Cleaning up environment...'
                    sh 'docker logout || true'
                    cleanWs()
                } catch (Exception e) {
                    echo "Cleanup bypassed: ${e.message}"
                }
            }
=======
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
        sh 'docker compose -f /c/jenkins_home/workspace/sprint3-pipeline/docker-compose.yml --project-directory /c/jenkins_home/workspace/sprint3-pipeline down'
        sh 'docker compose -f /c/jenkins_home/workspace/sprint3-pipeline/docker-compose.yml --project-directory /c/jenkins_home/workspace/sprint3-pipeline up -d'
    }
}
    }

    post {

        success {
            echo 'Pipeline completed successfully'
        }

        failure {
            echo 'Pipeline failed'
>>>>>>> b485ec14d5e88360bd0794f0fa63bdb60e3edea4
        }
    }
}