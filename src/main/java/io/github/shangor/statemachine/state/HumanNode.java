package io.github.shangor.statemachine.state;

import lombok.Data;

import java.util.List;

@Data
public class HumanNode implements Node {
    protected String id;
    protected String label;
    protected String description;
    protected Status status;
    protected List<String> actionUsers;

    @Override
    public Type getType() {
        return Type.HUMAN;
    }
}
