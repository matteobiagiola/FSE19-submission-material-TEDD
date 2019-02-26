package org.mb.tedd.algorithm.filter.nlp;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class POSTaggerStrategy {

    public enum Strategy {
        MAX_ENT ("max_ent"),
        OPTIMAL ("optimal"),
        CURATOR ("curator");

        private String strategy;

        Strategy(String strategy){
            this.strategy = strategy;
        }

        public String getStrategyName(){
            return this.strategy;
        }
    }

    public static List<String> getValues(){
        return Arrays.stream(Strategy.values())
                .map(Strategy::getStrategyName)
                .collect(Collectors.toList());
    }
}
