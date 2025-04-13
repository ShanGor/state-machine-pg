package io.github.shangor.statemachine.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.statemachine.event.GeneralEvent;
import io.github.shangor.statemachine.service.PubSubService;
import io.github.shangor.statemachine.task.MainFlowTask;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import io.github.shangor.statemachine.util.HttpUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StatemachineController {
    private final PubSubService pubSubService;
    private final MainFlowTask mainFlow;
    private final ObjectMapper objectMapper;

    @PreDestroy
    public void onDestroy() {
        log.info("Requesting to destroy this instance!");
        MainFlowTask.stopping = true;
        try {
            TimeUnit.SECONDS.sleep(10);
            log.info("Statemachine consumer closed gracefully!");
        } catch (InterruptedException e) {
            log.error("Error while waiting for Statemachine consumer to close gracefully: {}", e.getMessage());
        }
    }

    @GetMapping("/api/state-machine/generate-id")
    public String allocateTransactionId() {
        return ConcurrentUtil.uuidV7().toString();
    }

    @PostMapping("/api/state-machine/callback")
    public ResponseEntity<String> statemachineCallback(@RequestBody String body, ServerHttpRequest request) {
        try {
            var correlationIdOpt = HttpUtil.tryGetHeader(HttpUtil.HEADER_CORRELATION_ID, request.getHeaders());
            if (correlationIdOpt.isEmpty()) {
                return ResponseEntity.status(400).body("Correlation ID not found in the request header");
            }
            pubSubService.publishEvent(mainFlow.getBusTopic(), correlationIdOpt.get(), body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
        return ResponseEntity.ok("Response well received!");
    }

    @PostMapping("/api/state-machine/{useCase}")
    public ResponseEntity<?> test(@RequestBody String payload,
                               @PathVariable String useCase,
                               ServerHttpRequest request) {
        var correlationIdOpt = HttpUtil.tryGetHeader(HttpUtil.HEADER_CORRELATION_ID, request.getHeaders());
        if (correlationIdOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Correlation ID not found in the request header");
        }
        var correlationId = correlationIdOpt.get();

        log.info("Received payload for use case {}", useCase);
        if (!MainFlowTask.isConfigInitialized) {
            return ResponseEntity.status(502).body("Not initialized yet");
        }
        var flowCase = MainFlowTask.useCaseInitials.get(useCase);
        if (flowCase == null) {
            return ResponseEntity.status(400).body("Not found use case");
        }
        var firstItem = flowCase.getDetail().getFirst();

        try {
            log.info("Found use case {}: {}", useCase, flowCase);
            var evt = new GeneralEvent();
            evt.setEventType(GeneralEvent.EventType.ACTION_STARTED);
            evt.setState(firstItem.getFromState());
            evt.setNodeId(firstItem.getNodeId());
            evt.setUseCaseName(flowCase.getUseCaseName());
            evt.setUseCaseId(flowCase.getUseCaseId());
            evt.setCorrelationId(correlationId);
            evt.setEventName("%s.%s".formatted(flowCase.getUseCaseName(), firstItem.getFromState()));
            evt.setDomainContext(objectMapper.readValue(payload, Map.class));
            pubSubService.publishEvent(mainFlow.getBusTopic(), correlationId, objectMapper.writeValueAsString(evt));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("Received payload for use case %s".formatted(useCase));
    }
}
