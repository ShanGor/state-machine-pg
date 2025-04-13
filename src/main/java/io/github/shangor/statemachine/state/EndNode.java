package io.github.shangor.statemachine.state;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EndNode implements Node {
    private String id;
    private String label;
    private String description;
    private Status status;

    @Override
    public Type getType() {
        return Type.END;
    }
}
