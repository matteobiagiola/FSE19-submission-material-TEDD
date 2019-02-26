package org.mb.tedd.statement;

public class AssertFalseStatement extends AssertStatement {

    public AssertFalseStatement(DriverStatement driverStatement){
        super(AssertType.FALSE, driverStatement);
    }

    @Override
    public String toString(){
        return "assert" + this.getAssertType().getValue() + "(" + this.getDriverStatement() + ")";
    }
}
