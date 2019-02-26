package org.mb.tedd.algorithm.extraction;

import org.mb.tedd.graph.DependencyGraphManager;

public interface DependencyGraphExtractor {

    DependencyGraphManager<String> computeDependencies();
}
