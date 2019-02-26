package org.mb.tedd.statement;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Action implements ActionParameter {

    private String name;
    private List<ActionParameter> actionParameters;
    private boolean isLocator;
    private boolean isLocatorContainer;

    public Action(String name, List<ActionParameter> actionParameters){
        this.name = name;
        this.actionParameters = actionParameters;
        this.isLocator = LocatorStrategies.isLocatorStrategy(name);

        if(!this.isLocator && this.name.equals("iframe_locator"))
            this.isLocator = true;

        this.isLocatorContainer = LocatorContainerNames.isLocatorContainer(name);

        if(this.isLocator){
            Preconditions.checkArgument(actionParameters.size() == 1 && actionParameters.get(0).isLiteral(),
                    "Locator " + this.toString() + " must have only one literal parameter");
        }

        if(this.isLocatorContainer){
            Preconditions.checkArgument(actionParameters.size() == 1
                    && actionParameters.get(0).isAction() &&
                    ((Action) actionParameters.get(0)).isLocator(),
                    "Locator container " + this.toString()
                            + " must have only one parameter that is a locator.");
        }

    }

    public String getName() {
        return name;
    }

    public List<ActionParameter> getActionParameters() {
        return actionParameters;
    }

    public boolean isLocator() {
        return this.isLocator;
    }

    public boolean isLocatorContainer() { return this.isLocatorContainer; }

    @Override
    public String toString(){
        String name = this.name;
        if(this.isLocator){
            name = "By." + name;
        }
        return name + "(" + this.actionParameters.stream().map(ActionParameter::toString)
                .collect(Collectors.joining(",")) + ")";
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isAction() {
        return true;
    }

    @Override
    public boolean equals(Object other){

        if(other == this) return true;

        if(other instanceof Action){
            Action otherAction = (Action) other;
            return Objects.equals(this.name, otherAction.name) &&
                    Objects.equals(this.actionParameters, otherAction.actionParameters) &&
                    Objects.equals(this.isLocator, otherAction.isLocator) &&
                    Objects.equals(this.isLocatorContainer, otherAction.isLocatorContainer);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.name, this.actionParameters, this.isLocator, this.isLocatorContainer);
    }
}
