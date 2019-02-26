package org.mb.tedd.algorithm.filter.nlp;

public class VerbObjectType {

    private String obj;
    private String verb;

    public VerbObjectType(String obj, String verb){
        this.obj = obj;
        this.verb = verb;
    }

    public VerbObjectType(String verb){
        this.obj = AbstractVerbObjectDetector.NO_OBJ;
        this.verb = verb;
    }

    public VerbObjectType(){
        this.obj = AbstractVerbObjectDetector.NO_OBJ;
        this.verb = AbstractVerbObjectDetector.NO_VERB;
    }

    public String getObj() {
        return obj;
    }

    public String getVerb() {
        return verb;
    }

    public boolean equalsObject(VerbObjectType otherVerbObjectType){
        return Lemmatizer.lemmatize(this.obj).equals(Lemmatizer.lemmatize(otherVerbObjectType.obj));
    }

    @Override
    public String toString(){
        return "verb: " + this.verb + ", obj: " + this.obj;
    }
}
