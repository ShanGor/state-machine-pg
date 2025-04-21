package io.github.shangor.statemachine.state;

import lombok.Getter;

public class NodeStatus {
    public static final String RUNNING = "running";
    public static final String FAILED = "failed";
    public static final String SUCCESS = "success";
    public static final String DEFAULT = "default";
    public static final String UNKNOWN = "unknown";

    @Getter
    private final String name;

    public NodeStatus(String s) {
        name = s;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other instanceof NodeStatus o) {
            return name.equals(o.getName());
        } else {
            return false;
        }

    }

    public String toString() {
        return this.name;
    }
}
