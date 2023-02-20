from random import random, randint

import numpy as np
import pandas as pd


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
