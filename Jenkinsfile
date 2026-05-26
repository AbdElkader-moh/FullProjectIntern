pipeline {
    agent any

    environment {
        DOCKER_HUB_ID = 'docker-hub-credentials'
        DOCKER_HUB_USER = 'shahd22'
        IMAGE_USER_SERVICE = "${DOCKER_HUB_USER}/user-service"
        IMAGE_SENSOR_SERVICE = "${DOCKER_HUB_USER}/sensor-service"
        IMAGE_SIMULATOR = "${DOCKER_HUB_USER}/simulator"
        IMAGE_FRONTEND = "${DOCKER_HUB_USER}/frontend"
        TAG_SPRINT = 'sprint3'
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
                }
            }
        }

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
                sh 'docker compose pull'
                sh 'docker compose up -d'
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
        }
    }
}