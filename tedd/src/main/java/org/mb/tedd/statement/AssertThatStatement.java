package org.mb.tedd.statement;

public class AssertThatStatement extends AssertStatement {

    private LiteralParameter valueToBeChecked;

    public AssertThatStatement(LiteralParameter valueToBeChecked, DriverStatement driverStatement){
        super(AssertType.THAT, driverStatement);
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
