package org.mb.tedd.statement;

import java.util.Arrays;
import java.util.List;

public class SelectStatement implements SeleniumStatement {

    private DriverStatement driverStatement;
    private Action selectAction;

    public SelectStatement(Action selectAction, DriverStatement driverStatement){
        this.selectAction = selectAction;
        this.driverStatement = driverStatement;
    }

    @Override
    public List<Action> getActions() {
        return Arrays.asList(selectAction);
    }

    /**
     * @implNote assumption: the locator action (e.g. By.id("id")) is always a parameter of another driver action (e.g. findElement/findElements)
     * */
    @Override
    public Action getLocator() {
        return this.driverStatement.getLocator();
    }

    @Override
    public String toString(){
        return "Select(" + this.driverStatement + ")." + this.selectAction;
    }
}
