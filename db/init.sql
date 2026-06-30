-- USE project_db;
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    profile_picture VARCHAR(500) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS traffic_sensors_data (
    id CHAR(36) PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    traffic_density INT NOT NULL CHECK (traffic_density >= 0 AND traffic_density <= 500),
    avg_speed FLOAT NOT NULL CHECK (avg_speed >= 0 AND avg_speed <= 120),
    congestion_level ENUM('Low', 'Moderate', 'High', 'Severe') NOT NULL
);

CREATE TABLE IF NOT EXISTS air_pollution_sensors_data (
    id CHAR(36) PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    pm2_5 FLOAT,
    pm10 FLOAT,
    co FLOAT NOT NULL CHECK (co >= 0 AND co <= 50),
    no2 FLOAT,
    so2 FLOAT,
    ozone FLOAT NOT NULL CHECK (ozone >= 0 AND ozone <= 300),
    pollution_level ENUM('Good', 'Moderate', 'Unhealthy', 'Very_Unhealthy') NOT NULL
);

CREATE TABLE IF NOT EXISTS street_light_sensors_data (
    id CHAR(36) PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    brightness_level INT NOT NULL CHECK (brightness_level >= 0 AND brightness_level <= 100),
    power_consumption FLOAT NOT NULL CHECK (power_consumption >= 0 AND power_consumption <= 5000),
    status ENUM('ON', 'OFF') NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
    id CHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    metric VARCHAR(50) NOT NULL,
    threshold_value FLOAT NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    metric VARCHAR(50) NOT NULL,
    value FLOAT NOT NULL,
    threshold_value FLOAT NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    location VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);