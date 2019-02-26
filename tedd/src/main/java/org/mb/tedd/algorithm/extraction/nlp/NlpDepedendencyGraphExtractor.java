package org.mb.tedd.algorithm.extraction.nlp;

import org.mb.tedd.algorithm.extraction.DependencyGraphExtractor;
import org.mb.tedd.graph.DependencyGraphManager;

public class NlpDepedendencyGraphExtractor implements DependencyGraphExtractor {

    @Override
    public DependencyGraphManager<String> computeDependencies() {
        throw new UnsupportedOperationException("Nlp graph extraction not supported yet");
    }
}
