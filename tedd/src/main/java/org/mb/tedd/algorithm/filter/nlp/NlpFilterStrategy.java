package org.mb.tedd.algorithm.filter.nlp;

import com.google.common.base.Preconditions;
import edu.cmu.lti.ws4j.WS4J;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.mb.tedd.algorithm.filter.FilterStrategy;
import org.mb.tedd.graph.DependencyGraphManager;
import org.mb.tedd.graph.GraphEdge;
import org.mb.tedd.graph.GraphNode;
import org.mb.tedd.utils.Properties;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class NlpFilterStrategy<T> implements FilterStrategy<T> {

    private List<String> classifiedReadVerbsLemmatized;
    private List<String> classifiedWriteVerbsLemmatized;
    private POSTaggerInterface posTagger;
    private AbstractVerbObjectDetector verbObjectDetector;
    private NounsFinder nounsFinder;
    private final static Logger logger = Logger.getLogger(NlpFilterStrategy.class.getName());

    public NlpFilterStrategy(){
        this.classifiedReadVerbsLemmatized = Arrays.stream(Properties.CLASSIFIED_READ_VERBS)
                .map(Lemmatizer::lemmatize).collect(Collectors.toList());
        this.classifiedWriteVerbsLemmatized = Arrays.stream(Properties.CLASSIFIED_WRITE_VERBS)
                .map(Lemmatizer::lemmatize).collect(Collectors.toList());
        this.posTagger = this.getPOSTagger();
        if(Properties.VERB_OBJECT_DETECTION){
            logger.info("Verb object detection:  " + Properties.VERB_OBJECT_DETECTION_STRATEGY);
            this.verbObjectDetector = this.getVerbObjectDetector();
        } else if(Properties.NOUN_MATCHING){
            logger.info("Nouns matching");
            this.nounsFinder = new NounsFinder(this.posTagger);
        } else {
            logger.info("Nlp verb only");
        }

    }


    @Override
    public Graph<GraphNode<T>, GraphEdge> filterDependencies(Graph<GraphNode<T>, GraphEdge> dependencyGraph) {
        Preconditions.checkArgument(dependencyGraph != null, "Dependency graph must not be null!");
        DependencyGraphManager<T> dependencyGraphManager = new DependencyGraphManager<>(dependencyGraph);
        Graph<GraphNode<T>, GraphEdge> dependencyGraphClone = dependencyGraphManager.duplicate();
        for (GraphEdge graphEdge : dependencyGraph.edgeSet()) {
            GraphNode<T> graphSource = dependencyGraphClone.getEdgeSource(graphEdge);
            GraphNode<T> graphTarget = dependencyGraphClone.getEdgeTarget(graphEdge);
            if(Properties.VERB_OBJECT_DETECTION){
                VerbObjectType verbObjectTypeSource = this.getVerbObjectType(graphSource.getTestCase().toString());
                VerbObjectType verbObjectTypeTarget = this.getVerbObjectType(graphTarget.getTestCase().toString());
                VerbType verbTypeSource = this.getVerbType(graphSource.getTestCase().toString(), verbObjectTypeSource.getVerb());
                VerbType verbTypeTarget = this.getVerbType(graphTarget.getTestCase().toString(), verbObjectTypeTarget.getVerb());
                if(this.removeDependency(verbTypeSource, verbTypeTarget, verbObjectTypeSource, verbObjectTypeTarget)){
                    logger.info("Removing edge " + graphEdge);
                    dependencyGraphClone.removeEdge(graphEdge);
                }
            } else if(Properties.NOUN_MATCHING){
                VerbNouns verbNounsSource = this.getVerbNouns(graphSource.getTestCase().toString());
                VerbNouns verbNounsTarget = this.getVerbNouns(graphTarget.getTestCase().toString());
                VerbType verbTypeSource = this.getVerbType(graphSource.getTestCase().toString(), verbNounsSource.getVerb());
                VerbType verbTypeTarget = this.getVerbType(graphTarget.getTestCase().toString(), verbNounsTarget.getVerb());
                if(this.removeDependency(verbTypeSource, verbTypeTarget, verbNounsSource, verbNounsTarget)){
                    logger.info("Removing edge " + graphEdge);
                    dependencyGraphClone.removeEdge(graphEdge);
                }
            } else {
                // POS tagging only verb analysis
                VerbType verbTypeSource = this.getVerbType(graphSource.getTestCase().toString());
                VerbType verbTypeTarget = this.getVerbType(graphTarget.getTestCase().toString());
                if(this.removeDependency(verbTypeSource, verbTypeTarget)){
                    logger.info("Removing edge " + graphEdge);
                    dependencyGraphClone.removeEdge(graphEdge);
                }
            }
        }
        return dependencyGraphClone;
    }

    private VerbType getVerbType(String testCaseName){
        Optional<String> verbOptional = this.posTagger.tagVerb(testCaseName);
        if(verbOptional.isPresent()){
            VerbType classifiedVerbType = this.readOrWrite(testCaseName, verbOptional.get());
            if(classifiedVerbType.equals(VerbType.READ)){
                logger.info("Classifying verb " + verbOptional.get() + " of " + testCaseName + " with " + VerbType.READ);
            } else if(classifiedVerbType.equals(VerbType.WRITE)){
                logger.info("Classifying verb " + verbOptional.get() + " of "  + testCaseName + " with " + VerbType.WRITE);
            } else {
                logger.info("Classifying verb " + verbOptional.get() + " of " + testCaseName + " with " + VerbType.IGNORE);
            }
            return classifiedVerbType;
        }
        return VerbType.IGNORE;
    }

    private VerbType getVerbType(String testCaseName, String verb){
        if(!verb.equals(AbstractVerbObjectDetector.NO_VERB)){
            VerbType classifiedVerbType = this.readOrWrite(testCaseName, verb);
            if(classifiedVerbType.equals(VerbType.READ)){
                logger.info("Classifying verb " + verb + " of " + testCaseName + " with " + VerbType.READ);
            } else if(classifiedVerbType.equals(VerbType.WRITE)){
                logger.info("Classifying verb " + verb + " of "  + testCaseName + " with " + VerbType.WRITE);
            } else {
                logger.info("Classifying verb " + verb + " of " + testCaseName + " with " + VerbType.IGNORE);
            }
            return classifiedVerbType;
        } else {
            return VerbType.IGNORE;
        }
    }

    private VerbObjectType getVerbObjectType(String testCaseName){
        VerbObjectType verbObject = this.verbObjectDetector.getVerbObject(testCaseName);
        logger.info("Object of verb " + verbObject.getVerb() + ": " + verbObject.getObj() + ", in test case " + testCaseName);
        return verbObject;
    }

    private VerbNouns getVerbNouns(String testCaseName){
        VerbNouns verbNouns = this.nounsFinder.getNouns(testCaseName);
        logger.info("Nouns in test case " + testCaseName + ": " + verbNouns.getNouns() + " where verb is " + verbNouns.getVerb());
        return verbNouns;
    }



    /* A (source) -> B (target): if A is READ and B is WRITE then dependency remains;
    * if A is READ and B is READ then dependency is removed;
    * if A is WRITE and B is READ then dependency is removed;
    * if A is WRITE and B is WRITE: for now dependency remains (we should detect if the two actions are applied on the
    * same object. If yes then dependency remains otherwise dependency is removed)
    * if either A or B are IGNORE then dependency remains: may happen because the tagger fails in recognizing a verb in a test case description
    */
    private boolean removeDependency(VerbType verbTypeSource, VerbType verbTypeTarget){
        if(verbTypeSource.equals(VerbType.READ)
                && verbTypeTarget.equals(VerbType.READ)){
            logger.info("READ after READ");
            return true;
        } else if(verbTypeSource.equals(VerbType.WRITE)
                && verbTypeTarget.equals(VerbType.READ)){
            logger.info("WRITE after READ");
            return true;
        }
        else if(verbTypeSource.equals(VerbType.WRITE)
                && verbTypeTarget.equals(VerbType.WRITE)
                && Properties.FILTER_WRITE_AFTER_WRITE){
            logger.info("WRITE after WRITE");
            return true;
        }
        return false;
    }

    /* A (source) -> B (target):
     * if A is READ and B is WRITE then dependency remains only if A and B refer to the same object;
     * if A is WRITE and B is WRITE then dependency remains only if A and B refer to the same object;
     * in the other cases conditions on verbs only apply
     */
    private boolean removeDependency(VerbType verbTypeSource, VerbType verbTypeTarget, VerbObjectType verbObjectTypeSource,
                                     VerbObjectType verbObjectTypeTarget){
        if(this.removeDependency(verbTypeSource, verbTypeTarget)){
            return true;
        }
        if(verbTypeSource.equals(VerbType.READ)
                && verbTypeTarget.equals(VerbType.WRITE)
                && !verbObjectTypeSource.getObj().equals(AbstractVerbObjectDetector.NO_OBJ)
                && !verbObjectTypeTarget.getObj().equals(AbstractVerbObjectDetector.NO_OBJ)
                && !verbObjectTypeSource.equalsObject(verbObjectTypeTarget)){
            logger.info("READ after WRITE with different objects. Source: "
                    + Lemmatizer.lemmatize(verbObjectTypeSource.getObj()) + ", target: " + Lemmatizer.lemmatize(verbObjectTypeTarget.getObj()));
            return true;
        } else if(verbTypeSource.equals(VerbType.WRITE)
                && verbTypeTarget.equals(VerbType.WRITE)
                && !verbObjectTypeSource.getObj().equals(AbstractVerbObjectDetector.NO_OBJ)
                && !verbObjectTypeTarget.getObj().equals(AbstractVerbObjectDetector.NO_OBJ)
                && !verbObjectTypeSource.equalsObject(verbObjectTypeTarget)){
            logger.info("WRITE after WRITE with different objects. Source: "
                    + Lemmatizer.lemmatize(verbObjectTypeSource.getObj()) + ", target: " + Lemmatizer.lemmatize(verbObjectTypeTarget.getObj()));
            return true;
        }
        return false;
    }

    /* A (source) -> B (target):
     * if A is READ and B is WRITE then dependency remains only if A and B do not contain the same nouns;
     * if A is WRITE and B is WRITE then dependency remains only if A and B do not contain the same nouns;
     * in the other cases conditions on verbs only apply
     */
    private boolean removeDependency(VerbType verbTypeSource, VerbType verbTypeTarget, VerbNouns verbNounsSource,
                                     VerbNouns verbNounsTarget){
        if(this.removeDependency(verbTypeSource, verbTypeTarget)){
            return true;
        }
        if(verbTypeSource.equals(VerbType.READ)
                && verbTypeTarget.equals(VerbType.WRITE)
                && !verbNounsSource.contains(verbNounsTarget)){
            logger.info("READ after WRITE with different nouns. Source: "
                    + verbNounsSource.getNouns() + ", target: " + verbNounsTarget.getNouns());
            return true;
        } else if(verbTypeSource.equals(VerbType.WRITE)
                && verbTypeTarget.equals(VerbType.WRITE)
                && !verbNounsSource.contains(verbNounsTarget)){
            logger.info("WRITE after WRITE with different nouns. Source: "
                    + verbNounsSource.getNouns() + ", target: " + verbNounsTarget.getNouns());
            return true;
        }
        return false;
    }

    private VerbType readOrWrite(String testCaseName, String verb){
        String verbLemmatized = Lemmatizer.lemmatize(verb);
        if(this.classifiedReadVerbsLemmatized.contains(verbLemmatized)){
            return VerbType.READ;
        }
        if(this.classifiedWriteVerbsLemmatized.contains(verbLemmatized)){
            return VerbType.WRITE;
        }
        logger.warn("Cannot classify verb " + verbLemmatized + " in " + testCaseName + " as read or write! Returning " + VerbType.IGNORE);
        return VerbType.IGNORE;

//        double maxWUPRead = this.maxWUP(verbLemmatized, classifiedReadVerbsLemmatized);
//        double maxWUPWrite = this.maxWUP(verbLemmatized, classifiedWriteVerbsLemmatized);
//        if(maxWUPRead > maxWUPWrite){
//            logger.info("Classifying verb " + verbLemmatized + " of " + testCaseName + " with "
//                    + VerbType.READ + " because similarity read " + maxWUPRead
//                    + " > similarity write " + maxWUPWrite);
//            return VerbType.READ;
//        } else if (maxWUPRead < maxWUPWrite) {
//            logger.info("Classifying verb " + verbLemmatized + " of " + testCaseName + " with "
//                    + VerbType.WRITE + " because similarity read " + maxWUPRead
//                    + " < similarity write " + maxWUPWrite);
//            return VerbType.WRITE;
//        } else {
//            logger.info("Classifying verb " + verbLemmatized +  " of " + testCaseName + " with "
//                    + VerbType.IGNORE + " because similarity read " + maxWUPRead
//                    + " = similarity write " + maxWUPWrite);
//            return VerbType.IGNORE;
//        }

    }

    private double maxWUP(String verb, List<String> classifiedVerbs){
        List<Double> wups = classifiedVerbs.stream()
                .map(classifiedVerb -> WS4J.runWUP(verb, classifiedVerb))
                .collect(Collectors.toList());
        return wups.stream().mapToDouble(v -> v)
                .max().orElseThrow(NoSuchElementException::new);
    }

    private POSTaggerInterface getPOSTagger(){
        String posTaggerStrategy = Properties.POS_TAGGER_STRATEGY;
        for (String strategy: POSTaggerStrategy.getValues()){
            if(strategy.equals(POSTaggerStrategy.Strategy.MAX_ENT.getStrategyName()) && posTaggerStrategy.equals(strategy)){
                return new MaxEntropyTagger();
            } else if(strategy.equals(POSTaggerStrategy.Strategy.OPTIMAL.getStrategyName()) && posTaggerStrategy.equals(strategy)){
                return new OptimalTagger();
            } else if(strategy.equals(POSTaggerStrategy.Strategy.CURATOR.getStrategyName()) && posTaggerStrategy.equals(strategy)){
                return new CuratorTagger();
            }
        }
        throw new IllegalArgumentException("Unknown pos tagger strategy " + posTaggerStrategy + ". See " + POSTaggerStrategy.class + " for reference.");
    }

    private AbstractVerbObjectDetector getVerbObjectDetector(){
        String verbObjectDetectionStrategy = Properties.VERB_OBJECT_DETECTION_STRATEGY;
        for (String strategy: VerbObjectDetectionStrategy.getValues()){
            if(strategy.equals(VerbObjectDetectionStrategy.Strategy.DEPENDENCY_PARSER.getStrategyName()) && verbObjectDetectionStrategy.equals(strategy)){
                return new StanfordDependencyParser();
            }
        }
        throw new IllegalArgumentException("Unknown verb object detector strategy " + verbObjectDetectionStrategy + ". See "
                + VerbObjectDetectionStrategy.class + " for reference.");
    }

}
