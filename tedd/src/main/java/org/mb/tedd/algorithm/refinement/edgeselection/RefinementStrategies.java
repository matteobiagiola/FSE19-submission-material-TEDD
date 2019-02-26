package org.mb.tedd.algorithm.refinement.edgeselection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RefinementStrategies {

    public enum Strategy {
        SOURCE_FIRST ("source_first"),
        SINK_FIRST ("sink_first"),
        SIMPLE_RANDOM ("simple_random"),
        RANDOM ("random");

        private String strategy;

        Strategy(String strategy){
            this.strategy = strategy;
        }

        public String getStrategyName(){
            return this.strategy;
        }
    }

    public static boolean isRefinementStrategy(String strategy){
        return Arrays.stream(Strategy.values())
                .map(Strategy::getStrategyName)
                .collect(Collectors.toList())
                .contains(strategy);
    }

    public static List<String> getValues(){
        return Arrays.stream(Strategy.values())
                .map(Strategy::getStrategyName)
                .collect(Collectors.toList());
    }
}
