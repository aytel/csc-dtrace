import functools
import os

import flask

import simple_service as srv


app = srv.app


@app.route("/api/v1.0/presenter", methods=['POST'])
@srv.response
def dispath():
    val = flask.request.form.get('value')
    frmt = flask.request.form.get('format')
    formatter = FORMATTERS[frmt]
    return formatter(val)


def format_float(val):
    return '{:f}'.format(float(val))


FORMATTERS = {'float': format_float}
