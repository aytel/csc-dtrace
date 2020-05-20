import os
import flask


app = flask.Flask(__name__)


@app.route("/api/v1.0/presenter", methods=['POST'])
def dispath():
    val = flask.request.form.get('value')
    frmt = flask.request.form.get('format')
    formatter = FORMATTERS[frmt]
    return formatter(val)


def format_float(val):
    return '{:f}'.format(float(val))


FORMATTERS = {'float': format_float}
