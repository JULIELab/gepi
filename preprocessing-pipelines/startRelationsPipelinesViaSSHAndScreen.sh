SERVERLIST=`cat pipelineRunServers.lst`
declare -a SERVERS=($SERVERLIST)

for server in ${SERVERS[@]}; do
	echo "Starting pipelines on $server."
	ssh $server "screen -dmS JPP-relations bash -c \"export JAVA_BIN=~/tmp/jre1.7.0_21/bin/java; cd /home/faessler/Coding/workspace/jules-preprocessing-pipelines;./runGenesAndRelationsPipeline.sh -u\""
done
