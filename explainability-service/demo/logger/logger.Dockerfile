FROM python:3.10-slim-buster

WORKDIR /logger

COPY requirements-logger.txt requirements.txt

RUN pip3 install -r requirements.txt

COPY data.py data.py
COPY logger.py app.py

CMD [ "python3", "app.py"]