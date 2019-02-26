package org.mb.tedd.graph.dot.importgraph;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.EdgeProvider;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.graph.dot.EdgeLabel;
import org.mb.tedd.utils.Properties;

import java.util.Map;

public class EdgeLabelProvider<T> implements EdgeProvider<GraphNode<T>, GraphEdge> {

    @Override
    public GraphEdge buildEdge(GraphNode<T> graphSourceNode, GraphNode<T> graphTargetNode, String label, Map<String, Attribute> map) {
        GraphEdge graphEdge = new GraphEdge(EdgeLabel.getValues(label), graphSourceNode, graphTargetNode);
        if(map.get("color") != null){
            Attribute attribute = map.get("color");
            if(attribute.getValue().equals(EdgeLabel.manifest_dependency_color)){
                if(!Properties.GROUND_TRUTH_CHECK){
                    graphEdge.setManifest(true);
                }
            }
            else if(attribute.getValue().equals(EdgeLabel.uninteresting_dependency_color))
                graphEdge.setInteresting(false);
        }
        return graphEdge;
    }
}
