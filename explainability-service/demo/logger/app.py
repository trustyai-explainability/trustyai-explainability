import os
import time
from random import random, randint

import numpy as np
import pandas as pd
from minio import Minio

MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY")
BIAS_RATE = float(os.getenv("BIAS_RATE", 0.5))  # higher rate means getting biased dataset faster


def between(x: float, a: float, b: float) -> float:
    if a <= x <= b:
        return True
    else:
        return False


def synthetic_gender_biased(age, gender, race, bias: float = 2) -> float:
    if between(age, 0, 18):
        p = 0.1
        if gender == 1:
            return random() < p
        else:
            return random() < p / bias

    elif between(age, 18, 40):
        p = 0.2
        if gender == 1:
            return random() < p
        else:
            return random() < p / bias

    elif between(age, 40, 68):
        p = 0.3
        if gender == 1:
            return random() < p
        else:
            return random() < p / bias

    elif between(age, 68, 100):
        p = 0.4
        if gender == 1:
            return random() < p
        else:
            return random() < p / bias


print("Starting!")


def generate_data(N: int, bias: float):
    age = [randint(0, 100) for _ in range(N)]
    race = [randint(0, 7) for _ in range(N)]
    gender = np.random.choice([0, 1], N, p=[0.2, 0.8])

    income = [synthetic_gender_biased(age[i], gender[i], race[i], bias=bias) for i in range(N)]

    return pd.DataFrame(data={
        "age": age,
        "race": race,
        "gender": gender,
        "income": [1 if x else 0 for x in income]
    })


BIAS = 1.0
ORIGINAL = generate_data(1000, BIAS)


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

ORIGINAL = save(client, ORIGINAL, generate_data(1000, BIAS))
print("Saved initial dataframe")

while True:
    time.sleep(10)
    # create dataframe
    BIAS = BIAS + random() * BIAS_RATE
    ORIGINAL = save(client, ORIGINAL, generate_data(1000, BIAS))

    print(f"Saved new dataframe (bias={BIAS})")
