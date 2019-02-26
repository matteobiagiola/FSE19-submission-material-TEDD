package org.mb.tedd.algorithm.extraction.string;

import com.google.common.base.Preconditions;
import org.mb.tedd.statement.Action;
import org.mb.tedd.statement.LiteralParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StringAssertValue implements StringValue {

    private Action locator;
    private Action driverAction;
    private LiteralParameter valueToBeChecked;

    public StringAssertValue(Action locator, Action driverAction, LiteralParameter valueToBeChecked){

        Preconditions.checkArgument(locator.isLocator(), "Action " + locator + " is not a locator");

        this.locator = locator;
        this.driverAction = driverAction;
        this.valueToBeChecked = valueToBeChecked;
    }

    @Override
    public String toString(){
        return "(locator: " + this.locator + " -- action : " + this.driverAction
                + " -- value to be checked: " + this.valueToBeChecked + ")";
    }

    @Override
    public boolean equals(Object other){

        if(other == this) return true;

        if(other instanceof StringAssertValue){
            StringAssertValue otherStringAssertValue = (StringAssertValue) other;
            return Objects.equals(this.locator, otherStringAssertValue.locator) &&
                    Objects.equals(this.driverAction, otherStringAssertValue.driverAction) &&
                    Objects.equals(this.valueToBeChecked, otherStringAssertValue.valueToBeChecked);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.locator, this.driverAction, this.valueToBeChecked);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ASSERT;
    }

    @Override
    public List<String> getValues() {
        return Arrays.asList(this.valueToBeChecked.getValue());
    }
}
