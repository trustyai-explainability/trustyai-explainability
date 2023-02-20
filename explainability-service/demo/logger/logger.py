import os
import time
from random import random

import pandas as pd
from minio import Minio

import data

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY")
BIAS_RATE = float(os.getenv("BIAS_RATE", 0.5))  # higher rate means getting biased dataset faster

print("Starting!")

BIAS = 1.0
ORIGINAL = data.generate_data(1000, BIAS)


def save(client, original: pd.DataFrame, df: pd.DataFrame):
    updated = pd.concat([original, df])
    updated[["age", "race", "gender"]].to_csv("income-biased-inputs.csv", index=False)
    updated[["income"]].to_csv("income-biased-outputs.csv", index=False)
    client.fput_object(
        "inputs", "income-biased-inputs.csv", "./income-biased-inputs.csv"
    )
    client.fput_object(
        "inputs", "income-biased-outputs.csv", "./income-biased-outputs.csv"
    )
    return updated.copy()


client = Minio(
    MINIO_ENDPOINT,
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=False
)

# Make 'inputs' bucket if not exist.
found = client.bucket_exists("inputs")
if not found:
    client.make_bucket("inputs")
else:
    print("Bucket 'inputs' already exists")

ORIGINAL = save(client, ORIGINAL, data.generate_data(1000, BIAS))
print("Saved initial dataframe")

while True:
    time.sleep(10)
    # create dataframe
    BIAS = BIAS + random() * BIAS_RATE
    ORIGINAL = save(client, ORIGINAL, data.generate_data(1000, BIAS))

    print(f"Saved new dataframe (bias={BIAS})")
