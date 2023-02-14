#!/usr/bin/env bash

export MODEL_NAME="example"
export KSERVE_TARGET="localhost:8033"
export STORAGE_FORMAT="MINIO"
export MINIO_BUCKET_NAME="inputs"
# If running MinIO locally, otherwise change
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_INPUT_FILENAME="income-biased-inputs.csv"
export MINIO_OUTPUT_FILENAME="income-biased-outputs.csv"
export MINIO_SECRET_KEY="minioadmin"
export MINIO_ACCESS_KEY="minioadmin"
export METRICS_SCHEDULE="5s"
quarkus dev