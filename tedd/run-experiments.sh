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

if test $# -lt 2 ; then echo 'ARGS: application_name' ; exit 1 ; fi

application_name=$1
only_compute_graph_build_time=$2
checkApplicationName $application_name

echo BASELINE_COMPLETE
./run-experiment.sh $application_name baseline_complete baseline_complete
echo
echo
echo STRING_ANALYSIS
./run-experiment.sh $application_name tedd string_analysis
echo
echo
echo NLP_VERB_ONLY_BASELINE
./run-experiment.sh $application_name tedd nlp_verb_only_baseline
echo
echo
echo NLP_VERB_ONLY_STRING
./run-experiment.sh $application_name tedd nlp_verb_only_string
echo
echo
echo NLP_DOBJ_BASELINE
./run-experiment.sh $application_name tedd nlp_dobj_baseline
echo
echo
echo NLP_DOBJ_STRING
./run-experiment.sh $application_name tedd nlp_dobj_string
echo
echo
echo NLP_NOUN_MATCHING_BASELINE
./run-experiment.sh $application_name tedd nlp_noun_matching_baseline
echo
echo
echo NLP_NOUN_MATCHING_STRING
./run-experiment.sh $application_name tedd nlp_noun_matching_string
echo
echo