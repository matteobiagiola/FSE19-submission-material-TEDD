#!/bin/bash

function checkApplicationName(){
	local application_name_local=$1
	if [[ $application_name_local != "claroline" && $application_name_local != "addressbook" && $application_name_local != "ppma" \
		&& $application_name_local != "collabtive" && $application_name_local != "mrbs" && $application_name_local != "mantisbt" ]]; then
		echo Unknown application name: $application_name_local
		exit 1
	fi
}

function checkMode(){
	local mode=$1
	if [[  $mode != "string_analysis" \
	    && $mode != "baseline_complete" \
	    && $mode != "nlp_verb_only_baseline" && $mode != "nlp_verb_only_string" \
	    && $mode != "nlp_dobj_baseline" && $mode != "nlp_dobj_string" \
	    && $mode != "nlp_noun_matching_baseline" && $mode != "nlp_noun_matching_string" ]]; then
		echo Unknown mode: $mode
		exit 1
	fi
}

# -----------------------------------------------------------------------------------------------------------------

if test $# -lt 1 ; then echo 'ARGS: application_name mode' ; exit 1 ; fi

application_name=$1
mode=$2

checkApplicationName $application_name
if [[ -n $mode ]]; then
    checkMode $mode
fi

ready_to_run_parallelization_folder=$HOME/workspace/FSE19-submission-material/ready-to-run-parallelization

echo $application_name parallelization
./run-dependency-graphs-check.sh $ready_to_run_parallelization_folder $application_name parallelization $mode