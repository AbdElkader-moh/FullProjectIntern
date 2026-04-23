FROM mysql:8.0

ENV MYSQL_DATABASE=project_db
ENV MYSQL_ROOT_PASSWORD=root123
ENV MYSQL_USER=appuser
ENV MYSQL_PASSWORD=app123

COPY init.sql /docker-entrypoint-initdb.d/