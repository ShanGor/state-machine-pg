package io.github.shangor.statemachine.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class ActionNode implements Node {
    protected String id;
    protected String label;
    protected String description;
    protected Status status;

    /**
     * For the name of the action handlers.
     */
    public static final String ACTION_NAME = "actionName";

    @Data
    @Builder
    public static class Param {
        private StateFlow config;
        private String context;
    }

    @Override
    public Type getType() {
        return Type.ACTION;
    }

    public abstract String action(Param input);
}
