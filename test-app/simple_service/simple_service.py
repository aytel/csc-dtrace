import functools
import logging
import os
import socket
import time

import docker
import flask
import gunicorn
import gunicorn.glogging
import requests


def get_service_id():
    client = docker.from_env()
    container_list = client.containers.list(
        filters={'status': 'running','id': socket.gethostname()},
        ignore_removed=True
    )
    if container_list:
        this = container_list[0]
    else:
        raise Exception("Can't get service_id")
    project = this.labels['com.docker.compose.project']
    service = this.labels['com.docker.compose.service']
    number = this.labels['com.docker.compose.container-number']
    return f'{project}_{service}_{number}'


SERVECE_ID = get_service_id()
HEADERS = (('Content-Type', 'application/x-www-form-urlencoded'),)


class ContextFilter(logging.Filter):
    def filter(self, record):
        record.x_request_id = flask.request.headers.get('x-request-id')
        return True


class Formatter(logging.Formatter):
    def formatTime(self, record, datefmt=None):
        t = time.localtime(record.created)
        t_str = time.strftime('%Y-%m-%dT%H:%M:%S.{}%z', t)
        return t_str.format(round(record.msecs))


class GunicornLogger(gunicorn.glogging.Logger):
    def now(self):
        t = time.time()
        msec = round((t - int(t))*1000)
        t_str = time.strftime('%Y-%m-%dT%H:%M:%S.{}%z', time.localtime(t))
        return t_str.format(msec)


app = flask.Flask(SERVECE_ID)

logger = logging.getLogger(SERVECE_ID)
app_log = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s %(name)s %(x_request_id)s [%(levelname)s] %(message)s')
app_log.setFormatter(formatter)
logger.setLevel(logging.INFO)
logger.addHandler(app_log)
logger.addFilter(ContextFilter())
app.logger = logger


def post(endpoint, params):
    headers = dict(HEADERS)
    headers['x-request-id'] = flask.request.headers.get('x-request-id')
    return requests.post(endpoint, headers=headers, data=params)


def response(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        resp = flask.make_response(func(*args, **kwargs))
        resp.headers['x-request-id'] = flask.request.headers.get('x-request-id')
        return resp
    return wrapper
