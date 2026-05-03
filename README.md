# 🚀 Internship Project

Full-stack application using:

* Angular (Frontend)
* Spring Boot (Backend)
* MySQL (Database)
* Docker (Containerization)

---

# 📦 Prerequisites

Make sure you have installed:

* Docker
* Git
* Java 17
* Maven

---

# 📁 Project Structure

```
ProjectRoot/
│
├── Frontend/
├── backend/user/
├── run-project.ps1
├── run-project.sh
└── README.md
```

---

# ⚡ Run the Project (Recommended)

## 🟦 Windows (PowerShell)

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-project.ps1
```

---

## 🟩 WSL / Linux / Git Bash

```bash
chmod +x run-project.sh
./run-project.sh
```

---

# 🌍 Open the App

```text
http://localhost:4200
```

---

# 🧠 How It Works

```
Browser
 → Angular (Frontend)
 → Nginx
 → Spring Boot Backend
 → MySQL Database
```

---

# 🐳 Manual Setup (If Script Fails)

## 1. Create Network

```bash
docker network create project-net
```

---

## 2. Run MySQL

```bash
docker run -d --name internship-mysql \
  --network project-net \
  -p 3306:3306 \
  -v mysql_data:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_PASSWORD=app123 \
  custom-mysql
```

---

## 3. Run Backend

```bash
cd backend/user
mvn clean package -DskipTests
docker build -t spring-user-app .
docker run -d --name internship-backend \
  --network project-net \
  -p 8080:8080 \
  spring-user-app
cd ../..
```

---

## 4. Run Frontend

```bash
cd Frontend
docker build -t angular-frontend .
docker run -d --name internship-frontend \
  --network project-net \
  -p 4200:80 \
  angular-frontend
cd ..
```

---

# 🔍 Useful Commands

## Check running containers

```bash
docker ps
```

## View logs

```bash
docker logs internship-backend
docker logs internship-frontend
docker logs internship-mysql
```

---

# 🗄️ Database Info

```
Database: project_db
User: appuser
Password: app123
```

Connect:

```bash
docker exec -it internship-mysql mysql -uappuser -papp123 project_db
```

---

# 🔌 API Endpoints

```
POST /api/users/signup
POST /api/users/login
GET  /api/users/me
POST /api/users/logout
```

---

# ⚠️ Important Notes

* Backend URL:

```
jdbc:mysql://internship-mysql:3306/project_db
```

* Frontend MUST use:

```
/api
```

NOT localhost:8080

---

# 🛠 Troubleshooting

## Port already in use

```bash
docker rm -f <container-name>
```

## Reset everything

```bash
docker rm -f internship-frontend internship-backend internship-mysql
docker volume rm mysql_data
```

---

# 🎯 Done

Open:

```
http://localhost:4200
```

