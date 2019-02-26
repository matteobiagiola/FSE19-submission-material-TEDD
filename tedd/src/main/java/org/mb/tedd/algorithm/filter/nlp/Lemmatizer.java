package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.base.Preconditions;
import edu.stanford.nlp.simple.Sentence;

import java.util.List;

public class Lemmatizer {

    public static String lemmatize(String stringToLemmatize){
        List<String> lemmas = new Sentence(stringToLemmatize.toLowerCase()).lemmas();
        Preconditions.checkState(!lemmas.isEmpty() && lemmas.size() == 1,
                "Impossible to get lemma of " + stringToLemmatize);
        return lemmas.get(0);
    }
}
