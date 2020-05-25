import functools
import re
import flask


app = flask.Flask(__name__)


ST_RE = re.compile(r'(?P<lhs>[0-9]+)(?P<op>.)(?P<rhs>[0-9]+)')


def response(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        resp = flask.make_response(func(*args, **kwargs))
        resp.headers['x-request-id'] = flask.request.headers.get('x-request-id')
        return resp
    return wrapper


@app.route("/api/v1.0/parser", methods=['POST'])
@response
def parse():
    statement = flask.request.form.get('statement').strip()
    match = ST_RE.fullmatch(statement)
    if match:
        parsed = match.groupdict()
    else:
        parsed = {}
    return flask.json.jsonify(parsed)
