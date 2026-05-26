pipeline {
    agent any

    environment {
        DOCKER_HUB_ID = 'docker-hub-credentials'
        DOCKER_HUB_USER = 'shahd22'
        IMAGE_FRONTEND = "${DOCKER_HUB_USER}/frontend"
        IMAGE_BACKEND = "${DOCKER_HUB_USER}/backend"
        TAG_SPRINT = 'sprint3'
    }

    stages {

        stage('Backend Integration & Unit Tests') {
            steps {
                dir('backend/user') {
                    // Triggers the test framework using Maven wrapper for the user service
                    sh 'chmod +x mvnw'
                    sh './mvnw clean test'
                }
                dir('backend/sensor_data') {
                    // Triggers the test framework using Maven wrapper for the sensor service
                    sh 'chmod +x mvnw'
                    sh './mvnw clean test'
                }
            }
        }

        stage('Docker Hub Login & Build Push') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_ID}", passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                        // Securely login to Docker Hub
                        sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
                        
                        // Build and tag Backend services
                        sh "docker build -t ${IMAGE_BACKEND}-user:latest -t ${IMAGE_BACKEND}-user:${TAG_SPRINT} ./backend/user"
                        sh "docker build -t ${IMAGE_BACKEND}-sensor:latest -t ${IMAGE_BACKEND}-sensor:${TAG_SPRINT} ./backend/sensor_data"
                        
                        // Build and tag Frontend
                        sh "docker build -t ${IMAGE_FRONTEND}:latest -t ${IMAGE_FRONTEND}:${TAG_SPRINT} ./Frontend"
                        
                        // Push Backend images
                        sh "docker push ${IMAGE_BACKEND}-user:latest"
                        sh "docker push ${IMAGE_BACKEND}-user:${TAG_SPRINT}"
                        sh "docker push ${IMAGE_BACKEND}-sensor:latest"
                        sh "docker push ${IMAGE_BACKEND}-sensor:${TAG_SPRINT}"
                        
                        // Push Frontend images
                        sh "docker push ${IMAGE_FRONTEND}:latest"
                        sh "docker push ${IMAGE_FRONTEND}:${TAG_SPRINT}"
                    }
                }
            }
        }

        stage('Automated Infrastructure Deploy') {
            steps {
                // Gracefully shutdown old services
                sh 'docker compose down || true'
                
                // Recreate the secrets directory that is ignored by Git
                sh '''
                    mkdir -p secrets
                    echo "rootpass" > secrets/mysql_root_password.txt
                    echo "dbuser" > secrets/mysql_user.txt
                    echo "dbpass" > secrets/mysql_password.txt
                    echo "dummy_cloud" > secrets/cloudinary_cloud_name.txt
                    echo "dummy_key" > secrets/cloudinary_api_key.txt
                    echo "dummy_secret" > secrets/cloudinary_api_secret.txt
                '''
                
                // Launch cleanly in detached mode
                sh 'docker compose up -d'
                
                // Prune dangling, intermediate image layers to maintain disk health
                sh 'docker image prune -f'
            }
        }
    }
    
    post {
        always {
            script {
                try {
                    // Clean up workspace and docker login credentials
                    sh 'docker logout || true'
                    cleanWs()
                } catch (Exception e) {
                    echo "Could not clean workspace or logout: ${e.message}"
                }
            }
        }
    }
}
