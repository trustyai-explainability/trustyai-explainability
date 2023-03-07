#!/usr/bin/env bash


export SERVICE_STORAGE_FORMAT="MEMORY"
export SERVICE_DATA_FORMAT="CSV"
export SERVICE_METRICS_SCHEDULE="5s"
export SERVICE_BATCH_SIZE=5000
export STORAGE_DATA_FILENAME="income-biased-inputs.csv"
export STORAGE_DATA_FOLDER="/inputs"
export LOG_LEVEL="DEBUG"

quarkus dev