package io.github.shangor.statemachine.state;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Edge {
    protected String id;
    protected String source;
    protected String target;
    protected String label;
}