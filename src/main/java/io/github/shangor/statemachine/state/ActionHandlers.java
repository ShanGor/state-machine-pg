package io.github.shangor.statemachine.state;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class ActionHandlers {
    private final ConcurrentHashMap<String, ActionNode> actionHandlers = new ConcurrentHashMap<>();
    public void registerActionHandler(String name, ActionNode actionHandler) {
        actionHandlers.put(name, actionHandler);
    }

    public Optional<ActionNode> getActionHandler(String name) {
        return Optional.ofNullable(actionHandlers.get(name));
    }
}
