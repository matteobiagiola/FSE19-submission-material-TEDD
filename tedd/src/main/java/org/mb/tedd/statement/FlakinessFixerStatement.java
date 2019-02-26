package org.mb.tedd.statement;

import java.util.Arrays;
import java.util.List;

public class FlakinessFixerStatement implements SeleniumStatement {

    private Action seleniumAction;
    private String callerName;
    private Action locator;

    public FlakinessFixerStatement(Action locator, String callerName, Action seleniumAction){
        // actions are collected recursively, therefore in the reverse order
        this.seleniumAction = seleniumAction;
        this.callerName = callerName;
        this.locator = locator;
    }

    @Override
    public List<Action> getActions() {
        return Arrays.asList(this.seleniumAction);
    }

    /**
     * @implNote
     * */
    @Override
    public Action getLocator() {
        return this.locator;
    }

    @Override
    public String toString(){
        return this.callerName + "." + this.seleniumAction.toString() + " -- locator: " + this.locator;
    }
}
