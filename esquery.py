from elasticsearch import Elasticsearch
from elasticsearch.helpers import scan

import sys
import itertools
import argparse

class QueryParser(argparse.ArgumentParser):
	
	def __init__(self):
		argparse.ArgumentParser.__init__(self,
			description="ElasticSearch Query for Gene Events")
		self.add_argument(
				    'tid-list-1', type = str, nargs = 1,
					 help = 'comma seperated list of tids'
				)
		self.add_argument(
				    'tid-list-2', type = str, nargs = 1,
					 help = 'comma seperated list of tids'
				)
		self.add_argument(
			'-f', '--store-to-file', metavar = 'Store to file',
			action = 'store', nargs = 1, default = False,
			type = bool, help = 'Boolean if result of each combination ' + \
			 'shall be stored in a seperate file (default: False)'
				)

class ESQuery():

	def __init__(self, connection):
		self.connection = connection
		self.es = Elasticsearch(hosts=self.connection, verify_certs=True)
		self.clear_results()

	def create_json_query(self, tid1, tid2):
		format_tpl = (tid1, tid2)
		q = ('{{"_source": false,"query": {{"filtered": {{' +
		'"query": {{"match_all": {{}}}},' +
		'"filter": {{"nested": {{"path": "events",' +
		'"query": {{"filtered": {{"query": {{"match_all": {{}}}},' +
		'"filter": {{"and": [' +
		'{{"term": {{"events.allarguments": "{0}"}}}},' +
		'{{"term": {{"events.allarguments": "{1}"}}}}' +
		']}}}}}},"inner_hits": {{"_source": false,' +
		'"fields": ["events.sentence","events.allarguments",' +
		'"events.alleventtypes"]}}}}}}}}}},' +
		'"fields": ["pmid","pmcid"]}}').format(*format_tpl)
		return q

	def send_query(self, tid1, tid2):
		self.update_results(scan(self.es, self.create_json_query(tid1, tid2)))

	def update_results(self, scan_results):
		if not scan_results:
			return
		for result in scan_results:
			pids = self.get_id(result)
			events = self.evaluate_result(result, pids)
			self.doc_count += 1

	def evaluate_result(self, result, pids):
		hits = result["inner_hits"]["events"]["hits"]["hits"]
		for hit in hits:
			arguments = self.get_arguments(hit)
			sentence = self.get_sentence(hit)

			self.results[self.result_count] = \
				{'ids': pids, 'sentence':sentence, 'arguments':arguments}
			self.result_count += 1

	def get_fields_content(self, rdict, content):
		if rdict.get("fields", None):
			return rdict["fields"].get(content, None)
		return None

	def get_id(self, r):
		pmid = None
		pmcid = None
		if r.get("fields", None):
			pmid = r["fields"].get("pmid", None)
			pmcid = r["fields"].get("pmcid", None)
		return {'pmid':pmid, 'pmcid':pmcid}

	def get_arguments(self, r):
		return self.get_fields_content(r, "events.allarguments")

	def get_sentence(self, r):
		return self.get_fields_content(r, "events.sentence")

	def print_results(self, rfile=None):
		for res in self.results.values():
			rtxt = "IDs: {}\n\tSentence: {}\n\tArguments: {}".\
					format(res["ids"], res["sentence"], res["arguments"])
			if not rfile:
				print(rtxt, end="\n"+(20*"-")+"\n")
			else:
				print(rtxt, end="\n"+(20*"-")+"\n", file=rfile)
		if rfile:		
			rfile.close()
				

	def clear_results(self):
		self.results = {}
		self.result_count = 0
		self.doc_count = 0


if __name__ == "__main__":
	parser = QueryParser()
	args = vars(parser.parse_args())

	try:
		upw = (open("cred.txt","r").read()).rstrip("\n")
	except:
		print("please create a 'cred.txt' file that holds one line:\n\t" + \
				"user-name	user-pw")
		sys.exit()

	user, pw = upw.split()

	tid_group1 = (args['tid-list-1'][0]).split(",")
	tid_group2 = (args['tid-list-2'][0]).split(",")
	tofile = args['store_to_file']
	if tofile:
		tofile = args['store_to_file'][0]

	es = ESQuery("http://{}:{}@dawkins:9200/".format(user, pw))

	for comb in itertools.product(tid_group1, tid_group2):
		tid1, tid2 = comb

		if tofile:
			tofile = open("{}-{}.txt".format(tid1, tid2), "w")

		es.send_query(tid1, tid2)
		es.print_results(tofile)
		es.clear_results()
