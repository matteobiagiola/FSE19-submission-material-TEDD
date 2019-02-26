#!/bin/bash

application_name=$1
main_class=$2

mkdir -p ~/Desktop/$application_name/results

current_date=$(date '+%d-%m-%Y_%H-%M-%S')
mkdir -p ~/Desktop/$application_name/results/$current_date-$main_class

mkdir -p ~/Desktop/$application_name/results/$current_date-$main_class/graphs
mv ./src/main/resources/dependency-graph-*$application_name.txt ~/Desktop/$application_name/results/$current_date-$main_class/graphs

mv ~/Desktop/$application_name/results/$current_date-$main_class/graphs/dependency-graph-initial-*$application_name.txt \
    ~/Desktop/$application_name/results/$current_date-$main_class
mv ~/Desktop/$application_name/results/$current_date-$main_class/graphs/dependency-graph-final-*$application_name.txt \
    ~/Desktop/$application_name/results/$current_date-$main_class

mv ~/Desktop/*$main_class_$application_name.txt ~/Desktop/$application_name/results/$current_date-$main_class
