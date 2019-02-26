package org.mb.tedd.algorithm.filter.nlp;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;
import org.apache.log4j.Logger;
import org.mb.tedd.utils.TestCaseUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CuratorTagger implements POSTaggerInterface {

    private final static Logger logger = Logger.getLogger(CuratorTagger.class.getName());
    private AnnotatorService pipeline;

    public CuratorTagger(){
        try {
            this.pipeline = PipelineFactory.buildPipeline(ViewNames.POS);
        } catch (IOException | AnnotatorException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<String> tagVerb(String testCaseName) {
        List<String> words = TestCaseUtils.splitTestCaseName(testCaseName, true);
        String docId = "APW-20140101.3018"; // arbitrary string identifier
        String textId = "body"; // arbitrary string identifier
        String sentence = String.join(" ", words); // contains plain text to be annotated
        try {
            TextAnnotation ta = this.pipeline.createAnnotatedTextAnnotation(docId, textId, sentence);
            if(this.containsVerb(ta.getView(ViewNames.POS))){
                return Optional.of(this.getVerb(ta.getView(ViewNames.POS)));
            } else {
                return this.canBeVerb(ta.getView(ViewNames.POS));
            }
        } catch (AnnotatorException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public String tagSentence(String testCaseName, String separatorTagWord, String wordDelimiter) {
        throw new UnsupportedOperationException("Tag sentence in CuratorTagger not supported yet!");
    }

    private boolean containsVerb(View view){
        List<Constituent> constituents = view.getConstituents();
        return constituents.stream().anyMatch(constituent -> {
            List<String> tags = view.getLabelsCovering(constituent);
            return tags.stream().anyMatch(tag -> tag.startsWith("V"));
        });
    }

    private String getVerb(View view){
        List<Constituent> constituents = view.getConstituents();
        Optional<Constituent> optionalVerb = constituents.stream()
                .filter(constituent -> {
                    List<String> tags = view.getLabelsCovering(constituent);
                    return tags.stream().anyMatch(tag -> tag.startsWith("V"));
                })
                .findFirst();
        if(optionalVerb.isPresent())
            return optionalVerb.get().toString();
        else
            throw new IllegalStateException("Error in retrieving verb from " + view.getConstituents());
    }

    /**
     * @return first word that can be a verb according to Wordnet
     * */
    private Optional<String> canBeVerb(View view){
        List<Constituent> constituents = view.getConstituents();
        for (Constituent constituent : constituents) {
            if(Wordnet.getInstance().getVerbTag(constituent.toString()))
                return Optional.of(constituent.toString());
        }
        return Optional.empty();
    }

}
