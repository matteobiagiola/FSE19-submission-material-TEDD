package org.mb.tedd.parsing;

import java.util.Arrays;

/*
* Needed for now since in order to fix flakiness I need to use objects different from the selenium
* driver object. On these different objects it is possible to call actions that have the same name of selenium
* actions listed below.
* */
public class SeleniumActions {

    private enum Action {
        SEND_KEYS ("sendKeys"),
        CLEAR ("clear"),
        CLICK ("click"),
        SUBMIT ("submit");

        private String action;

        Action(String action){
            this.action = action;
        }

        public String getActionName(){
            return this.action;
        }
    }

    public static boolean isContained(String stringContainingAction){
        return Arrays.stream(Action.values())
                .map(Action::getActionName)
                .anyMatch(actionName -> stringContainingAction.contains(actionName));
    }
}
