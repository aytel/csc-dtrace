FROM python:3.6.10-alpine3.11

COPY . /app
WORKDIR /app

RUN pip install -r requirements.txt

# EXPOSE 5000
# CMD ["gunicorn", "-w 4", "-b 0.0.0.0:5000", "main:app"]