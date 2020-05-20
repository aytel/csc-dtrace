import re
import flask


app = flask.Flask(__name__)


ST_RE = re.compile(r'(?P<lhs>[0-9]+)(?P<op>.)(?P<rhs>[0-9]+)')


@app.route("/api/v1.0/parser", methods=['POST'])
def parse():
    # return str(flask.request.form)
    statement = flask.request.form.get('statement').strip()
    match = ST_RE.fullmatch(statement)
    if match:
        parsed = match.groupdict()
    else:
        parsed = {}
    return flask.json.jsonify(parsed)
