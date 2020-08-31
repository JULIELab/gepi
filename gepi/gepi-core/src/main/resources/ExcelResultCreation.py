#!/usr/bin/env python
# coding: utf-8

import pandas as pd
from pandas import ExcelWriter
import csv
import sys
import os

def makeArgumentSymbolPivotTable(df, column, order):
    givengenesfreq = df.pivot_table('docid', index=column, columns=['arg1matchtype','arg2matchtype'],aggfunc='count', fill_value=0)
    # We want to order to pivot table counted arg1symbol occurrences with respect to the match type.
    # We we cannot know if a specific match type combination is actually in the data, so we iterate over the possible
    # combinations and try.
    for o in order:
        if o in givengenesfreq:
            givengenesfreq.sort_values(by=o,ascending=False)
            break
    # Here we summarize over all exact arg1 events:
    givengenesfreq[('exact','sum')] = 0
    if ('exact', 'exact') in givengenesfreq:
        givengenesfreq[('exact', 'sum')] += givengenesfreq[('exact','exact')]
    if ('exact', 'fuzzy') in givengenesfreq:
        givengenesfreq[('exact', 'sum')] += givengenesfreq[('exact','fuzzy')]
    # Here we summarize over all fuzzy arg1 events:
    givengenesfreq[('fuzzy','sum')] = 0
    if ('fuzzy', 'exact') in givengenesfreq:
        givengenesfreq[('fuzzy', 'sum')] += givengenesfreq[('fuzzy','exact')]
    if ('fuzzy', 'fuzzy') in givengenesfreq:
        givengenesfreq[('fuzzy', 'sum')] += givengenesfreq[('fuzzy','fuzzy')]
    givengenesfreq = givengenesfreq[[o for o in order if o in givengenesfreq]]
    givengenesfreq[('both','total sum')] = givengenesfreq[('exact','sum')] + givengenesfreq[('fuzzy','sum')]
    return givengenesfreq

def writeresults(input,output):
    header = ["arg1symbol", "arg2symbol", "arg1text", "arg2text", "arg1entrezid", "arg2entrezid",  "arg1matchtype", "arg2matchtype", "relationtypes", "docid", "sentence"]
    df = pd.read_csv(input, names=header,sep="\t",dtype={'arg1entrezid': object,'arg2entrezid':object,'docid':object,'relationtypes':object},quoting=csv.QUOTE_NONE)
    print(f'Read {len(df)} data rows from {input}.')
    # Remove duplicates in the event types and sort them alphabetically
    reltypes=df["relationtypes"]
    for i in reltypes.index:
        types = list(set(reltypes.at[i].split(',')))
        reltypes.at[i]= ','.join(types)
    columnsorder=[ 'arg1symbol',  'arg2symbol', 'arg1text', 'arg2text', 'arg1entrezid', 'arg2entrezid',
         'arg1matchtype',  'arg2matchtype', 'relationtypes','docid', 'sentence']
    df = df[columnsorder]
    df = df.query('arg1entrezid != arg2entrezid')
    # Input genes argument counts
    order=[('exact', 'exact'),
          ('exact', 'fuzzy'),
          ('exact',   'sum'),
          ('fuzzy', 'exact'),
          ('fuzzy', 'fuzzy'),
          ('fuzzy', 'sum')]

    # Arg1 counts
    givengenesfreq = makeArgumentSymbolPivotTable(df, 'arg1symbol', order)
    givengenesfreq.rename(columns={'exact':'exact match', 'fuzzy':'fuzzy match'},inplace=True)
    # Arg2 counts
    othergenesfreq = makeArgumentSymbolPivotTable(df, 'arg2symbol', order)
    othergenesfreq.rename(columns={'exact':'exact match', 'fuzzy':'fuzzy match'},inplace=True)
    # Relation counts
    relfreq = makeArgumentSymbolPivotTable(df, ['arg1symbol','arg2symbol'], order)
    relfreq.rename(columns={'docid':'numrelations'}, inplace=True)
    # Index resets
    othergenesfreq.reset_index(inplace=True)
    givengenesfreq.reset_index(inplace=True)
    relfreq.reset_index(inplace=True)
    giventodistinctothercount = df[['arg1symbol', 'arg2symbol']].drop_duplicates().groupby(['arg1symbol']).count().sort_values(by=["arg2symbol"], ascending=False)
    giventodistinctothercount.reset_index(inplace=True)

    columndesc=[ 'Input gene symbol',
                'Event partner gene symbol',
                'the document text of the input gene in the found sentence',
                'the document text of the event partner gene in the found sentence',
                'Entrez ID the input gene',
                'Entrez ID of the event partner gene',
                'Input gene match type',
                'Event partner gene match type',
                'The type(s) of events the input gene and its event partner are involved in',
                'PubMed or PMC document ID',
                'The sentence from the literature in which the event was found.']
    resultsdesc = pd.DataFrame({'column':columnsorder, 'description':columndesc})
    print(f'Writing results to {output}.')
    with ExcelWriter(output, mode="w") as ew:
        pd.DataFrame().to_excel(ew, sheet_name='Frontpage')
        df.to_excel(ew, sheet_name="Results", index=False)
        givengenesfreq.to_excel(ew, sheet_name="Given Genes Statistics")
        othergenesfreq.to_excel(ew, sheet_name="Event Partner Statistics")
        relfreq.to_excel(ew, sheet_name="Event Statistics")
        giventodistinctothercount.to_excel(ew, sheet_name="Input Gene Event Div", index=False)
        frontpage = ew.sheets['Frontpage']
        #frontpage.hide_gridlines(2)
        bold = ew.book.add_format({'bold': True})
        frontpage.write(0,0, f'This is a GePi statistics file which contains results of event extraction.')
        frontpage.write(1,0, 'The contained worksheets contain the actual text mining results as well as statistics extracted from them.')
        frontpage.write(3,0, 'The "Results" sheet is a large table containing the gene event arguments, an indication of how well the text matched')
        frontpage.write(4,0, 'a gene synonym ("exact" or "fuzzy"), the recognized type of the event (such as "phosphorylation" or "regulation"),')
        frontpage.write(5,0, 'the document ID (PubMed ID for PubMed results, PMC ID for PubMed Central results) and the sentence in which the')
        frontpage.write(6,0, 'respective event was found.')
        resultsdesc.to_excel(ew, startrow=7, index=False, sheet_name='Frontpage')
        frontpage.write(20,0, 'The matchtype "exact" means that the textual gene name could be matched perfectly to a synonym of a NCBI Gene database entry.')
        frontpage.write(21,0, '"Fuzzy" means that the gene name found in the literature could only be mapped to an NCBI Gene record by allowing minor differences when comparing with the synonyms.')
        frontpage.write(22,0, 'Example: Assume the text match was "{}". This cannot be found exactly in NCBI Gene. However, the synonym "{}" exists which could be used for the mapping.'.format('25 kDa lysophospholipid-specific lysophospholipase', 'lysophospholipid-specific lysophospholipase'))
        #frontpage.write(24,0,  'Description of the sheets:', bold)
        frontpage.write(24,0,  'Description of the sheets:')
        frontpage.write(25,0,  '"Given Genes Statistics" shows how often the input gene symbols were found in relations with other genes, separated by exact and fuzzy matches.')
        frontpage.write(26,0,  '"Event Partner Statistics" shows the same but from the perspective of the interaction partners of the input genes.')
        frontpage.write(27,0,  '"Event Statistics" lists the extracted events grouped by their combination of input and event partner genes. In other words, it counts how often two genes interact with each other in the results.')
        frontpage.write(28,0,  '"Input Gene Event Diversity" shows for each input gene symbol how many different interaction partners it has in the results.')

    return df

if __name__ == "__main__":
    input  = sys.argv[1]
    output = sys.argv[2]

    writeresults(input,output)

