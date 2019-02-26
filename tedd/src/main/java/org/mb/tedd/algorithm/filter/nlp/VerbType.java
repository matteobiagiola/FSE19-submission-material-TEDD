package org.mb.tedd.algorithm.filter.nlp;

public enum VerbType {

    READ ("read"),
    WRITE ("write"),
    IGNORE ("ignore");

    private String verbType;

    VerbType(String verbType){
        this.verbType = verbType;
    }

    public String getVerbType(){
        return this.verbType;
    }
}
