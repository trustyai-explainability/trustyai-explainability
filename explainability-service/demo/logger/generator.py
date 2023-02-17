import os
import requests
from time import sleep

SERVICE_ENDPOINT = os.getenv("SERVICE_ENDPOINT")

print("Starting the gRPC generator!")
print(f"Sending requests to {SERVICE_ENDPOINT}")

with open('inputs.b64') as f:
    inputs = f.read().splitlines()

with open('outputs.b64') as f:
    outputs = f.read().splitlines()

sleep(10)  # Wait a little for the service

for i in range(len(inputs)):
    print("=" * 80)

    print("Sending data")

    req = requests.post(SERVICE_ENDPOINT, json={
        "input": inputs[i],
        "output": outputs[i]
    })

    sleep(5)
