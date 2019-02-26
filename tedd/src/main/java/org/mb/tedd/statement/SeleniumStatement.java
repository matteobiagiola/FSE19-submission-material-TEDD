package org.mb.tedd.statement;

import java.util.List;

public interface SeleniumStatement {

    List<Action> getActions();

    Action getLocator();
}
