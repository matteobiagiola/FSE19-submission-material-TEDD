package org.mb.tedd.graph;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

public class GraphNode<T> {

    private T testCase;
    private int numOrder;

    public GraphNode(T testCase, int numOrder){
        this.testCase = testCase;
        this.numOrder = numOrder;
    }

    public T getTestCase() {
        return testCase;
    }

    public int getNumOrder() {
        return numOrder;
    }

    @Override
    public String toString(){
        return this.testCase.toString() + "_" + this.numOrder;
    }

    @Override
    public boolean equals(Object other){
        if(other == this)
            return true;

        if(other instanceof GraphNode){
            GraphNode<T> otherGraphNode = (GraphNode<T>) other;
            return Objects.equals(this.testCase, otherGraphNode.testCase) &&
                    Objects.equals(this.numOrder, otherGraphNode.numOrder);
        }

        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.testCase, this.numOrder);
    }

    @Override
    public GraphNode<T> clone(){
        return new GraphNode<>(ObjectUtils.clone(this.testCase), this.numOrder);
    }
}
