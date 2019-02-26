package org.mb.tedd.algorithm.filter.nlp;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.POS;
import org.apache.log4j.Logger;
import org.mb.tedd.utils.Properties;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Wordnet {

    private IDictionary dict;
    private final static Logger logger = Logger.getLogger(Wordnet.class.getName());
    private Map<String, String> verbTag = new LinkedHashMap<>();
    private Set<String> verbs = new LinkedHashSet<>();
    private static final String NOVERB = "NOVERB";
    private static final String VERB = "VERB";


    private static Wordnet ourInstance = new Wordnet();

    public static Wordnet getInstance() {
        return ourInstance;
    }

    private Wordnet() {
        try {
            URL url = new URL("file", null, Properties.WORDNET_DICT_PATH);
            this.dict = new Dictionary(url);
            this.dict.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getVerbTag(String word){
        String classifiedWord = this.verbTag.get(word);
        if(classifiedWord != null){
            return classifiedWord.equals(VERB);
        }
        IIndexWord indexWord = dict.getIndexWord(word, POS.VERB);
        if(indexWord != null){
            this.verbTag.put(word, VERB);
            return true;
        }
        this.verbTag.put(word, NOVERB);
        return false;
    }

    public boolean isNoun(String word){
        return dict.getIndexWord(word, POS.NOUN) != null;
    }

    public boolean isVerb(String word){
        return dict.getIndexWord(word, POS.VERB) != null;
    }




}
