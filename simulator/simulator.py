import os
import time
import random
import requests
import threading

BASE_URL = os.getenv("SENSOR_SERVICE_URL", "http://sensor-service:8081")

TRAFFIC_INTERVAL = int(os.getenv("TRAFFIC_INTERVAL_SECONDS", 300))
LIGHT_INTERVAL = int(os.getenv("LIGHT_INTERVAL_SECONDS", 120))
AIR_INTERVAL = int(os.getenv("AIR_INTERVAL_SECONDS", 30))


def send_traffic():
    while True:
        data = {
            "location": "Alexandria",
            "trafficDensity": random.randint(0, 500),
            "avgSpeed": round(random.uniform(0, 120), 2),
            "congestionLevel": random.choice(["Low", "Moderate", "High","Severe"])
        }

        try:
            response = requests.post(
                f"{BASE_URL}/api/sensors/traffic",
                json=data,
                timeout=20
            )

            print("\n[TRAFFIC]")
            print("Sent:", data)
            print("Status:", response.status_code)
            print("Response:", response.text)

        except requests.exceptions.RequestException as e:
            print("\n[TRAFFIC ERROR]", e)

        time.sleep(TRAFFIC_INTERVAL)


def send_light():
    while True:
        data = {
            "location": "Smouha",
            "brightnessLevel": random.randint(0, 100),
            "powerConsumption": round(random.uniform(0, 5000), 2),
            "status": random.choice(["ON", "OFF"])
        }

        try:
            response = requests.post(
                f"{BASE_URL}/api/sensors/light",
                json=data,
                timeout=20
            )

            print("\n[LIGHT]")
            print("Sent:", data)
            print("Status:", response.status_code)
            print("Response:", response.text)

        except requests.exceptions.RequestException as e:
            print("\n[LIGHT ERROR]", e)

        time.sleep(LIGHT_INTERVAL)


def send_air():
    while True:
        data = {
            "location": "Cairo",
            "pm2_5": round(random.uniform(0, 100), 2),
            "pm10": round(random.uniform(0, 200), 2),
            "co": round(random.uniform(0, 50), 2),
            "no2": round(random.uniform(0, 100), 2),
            "so2": round(random.uniform(0, 100), 2),
            "ozone": round(random.uniform(0, 300), 2),
            "pollutionLevel": random.choice(["Good", "Moderate", "Unhealthy","Very_Unhealthy"])
        }

        try:
            response = requests.post(
                f"{BASE_URL}/api/sensors/air",
                json=data,
                timeout=20
            )

            print("\n[AIR]")
            print("Sent:", data)
            print("Status:", response.status_code)
            print("Response:", response.text)

        except requests.exceptions.RequestException as e:
            print("\n[AIR ERROR]", e)

        time.sleep(AIR_INTERVAL)


if __name__ == "__main__":
    t1 = threading.Thread(target=send_traffic, daemon=True)
    t2 = threading.Thread(target=send_light, daemon=True)
    t3 = threading.Thread(target=send_air, daemon=True)

    t1.start()
    t2.start()
    t3.start()

    # Keep main thread alive
    while True:
        time.sleep(1)