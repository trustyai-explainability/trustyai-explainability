import os
import random
import requests
import uuid
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

# generate full list of partials
partial_requests = []
partial_responses = []
for i in range(len(inputs)):
    id = uuid.uuid4()
    request = {
        "modelid": MODEL_NAME,
        "id": str(id),
        "data": inputs[i],
        "kind": "request"
    }
    partial_requests.append(request)
    response = {
        "modelid": MODEL_NAME,
        "id": str(id),
        "data": outputs[i],
        "kind": "response"
    }
    partial_responses.append(response)

request_indices = list(range(len(inputs)))
random.shuffle(request_indices)
response_indices = list(range(len(inputs)))
random.shuffle(response_indices)

for index in range(len(inputs)):
    print(f"🚀 Sending partial request to {SERVICE_ENDPOINT}")
    req = requests.post(SERVICE_ENDPOINT, json=partial_requests[request_indices[index]])
    sleep(random.randint(1, 3))
    # use same indices, for now
    print(f"🎲 Sending partial response to {SERVICE_ENDPOINT}")
    req = requests.post(SERVICE_ENDPOINT, json=partial_responses[request_indices[index]])
