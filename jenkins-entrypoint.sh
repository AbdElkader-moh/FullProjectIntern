#!/bin/bash
# Dynamic GID mapping for Docker socket permission handling on startup

if [ -S /var/run/docker.sock ]; then
    # 1. Identify the GID of the host's /var/run/docker.sock
    DOCKER_SOCKET_GID=$(stat -c '%g' /var/run/docker.sock)
    echo "Detected host /var/run/docker.sock GID: $DOCKER_SOCKET_GID"

    # 2. Update the container's docker group GID to match
    EXISTING_GROUP=$(getent group "$DOCKER_SOCKET_GID" | cut -d: -f1)
    
    if [ -n "$EXISTING_GROUP" ]; then
        echo "Group with GID $DOCKER_SOCKET_GID already exists: $EXISTING_GROUP"
        if [ "$EXISTING_GROUP" != "docker" ]; then
            echo "Adding 'jenkins' user to existing group: $EXISTING_GROUP"
            usermod -aG "$EXISTING_GROUP" jenkins
        fi
    else
        echo "Updating container 'docker' group GID to $DOCKER_SOCKET_GID"
        groupmod -g "$DOCKER_SOCKET_GID" docker
    fi
fi

# Ensure all files in /var/jenkins_home are owned by the jenkins user
echo "Fixing ownership of /var/jenkins_home to jenkins:jenkins..."
chown -R jenkins:jenkins /var/jenkins_home

# 3. Switch to the 'jenkins' user to launch Jenkins
echo "Starting Jenkins as 'jenkins' user..."
exec runuser -u jenkins -- /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
