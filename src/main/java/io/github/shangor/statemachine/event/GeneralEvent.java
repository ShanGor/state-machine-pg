package io.github.shangor.statemachine.event;

import lombok.Data;

@Data
public class GeneralEvent {
    public enum EventType {
        ACTION_COMPLETED, ACTION_STARTED, ACTION_FAILED, ACTION_SKIPPED, ACTION_RETRIED, ACTION_TIMEOUT, ACTION_CANCELLED
    }
    private String correlationId;
    private EventType eventType;
    private String eventName;
    private String useCaseId;
    private String nodeId;
    private String useCaseName;
    private String state;
    private String domainContext;
    private long timestamp;

    public GeneralEvent() {
        timestamp = System.currentTimeMillis();
    }
}
