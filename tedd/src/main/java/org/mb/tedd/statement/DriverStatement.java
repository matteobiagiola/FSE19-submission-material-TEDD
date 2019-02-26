package org.mb.tedd.statement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DriverStatement implements SeleniumStatement {

    private List<Action> actions;
    private Action alertLocator = new Action("iframe_locator", Arrays.asList(new LiteralParameter("switch_to")));

    public DriverStatement(List<Action> actions){
        // actions are collected recursively, therefore in the reverse order
        this.actions = Lists.reverse(actions);
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    /**
     * @implNote assumption: the locator action (e.g. By.id("id")) is always a parameter of another driver action (e.g. findElement/findElements)
     * */
    @Override
    public Action getLocator() {
        Optional<Action> actionWithLocatorOptional = this.actions.stream()
                .filter(action -> action.getActionParameters()
                        .stream()
                        .anyMatch(actionParameter -> {
                            if(actionParameter.isAction()){
                                return ((Action) actionParameter).isLocator();
                            }
                            return false;
                        }))
                .findFirst();
        if(actionWithLocatorOptional.isPresent()){
            Action actionWithLocator = actionWithLocatorOptional.get();
            List<ActionParameter> actionParameters = actionWithLocator.getActionParameters();
            Optional<Action> locatorOptional = actionParameters.stream()
                    .filter(ActionParameter::isAction)
                    .map(actionParameter -> (Action) actionParameter)
                    .findAny();
            Preconditions.checkState(locatorOptional.isPresent(), "Selenium statement "
                    + this.toString() + " has no locator");
            return locatorOptional.get();
        } else if(this.toString().contains("alert")){
            return this.alertLocator;
        } else {
            throw new IllegalStateException("Selenium statement "
                    + this.toString() + " has no action with locator");
        }

    }

    @Override
    public String toString(){
        return "driver." + this.actions.stream().map(Action::toString).collect(Collectors.joining("."));
    }
}
