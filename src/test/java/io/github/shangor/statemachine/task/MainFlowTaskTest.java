package io.github.shangor.statemachine.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.shangor.statemachine.dao.StatemachineControlEntity;
import io.github.shangor.statemachine.dao.StatemachineFlowStateRepository;
import io.github.shangor.statemachine.dao.StatemachineTransactionDetailRepository;
import io.github.shangor.statemachine.dao.StatemachineTransactionHistoryRepository;
import io.github.shangor.statemachine.event.GeneralEvent;
import io.github.shangor.statemachine.pojo.ConsumerRecord;
import io.github.shangor.statemachine.service.PubSubService;
import io.github.shangor.statemachine.service.StateMachineControlService;
import io.github.shangor.statemachine.state.ActionHandlers;
import io.github.shangor.statemachine.state.StateFlow;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainFlowTaskTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StateMachineControlService stateMachineControlService;

    @Mock
    private PubSubService pubSubService;

    @Mock
    private StatemachineTransactionHistoryRepository histRepo;

    @Mock
    private StatemachineTransactionDetailRepository txnRepo;

    private MainFlowTask mainFlowTask;

    @Resource
    private ActionHandlers actionHandlers;

    @Mock
    private StatemachineFlowStateRepository flowStateRepo;



    @BeforeEach
    void setUp() {
        try(var m = MockitoAnnotations.openMocks(this)) {
            mainFlowTask = new MainFlowTask(objectMapper, stateMachineControlService, jdbcTemplate, txnRepo, histRepo, pubSubService, actionHandlers, flowStateRepo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testFromEventAndItem() {
        // Setup
        GeneralEvent event = new GeneralEvent();
        event.setCorrelationId("123");
        event.setUseCaseId("1");
        event.setState("START");
        event.setDomainContext(new HashMap<>());
        event.setUseCaseName("TestUseCase");

        var item = new StateFlow();
        item.setToState("END");
        item.setNodeId("1");
        item.setFirst(true);
        item.setNodeName("TestUseCase");

        // Execute
        GeneralEvent result = mainFlowTask.fromEventAndItem(event, item);

        // Verify
        assertNotNull(result);
        assertEquals("END", result.getState());
        assertEquals("1", result.getNodeId());
        assertEquals("123", result.getCorrelationId());
        assertEquals("1", result.getUseCaseId());
        assertEquals("TestUseCase", result.getUseCaseName());
        assertEquals("TestUseCase.END", result.getEventName());
        assertEquals(GeneralEvent.EventType.ACTION_STARTED, result.getEventType());
        assertEquals(event.getDomainContext(), result.getDomainContext());
    }

    @Test
    void testInit() throws InterruptedException {
        var item = new StatemachineControlEntity();
        item.setUseCaseName("hello");
        when(stateMachineControlService.loadAll()).thenReturn(Flux.just(item));
        MainFlowTask.stopping = true;
        mainFlowTask.init();
        TimeUnit.SECONDS.sleep(1);

        assertTrue(MainFlowTask.isConfigInitialized);
    }


    @Test
    void testProcessMessage_WithInvalidMessage() throws Exception {
        // Setup
        String topic = "test_topic";
        String payload = "invalid_json";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("1", topic, payload);

        when(objectMapper.readValue(payload, GeneralEvent.class)).thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Execute & Verify
        assertThrows(RuntimeException.class, () -> mainFlowTask.processMessage(record));
    }

}
