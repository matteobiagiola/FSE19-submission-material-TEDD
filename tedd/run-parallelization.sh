#!/bin/bash

function checkApplicationName(){
	local application_name_local=$1
	if [[ $application_name_local != "claroline" && $application_name_local != "addressbook" && $application_name_local != "ppma" \
		&& $application_name_local != "collabtive" && $application_name_local != "mrbs" && $application_name_local != "mantisbt" ]]; then
		echo Unknown application name: $application_name_local
		exit 1
	fi
}

# -----------------------------------------------------------------------------------------------------------------

if test $# -lt 1 ; then echo 'ARGS: application_name' ; exit 1 ; fi

application_name=$1
checkApplicationName $application_name

ready_to_run_parallelization_folder=$HOME/workspace/FSE19-submission-material/ready-to-run-parallelization

echo $application_name parallelization
./run-dependency-graphs-check.sh $ready_to_run_parallelization_folder $application_name parallelization