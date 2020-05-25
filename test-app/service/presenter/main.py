import functools
import os
import flask


app = flask.Flask(__name__)


def response(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        resp = flask.make_response(func(*args, **kwargs))
        resp.headers['x-request-id'] = flask.request.headers.get('x-request-id')
        return resp
    return wrapper


@app.route("/api/v1.0/presenter", methods=['POST'])
@response
def dispath():
    val = flask.request.form.get('value')
    frmt = flask.request.form.get('format')
    formatter = FORMATTERS[frmt]
    return formatter(val)


def format_float(val):
    return '{:f}'.format(float(val))


FORMATTERS = {'float': format_float}
