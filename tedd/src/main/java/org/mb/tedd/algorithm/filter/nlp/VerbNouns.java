package org.mb.tedd.algorithm.filter.nlp;

import java.util.List;
import java.util.stream.Collectors;

public class VerbNouns {

    private String verb;
    private List<String> nouns;

    public VerbNouns(List<String> nouns, String verb){
        this(nouns);
        this.verb = verb;
    }

    public VerbNouns(List<String> nouns){
        this.nouns = this.normalizeNouns(nouns);
        this.verb = AbstractVerbObjectDetector.NO_VERB;
    }

    private List<String> normalizeNouns(List<String> nouns){
        return nouns.stream()
                .map(Lemmatizer::lemmatize)
                .collect(Collectors.toList());
    }

    public List<String> getNouns() {
        return nouns;
    }

    public boolean contains(VerbNouns otherVerbNouns){
        List<String> otherNouns = otherVerbNouns.getNouns();
        for (String noun : this.nouns) {
            if(otherNouns.contains(noun)){
                return true;
            }
        }
        return false;
    }

    public String getVerb() {
        return verb;
    }

    @Override
    public String toString(){
        return "verb: " + this.verb + ", nouns: " + this.nouns;
    }
}
