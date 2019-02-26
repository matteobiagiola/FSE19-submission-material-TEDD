package org.mb.tedd.statement;

public class AssertEqualsStatement extends AssertStatement {

    private LiteralParameter valueToBeChecked;

    public AssertEqualsStatement(LiteralParameter valueToBeChecked, DriverStatement driverStatement){
        super(AssertType.EQUALS, driverStatement);
        this.valueToBeChecked = valueToBeChecked;
    }

    public LiteralParameter getValueToBeChecked() {
        return this.valueToBeChecked;
    }

    @Override
    public String toString(){
        return "assert" + this.getAssertType().getValue()
                + "(" + this.valueToBeChecked + "," + this.getDriverStatement() + ")";
    }
}
