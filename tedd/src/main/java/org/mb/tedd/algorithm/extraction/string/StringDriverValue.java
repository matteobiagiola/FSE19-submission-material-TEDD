package org.mb.tedd.algorithm.extraction.string;

import com.google.common.base.Preconditions;
import org.mb.tedd.statement.Action;
import org.mb.tedd.statement.ActionParameter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StringDriverValue implements StringValue {

    private Action locator;
    private Action driverAction;

    public StringDriverValue(Action locator, Action driverAction){

        Preconditions.checkArgument(locator.isLocator(), "Action " + locator + " is not a locator");

        this.locator = locator;
        this.driverAction = driverAction;
    }

    public Action getDriverAction() {
        return this.driverAction;
    }

    @Override
    public String toString(){
        return "(locator: " + this.locator + " -- action : " + this.driverAction + ")";
    }

    @Override
    public boolean equals(Object other){

        if(other == this) return true;

        if(other instanceof StringDriverValue){
            StringDriverValue otherStringDriverValue = (StringDriverValue) other;
            return Objects.equals(this.locator, otherStringDriverValue.locator) &&
                    Objects.equals(this.driverAction, otherStringDriverValue.driverAction);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.locator, this.driverAction);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.DRIVER;
    }

    @Override
    public List<String> getValues() {
        return this.driverAction.getActionParameters()
                .stream()
                .map(ActionParameter::toString)
                .collect(Collectors.toList());
    }
}
