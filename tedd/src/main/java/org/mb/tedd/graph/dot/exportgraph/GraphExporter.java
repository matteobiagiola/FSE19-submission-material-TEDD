package org.mb.tedd.graph.dot.exportgraph;

import org.jgrapht.Graph;
import org.jgrapht.io.DOTExporter;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.Properties;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class GraphExporter<T> {

    private Graph<GraphNode<T>, GraphEdge> dependencyGraph;
    private EdgeLabelProvider edgeLabelProvider;
    private NodeNameProvider<T> nodeNameProvider;

    public GraphExporter(Graph<GraphNode<T>, GraphEdge> dependencyGraph){
        this.dependencyGraph = dependencyGraph;
        this.edgeLabelProvider = new EdgeLabelProvider();
        this.nodeNameProvider = new NodeNameProvider<>();
    }

    public void export(String exportedGraphName){
        String dependencyGraphPath = Properties.DEPENDENCY_GRAPH_PATH;
        DOTExporter<GraphNode<T>, GraphEdge> dotExporter = new DOTExporter<>(nodeNameProvider, null, edgeLabelProvider);
        try {
            Writer writer = new PrintWriter(dependencyGraphPath + "/" + exportedGraphName + "-"
                    + Properties.PROJECT_NAME + ".txt", "UTF-8");
            dotExporter.exportGraph(this.dependencyGraph, writer);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
