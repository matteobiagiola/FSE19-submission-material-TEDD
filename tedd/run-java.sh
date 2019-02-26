#!/bin/bash

function checkMainClassName(){
	local main_class_name=$1
	if [[ $main_class_name != "baseline_complete" \
	    && $main_class_name != "tedd" \
	    && $main_class_name != "check_final_graph_correctness" ]]; then
		echo Unknown main class name: $main_class_name
		exit 1
	fi
}

function checkProjectName(){
	local application_name_local=$1
	if [[ $application_name_local != "claroline" && $application_name_local != "addressbook" && $application_name_local != "ppma" \
		&& $application_name_local != "collabtive" && $application_name_local != "mrbs" && $application_name_local != "mantisbt" ]]; then
		echo Unknown application name: $application_name_local
		exit 1
	fi
}

function runContainer(){
    local application_name_local=$1
    local current_date=$2
    local testsuite_path=~/workspace/testsuite-$application_name_local
    if [[ -e $testsuite_path ]]; then
        if [[ -d $testsuite_path ]]; then
            if [[ -f $testsuite_path/run-docker.sh ]]; then
                local pwd=$(pwd)
                cd $testsuite_path
                ./run-docker.sh -p yes -n $application_name_local-$current_date
                sleep 15 # wait for the application to start
                cd $pwd
            else
                echo $testsuite_path/run-docker.sh does not exist
                exit 1
            fi
        else
            echo $testsuite_path is not a directory
            exit 1
        fi
    else
        echo $testsuite_path path does not exists
        exit 1
    fi
}

function stopContainer(){
    local application_name_local=$1
    local current_date=$2
    docker stop $application_name_local-$current_date
    docker rm $application_name_local-$current_date
}

function checkBoolean(){
    local start_container=$1
	if [[ $start_container != "true" && $start_container != "false" ]]; then
		echo Unknown start container value: $start_container. It is either true or false.
		exit 1
	fi
}

#----------------------------------------------------------------------------------------------------------------------

classpath=$HOME/.m2/repository/fr/inria/gforge/spoon/spoon-core/6.0.0/spoon-core-6.0.0.jar:$HOME/.m2/repository/org/eclipse/tycho/org.eclipse.jdt.core/3.13.50.v20171007-0855/org.eclipse.jdt.core-3.13.50.v20171007-0855.jar:$HOME/.m2/repository/com/martiansoftware/jsap/2.1/jsap-2.1.jar:$HOME/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:$HOME/.m2/repository/commons-io/commons-io/2.5/commons-io-2.5.jar:$HOME/.m2/repository/org/apache/maven/maven-model/3.5.0/maven-model-3.5.0.jar:$HOME/.m2/repository/org/codehaus/plexus/plexus-utils/3.0.24/plexus-utils-3.0.24.jar:$HOME/.m2/repository/org/jgrapht/jgrapht-core/1.2.0/jgrapht-core-1.2.0.jar:$HOME/.m2/repository/com/google/guava/guava/19.0/guava-19.0.jar:$HOME/.m2/repository/org/jgrapht/jgrapht-ext/1.1.0/jgrapht-ext-1.1.0.jar:$HOME/.m2/repository/org/jgrapht/jgrapht-io/1.1.0/jgrapht-io-1.1.0.jar:$HOME/.m2/repository/org/antlr/antlr4-runtime/4.6/antlr4-runtime-4.6.jar:$HOME/.m2/repository/org/tinyjee/jgraphx/jgraphx/2.0.0.1/jgraphx-2.0.0.1.jar:$HOME/.m2/repository/jgraph/jgraph/5.13.0.0/jgraph-5.13.0.0.jar:$HOME/.m2/repository/junit/junit/4.12/junit-4.12.jar:$HOME/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:$HOME/.m2/repository/com/lexicalscope/jewelcli/jewelcli/0.8.9/jewelcli-0.8.9.jar:$HOME/.m2/repository/org/apache/commons/commons-lang3/3.8.1/commons-lang3-3.8.1.jar:$HOME/.m2/repository/org/apache/logging/log4j/log4j-core/2.11.1/log4j-core-2.11.1.jar:$HOME/.m2/repository/org/apache/logging/log4j/log4j-api/2.11.1/log4j-api-2.11.1.jar:$HOME/.m2/repository/org/slf4j/slf4j-nop/1.7.25/slf4j-nop-1.7.25.jar:$HOME/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:$HOME/.m2/repository/edu/stanford/nlp/stanford-corenlp/3.9.2/stanford-corenlp-3.9.2.jar:$HOME/.m2/repository/com/apple/AppleJavaExtensions/1.4/AppleJavaExtensions-1.4.jar:$HOME/.m2/repository/de/jollyday/jollyday/0.4.9/jollyday-0.4.9.jar:$HOME/.m2/repository/org/apache/lucene/lucene-queryparser/4.10.3/lucene-queryparser-4.10.3.jar:$HOME/.m2/repository/org/apache/lucene/lucene-sandbox/4.10.3/lucene-sandbox-4.10.3.jar:$HOME/.m2/repository/org/apache/lucene/lucene-analyzers-common/4.10.3/lucene-analyzers-common-4.10.3.jar:$HOME/.m2/repository/org/apache/lucene/lucene-queries/4.10.3/lucene-queries-4.10.3.jar:$HOME/.m2/repository/org/apache/lucene/lucene-core/4.10.3/lucene-core-4.10.3.jar:$HOME/.m2/repository/javax/servlet/javax.servlet-api/3.0.1/javax.servlet-api-3.0.1.jar:$HOME/.m2/repository/com/io7m/xom/xom/1.2.10/xom-1.2.10.jar:$HOME/.m2/repository/xml-apis/xml-apis/1.3.03/xml-apis-1.3.03.jar:$HOME/.m2/repository/xerces/xercesImpl/2.8.0/xercesImpl-2.8.0.jar:$HOME/.m2/repository/xalan/xalan/2.7.0/xalan-2.7.0.jar:$HOME/.m2/repository/joda-time/joda-time/2.9.4/joda-time-2.9.4.jar:$HOME/.m2/repository/com/googlecode/efficient-java-matrix-library/ejml/0.23/ejml-0.23.jar:$HOME/.m2/repository/org/glassfish/javax.json/1.0.4/javax.json-1.0.4.jar:$HOME/.m2/repository/com/google/protobuf/protobuf-java/3.2.0/protobuf-java-3.2.0.jar:$HOME/.m2/repository/javax/activation/javax.activation-api/1.2.0/javax.activation-api-1.2.0.jar:$HOME/.m2/repository/javax/xml/bind/jaxb-api/2.4.0-b180830.0359/jaxb-api-2.4.0-b180830.0359.jar:$HOME/.m2/repository/com/sun/xml/bind/jaxb-core/2.3.0.1/jaxb-core-2.3.0.1.jar:$HOME/.m2/repository/com/sun/xml/bind/jaxb-impl/2.4.0-b180830.0438/jaxb-impl-2.4.0-b180830.0438.jar:$HOME/.m2/repository/edu/stanford/nlp/stanford-corenlp/3.9.2/stanford-corenlp-3.9.2-models-english.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-pos/4.0.13/illinois-pos-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-core-utilities/3.1.36/illinois-core-utilities-3.1.36.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/jwnl-prime/1.0.4/jwnl-prime-1.0.4.jar:$HOME/.m2/repository/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/wordnet/1.0-binary/wordnet-1.0-binary.jar:$HOME/.m2/repository/net/sf/trove4j/trove4j/3.0.3/trove4j-3.0.3.jar:$HOME/.m2/repository/commons-codec/commons-codec/1.8/commons-codec-1.8.jar:$HOME/.m2/repository/commons-lang/commons-lang/2.3/commons-lang-2.3.jar:$HOME/.m2/repository/net/sf/ehcache/ehcache/2.8.3/ehcache-2.8.3.jar:$HOME/.m2/repository/com/googlecode/json-simple/json-simple/1.1/json-simple-1.1.jar:$HOME/.m2/repository/com/google/code/gson/gson/2.3.1/gson-2.3.1.jar:$HOME/.m2/repository/edu/princeton/wordnet-dict/3.1/wordnet-dict-3.1.jar:$HOME/.m2/repository/com/h2database/h2/1.4.191/h2-1.4.191.jar:$HOME/.m2/repository/org/mapdb/mapdb/3.0.4/mapdb-3.0.4.jar:$HOME/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/1.0.7/kotlin-stdlib-1.0.7.jar:$HOME/.m2/repository/org/jetbrains/kotlin/kotlin-runtime/1.0.7/kotlin-runtime-1.0.7.jar:$HOME/.m2/repository/org/eclipse/collections/eclipse-collections-api/7.1.2/eclipse-collections-api-7.1.2.jar:$HOME/.m2/repository/net/jcip/jcip-annotations/1.0/jcip-annotations-1.0.jar:$HOME/.m2/repository/org/eclipse/collections/eclipse-collections/7.1.2/eclipse-collections-7.1.2.jar:$HOME/.m2/repository/org/eclipse/collections/eclipse-collections-forkjoin/7.1.2/eclipse-collections-forkjoin-7.1.2.jar:$HOME/.m2/repository/net/jpountz/lz4/lz4/1.3.0/lz4-1.3.0.jar:$HOME/.m2/repository/org/mapdb/elsa/3.0.0-M5/elsa-3.0.0-M5.jar:$HOME/.m2/repository/com/google/protobuf/protobuf-java-util/3.2.0/protobuf-java-util-3.2.0.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/LBJava/1.3.0/LBJava-1.3.0.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-inference/0.6.0/illinois-inference-0.6.0.jar:$HOME/.m2/repository/org/ojalgo/ojalgo/37.1.1/ojalgo-37.1.1.jar:$HOME/.m2/repository/net/sf/jgrapht/jgrapht/0.8.3/jgrapht-0.8.3.jar:$HOME/.m2/repository/nz/ac/waikato/cms/weka/weka-stable/3.6.10/weka-stable-3.6.10.jar:$HOME/.m2/repository/net/sf/squirrel-sql/thirdparty-non-maven/java-cup/0.11a/java-cup-0.11a.jar:$HOME/.m2/repository/de/bwaldvogel/liblinear/1.94/liblinear-1.94.jar:$HOME/.m2/repository/org/apache/commons/commons-math3/3.6/commons-math3-3.6.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/LBJava-NLP-tools/3.1.36/LBJava-NLP-tools-3.1.36.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-pos-models/3.0.13/illinois-pos-models-3.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-nlp-pipeline/4.0.13/illinois-nlp-pipeline-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-tokenizer/4.0.13/illinois-tokenizer-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-chunker/4.0.13/illinois-chunker-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-chunker-model/3.0.77/illinois-chunker-model-3.0.77.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-curator/3.1.36/illinois-curator-3.1.36.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/curator-interfaces/0.7/curator-interfaces-0.7.jar:$HOME/.m2/repository/org/apache/thrift/libthrift/0.8.0/libthrift-0.8.0.jar:$HOME/.m2/repository/org/apache/httpcomponents/httpclient/4.1.2/httpclient-4.1.2.jar:$HOME/.m2/repository/org/apache/httpcomponents/httpcore/4.1.3/httpcore-4.1.3.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-lemmatizer/4.0.13/illinois-lemmatizer-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-edison/4.0.13/illinois-edison-4.0.13.jar:$HOME/.m2/repository/edu/mit/jwi/2.2.3/jwi-2.2.3.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-corpusreaders/4.0.13/illinois-corpusreaders-4.0.13.jar:$HOME/.m2/repository/mysql/mysql-connector-java/5.1.34/mysql-connector-java-5.1.34.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-quantifier/4.0.13/illinois-quantifier-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-quantifier-models/2.0.5/illinois-quantifier-models-2.0.5.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-prep-srl/4.0.13/illinois-prep-srl-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-prepsrl-model/3.1/illinois-prepsrl-model-3.1.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-comma/4.0.13/illinois-comma-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-sl/1.3.1/illinois-sl-1.3.1.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-verbsense/4.0.13/illinois-verbsense-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-common-resources/1.4/illinois-common-resources-1.4-illinoisSRL.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/IllinoisSL-core/0.1-withJLIS/IllinoisSL-core-0.1-withJLIS.jar:$HOME/.m2/repository/colt/colt/1.2.0/colt-1.2.0.jar:$HOME/.m2/repository/concurrent/concurrent/1.3.4/concurrent-1.3.4.jar:$HOME/.m2/repository/commons-configuration/commons-configuration/1.6/commons-configuration-1.6.jar:$HOME/.m2/repository/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-question-typer/4.0.13/illinois-question-typer-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-pipeline-client/4.0.13/illinois-pipeline-client-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/stanford_3.3.1/4.0.13/stanford_3.3.1-4.0.13.jar:$HOME/.m2/repository/edu/stanford/nlp/stanford-corenlp/3.3.1/stanford-corenlp-3.3.1-models.jar:$HOME/.m2/repository/org/cogcomp/cogcomp-datastore/1.9.12/cogcomp-datastore-1.9.12.jar:$HOME/.m2/repository/io/minio/minio/3.0.3/minio-3.0.3.jar:$HOME/.m2/repository/com/google/http-client/google-http-client-xml/1.20.0/google-http-client-xml-1.20.0.jar:$HOME/.m2/repository/com/google/http-client/google-http-client/1.20.0/google-http-client-1.20.0.jar:$HOME/.m2/repository/com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar:$HOME/.m2/repository/xpp3/xpp3/1.1.4c/xpp3-1.1.4c.jar:$HOME/.m2/repository/com/squareup/okhttp/okhttp/2.7.2/okhttp-2.7.2.jar:$HOME/.m2/repository/com/squareup/okio/okio/1.6.0/okio-1.6.0.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.8.4/jackson-annotations-2.8.4.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.8.4/jackson-core-2.8.4.jar:$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.8.4/jackson-databind-2.8.4.jar:$HOME/.m2/repository/me/tongfei/progressbar/0.5.3/progressbar-0.5.3.jar:$HOME/.m2/repository/jline/jline/2.12/jline-2.12.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-ner/4.0.13/illinois-ner-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-md/4.0.13/illinois-md-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-relation-extraction/4.0.13/illinois-relation-extraction-4.0.13.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-datalessclassification/4.0.13/illinois-datalessclassification-4.0.13.jar:$HOME/.m2/repository/net/sf/jung/jung-api/2.0.1/jung-api-2.0.1.jar:$HOME/.m2/repository/net/sourceforge/collections/collections-generic/4.01/collections-generic-4.01.jar:$HOME/.m2/repository/net/sf/jung/jung-graph-impl/2.0.1/jung-graph-impl-2.0.1.jar:$HOME/.m2/repository/commons-cli/commons-cli/1.4/commons-cli-1.4.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-srl/5.1.15/illinois-srl-5.1.15.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-srl-models/5.1/illinois-srl-models-5.1-verb-stanford.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-srl-models/5.1/illinois-srl-models-5.1-nom-stanford.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-sl-core/1.0.3/illinois-sl-core-1.0.3.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/inference/0.5.0/inference-0.5.0.jar:$HOME/.m2/repository/org/tartarus/snowball/1.0/snowball-1.0.jar:$HOME/.m2/repository/net/sourceforge/argparse4j/argparse4j/0.7.0/argparse4j-0.7.0.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-depparse/4.0.13/illinois-depparse-4.0.13.jar:$HOME/.m2/repository/edu/cmu/cs/ark/ChuLiuEdmonds/1.0-SNAPSHOT/ChuLiuEdmonds-1.0-SNAPSHOT.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-depparse-model/3.0/illinois-depparse-model-3.0.jar:$HOME/.m2/repository/com/sparkjava/spark-core/2.5.5/spark-core-2.5.5.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-server/9.3.6.v20151106/jetty-server-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-http/9.3.6.v20151106/jetty-http-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-util/9.3.6.v20151106/jetty-util-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-io/9.3.6.v20151106/jetty-io-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-webapp/9.3.6.v20151106/jetty-webapp-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-xml/9.3.6.v20151106/jetty-xml-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-servlet/9.3.6.v20151106/jetty-servlet-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/jetty-security/9.3.6.v20151106/jetty-security-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/websocket/websocket-server/9.3.6.v20151106/websocket-server-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/websocket/websocket-common/9.3.6.v20151106/websocket-common-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/websocket/websocket-client/9.3.6.v20151106/websocket-client-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/websocket/websocket-servlet/9.3.6.v20151106/websocket-servlet-9.3.6.v20151106.jar:$HOME/.m2/repository/org/eclipse/jetty/websocket/websocket-api/9.3.6.v20151106/websocket-api-9.3.6.v20151106.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-time/4.0.13/illinois-time-4.0.13.jar:$HOME/.m2/repository/org/apache/uima/uimaj-core/2.8.1/uimaj-core-2.8.1.jar:$HOME/.m2/repository/com/github/heideltime/heideltime/2.1/heideltime-2.1.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-time-models/1.0/illinois-time-models-1.0.jar:$HOME/.m2/repository/edu/illinois/cs/cogcomp/illinois-transliteration/4.0.13/illinois-transliteration-4.0.13.jar:$HOME/.m2/repository/com/ibm/icu/icu4j/56.1/icu4j-56.1.jar:$HOME/.m2/repository/com/belerweb/pinyin4j/2.5.0/pinyin4j-2.5.0.jar:$HOME/workspace/FSE19-submission-material/tedd/src/main/resources/lib/edu.mit.jwi.jar:$HOME/.m2/repository/org/xerial/sqlite-jdbc/3.7.2/sqlite-jdbc-3.7.2.jar:$HOME/workspace/FSE19-submission-material/tedd/src/main/resources/lib/jawjaw.jar:$HOME/workspace/FSE19-submission-material/tedd/src/main/resources/lib/ws4j.jar:./target/classes

main_class_name=$1
application_name=$2
start_container=$3
collect_stats=$4

current_date=$(date '+%d-%m-%Y_%H-%M')

checkMainClassName $main_class_name
checkProjectName $application_name
checkBoolean $start_container
checkBoolean $collect_stats

./setup-application.sh $application_name

if [[ $start_container == "true" ]]; then
    runContainer $application_name $current_date
fi

if [[ $main_class_name == "baseline_complete" ]]; then
    java -Xms4096m -Xmx4096m -cp $classpath org.mb.tedd.main.BaselineCompleteGraph
elif [[ $main_class_name == "check_final_graph_correctness" ]]; then
    java -Xms4096m -Xmx4096m -cp $classpath org.mb.tedd.main.CheckFinalGraphCorrectness
elif [[ $main_class_name == "tedd" ]]; then
    java -Xms4096m -Xmx4096m -cp $classpath org.mb.tedd.main.Tedd
fi

if [[ $collect_stats == "true" ]]; then
    ./collect-stats.sh $application_name $main_class_name
fi

if [[ $start_container == "true" ]]; then
    stopContainer $application_name $current_date
fi