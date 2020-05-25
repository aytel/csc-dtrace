import functools
import logging
import os
import time

import flask
import gunicorn
import gunicorn.glogging
import requests


APP_NAME = os.getenv('APP_NAME')
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


app = flask.Flask(APP_NAME)

logger = logging.getLogger(APP_NAME)
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
