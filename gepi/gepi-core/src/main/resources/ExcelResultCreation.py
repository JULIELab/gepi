#!/usr/bin/env python
# coding: utf-8

import csv
import pandas as pd
import re
import regex
import sys
from datetime import date
from pandas import ExcelWriter

class Counter:
    # Helper class to count rows
    def __init__(self, start=0):
        self.value = start

    def inc(self, amount=1):
        current_value = self.value
        self.value += amount
        return current_value

def makeNorm2MajorityMap(originalDf, rx):
    allsymbols = pd.concat([originalDf['arg1symbol'], originalDf['arg2symbol']])
    allsymbolsnorm = allsymbols.apply(lambda x: rx.sub('', x.lower()))
    df2 = pd.DataFrame({'norm': allsymbolsnorm, 'symbol': allsymbols})
    # Map each normalized gene to its majority symbol.
    # This is a Series where the index is the normalized name and the value is majority symbol.
    norm2symbol = df2.groupby('norm').agg(lambda s: s.value_counts().index[0])
    return norm2symbol

def mapNormalizedColumnToMajoritySymbol(df, column, norm2symbol):
    df[column] = df[column].map(lambda x: norm2symbol.query(f'norm=="{x}"')['symbol'].iloc[0])

def mapNormalizedIndexToMajoritySymbol(df, norm2symbol):
    df.index = df.index.map(lambda x: norm2symbol.query(f'norm=="{x}"')['symbol'].iloc[0])

def makeGeneHyperlink(value):
    url = ""
    id = ""
    if re.match(r"[0-9]+", value):
        id = value
        url = "https://www.ncbi.nlm.nih.gov/gene/{}"
    elif "HGNCG" in value:
        id = re.search(r"HGNCG:([0-9]+)", value).group(1)
        url = "https://www.genenames.org/data/genegroup/#!/group/{}"
    elif "FPLX" in value:
        url = "https://github.com/sorgerlab/famplex/{}"
    return f'=HYPERLINK("{url.format(id)}", "{value}")'

def writeresults(input,output,searchParameters):
    columndesc=[ 'Input gene symbol',
                 'Event partner gene symbol',
                 'the document text of the input gene in the found sentence',
                 'the document text of the event partner gene in the found sentence',
                 'Entrez ID the input gene',
                 'Entrez ID of the event partner gene',
                 'The type(s) of events the input gene and its event partner are involved in',
                 'Factuality level of the event as determined by text expressions like "suggest", "may" etc.',
                 'PubMed or PMC document ID. PMC documents carry the "PMC" prefix.',
                 'Internal event ID. Useful to find unique identifiers for each event.',
                 'Place of fulltext filter match. Only applicable if filter terms were specified.',
                 'The textual context from the literature in which the event was found. That is the sentence enclosing the event by default. In case of a paragraph-level filter query this can also be the enclosing paragraph. This would then be indicated by the value of the fulltextmatchtype column.']
    df = pd.read_csv(input,sep="\t",dtype={'arg1entrezid': object,'arg2entrezid':object,'docid':object,'relationtypes':object,'fulltextmatchtype':object,'factuality':object},quoting=csv.QUOTE_NONE,keep_default_na=False)
    # The header should be included in the TSV file anyway. But in case we change the names there let's use a fixed
    # set of headers here. As long as the columns themselves don't change, everything will work fine.
    df.columns = ["arg1symbol", "arg2symbol", "arg1text", "arg2text", "arg1entrezid", "arg2entrezid",  "relationtypes", "factuality", "docid", "eventid", "fulltextmatchtype", "context"]
    print(f'Read {len(df)} data rows from {input}.')
    # Remove duplicates in the event types and sort them alphabetically
    reltypes=df["relationtypes"]
    for i in reltypes.index:
        types = list(set(reltypes.at[i].split(',')))
        reltypes.at[i]= ','.join(types)
    columnsorder=[ 'arg1symbol',  'arg2symbol', 'arg1text', 'arg2text', 'arg1entrezid', 'arg2entrezid',
          'relationtypes', 'factuality', 'docid', 'eventid', 'fulltextmatchtype', 'context']
    df = df[columnsorder]

    # Create a df where all symbols are normalized.
    # Use that df to make all statistical calculations.
    # After calculations are done, map the normalized symbols to the most occuring of its original variants.
    rx = regex.compile("\\p{P}+|\\s+")
    normalizedArguments = df[['arg1symbol', 'arg2symbol']].apply(lambda series: series.apply(lambda s: rx.sub('', s.lower())))
    dfNorm = pd.DataFrame({'docid':df['docid'], 'arg1symbol': normalizedArguments['arg1symbol'], 'arg2symbol': normalizedArguments['arg2symbol']})
    norm2symbol = makeNorm2MajorityMap(df, rx)
    
    # Arg1 counts, calculated on the normalized argument names
    givengenesfreq = dfNorm[['docid', 'arg1symbol']].groupby('arg1symbol').count()
    givengenesfreq.columns = ['frequency']
    givengenesfreq.sort_values(by='frequency',ascending=False,inplace=True)
    mapNormalizedIndexToMajoritySymbol(givengenesfreq, norm2symbol)
    # Arg2 counts
    othergenesfreq = dfNorm[['docid', 'arg2symbol']].groupby('arg2symbol').count()
    othergenesfreq.columns = ['frequency']
    othergenesfreq.sort_values(by='frequency',ascending=False,inplace=True)
    mapNormalizedIndexToMajoritySymbol(othergenesfreq, norm2symbol)
    # Directionless counts
    bothgenesfreq = givengenesfreq.add(othergenesfreq, fill_value=0)
    bothgenesfreq.sort_values(by='frequency',ascending=False,inplace=True)
    bothgenesfreq.index.name = 'symbol'
    # No need to adapt the symbols in bothgenesfreq because its just an addition of given- and othergenesfreq which have already been processed
    # Relation counts
    relfreq = dfNorm.pivot_table(values="docid", index=["arg1symbol", "arg2symbol"], aggfunc="count")
    relfreq.rename(columns={'docid':'numrelations'}, inplace=True)
    relfreq.sort_values(by='numrelations',ascending=False,inplace=True)
    relfreq.reset_index(inplace=True)
    mapNormalizedColumnToMajoritySymbol(relfreq, 'arg1symbol', norm2symbol)
    mapNormalizedColumnToMajoritySymbol(relfreq, 'arg2symbol', norm2symbol)
    relfreq.set_index(['arg1symbol', 'arg2symbol'],inplace=True)
    # Distinct gene interaction partner counts
    giventodistinctothercount = dfNorm[['arg1symbol', 'arg2symbol']].drop_duplicates().groupby(['arg1symbol']).count().sort_values(by=["arg2symbol"], ascending=False)
    giventodistinctothercount.rename(columns={'arg2symbol': 'interaction_partner_count'},inplace=True)
    giventodistinctothercount.reset_index(inplace=True)
    mapNormalizedColumnToMajoritySymbol(giventodistinctothercount, 'arg1symbol', norm2symbol)
    # Make lists of argument pairs in both directions for concatenation and distinct counting
    arg1arg2 = dfNorm[['arg1symbol', 'arg2symbol']]
    arg2arg1 = dfNorm[['arg2symbol', 'arg1symbol']]
    # Switch the column names so that they match arg1arg2
    arg2arg1 = arg2arg1.rename(columns={'arg1symbol':'arg2symbol', 'arg2symbol':'arg1symbol'})
    allgenesdistinctcounts = pd.concat([arg1arg2, arg2arg1]).drop_duplicates().groupby(['arg1symbol']).count().sort_values(by=["arg2symbol"], ascending=False)
    allgenesdistinctcounts.reset_index(inplace=True)
    allgenesdistinctcounts.rename(columns={'arg1symbol':'symbol', 'arg2symbol':'count'},inplace=True)
    mapNormalizedColumnToMajoritySymbol(allgenesdistinctcounts, 'symbol', norm2symbol)
    resultsdesc = pd.DataFrame({'column':columnsorder, 'description':columndesc})
    df['arg1entrezid'] = df['arg1entrezid'].apply(makeGeneHyperlink)
    df['arg2entrezid'] = df['arg2entrezid'].apply(makeGeneHyperlink)
    print(f'Writing results to {output}.')
    with ExcelWriter(output, mode="w") as ew:
        pd.DataFrame().to_excel(ew, sheet_name='Frontpage')
        df.to_excel(ew, sheet_name="Results", index=False)
        if 'A' in searchParameters['inputMode'] or 'AB' in searchParameters['inputMode']:
            givengenesfreq.to_excel(ew, sheet_name="Given Genes Statistics")
            othergenesfreq.to_excel(ew, sheet_name="Event Partner Statistics")
            relfreq.to_excel(ew, sheet_name="Event Statistics")
            giventodistinctothercount.to_excel(ew, sheet_name="Input Gene Event Div", index=False)
            allgenesdistinctcounts.to_excel(ew, sheet_name="Gene Argument Event Div", index=False)
        else:
            bothgenesfreq.to_excel(ew, sheet_name="Gene Interaction Statistics")
            relfreq.to_excel(ew, sheet_name="Event Statistics")
            allgenesdistinctcounts.to_excel(ew, sheet_name="Gene Argument Event Div", index=False)
        frontpage = ew.sheets['Frontpage']
        #frontpage.hide_gridlines(2)
        bold = ew.book.add_format({'bold': True})
        c = Counter()
        frontpage.write(c.inc(),0, f'This is a GePi statistics file which contains results of event extraction. Creation date is {date.today()}.')
        frontpage.write(c.inc(),0, 'The contained worksheets contain the actual text mining results as well as statistics extracted from them.')
        frontpage.write(c.inc(),0, 'The result was obtained using the following filter terms:')
        frontpage.write(c.inc(),0, f'Sentence level filter query: {searchParameters["sentenceFilterString"]}')
        frontpage.write(c.inc(),0, f'Paragraph level filter query: {searchParameters["paragraphFilterString"]}')
        frontpage.write(c.inc(),0, f'Section Heading filter query: {searchParameters["sectionNameFilterString"]}')
        frontpage.write(c.inc(),0, f'Event types included: {searchParameters["eventTypes"]}')
        frontpage.write(c.inc(),0, f'Unary events allowed: {searchParameters["includeUnary"]}')
        frontpage.write(c.inc(),0, f'Minimum event likelihood: {searchParameters["likelihood"]}')
        frontpage.write(c.inc(),0, f'Organism filter A or B: {searchParameters["taxId"]}')
        frontpage.write(c.inc(),0, f'Organism filter A: {searchParameters["taxIdsA"]}')
        frontpage.write(c.inc(),0, f'Organism filter B: {searchParameters["taxIdsB"]}')
        frontpage.write(c.inc(),0, 'Only molecular events that were described in a sentence or a paragraph containing the filter terms was returned for this result.')
        frontpage.write(c.inc(),0, 'The "Results" sheet is a large table containing the gene event arguments, an indication of how well the text matched')
        frontpage.write(c.inc(),0, 'a gene synonym, the recognized type of the event (such as "phosphorylation" or "regulation"),')
        frontpage.write(c.inc(),0, 'the document ID (PubMed ID for PubMed results, PMC ID for PubMed Central results) and the sentence in which the')
        frontpage.write(c.inc(),0, 'respective event was found.')
        c.inc()
        resultsdesc.to_excel(ew, startrow=c.inc(14), index=False, sheet_name='Frontpage')
        #frontpage.write(24,0,  'Description of the sheets:', bold)

        frontpage.write(c.inc(),0,  'Description of the sheets:')
        if 'A' in searchParameters['inputMode'] or 'AB' in searchParameters['inputMode']:
            frontpage.write(c.inc(),0,  '"Given Genes Statistics" shows how often the input gene symbols were found in relations with other genes.')
            frontpage.write(c.inc(),0,  '"Event Partner Statistics" shows the same but from the perspective of the interaction partners of the input genes.')
            frontpage.write(c.inc(),0,  '"Event Statistics" lists the extracted events grouped by their combination of input and event partner genes. In other words, it counts how often two genes interact with each other in the results. The value "none" indicates unary events without a second interaction partner.')
            frontpage.write(c.inc(),0,  '"Input Gene Event Diversity" shows for each input gene symbol how many different interaction partners it has in the results.')
            frontpage.write(c.inc(),0,  '"Gene Argument Event Diversity" shows for each gene that participated in an event the number of different interaction partners in the results.')
        else:
            frontpage.write(c.inc(),0,  '"Gene Interaction Statistics" shows how often gene symbols were found in relations with other genes.')
            frontpage.write(c.inc(),0,  '"Event Statistics" lists the extracted events grouped by their combination of input and event partner genes. In other words, it counts how often two genes interact with each other in the results.')
            frontpage.write(c.inc(),0,  '"Gene Argument Event Diversity" shows for each gene that participated in an event the number of different interaction partners in the results. The value "none" indicates the number of unary events without a second interaction partner.')

    return df

if __name__ == "__main__":
    input     = sys.argv[1]
    output    = sys.argv[2]
    searchParameters = {
        'inputMode': sys.argv[3].split(' '),
        'sentenceFilterString': sys.argv[4],
        'paragraphFilterString': sys.argv[5],
        'sectionNameFilterString': sys.argv[6],
        'eventTypes': sys.argv[7].split(' '),
        'includeUnary': sys.argv[8],
        'likelihood': sys.argv[9],
        'taxId': sys.argv[10],
        'taxIdsA': sys.argv[11],
        'taxIdsB': sys.argv[12]
    }
    writeresults(input,output,searchParameters)

