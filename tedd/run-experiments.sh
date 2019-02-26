#!/bin/bash

function checkApplicationName(){
	local application_name_local=$1
	if [[ $application_name_local != "claroline" && $application_name_local != "addressbook" && $application_name_local != "ppma" \
		&& $application_name_local != "collabtive" && $application_name_local != "mrbs" && $application_name_local != "mantisbt" ]]; then
		echo Unknown application name: $application_name_local
		exit 1
	fi
}

function checkBoolean(){
    local boolean=$1
	if [[ $boolean != "true" && $boolean != "false" ]]; then
		echo Unknown boolean value: $boolean. It is either true or false.
		exit 1
	fi
}

# -----------------------------------------------------------------------------------------------------------------

if test $# -lt 2 ; then echo 'ARGS: application_name, only_compute_graph_build_time' ; exit 1 ; fi

application_name=$1
only_compute_graph_build_time=$2
checkApplicationName $application_name
checkBoolean $only_compute_graph_build_time

echo BASELINE_COMPLETE
./run-experiment.sh $application_name baseline_complete baseline_complete $only_compute_graph_build_time
echo
echo
echo STRING_ANALYSIS
./run-experiment.sh $application_name tedd string_analysis $only_compute_graph_build_time
echo
echo
echo NLP_VERB_ONLY_BASELINE
./run-experiment.sh $application_name tedd nlp_verb_only_baseline $only_compute_graph_build_time
echo
echo
echo NLP_VERB_ONLY_STRING
./run-experiment.sh $application_name tedd nlp_verb_only_string $only_compute_graph_build_time
echo
echo
echo NLP_DOBJ_BASELINE
./run-experiment.sh $application_name tedd nlp_dobj_baseline $only_compute_graph_build_time
echo
echo
echo NLP_DOBJ_STRING
./run-experiment.sh $application_name tedd nlp_dobj_string $only_compute_graph_build_time
echo
echo
echo NLP_NOUN_MATCHING_BASELINE
./run-experiment.sh $application_name tedd nlp_noun_matching_baseline $only_compute_graph_build_time
echo
echo
echo NLP_NOUN_MATCHING_STRING
./run-experiment.sh $application_name compute_filter_and_refine nlp_noun_matching_string $only_compute_graph_build_time
echo
echo