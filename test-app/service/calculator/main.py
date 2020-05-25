import functools
import logging
import operator
import os

import flask
import requests

import simple_service as srv


app = srv.app


OP_MAP = {
    '+': operator.add,
    '-': operator.sub,
    '/': operator.truediv,
    '*': operator.mul
}


@app.route("/api/v1.0/calculator", methods=['POST'])
@srv.response
def calc():
    statement = flask.request.form.get('statement')
    parsed = parse(statement)
    app.logger.info(f'Parsed statement: {parsed}')
    lhs = float(parsed['lhs'])
    rhs = float(parsed['rhs'])
    op = parsed['op']
    result = OP_MAP[op](lhs, rhs)
    return render(result, 'float')


def parse(statement):
    params = {'statement': statement}
    response = srv.post('http://parser:5000/api/v1.0/parser', params)
    return response.json()


def render(val, frmt):
    params = {'value': val, 'format': frmt}
    response = srv.post('http://presenter:5000/api/v1.0/presenter', params)
    return response.text
