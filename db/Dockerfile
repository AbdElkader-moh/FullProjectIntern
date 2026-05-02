FROM mysql:8.0

ENV MYSQL_DATABASE=project_db
ENV MYSQL_USER=appuser

COPY init.sql /docker-entrypoint-initdb.d/
