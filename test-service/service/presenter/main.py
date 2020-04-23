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


if __name__ == '__main__':
    app.run(host="127.0.0.1", port=5002, debug=True)
