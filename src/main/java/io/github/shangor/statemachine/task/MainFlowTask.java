
package io.github.shangor.statemachine.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.statemachine.dao.*;
import io.github.shangor.statemachine.event.GeneralEvent;
import io.github.shangor.statemachine.pojo.ActionDetails;
import io.github.shangor.statemachine.pojo.ConsumerRecord;
import io.github.shangor.statemachine.pojo.UseCaseEventKey;
import io.github.shangor.statemachine.service.PubSubService;
import io.github.shangor.statemachine.service.StateMachineControlService;
import io.github.shangor.statemachine.state.ActionHandlers;
import io.github.shangor.statemachine.state.ActionNode;
import io.github.shangor.statemachine.state.StateFlow;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import io.github.shangor.statemachine.util.HttpUtil;
import io.github.shangor.statemachine.util.JsonUtil;
import io.github.shangor.statemachine.util.SecurityUtil;
import io.github.shangor.state.StateUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class MainFlowTask {
    private final ObjectMapper objectMapper;
    private final StateMachineControlService stateMachineControlService;

    private static final Map<String, Map<UseCaseEventKey, List<StateFlow>>> config = new HashMap<>();

    /**
     * - key: useCaseName
     * - value: StatemachineControlEntity
     */
    public static Map<String, StatemachineControlEntity> useCaseInitials = new HashMap<>();

    /**
     * nodeByPossibleState:
     *  - key: useCaseId
     *  - value:
     *    - key: toState / possible target state which calculated by stateFormula
     *    - value: list of StateFlow
     */
    public static Map<String, Map<String, List<StateFlow>>> nodeByPossibleState = new HashMap<>();
    public static volatile boolean isConfigInitialized = false;
    private static final TypeReference<Map<String, Boolean>> collectedStatesTypeRef = new TypeReference<>() {};
    private final JdbcTemplate jdbcTemplate;
    private final StatemachineTransactionDetailRepository txnRepo;
    private final StatemachineTransactionHistoryRepository histRepo;
    public static volatile boolean stopping = false;
    private final PubSubService pubSubService;

    private final ActionHandlers actionHandlers;

    private final StatemachineFlowStateRepository flowStateRepo;

    @Value("${state.pub-sub.bus-topic:bus}")
    @Setter
    @Getter
    private String busTopic;

    @PostConstruct
    public void init() {
        log.info("Starting MainFlowTask");

        ConcurrentUtil.runAsync(() -> {
            log.info("Initializing configs!");

            var configList = stateMachineControlService.loadAll().collectList().block();

            if (configList == null || configList.isEmpty()) {
                log.error("No config found, please check your database!");
                return;
            }

            configList.forEach(item -> {
                putConfigToMemory(item);

                useCaseInitials.put(item.getUseCaseName(), item);

                isConfigInitialized = true;
            });

            log.info("Config loaded to memory!");

            Thread.ofVirtual().start(this::subscribeAndRock);

        });
    }

    /**
     * subscribe the bus topic, process the messages, and transit status.
     */
    void subscribeAndRock() {
        //var topics = config.keySet();
        var topics = Set.of(busTopic);

        while(!stopping) {
            try {
                var messages = pubSubService.poll(topics);
                for(var msg : messages) {
                    log.info("Orchestrator processing message: {}", msg.getValue());
                    ConcurrentUtil.runAsync(() -> {
                        try {
                            processMessage(msg);
                        } catch (Exception e) {
                            log.error("Error while processing bus message: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Error while polling/handling bus messages: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void processMessage(ConsumerRecord<String, String> msg) {
        var topic = msg.getTopic();
        var payload = msg.getValue();
        try {
            var event = objectMapper.readValue(payload, GeneralEvent.class);

            var configItem = config.get(topic);
            if (configItem == null) {
                log.info("No config found for topic {}, skip this event!", topic);
                pubSubService.acknowledge(msg.getTopic(), msg.getId());
                return;
            }
            var key = new UseCaseEventKey(event.getUseCaseId(), event.getState());
            var items = configItem.get(key);
            if (items == null || items.isEmpty()) {
                log.info("No further action found for key {}, skip this event!", key);
                pubSubService.acknowledge(msg.getTopic(), msg.getId());
                return;
            }

            var toWrittenHistory = true;
            for (var item : items) {
                GeneralEvent.EventType eventType = GeneralEvent.EventType.ACTION_COMPLETED;

                Optional<Map<String, Object>> updatedEntity = Optional.empty();
                if (item.isFirst()) {
                    createTransaction(event, item);
                } else {
                    updatedEntity = updateTransaction(event, item);
                }
                if (toWrittenHistory) {
                    writeTransactionHistory(event);
                    toWrittenHistory = false;
                }

                switch (item.getNodeType()) {
                    case ACTION -> {
                        log.info("Processing action {}", item.getActionName());
                        updateFlowStatus(event, item, StatemachineFlowStateEntity.Status.RUNNING);

                        var handler = actionHandlers.getActionHandler(item.getActionName());
                        if (handler.isEmpty()) {
                            log.error("No handler found for action {}", item.getActionName());
                            pubSubService.acknowledge(msg.getTopic(), msg.getId());
                            return;
                        }
                        var input = ActionNode.Param.builder().config(item).context(event.getDomainContext()).build();

                        try {
                            var resp = handler.get().action(input);
                            event.setDomainContext(resp);
                            updateFlowStatus(event, item, StatemachineFlowStateEntity.Status.SUCCESS);
                        } catch (Exception e) {
                            log.error("Error while processing action message: {}", e.getMessage());
                            eventType = GeneralEvent.EventType.ACTION_FAILED;
                            event.setState("%s.failed".formatted(item.getNodeId()));
                            event.setDomainContext(JsonUtil.getObjectMapper().writeValueAsString(Map.of("error", e.getMessage())));
                            updateFlowStatus(event, item, StatemachineFlowStateEntity.Status.FAILED);
                        }
                    }
                    case HUMAN -> {
                        //TODO write to a human queue, and not transit state yet.
                        updateFlowStatus(event, item, StatemachineFlowStateEntity.Status.RUNNING);
                        pubSubService.acknowledge(msg.getTopic(), msg.getId());
                        return;
                    }
                    default -> {
                        updateFlowStatus(event, item, StatemachineFlowStateEntity.Status.UNKNOWN);
                    }
                }

                transitState(event, item, updatedEntity, eventType);
            }

            pubSubService.acknowledge(msg.getTopic(), msg.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    GeneralEvent fromEventAndItem(GeneralEvent event,StateFlow item, GeneralEvent.EventType eventType) {
        var toState = item.getToState();
        var evt = new GeneralEvent();
        evt.setEventType(eventType);
        evt.setNodeId(item.getNodeId());
        evt.setCorrelationId(event.getCorrelationId());
        evt.setState(toState);
        evt.setUseCaseId(event.getUseCaseId());
        evt.setDomainContext(event.getDomainContext());
        evt.setUseCaseName(event.getUseCaseName());
        evt.setEventName("%s.%s".formatted(event.getUseCaseName(), toState));
        return evt;
    }

    void transitState(GeneralEvent event, StateFlow item, Optional<Map<String, Object>> updatedEntity, GeneralEvent.EventType eventType) throws JsonProcessingException {
        var evt = fromEventAndItem(event, item, eventType);
        if (StringUtils.isNotBlank(item.getPublishTopic()) && StringUtils.isNotBlank(evt.getState())) {
            publishEvent(item.getPublishTopic(), ConcurrentUtil.uuidV7().toString(), evt);
        } else if (item.getApiCallConfig() != null) {
            var apiCallConfig = item.getApiCallConfig();
            var url = apiCallConfig.getUrl();
            var resultOpt = HttpUtil.post(url, objectMapper.writeValueAsString(evt), apiCallConfig.isAsync());
            if (resultOpt.isPresent()) { // Synchronous API call
                var resp = objectMapper.readValue(resultOpt.get(), LinkedHashMap.class);
                evt.setDomainContext(event.getDomainContext());
                updateTransaction(evt, item);

                publishEvent(busTopic, ConcurrentUtil.uuidV7().toString(), evt);
            }
        } else if (updatedEntity.isPresent() && StringUtils.isNotBlank(item.getStateFormula())) {
            /*
             * Do 3 things
             * - Calculate the target state
             * - update transaction
             * - publish event to the event-bus topic
             */
            Object collectedStatusObj = updatedEntity.get().get("collected_states");
            String collected;
            if (collectedStatusObj instanceof org.postgresql.util.PGobject pgObject) {
                collected = pgObject.getValue();
            } else {
                collected = (String) collectedStatusObj;
            }
            var collectedStates = objectMapper.readValue(collected, collectedStatesTypeRef);
            var targetStateOpt = StateUtil.determineState(event.getState(), item.getStateFormula(), collectedStates);
            if (targetStateOpt.isPresent()) {
                var targetState = targetStateOpt.get();
                event.setState(targetState);
                updateTransaction(event, item);

                publishEvent(busTopic, ConcurrentUtil.uuidV7().toString(), event);
            }
        }
    }

    void publishEvent(String topic, String id, GeneralEvent event) throws JsonProcessingException {
        pubSubService.publishEvent(topic, id, objectMapper.writeValueAsString(event));
    }

    public void updateFlowStatus(GeneralEvent event, StateFlow item, StatemachineFlowStateEntity.Status knownStatus) {
        ConcurrentUtil.runAsync(() -> {
            try {
                String nodeStatus;
                if (!knownStatus.equals(StatemachineFlowStateEntity.Status.UNKNOWN)) {
                    nodeStatus = knownStatus.toString();
                } else {
                    switch (item.getNodeType()) {
                        case START, END -> nodeStatus = "success";
                        default -> {
                            if (StringUtils.isBlank(event.getState())) {
                                nodeStatus = "success";
                            } else {
                                var state = event.getState().toLowerCase(Locale.ROOT);
                                if (state.contains("success") || state.contains("complete")) {
                                    nodeStatus = "success";
                                } else if (state.contains("fail") || state.contains("error")) {
                                    nodeStatus = "failed";
                                } else {
                                    nodeStatus = "default";
                                }
                            }
                        }
                    }
                }

                var o = new StatemachineFlowStateEntity();
                var id = new StatemachineFlowStateEntity.Id();
                id.setTransactionId(event.getCorrelationId());
                id.setUseCaseId(event.getUseCaseId());
                id.setNodeId(item.getNodeId());
                o.setId(id);
                o.setState(nodeStatus);
                o.setContext(event.getDomainContext());
                flowStateRepo.save(o);
            } catch (Exception e) {
                log.error("Error updating flow status: {}", e.getMessage());
            }
        });
    }

    void writeTransactionHistory(GeneralEvent event) {

        var hist = new StateMachineTransactionHistoryEntity();
        hist.setId(ConcurrentUtil.uuidV7().toString());
        hist.setTransactionId(event.getCorrelationId());
        hist.setEventName(event.getEventName());
        hist.setEventType(event.getEventType().name());
        hist.setUseCaseId(event.getUseCaseId());
        hist.setState(event.getState());
        hist.setPayload(event.getDomainContext());
        hist.setCreationTime(new Timestamp(System.currentTimeMillis()));
        hist.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        histRepo.save(hist);
    }

    Optional<Map<String, Object>> updateTransaction(GeneralEvent event, StateFlow item) {
        var sql = """
               update sm_txn_detail
                set last_update_time=current_timestamp,
                    collected_states = jsonb_set(collected_states, '{%s}', 'true', true),
                    current_state=?,
                    domain_context=?,
                    action_details=?
                where id=? and use_case_id=?
                RETURNING last_update_time, collected_states, current_state, domain_context, action_details"""
                .formatted(SecurityUtil.sanitizeSQL(event.getState()));
        try {
            var args = new Object[]{
                    event.getState(),
                    event.getDomainContext(),
                    objectMapper.writeValueAsString(composeActionDetails(item)),
                    event.getCorrelationId(),
                    event.getUseCaseId()
            };
            int[] types = new int[]{Types.VARCHAR, Types.LONGVARCHAR, Types.LONGVARCHAR, Types.VARCHAR, Types.VARCHAR};
            var updatedEntity = jdbcTemplate.queryForMap(sql, args, types);

            log.info("Updated transaction {}.{} for {}.{}", event.getCorrelationId(), event.getUseCaseId(), event.getUseCaseName(), event.getState());
            return Optional.of(updatedEntity);
        } catch (Exception e) {
            log.error("Failed to update transaction {}.{} for {}.{}", event.getCorrelationId(), event.getUseCaseId(), event.getUseCaseName(), event.getState());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    ActionDetails composeActionDetails(StateFlow item) {
        return ActionDetails.builder()
                .publishTopic(item.getPublishTopic())
                .subscribeTopic(item.getSubscribeTopic())
                .apiConfig(item.getApiCallConfig())
                .build();
    }

    void createTransaction(GeneralEvent event, StateFlow item) {
        var collectedStates = Map.of(event.getState(), true);
        var o = new StatemachineTransactionDetailEntity();
        o.setId(event.getCorrelationId());
        o.setUseCaseId(event.getUseCaseId());
        o.setCurrentState(event.getState());
        o.setDomainContext(event.getDomainContext());
        o.setCollectedStates(collectedStates);
        o.setActionDetails(composeActionDetails(item));
        o.setCreationTime(new Timestamp(System.currentTimeMillis()));
        o.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        txnRepo.save(o);
    }

    void putConfigToMemory(StatemachineControlEntity useCase) {
        var nodeByState = new HashMap<String, List<StateFlow>>();
        nodeByPossibleState.put(useCase.getUseCaseId(), nodeByState);

        var details = useCase.getDetail();
        if (details != null && !details.isEmpty()) {
            for (var item : details) {
                var subscribeTopic = item.getSubscribeTopic();

                var configItem = config.computeIfAbsent(subscribeTopic, k -> new HashMap<>());

                var key = new UseCaseEventKey(useCase.getUseCaseId(), item.getFromState());
                var list = configItem.computeIfAbsent(key, k -> new LinkedList<>());
                list.add(item);

                var outputState = item.getToState();
                if (StringUtils.isNotBlank(outputState)) {
                    List<StateFlow> listNodes;
                    if (!nodeByState.containsKey(outputState)) {
                        listNodes = new LinkedList<StateFlow>();
                        nodeByState.put(outputState, listNodes);
                    } else {
                        listNodes = nodeByState.get(outputState);
                    }

                    listNodes.add(item);
                }

                if (StringUtils.isNotBlank(item.getStateFormula())) {
                    var p =StateUtil.parseFormulaForInput(item.getStateFormula());
                    var variables = p.getLeft();
                    var possibleOutputStates = p.getRight();
                    for (var v : variables) {
                        key = new UseCaseEventKey(useCase.getUseCaseId(), v.trim());
                        list = configItem.computeIfAbsent(key, k -> new LinkedList<>());
                        list.add(item);
                    }

                    // The following is to add the possible output states to the nodeByState map
                    for (var s : possibleOutputStates) {
                        List<StateFlow> listNodes;
                        if (!nodeByState.containsKey(s)) {
                            listNodes = new LinkedList<>();
                            nodeByState.put(s, listNodes);
                        } else {
                            listNodes = nodeByState.get(s);
                        }
                        listNodes.add(item);
                    }
                }
            }
        }


    }

}
