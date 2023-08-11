FROM registry.access.redhat.com/ubi8/python-311:latest

WORKDIR /generator

COPY requirements-generator.txt requirements.txt

RUN pip3 install -r requirements.txt


COPY inputs.b64 inputs.b64
COPY inputs-2.b64 inputs-2.b64
COPY outputs.b64 outputs.b64
COPY outputs-2.b64 outputs-2.b64
COPY partial.py app.py

CMD [ "python3", "app.py"]