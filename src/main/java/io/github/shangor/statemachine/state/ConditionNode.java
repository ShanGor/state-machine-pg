package io.github.shangor.statemachine.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionNode implements Node {
    protected String id;
    protected String label;
    protected String description;
    protected Status status;
    @Override
    public Type getType() {
        return Type.CONDITION;
    }
}
