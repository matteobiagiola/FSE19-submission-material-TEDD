package org.mb.tedd.algorithm.filter.common_values;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.mb.tedd.algorithm.filter.FilterStrategy;
import org.mb.tedd.algorithm.extraction.string.StringValue;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.ListsUtils;
import org.mb.tedd.utils.Properties;
import org.mb.tedd.utils.SetsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommonValuesStrategy<T> implements FilterStrategy<T> {

    private final static Logger logger = Logger.getLogger(CommonValuesStrategy.class.getName());

    @Override
    public Graph<GraphNode<T>, GraphEdge> filterDependencies(Graph<GraphNode<T>, GraphEdge> dependencyGraph) {
        Set<GraphEdge> edges = dependencyGraph.edgeSet();
        Set<Set<StringValue>> setsOfDependentValues = new LinkedHashSet<>();
        Multiset<StringValue> constantValuesRanking = HashMultiset.create();
        for (GraphEdge edge : edges) {
            setsOfDependentValues.add((Set<StringValue>) edge.getDependentValues());
            constantValuesRanking.addAll(edge.getDependentValues());
        }
        Set<StringValue> commonValues = setsOfDependentValues.iterator().next();
        for (Set<StringValue> dependentValues : setsOfDependentValues) {
            commonValues = SetsUtils.intersection(commonValues, dependentValues);
        }
        if(!commonValues.isEmpty()){
            logger.info("Filtering dependencies with common values: " + commonValues);
            DependencyGraphManager<T> dependencyGraphManager = new DependencyGraphManager<>(dependencyGraph);
            Graph<GraphNode<T>, GraphEdge> dependencyGraphClone = dependencyGraphManager.duplicate();
            ConnectivityInspector<GraphNode<T>, GraphEdge> connectivityInspector = new ConnectivityInspector<>(dependencyGraphClone);
            if(connectivityInspector.isConnected()){
                logger.info("Dependency graph BEFORE filtering IS weakly connected");
            } else {
                logger.info("Dependency graph BEFORE filtering IS NOT weakly connected");
            }
            for (GraphEdge graphEdge : dependencyGraphManager.getDependencyGraph().edgeSet()) {
                Set<StringValue> dependentValues = (Set<StringValue>) graphEdge.getDependentValues();
                Set<StringValue> difference = SetsUtils.setsDifference(dependentValues, commonValues);
                if(difference.isEmpty()){
                    logger.info("Removing dependency " + graphEdge);
                    dependencyGraphClone.removeEdge(graphEdge);
                }
            }
            connectivityInspector = new ConnectivityInspector<>(dependencyGraphClone);
            if(connectivityInspector.isConnected()){
                logger.info("Dependency graph AFTER filtering IS weakly connected");
            } else {
                logger.info("Dependency graph AFTER filtering IS NOT weakly connected");
            }
            return dependencyGraphClone;
        } else if(Arrays.asList(Properties.VALUES_TO_FILTER).size() > 0){
            return this.filterInputValues(dependencyGraph);
        }
        else {
            logger.info("No common values to filter.");
            List<Multiset.Entry<StringValue>> constantValuesSorted
                    = new ArrayList<>(constantValuesRanking.entrySet());
            constantValuesSorted.sort((entry1, entry2) -> entry2.getCount() - entry1.getCount());
            String toPrint = constantValuesSorted
                    .stream()
                    .map(entry -> entry.getElement() + " " + entry.getCount())
                    .collect(Collectors.joining("\n"));
            logger.info("Values ranking: \n" + toPrint);
            if(constantValuesSorted.size() > 1){
                logger.info("Consider removing most recurrent values by filling the values_to_filter property."
                        + " First two most recurrent values: \n"
                        + constantValuesSorted.get(0).getElement() + ": " + constantValuesSorted.get(0).getCount() + "\n"
                        + constantValuesSorted.get(1).getElement() + ": " + constantValuesSorted.get(1).getCount());
            }
            return dependencyGraph;
        }
    }

    private Graph<GraphNode<T>, GraphEdge> filterInputValues(Graph<GraphNode<T>, GraphEdge> dependencyGraph){
        List<String> valuesToFilter = Arrays.asList(Properties.VALUES_TO_FILTER);
        logger.info("Filtering dependencies with input values: " + valuesToFilter);
        DependencyGraphManager<T> dependencyGraphManager = new DependencyGraphManager<>(dependencyGraph);
        Graph<GraphNode<T>, GraphEdge> dependencyGraphClone = dependencyGraphManager.duplicate();
        for (GraphEdge graphEdge : dependencyGraphManager.getDependencyGraph().edgeSet()) {
            Set<StringValue> dependentValues = (Set<StringValue>) graphEdge.getDependentValues();
            List<String> dependentValuesString = new ArrayList<>();
            dependentValues.forEach(stringValue -> dependentValuesString.addAll(stringValue.getValues().stream()
                    .map(value -> value.replaceAll("\"","")).collect(Collectors.toSet())));
            List<String> difference = ListsUtils.listsDifference(dependentValuesString, valuesToFilter);
            if(difference.isEmpty()){
                logger.info("Values of dependency " + graphEdge + ": " + dependentValuesString);
                logger.info("Removing dependency " + graphEdge);
                dependencyGraphClone.removeEdge(graphEdge);
            }
        }
        if(dependencyGraph.edgeSet().size() == dependencyGraphClone.edgeSet().size()){
            logger.info("Any dependency has been filtered");
        } else {
            logger.info((dependencyGraph.edgeSet().size() - dependencyGraphClone.edgeSet().size()) + " dependencies with values "
                    + valuesToFilter + " have been filtered");
        }
        return dependencyGraphClone;
    }
}
