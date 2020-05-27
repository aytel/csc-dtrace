import functools
import re
import flask
import simple_service as srv

app = srv.app


ST_RE = re.compile(r'(?P<lhs>[0-9]+)(?P<op>.)(?P<rhs>[0-9]+)')


@app.route("/api/v1.0/parser", methods=['POST'])
@srv.response
def parse():
    statement = flask.request.form.get('statement').strip()
    app.logger.info(f'Statement: {statement}')
    match = ST_RE.fullmatch(statement)
    if match:
        parsed = match.groupdict()
    else:
        parsed = {}
    return flask.json.jsonify(parsed)
