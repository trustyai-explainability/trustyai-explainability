import os
import random
import requests
from time import sleep

SERVICE_ENDPOINT = os.getenv("SERVICE_ENDPOINT")
MODEL_NAME = os.getenv("MODEL_NAME", "example-model-1")
INPUT_FILE = os.getenv("INPUT_FILE", 'inputs.b64')
OUTPUT_FILE = os.getenv("OUTPUT_FILE", 'outputs.b64')

print("Starting the gRPC generator!")
print(f"Sending requests to {SERVICE_ENDPOINT}")

with open(INPUT_FILE) as f:
    inputs = f.read().splitlines()

with open(OUTPUT_FILE) as f:
    outputs = f.read().splitlines()

sleep(10)  # Wait a little for the service

while True:
    i = random.randint(0, len(inputs))
    print("=" * 80)

    print("Sending data")

    req = requests.post(SERVICE_ENDPOINT, json={
        "input": inputs[i],
        "output": outputs[i],
        "modelId": MODEL_NAME
    })

    sleep(random.randint(2, 5))
