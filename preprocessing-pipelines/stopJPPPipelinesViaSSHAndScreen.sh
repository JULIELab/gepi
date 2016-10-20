#!/bin/bash
SERVERLIST=`cat pipelineRunServers.lst`
declare -a SERVERS=($SERVERLIST)

for server in ${SERVERS[@]}; do
# Send CTRL-C into the screen sessions to stop the Solr servers. The screen
# sessions will then quit themselves.
# Command explanation:
# ‘\003' is the octal notation for CTRL-C. -p 0 does circumvent an issue
# where you can't use the 'stuff' command on detached sessions
# (see http://old.nabble.com/screen--X-stuff-to11415446.html).
# The 'stuff' command itself appends the string following the command
# into the screen session's input buffer. See the screen man page
# for more information.
# Do it two times because we have to pipelines running in a sequence
ssh $server "screen -S JPP-processing -p 0 -X stuff $'\003';screen -S JPP-processing -p 0 -X stuff $'\003'"
done;
