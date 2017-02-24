import sys
import glob
import os
import subprocess
import time
import pickle


def get_xml(filename):
    return """<?xml version="1.0" encoding="UTF-8"?>
<cpeDescription xmlns="http://uima.apache.org/resourceSpecifier">

	<collectionReader>
		<collectionIterator>
			<descriptor>
				<import location="jules-bioc-pmc-reader-single.xml" />
			</descriptor>
			<configurationParameterSettings>
				<nameValuePair>
					<name>inputFile</name>
					<value>
						<string>/data/data_corpora/BioC-PMC/Dec2016/unicode/{}</string>
					</value>
				</nameValuePair>
			</configurationParameterSettings>
		</collectionIterator>
	</collectionReader>
	<casProcessors casPoolSize="32" processingUnitThreadCount="1">
		<casProcessor deployment="integrated" name="NoOp AE">
			<descriptor>
				<import location="nothingDescriptor.xml" />
			</descriptor>
			<deploymentParameters />
			<errorHandling>
				<errorRateThreshold action="terminate" value="0/1000" />
				<maxConsecutiveRestarts action="terminate"
					value="30" />
				<timeout max="100000" default="-1" />
			</errorHandling>
			<checkpoint batch="5000" time="1000ms" />
		</casProcessor>
		<casProcessor deployment="integrated" name="CasToXmiDBConsumer">
			<descriptor>
				<import name="jules-cas-xmi-to-db-consumer" />
			</descriptor>
			<deploymentParameters />
			<errorHandling>
				<errorRateThreshold action="terminate" value="0/1000" />
				<maxConsecutiveRestarts action="terminate"
					value="30" />
				<timeout max="100000" default="-1" />
			</errorHandling>
			<checkpoint batch="250" time="1000ms" />
			<configurationParameterSettings>
				<nameValuePair>
					<name>DocumentTable</name>
					<value>
						<string>documents</string>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>PerformGZIP</name>
					<value>
						<boolean>true</boolean>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>UpdateMode</name>
					<value>
						<boolean>true</boolean>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>StoreBaseDocument</name>
					<value>
						<boolean>true</boolean>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>FirstAnnotationType</name>
					<value>
						<string>de.julielab.jules.types.Sentence</string>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>StoreEntireXmiData</name>
					<value>
						<boolean>false</boolean>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>dbcConfigFile</name>
					<value>
						<string>conf/dbcConfiguration.xml</string>
					</value>
				</nameValuePair>
				<nameValuePair>
					<name>IncreasedAttributeSize</name>
					<value>
						<integer>134217728</integer>
					</value>
				</nameValuePair>
			</configurationParameterSettings>
		</casProcessor>
	</casProcessors>
	<cpeConfig>
		<numToProcess>-1</numToProcess>
		<deployAs>immediate</deployAs>
		<checkpoint batch="0" time="300000ms" />
		<timerImpl />
	</cpeConfig>
</cpeDescription>
    """.format(filename)


def get_file_list(root, dmp_list):
    if dmp_list:
        dmp_list = pickle.load(open(dmp_list, "rb"))
    for xml_fi in glob.glob(os.path.abspath(root) + "/*.xml"):
        xml = os.path.basename(xml_fi)
        xml_base_name, ending = os.path.splitext(xml)
        if dmp_list:
            if xml_base_name in dmp_list:
                yield xml
        else:
            yield xml


def start_process(fi_name):
    print("[IMPORT] Importing file: {}".format(fi_name))
    fi_name = os.path.splitext(fi_name)[0]
    with open("log/error/{}".format(fi_name), "w") as stde:
        proc = subprocess.Popen('./runimportPython.sh', shell=True, stderr=stde)


if __name__ == "__main__":
    dump_list = None

    if len(sys.argv) > 1:
        root_folder = sys.argv[1]
    if len(sys.argv) > 2:
        dump_list = sys.argv[2]

    for fi in get_file_list(root_folder, dump_list):
        with open("desc/CPEPython.xml", "w") as py_cpe:
            py_cpe.write(get_xml(fi))
        start_process(fi)
        time.sleep(5)
