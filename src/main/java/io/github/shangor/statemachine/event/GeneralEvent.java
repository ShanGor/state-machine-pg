package io.github.shangor.statemachine.event;

import lombok.Data;

@Data
public class GeneralEvent {
    private String correlationId;
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
