# FSE19 replication package

## Manual Setup

### TEDD and the test suite subjects have the following dependencies:

1. Java JDK 1.8 (https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
2. Maven 3.5.4 (https://maven.apache.org/download.cgi)
3. Chrome browser (https://www.google.com/intl/it_ALL/chrome/)
4. Firefox browser (https://www.mozilla.org/it/firefox/new/)
5. Docker CE (https://docs.docker.com/install/)

TEDD has been tested in MacOS Mojave 10.14.3 and Ubuntu 18.04 LTS.

### Download docker images
Before running the experiments: 
- clone the repository (`git clone https://github.com/anonymous-fse19-submitter/FSE19-submission-material.git`)
- `cd FSE19-submission-material/tedd && mvn clean compile`
- `cd FSE19-submission-material/testsuite-<application_name> && mvn clean compile` where `<application_name>` is `claroline|addressbook|ppma|collabtive|mrbs|mantisbt`
- download docker web application images. The instructions to run each web application are in the relative folders (`FSE19-submission-material/testsuite-<application_name>`):
  - `docker pull dockercontainervm/claroline:1.11.10`,
  - `docker pull dockercontainervm/addressbook:8.0.0.0`,
  - `docker pull dockercontainervm/ppma:0.6.0`,
  - `docker pull dockercontainervm/collabtive:3.1`,
  - `docker pull dockercontainervm/mrbs:1.4.9`,
  - `docker pull dockercontainervm/mantisbt:1.2.0`
  
## Automatic Setup

Coming soon...

## Run the experiments (validation - after the setup)

The main script to run experiments is `FSE19-submission-material/tedd/run-experiment.sh`. The first argument is the `application_name`; the available values are listed above. The second argument is the `main_class` that is going to be executed and the available values  are `baseline_complete|tedd`. The third argument is the `mode` or the desired configuration of the tool. The available values are `baseline_complete|string_analysis|nlp_verb_only_baseline|nlp_verb_only_string|nlp_dobj_baseline|nlp_dobj_string|nlp_noun_matching_baseline|nlp_noun_matching_string`.

The possible combinations of those arguments are listed below (leaving out the `application_name`):
1. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> baseline_complete baseline_complete` to run the baseline (original order graph extraction)
2. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd string_analysis`
3. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_verb_only_baseline`
4. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_verb_only_string`
5. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_dobj_baseline`
6. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_dobj_string`
7. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_noun_matching_baseline`
8. `cd FSE19-submission-material/tedd ./run-experiment.sh <application_name> tedd nlp_noun_matching_string`

Alternatively, the `FSE19-submission-material/tedd/run-experiments.sh` runs all the available configurations. The only required parameter is the application name. To run it type `cd FSE19-submission-material/tedd && ./run-experiments <application_name>`.

The `FSE19-submission-material/tedd/run-experiment.sh` starts the docker container for the given application and stops it when the validation ends. While the script is running the logs are saved in the `Desktop` as `logs_main_class_application_name.txt`. It is possible to stop the script by typing `^C` on the terminal. The script saves a directory on the `Desktop` folder with the application name containing the results of the validation. The folder containing the results is named as `current_date_main_class`. It contains the logs, the final dependency graph and the intermediate graphs obtained during the validation.

## Run the experiments (parallelization - after the setup)

The repository has a directory called `FSE19-submission-material/ready-to-run-parallelization` that contains the final dependencies graphs for all the possible configurations of TEDD and for the baseline approach.

The script `FSE19-submission-material/tedd/run-parallelization` extracts and execute all the possible test suites from every dependency graph in the `FSE19-submission-material/ready-to-run-parallelization` for each application.

To run it type `cd FSE19-submission-material/tedd && ./run-parallelization.sh <application_name>`. The script starts the docker container for the given application and stops it when the computation finishes. The unique test suites for each dependency graph are executed sequentially and the speed-up factors are computed (worst case and average case) by executing the test suite in its original order after the sequential executions.

As in the previous script the results are saved in the `Desktop` folder. The script creates a folder with the application name that contains the results directory with the logs. In the logs the metrics can be found.
