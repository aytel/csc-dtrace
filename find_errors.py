import requests
import sys
import json
from collections import defaultdict

rid = sys.argv[1]
evts = requests.get('http://localhost:9200/unknown-/_doc/_search?pretty', json={"query": {"term": {"rid": rid}}}).json()['hits']['hits']
pretty = json.dumps(evts, indent=2)

par = {}

balancers = set()

balancers_req = defaultdict(list)
balancers_res = defaultdict(list)

for item in evts:
	data = item['_source']
	if data['type'] == 'balancer':
		if data['spid'] != '-':
			par[data['spid']] = data['parid']
		this = data['srv_name']
		ev_type = data.get('ev_type', '')
		balancers.add(this)
		if ev_type == 'SUBM':
			balancers_req[this].append(data)
		if ev_type == '':
			balancers_res[this].append(data)

bad = []
bad_spids = set()

for balancer in balancers:
	balancers_req[balancer].sort(key=lambda data: data['ts'])
	balancers_res[balancer].sort(key=lambda data: data['ts'])
	while len(balancers_res[balancer]) > 0 and balancers_res[balancer][-1]['status'] == '200':
		balancers_res[balancer].pop()
		balancers_req[balancer].pop()
	if len(balancers_req[balancer]) > 0:
		if len(balancers_res[balancer]) > 0:
			res = balancers_res[balancer][-1]
		else:
			res = None
		bad.append({"req": balancers_req[balancer][-1], "res": res})
		bad_spids.add(balancers_req[balancer][-1]['spid'])

used = set()
to_del = set()

for spid in bad_spids:
	spid = par[spid]
	while spid not in used and spid != '-':
		used.add(spid)
		if spid in bad_spids:
			to_del.add(spid)
		spid = par[spid]

for bad_item in bad:
	if (bad_item['req']['spid'] not in to_del):
		print(json.dumps(bad_item, indent=2))
