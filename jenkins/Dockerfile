FROM jenkins/jenkins:lts

USER root

RUN apt-get update && apt-get install -y docker.io docker-compose git && \
    git config --global --add safe.directory '*'

# Keep user as root to allow access to docker.sock mounted from Windows Docker Desktop
# USER jenkins