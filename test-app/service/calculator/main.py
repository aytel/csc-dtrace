import logging
import operator
import os
import flask
import requests


HEADERS = (('Content-Type', 'application/x-www-form-urlencoded'),)
OP_MAP = {
    '+': operator.add,
    '-': operator.sub,
    '/': operator.truediv,
    '*': operator.mul
}
SERVICES = {
    'parser': os.getenv('SRV_PARSER'),
    'presenter': os.getenv('SRV_PRESENTER')
}
APP_NAME = os.getenv('APP_NAME')


class ContextFilter(logging.Filter):

    def filter(self, record):
        record.x_request_id = flask.request.headers.get('x-request-id')
        return True


app = flask.Flask(__name__)

logger = logging.getLogger(APP_NAME)
app_log = logging.StreamHandler()
formatter = logging.Formatter('%(x_request_id)s %(asctime)s %(name)s [%(levelname)s] %(message)s')
app_log.setFormatter(formatter)
logger.setLevel(logging.INFO)
logger.addHandler(app_log)
logger.addFilter(ContextFilter())
app.logger = logger


@app.route("/api/v1.0/calculator", methods=['POST'])
def calc():
    statement = flask.request.form.get('statement')
    parsed = parse(statement)
    app.logger.info(f'Parsed statement: {parsed}')
    lhs = float(parsed['lhs'])
    rhs = float(parsed['rhs'])
    op = parsed['op']
    result = OP_MAP[op](lhs, rhs)
    return render(result, 'float')


def post(endpoint, params):
    headers = dict(HEADERS)
    headers['x-request-id'] = flask.request.headers.get('x-request-id')
    return requests.post(endpoint, headers=headers, data=params)


def parse(statement):
    params = {'statement': statement}
    srv_name = SERVICES['parser']
    response = post(f'http://{srv_name}:5000/api/v1.0/parser', params)
    return response.json()


def render(val, frmt):
    params = {'value': val, 'format': frmt}
    srv_name = SERVICES['presenter']
    response = post(f'http://{srv_name}:5000/api/v1.0/presenter', params)
    return response.text
