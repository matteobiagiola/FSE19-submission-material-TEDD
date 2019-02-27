#!/bin/bash

function checkMainClassName(){
	local main_class_name=$1
	if [[ $main_class_name != "baseline_complete" \
	    && $main_class_name != "tedd" ]]; then
		echo Unknown main class name: $main_class_name
		exit 1
	fi
}

function checkApplicationName(){
	local application_name_local=$1
	if [[ $application_name_local != "claroline" && $application_name_local != "addressbook" && $application_name_local != "ppma" \
		&& $application_name_local != "collabtive" && $application_name_local != "mrbs" && $application_name_local != "mantisbt" ]]; then
		echo Unknown application name: $application_name_local
		exit 1
	fi
}

function runExp(){
    local application_name=$1
    local main_class=$2
    local start_container=$3
    local collect_stats=$4
    if [[ $collect_stats == "true" ]]; then
        ./run-java.sh $main_class $application_name $start_container $collect_stats \
            > $logs_name 2> $errors_logs_name
    else
        ./run-java.sh $main_class $application_name $start_container $collect_stats
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

if test $# -lt 3 ; then echo 'ARGS: application_name main_class mode' ; exit 1 ; fi

application_name=$1
main_class=$2
mode=$3

os=$(uname)

properties_file=$PWD/src/main/resources/app.properties

checkApplicationName $application_name
checkMainClassName $main_class
checkMode $mode

start_container=true
collect_stats=true

if [[ $only_compute_graph_build_time == "true" ]]; then
    collect_stats=false
    start_container=false
fi

if [[ $os == "Darwin" ]]; then
    sed -i "" "s%dependency_graph_path=.*$%dependency_graph_path=$HOME/workspace/FSE19-submission-material/tedd/src/main/resources%g" \
        $properties_file
    sed -i "" "s%test_suite_path=.*$%test_suite_path=$HOME/workspace/FSE19-submission-material/testsuite-$application_name/src/main/java/main/TestSuite.java%g" \
        $properties_file
    sed -i "" "s%tests_path=.*$%tests_path=$HOME/workspace/FSE19-submission-material/testsuite-$application_name/src/main/java/tests%g" \
        $properties_file
    sed -i "" "s%only_compute_graph_build_time=.*$%only_compute_graph_build_time=$only_compute_graph_build_time%g" \
        $properties_file
    sed -i "" "s%recover_missed_dependencies=.*$%recover_missed_dependencies=true%g" $properties_file
    if [[ $main_class == "baseline_complete" && $mode == "baseline_complete" ]]; then
        sed -i "" "s%recover_missed_dependencies=.*$%recover_missed_dependencies=false%g" $properties_file
        sed -i "" "s%baseline=.*$%baseline=true%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "string_analysis" ]]; then
        sed -i "" "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=common_values%g" $properties_file
        sed -i "" "s%values_to_filter=.*$%values_to_filter=%g" $properties_file
        if [[ $application_name != "claroline" ]]; then
            sed -i "" "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=true%g" $properties_file
        else
            sed -i "" "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
        fi
        if [[ $application_name == "collabtive" || $application_name == "claroline" ]]; then
            sed -i "" "s%values_to_filter=.*$%values_to_filter=admin:admin%g" $properties_file
        elif [[ $application_name == "mantisbt" ]]; then
            sed -i "" "s%values_to_filter=.*$%values_to_filter=administrator:root%g" $properties_file
        fi
    elif [[ $main_class == "tedd" && $mode == "nlp_verb_only_baseline" ]]; then
        sed -i "" "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_verb_only_string" ]]; then
        sed -i "" "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=false%g" $properties_file
        sed -i "" "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_dobj_baseline" ]]; then
        sed -i "" "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=true%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_dobj_string" ]]; then
        sed -i "" "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=true%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=false%g" $properties_file
        sed -i "" "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_noun_matching_baseline" ]]; then
        sed -i "" "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=true%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_noun_matching_string" ]]; then
        sed -i "" "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "" "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "" "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "" "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "" "s%noun_matching=.*$%noun_matching=true%g" $properties_file
        sed -i "" "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    else
        echo Unknown combination of main_class=$main_class and mode=$mode
        exit 1
    fi
else
    sed -i "s%dependency_graph_path=.*$%dependency_graph_path=$HOME/workspace/FSE19-submission-material/tedd/src/main/resources%g" \
        $properties_file
    sed -i "s%test_suite_path=.*$%test_suite_path=$HOME/workspace/FSE19-submission-material/testsuite-$application_name/src/main/java/main/TestSuite.java%g" \
        $properties_file
    sed -i "s%tests_path=.*$%tests_path=$HOME/workspace/FSE19-submission-material/testsuite-$application_name/src/main/java/tests%g" \
        $properties_file
    sed -i "s%only_compute_graph_build_time=.*$%only_compute_graph_build_time=$only_compute_graph_build_time%g" \
        $properties_file
    sed -i "s%recover_missed_dependencies=.*$%recover_missed_dependencies=true%g" $properties_file
    if [[ $main_class == "baseline_complete" && $mode == "baseline_complete" ]]; then
        sed -i "s%recover_missed_dependencies=.*$%recover_missed_dependencies=false%g" $properties_file
        sed -i "s%baseline=.*$%baseline=true%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "string_analysis" ]]; then
        sed -i "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=common_values%g" $properties_file
        sed -i "s%values_to_filter=.*$%values_to_filter=%g" $properties_file
        if [[ $application_name != "claroline" ]]; then
            sed -i "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=true%g" $properties_file
        else
            sed -i "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
        fi
        if [[ $application_name == "collabtive" || $application_name == "claroline" ]]; then
            sed -i "s%values_to_filter=.*$%values_to_filter=admin:admin%g" $properties_file
        elif [[ $application_name == "mantisbt" ]]; then
            sed -i "s%values_to_filter=.*$%values_to_filter=administrator:root%g" $properties_file
        fi
    elif [[ $main_class == "tedd" && $mode == "nlp_verb_only_baseline" ]]; then
        sed -i "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_verb_only_string" ]]; then
        sed -i "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=false%g" $properties_file
        sed -i "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_dobj_baseline" ]]; then
        sed -i "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=true%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_dobj_string" ]]; then
        sed -i "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=true%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=false%g" $properties_file
        sed -i "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_noun_matching_baseline" ]]; then
        sed -i "s%baseline=.*$%baseline=true%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=true%g" $properties_file
    elif [[ $main_class == "tedd" && $mode == "nlp_noun_matching_string" ]]; then
        sed -i "s%baseline=.*$%baseline=false%g" $properties_file
        sed -i "s%filter_dependencies=.*$%filter_dependencies=true%g" $properties_file
        sed -i "s%filter_type=.*$%filter_type=nlp%g" $properties_file
        sed -i "s%verb_object_detection=.*$%verb_object_detection=false%g" $properties_file
        sed -i "s%noun_matching=.*$%noun_matching=true%g" $properties_file
        sed -i "s%edit_distance_string_analysis=.*$%edit_distance_string_analysis=false%g" $properties_file
    else
        echo Unknown combination of main_class=$main_class and mode=$mode
        exit 1
    fi
fi

if [[ $collect_stats == "true" ]]; then
    logs_name=$HOME/Desktop/logs"_"$main_class"_"$application_name.txt
    errors_logs_name=$HOME/Desktop/errors"_"$main_class"_"$application_name.txt
fi

runExp $application_name $main_class $start_container $collect_stats

if [[ $collect_stats == "false" ]]; then
    rm ./src/main/resources/dependency-graph-*$application_name.txt
fi