FROM python:3.6.10-alpine3.11

COPY . /app
WORKDIR /app

RUN pip install -r requirements.txt

# EXPOSE 5001
# CMD ["gunicorn", "-w 4", "-b 0.0.0.0:5001", "main:app"]