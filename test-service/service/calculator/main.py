import logging
import operator
import os
import flask
import requests


HEADERS = {'Content-Type': 'application/x-www-form-urlencoded'}
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

app = flask.Flask(__name__)
if __name__ != '__main__':
    gunicorn_logger = logging.getLogger('gunicorn.error')
    app.logger.handlers = gunicorn_logger.handlers
    app.logger.setLevel(gunicorn_logger.level)


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
    return requests.post(endpoint, headers=HEADERS, data=params)


def parse(statement):
    params = {'statement': statement}
    srv_name = SERVICES['parser']
    response = post(f'http://{srv_name}:5001/api/v1.0/parser', params)
    return response.json()


def render(val, frmt):
    params = {'value': val, 'format': frmt}
    srv_name = SERVICES['presenter']
    response = post(f'http://{srv_name}:5002/api/v1.0/presenter', params)
    return response.text


if __name__ == '__main__':
    app.run(host="127.0.0.1", port=os.getenv['LISTEN_PORT'], debug=True)
