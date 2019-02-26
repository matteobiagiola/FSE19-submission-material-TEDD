#!/bin/bash

function checkFolderExistence(){
    local folder=$1
    if [[ -e $folder ]]; then
        if [[ -d $folder ]]; then
            echo $folder exists
        else
            echo $folder is not a directory
            exit 1
        fi
    else
        echo $folder folder does not exist
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

function checkMode(){
	local mode=$1
	if [[ $mode != "parallelization" ]]; then
		echo Unknown mode: $mode
		exit 1
	fi
}

logs_name=
errors_logs_name=

function runExp(){
    local application_name=$1
    local main_class=$2
    local start_container=$3
    local collect_stats=$4
    ./run-java.sh $main_class $application_name $start_container $collect_stats > $logs_name 2> $errors_logs_name
}

# -----------------------------------------------------------------------------------------------------------------

if test $# -lt 3 ; then echo 'ARGS: results_folder application_name mode' ; exit 1 ; fi

results_folder=$1
application_name=$2
mode=$3

os=$(uname)

properties_file=$PWD/src/main/resources/app.properties

checkFolderExistence $results_folder
checkApplicationName $application_name
checkMode $mode

final_graph_prefix=dependency-graph-final-
final_graph_recover_missed_suffix=-recover-missed-

folder="$results_folder/$application_name"

for exp in $(ls $folder) ; do
    echo "Checking $exp for application $application_name in mode $mode"
    echo
    final_graph_exp=$(find $folder/$exp -name "$final_graph_prefix*.txt")
    list_length=$(wc -w <<< "$final_graph_exp")
    final_graph_chosen=
    if [[ $list_length -eq 1 ]] ; then
        # it is baseline graph
        final_graph_chosen=$final_graph_exp
    elif [[ $list_length -eq 2 ]] ; then
        # it is any other method that filters graph
        final_graph_recover_missed=none
        for final_graph in $final_graph_exp ; do
            if [[ $final_graph == *$final_graph_recover_missed_suffix* ]]; then
                final_graph_recover_missed=$final_graph
            fi
        done
        if [[ $final_graph_recover_missed == none ]]; then
            echo Final graph recover missed with this suffix $final_graph_recover_missed_suffix not found \
                in $exp folder. List of the graphs found: $final_graph_exp
            exit 1
        fi
        echo Recover missed final graph: $final_graph_recover_missed
        final_graph_chosen=$final_graph_recover_missed

    else
        echo Folder $exp contains more than two final graphs with this prefix $final_graph_prefix
        exit 1
    fi

    start_container=
    main_class=
    collect_stats=false

    if [[ $mode == "parallelization" ]]; then

        if [[ $os == "Darwin" ]]; then
            sed -i "" "s%final_graph_path=.*$%final_graph_path=$final_graph_chosen%g" $properties_file
            sed -i "" "s%check_final_graph=.*$%check_final_graph=true%g" $properties_file
            sed -i "" "s%execute_whole_test_suite=.*$%execute_whole_test_suite=true%g" $properties_file
        else
            sed -i "s%final_graph_path=.*$%final_graph_path=$final_graph_chosen%g" $properties_file
            sed -i "s%check_final_graph=.*$%check_final_graph=true%g" $properties_file
            sed -i "s%execute_whole_test_suite=.*$%execute_whole_test_suite=true%g" $properties_file
        fi

        start_container=true
        main_class=check_final_graph_correctness
        collect_stats=true
    else
        echo Unknown mode: $mode
        exit 1
    fi

    logs_name=$HOME/Desktop/logs"_"$exp"_"$main_class"_"$application_name.txt
    errors_logs_name=$HOME/Desktop/errors"_"$exp"_"$main_class"_"$application_name.txt
    runExp $application_name $main_class $start_container $collect_stats

    if [[ $collect_stats == "false" ]]; then
        index_of_last_slash=$(echo $final_graph_chosen | awk -F"/" '{print length($0)-length($NF)}')

        if [[ $index_of_last_slash < 0 ]] ; then
            echo Cannot find slashes in $final_graph_chosen path
            exit 1
        fi

        # Take all the characters from 1 to $index with that cut syntax
        target_folder_for_logs=$(echo $final_graph_chosen | cut -c1-$index_of_last_slash)

        checkFolderExistence $target_folder_for_logs

        echo Moving $logs_name in $target_folder_for_logs
        mv $logs_name $target_folder_for_logs
        echo Moving $errors_logs_name in $target_folder_for_logs
        mv $errors_logs_name $target_folder_for_logs
        echo
    fi

done


