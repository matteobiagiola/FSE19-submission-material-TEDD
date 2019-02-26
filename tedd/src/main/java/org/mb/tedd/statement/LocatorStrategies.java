package org.mb.tedd.statement;

import java.util.Arrays;
import java.util.stream.Collectors;

public class LocatorStrategies {

    private enum Strategy {
        ID ("id"),
        XPATH ("xpath"),
        CSS_SELECTOR ("cssSelector"),
        CLASS_NAME ("className"),
        TAG_NAME ("tagName"),
        PARTIAL_LINK_TEXT ("partialLinkText"),
        LINK_TEXT ("linkText"),
        NAME ("name");

        private String strategy;

        Strategy(String strategy){
            this.strategy = strategy;
        }

        public String getStrategyName(){
            return this.strategy;
        }
    }

    public static boolean isLocatorStrategy(String strategy){
        return Arrays.stream(Strategy.values())
                .map(Strategy::getStrategyName)
                .collect(Collectors.toList())
                .contains(strategy);
    }
}
