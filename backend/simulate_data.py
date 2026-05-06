import os
import random
import time
import requests
from datetime import datetime

SENSOR_HOST = os.getenv("SENSOR_HOST", "localhost")
BASE_URL = f"http://{SENSOR_HOST}:8081/api/sensors"
TRAFFIC_URL = f"{BASE_URL}/traffic"
AIR_URL = f"{BASE_URL}/air"
LIGHT_URL = f"{BASE_URL}/light"

def generate_traffic_data():
    return {
        "location": random.choice(["Main St", "1st Avenue", "Broadway", "Elm St"]),
        "trafficDensity": random.randint(0, 500),
        "avgSpeed": round(random.uniform(0.0, 120.0), 2),
        "congestionLevel": random.choice(["Low", "Moderate", "High", "Severe"])
    }

def generate_air_pollution_data():
    return {
        "location": random.choice(["Central Park", "Downtown", "Industrial Area"]),
        "pm2_5": round(random.uniform(0.0, 100.0), 2),
        "pm10": round(random.uniform(0.0, 150.0), 2),
        "co": round(random.uniform(0.0, 50.0), 2),
        "no2": round(random.uniform(0.0, 100.0), 2),
        "so2": round(random.uniform(0.0, 80.0), 2),
        "ozone": round(random.uniform(0.0, 300.0), 2),
        "pollutionLevel": random.choice(["Good", "Moderate", "Unhealthy", "Very_Unhealthy"])
    }

def generate_street_light_data():
    return {
        "location": random.choice(["Zone A", "Zone B", "Zone C"]),
        "brightnessLevel": random.randint(0, 100),
        "powerConsumption": round(random.uniform(0.0, 5000.0), 2),
        "status": random.choice(["ON", "OFF"])
    }

def post_data(url, data, name):
    try:
        response = requests.post(url, json=data)
        if response.status_code in [200, 201]:
            print(f"[{datetime.now()}] Successfully sent {name} data.")
        else:
            print(f"[{datetime.now()}] Failed to send {name} data. Status: {response.status_code}")
    except Exception as e:
        print(f"[{datetime.now()}] Connection error for {name}: {e}")

if __name__ == "__main__":
    print("Starting Sensor Data Simulation...")
    while True:
        post_data(TRAFFIC_URL, generate_traffic_data(), "Traffic")
        post_data(AIR_URL, generate_air_pollution_data(), "Air Pollution")
        post_data(LIGHT_URL, generate_street_light_data(), "Street Light")
        print("Waiting for 5 minutes before the next simulation cycle...")
        time.sleep(300)
