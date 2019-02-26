package org.mb.tedd.statement;

import java.util.List;

public abstract class AssertStatement implements SeleniumStatement {

    private AssertType assertType;
    private DriverStatement driverStatement;

    public AssertStatement(AssertType type, DriverStatement driverStatement){
        this.assertType = type;
        this.driverStatement = driverStatement;
    }

    public AssertType getAssertType() {
        return assertType;
    }

    public DriverStatement getDriverStatement() {
        return driverStatement;
    }

    @Override
    public List<Action> getActions(){
        return this.driverStatement.getActions();
    }

    @Override
    public Action getLocator(){
        return this.getDriverStatement().getLocator();
    }

}
