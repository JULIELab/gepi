# Sometimes it happens that the actual document storage of ElasticSearch gets
# out of sync with actually processed documents. After importing all existing
# document IDs from ElasticSearch into the 'semedico.pmidsines' table, this
# command resets those rows that have to be imported into ES again.
update semedico.semedico_mirror set is_processed=FALSE where pmid in (select pmid from _data._data where pmid not in (select pmid from semedico.pmidsines))

