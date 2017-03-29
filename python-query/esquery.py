from elasticsearch import Elasticsearch
from elasticsearch.helpers import scan
from os.path import expanduser

import pandas as pd

import sys
import os
import itertools
import argparse


class QueryParser(argparse.ArgumentParser):
    def __init__(self):
        argparse.ArgumentParser.__init__(self,
                                         description="ElasticSearch Query for Gene Events")
        self.add_argument(
            'tid-list-1', type=str, nargs=1,
            help='comma seperated list of tids'
        )
        self.add_argument(
            'tid-list-2', type=str, nargs=1,
            help='comma seperated list of tids'
        )
        self.add_argument(
            '-i', '--input-as-files', metavar='input',
            action='store', nargs=1, default=False,
            type=bool, help='[Boolean] Interpreting tid-list arguments as files. (default: False)'
        )
        self.add_argument(
            '-s', '--storage', metavar='storage',
            action='store', nargs=1, default=False,
            type=bool, help='[Boolean] if print out shall be redirected to seperate files (default: False)'
        )
        self.add_argument(
            '-m', '--mapping', metavar='mapping',
            action='store', nargs=1, default=None,
            type=str, help='Location of a mapping file if no tids are ' + \
                           'used for querying. The file has one tab-seperated entry per line.'
        )
        self.add_argument(
            '-f', '--save_to_file', metavar='filesave',
            action='store', nargs=1, default=None,
            type=str, help="Save the results to a specified csv file."
        )
        self.add_argument(
            '-x', '--index', metavar='index',
            action='store', nargs=1, default="documents",
            type=str, help="Name of the Elasticsearch index (default: 'documents')."
        )


class ESQuery():
    def __init__(self, _connection, _mapping=None, _timeout=50):
        self.connection = _connection
        self.mapping = dict()
        if mapping:
            self.mapping = _mapping
        self.es = Elasticsearch(hosts=self.connection, verify_certs=True, timeout=_timeout)
        self.clear_results()

    def create_json_query(self, tid1, tid2):
        format_tpl = (tid1, tid2)
        q = ('{{"_source": false,"query": {{"filtered": {{' +
             '"query": {{"terms": {{"_type": ["medline","pmc"]}}}},' +
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

    def create_json_single_query(self, tid1):
        q = ('{{"_source": false,"query": {{"filtered": {{' +
             '"query": {{"terms": {{"_type": ["medline","pmc"]}}}},' +
             '"filter": {{"nested": {{"path": "events",' +
             '"query": {{"filtered": {{"query": {{"match_all": {{}}}},' +
             '"filter": {{ ' +
             '"term": {{"events.allarguments": "{0}"}}}}' +
             '}}}},"inner_hits": {{"_source": false,' +
             '"fields": ["events.sentence","events.allarguments",' +
             '"events.alleventtypes"]}}}}}}}}}},' +
             '"fields": ["pmid","pmcid"]}}').format(tid1)
        return q

    def send_query(self, tid1, tid2, _index='documents'):
        if tid2 is None:
            self.update_results(scan(self.es, self.create_json_single_query(tid1), index=_index), tid1)
        else:
            self.update_results(scan(self.es, self.create_json_query(tid1, tid2), index=_index), tid1)

    def update_results(self, scan_results, ftid):
        if not scan_results:
            return
        for result in scan_results:
            pids = self.get_id(result)
            events = self.evaluate_result(result, pids, ftid)
            self.doc_count += 1

    def evaluate_result(self, result, pids, ftid):
        if result.get("inner_hits"):
            hits = result["inner_hits"]["events"]["hits"]["hits"]
        else:
            return None
        _entrez = self.mapping.get(ftid, None)
        for hit in hits:
            arguments = self.get_arguments(hit)
            sentence = self.get_sentence(hit)

            self.results[self.result_count] = \
                {'ids': pids,
                 'sentence': sentence,
                 'arguments': [self.mapping.get(a)
                               if a.startswith("tid") and self.mapping.get(a, None) else
                               a for a in arguments],
                 'main_gene_id': _entrez if _entrez else ftid}
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
            if (pmid is not None) and len(pmid) > 0:
                pmid = pmid[0]
            pmcid = r["fields"].get("pmcid", None)
            if (pmcid is not None) and len(pmcid) > 0:
                pmcid = pmcid[0]
        return {'pmid': pmid, 'pmcid': pmcid}

    def get_arguments(self, r):
        return self.get_fields_content(r, "events.allarguments")

    def get_sentence(self, r):
        _sent = self.get_fields_content(r, "events.sentence")
        if _sent and len(_sent) > 0:
            _sent = _sent[0]
        return _sent

    def print_results(self, rfile=None):
        no_results = False
        if len(self.results) == 0:
            no_results = True
        for res in self.results.values():
            rtxt = "Main Gene ID: {}\n\tDocument IDs: {}\n\tSentence: {}\n\tArguments: {}". \
                format(res["main_gene_id"], res["ids"], res["sentence"], res["arguments"])
            if not rfile:
                print(rtxt, end="\n" + (20 * "-") + "\n")
            else:
                print(rtxt, end="\n" + (20 * "-") + "\n", file=rfile)
        if rfile:
            rfile.close()
            if no_results:
                os.remove(rfile.name)

    def save_results_dframe(self, rfile=None):
        if rfile:
            df1 = pd.DataFrame(
                columns=['Gene Name 1', 'Entrez ID 1', 'Gene Name 2', 'Entrez ID 2',
                         'Medline ID', 'PMC ID', 'Sentence']
            )
            for _key, _value in self.results.items():
                _mid = _value['main_gene_id']
                df1.loc[_key, "Entrez ID 1"] = _mid
                df1.loc[_key, "Medline ID"] = _value['ids'].get('pmid')
                df1.loc[_key, "PMC ID"] = _value['ids'].get('pmcid')
                df1.loc[_key, "Sentence"] = _value['sentence']

                _gene_count = 0
                _is_mid = False
                _mid_names = list()
                _others = list()
                _others_names = list()
                for _arg in _value['arguments']:
                    if _arg.isdigit():
                        _id = int(_arg)
                        if _arg == _mid:
                            _is_mid = True
                        else:
                            _is_mid = False
                            _others.append(_id)
                    else:
                        if _is_mid:
                            _mid_names.append(_arg)
                        else:
                            _others_names.append(_arg)
                df1.loc[_key, "Gene Name 1"] = ", ".join(_mid_names)
                df1.loc[_key, "Entrez ID 2"] = ", ".join(map(str, _others))
                df1.loc[_key, "Gene Name 2"] = ", ".join(_others_names)

            df1.to_csv(rfile, sep='§', index=False)

    def clear_results(self):
        self.results = {}
        self.result_count = 0
        self.doc_count = 0


def read_file(fi):
    gene_list = list()
    try:
        ifi = os.path.abspath(fi)
        with open(ifi, 'r') as ifile:
            for line in ifile.readlines():
                line = line.rstrip('\n')
                if len(line.split('\t')) == 1:
                    gene_list.append(line)
                else:
                    gene_list.append(line.split('\t')[1])
    except IOError:
        print("One or more input files don't exist or can't be read.")
    return gene_list


def build_mapping_dict(mapping):
    gene_dict = dict()
    gene_dict_reversed = dict()
    try:
        mfi = os.path.abspath(mapping)
        with open(mfi, 'r') as mfile:
            for line in mfile.readlines():
                line = line.rstrip('\n')
                line = line.split('\t')
                gene_dict[line[0]] = line[1]
                gene_dict_reversed[line[1]] = line[0]
    except IOError:
        print("mapping file {} does not exist or can't be read.".format(mapping))
    return gene_dict, gene_dict_reversed


def replace_tids(tlist, mapping):
    for i in range(len(tlist)):
        tid = tlist.pop()
        ntid = mapping.get(tid, tid)
        tlist.insert(0, ntid)


if __name__ == "__main__":
    parser = QueryParser()
    args = vars(parser.parse_args())

    try:
        upw = (open(expanduser("~") + "/.server_cred", "r").read()).rstrip("\n")
    except IOError:
        print("please create a '.server_cred' in your home-folder " + \
              "file that holds one line (with your server credentials):\n\t" + \
              "user-name	user-pw")
        sys.exit()

    user, pw = upw.split()

    finput = args['input_as_files']
    tid_group2 = None
    if finput:
        finput = finput[0]
        print("[Config] Interpreting tid lists as input file")
        tid_group1 = read_file(args['tid-list-1'][0])
        if (args['tid-list-2'][0]).lower() != "none":
            tid_group2 = read_file(args['tid-list-2'][0])
    else:
        tid_group1 = (args['tid-list-1'][0]).split(",")
        if (args['tid-list-2'][0]).lower() != "none":
            tid_group2 = (args['tid-list-2'][0]).split(",")

    tofile = args['storage']
    if tofile:
        tofile = tofile[0]
        print("[Config] Redirecting print out to seperate files")

    mapping = args['mapping']
    mdict, mdict_reversed = None, None
    if mapping:
        mapping = mapping[0]
        print("[Config] Using '{}' as mapping file".format(mapping))
        # mdict: entrezID to tid
        # mdict_reversed: the other way 'round
        mdict, mdict_reversed = build_mapping_dict(mapping)
        replace_tids(tid_group1, mdict)
        if tid_group2:
            replace_tids(tid_group2, mdict)

    es = ESQuery("http://{}:{}@darwin:9200/".format(user, pw), mdict_reversed)

    file_store = args['save_to_file']
    ofile = None
    if file_store:
        file_store = file_store[0]
        file_store = os.path.abspath(file_store)
        ofile = open(file_store, 'w')
        print("[Config] Saving results to '{}'".format(file_store))

    index_name = args['index']
    if not index_name == "documents":
        index_name = args['index'][0]
    if tid_group2:
        for comb in itertools.product(tid_group1, tid_group2):
            tid1, tid2 = comb

            if tofile:
                tofile = open("out/{}-{}.txt".format(mdict_reversed[tid1], mdict_reversed[tid2]), "w")

            es.send_query(tid1, tid2, index_name)
            es.print_results(tofile)
            es.save_results_dframe(ofile)
            es.clear_results()
    else:
        for tid in tid_group1:
            if tofile:
                tofile = open("out/{}.txt".format(mdict_reversed[tid]), "w")

            es.send_query(tid, None, index_name)
            es.print_results(tofile)
            es.save_results_dframe(ofile)
            es.clear_results()

    if ofile:
        ofile.close()
