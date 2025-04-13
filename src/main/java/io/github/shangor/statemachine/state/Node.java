package io.github.shangor.statemachine.state;

public interface Node {
    String getId();
    String getLabel();
    String getDescription();
    Status getStatus();
    Type getType();

    enum Type {
        START,
        END,
        EDGE,
        HUMAN,
        ACTION,
        CONDITION
    }

    enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILURE
    }
}
