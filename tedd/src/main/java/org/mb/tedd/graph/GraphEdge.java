package org.mb.tedd.graph;

import org.apache.commons.lang3.ObjectUtils;
import org.jgraph.graph.DefaultEdge;
import org.mb.tedd.algorithm.extraction.string.StringValue;

import java.util.Collection;
import java.util.Objects;

public class GraphEdge extends DefaultEdge {

    private Collection<StringValue> dependentValues;

    private boolean manifest = false;
    private boolean introducesCycle = false;
    private boolean ignored = false;
    private boolean underTest = false;
    private boolean isInteresting = true;
    private GraphNode<?> graphSourceNode;
    private GraphNode<?> graphTargetNode;

    public GraphEdge(Collection<StringValue> dependentValues,
                     GraphNode<?> graphSourceNode,
                     GraphNode<?> graphTargetNode){
        super();
        this.dependentValues = dependentValues;
        this.graphSourceNode = graphSourceNode;
        this.graphTargetNode = graphTargetNode;
    }

    public Collection<StringValue> getDependentValues() {
        return dependentValues;
    }

    public boolean isManifest() {
        return manifest;
    }

    public void setManifest(boolean manifest) {
        this.manifest = manifest;
    }

    public boolean isIntroducesCycle() {
        return introducesCycle;
    }

    public void setIntroducesCycle(boolean introducesCycle) {
        this.introducesCycle = introducesCycle;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public boolean isUnderTest() {
        return underTest;
    }

    public void setUnderTest(boolean underTest) {
        this.underTest = underTest;
    }

    public boolean isInteresting() {
        return isInteresting;
    }

    public void setInteresting(boolean interesting) {
        isInteresting = interesting;
    }

    @Override
    public String toString(){
        return this.graphSourceNode + " -> " + this.graphTargetNode;
    }

    @Override
    public boolean equals(Object other){
        if(other == this)
            return true;

        if(other instanceof GraphEdge){
            GraphEdge otherGraphEdge = (GraphEdge) other;
            return Objects.equals(this.graphSourceNode, otherGraphEdge.graphSourceNode) &&
                    Objects.equals(this.graphTargetNode, otherGraphEdge.graphTargetNode);
        }

        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.graphSourceNode, this.graphTargetNode);
    }

    @Override
    public GraphEdge clone(){
        GraphEdge newGraphEdge = new GraphEdge(ObjectUtils.clone(this.dependentValues),
                graphSourceNode.clone(), graphSourceNode.clone());
        newGraphEdge.manifest = this.manifest;
        newGraphEdge.introducesCycle = this.introducesCycle;
        newGraphEdge.ignored = this.ignored;
        newGraphEdge.underTest = this.underTest;
        newGraphEdge.isInteresting = this.isInteresting;
        return newGraphEdge;
    }
}
