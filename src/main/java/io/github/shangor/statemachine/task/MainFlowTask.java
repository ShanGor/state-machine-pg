
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
import io.github.shangor.statemachine.state.ActionNode;
import io.github.shangor.statemachine.state.StateFlow;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import io.github.shangor.statemachine.util.HttpUtil;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class MainFlowTask {
    private final ObjectMapper objectMapper;
    private final StateMachineControlService stateMachineControlService;

    private static final Map<String, Map<UseCaseEventKey, List<StateFlow>>> config = new HashMap<>();
    public static Map<String, StateMachineControlEntity> useCaseInitials = new HashMap<>();
    public static volatile boolean isConfigInitialized = false;
    private static final TypeReference<Map<String, Boolean>> collectedStatesTypeRef = new TypeReference<>() {};
    private final JdbcTemplate jdbcTemplate;
    private final StateMachineTransactionDetailRepository txnRepo;
    private final StateMachineTransactionHistoryRepository histRepo;
    public static volatile boolean stopping = false;
    private final PubSubService pubSubService;

    private static final ConcurrentHashMap<String, ActionNode> actionHandlers = new ConcurrentHashMap<>();

    @Value("${state.pub-sub.bus-topic:bus}")
    @Setter
    @Getter
    private String busTopic;

    public static void registerActionHandler(String name, ActionNode actionHandler) {
        actionHandlers.put(name, actionHandler);
    }

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
            Thread.ofVirtual().start(this::agentRocks);

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

    /**
     * Pretend to be agents, and process the messages.
     */
    void agentRocks() {
        var includeTopics = new HashSet<String>();
        config.values().forEach(configItem -> {
            configItem.forEach((key, items) -> {
                items.forEach(item -> {
                    if (StringUtils.isNotBlank(item.getSubscribeTopic())) {
                        includeTopics.add(item.getSubscribeTopic());
                    }
                });
            });
        });
        // remove the bus topic from the include topics
        // config.keySet().forEach(includeTopics::remove);
        includeTopics.remove(busTopic);

        while(!stopping) {
            try {
                var messages = pubSubService.poll(includeTopics);
                for(var msg : messages) {
                    log.info("Agent processing message: {}", msg.getValue());
                    ConcurrentUtil.runAsync(() -> {
                        try {
                            processAgent(msg);
                        } catch (Exception e) {
                            log.error("Agent error while processing message: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Agent error while polling/handling messages: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void processAgent(ConsumerRecord<String, String> msg) {
        var payload = msg.getValue();
        try {
            var event = objectMapper.readValue(payload, GeneralEvent.class);

            List<StateFlow> items = new LinkedList<>();
            for (var flow : useCaseInitials.get(event.getUseCaseName()).getDetail()) {
                if (event.getState().equals(flow.getFromState())) {
                    items.add(flow);
                }
            }
            if (items.isEmpty()) {
                pubSubService.acknowledge(msg.getTopic(), msg.getId());
                log.info("Agent: Not found the item {}.{}", event.getUseCaseId(), event.getNodeId());
                return;
            }

            for (var item : items) {
                ConcurrentUtil.runAsync(() -> {
                    try {
                        log.info("Agent: preprocessing {}- {}", event.getUseCaseName(), event.getEventName());

                        log.info("Agent: processed {}- {}", event.getUseCaseName(), event.getEventName());

                        event.setState(item.getToState());
                        event.setNodeId(item.getNodeId());
                        event.setEventName("%s.%s".formatted(event.getUseCaseName(), item.getToState()));
                        publishEvent(item.getPublishTopic(), ConcurrentUtil.uuidV7().toString(), event);
                    } catch (Exception e) {
                        log.error("Error while processing agent message: {}", e.getMessage());
                        e.printStackTrace();
                    }

                });

            }

            pubSubService.acknowledge(msg.getTopic(), msg.getId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
                        var handler = actionHandlers.get(item.getActionName());
                        if (handler == null) {
                            log.error("No handler found for action {}", item.getActionName());
                            pubSubService.acknowledge(msg.getTopic(), msg.getId());
                            return;
                        }
                        var input = ActionNode.Param.builder().config(item).context(event.getDomainContext()).build();

                        try {
                            var resp = handler.action(input);
                            event.setDomainContext(resp);
                        } catch (Exception e) {
                            log.error("Error while processing action message: {}", e.getMessage());
                            // TODO, give it a failure state
                        }
                    }
                    case HUMAN -> {
                        //TODO write to a human queue, and not transit state yet.
                        pubSubService.acknowledge(msg.getTopic(), msg.getId());
                        return;
                    }
                }

                transitState(event, item, updatedEntity);
            }

            pubSubService.acknowledge(msg.getTopic(), msg.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    GeneralEvent fromEventAndItem(GeneralEvent event,StateFlow item) {
        var toState = item.getToState();
        var evt = new GeneralEvent();
        evt.setEventType(GeneralEvent.EventType.ACTION_STARTED);
        evt.setNodeId(item.getNodeId());
        evt.setCorrelationId(event.getCorrelationId());
        evt.setState(toState);
        evt.setUseCaseId(event.getUseCaseId());
        evt.setDomainContext(event.getDomainContext());
        evt.setUseCaseName(event.getUseCaseName());
        evt.setEventName("%s.%s".formatted(event.getUseCaseName(), toState));
        return evt;
    }

    void transitState(GeneralEvent event, StateFlow item, Optional<Map<String, Object>> updatedEntity) throws JsonProcessingException {
        var evt = fromEventAndItem(event, item);
        if (StringUtils.isNotBlank(item.getPublishTopic()) && StringUtils.isNotBlank(evt.getState())) {
            publishEvent(item.getPublishTopic(), ConcurrentUtil.uuidV7().toString(), evt);
        } else if (item.getApiCallConfig() != null) {
            var apiCallConfig = item.getApiCallConfig();
            var url = apiCallConfig.getUrl();
            var resultOpt = HttpUtil.post(url, objectMapper.writeValueAsString(evt), apiCallConfig.isAsync());
            if (resultOpt.isPresent()) { // Synchronous API call
                var resp = objectMapper.readValue(resultOpt.get(), LinkedHashMap.class);
                var ctx = new HashMap<String, Object>();
                ctx.putAll(event.getDomainContext());
                ctx.putAll(resp);
                evt.setDomainContext(ctx);
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

    void writeTransactionHistory(GeneralEvent event) throws JsonProcessingException {
        var hist = new StateMachineTransactionHistoryEntity();
        hist.setId(ConcurrentUtil.uuidV7().toString());
        hist.setTransactionId(event.getCorrelationId());
        hist.setEventName(event.getEventName());
        hist.setUseCaseId(event.getUseCaseId());
        hist.setState(event.getState());
        hist.setPayload(event.getDomainContext());
        hist.setCreationTime(new Timestamp(System.currentTimeMillis()));
        hist.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        histRepo.save(hist);
    }

    Optional<Map<String, Object>> updateTransaction(GeneralEvent event, StateFlow item) throws JsonProcessingException {
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
                    objectMapper.writeValueAsString(event.getDomainContext()),
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

    void createTransaction(GeneralEvent event, StateFlow item) throws JsonProcessingException {
        var collectedStates = Map.of(event.getState(), true);
        var o = new StateMachineTransactionDetailEntity();
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

    void putConfigToMemory(StateMachineControlEntity useCase) {
        var details = useCase.getDetail();
        if (details != null && !details.isEmpty()) {
            for (var item : details) {
                var subscribeTopic = item.getSubscribeTopic();

                var configItem = config.computeIfAbsent(subscribeTopic, k -> new HashMap<>());

                var key = new UseCaseEventKey(useCase.getUseCaseId(), item.getFromState());
                var list = configItem.computeIfAbsent(key, k -> new LinkedList<>());
                list.add(item);

                if (StringUtils.isNotBlank(item.getStateFormula())) {
                    var p =StateUtil.parseFormulaForInput(item.getStateFormula());
                    var variables = p.getLeft();
                    for (var v : variables) {
                        key = new UseCaseEventKey(useCase.getUseCaseId(), v.trim());
                        list = configItem.computeIfAbsent(key, k -> new LinkedList<>());
                        list.add(item);
                    }
                }
            }
        }

    }

}
