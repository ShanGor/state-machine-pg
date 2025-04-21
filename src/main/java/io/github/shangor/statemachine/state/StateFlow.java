package io.github.shangor.statemachine.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.shangor.statemachine.pojo.ApiCallConfig;
import io.github.shangor.statemachine.util.JsonUtil;
import jakarta.persistence.AttributeConverter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Data
public class StateFlow {

    private boolean isFirst;
    private String nodeId;
    private String nodeName;
    private Node.Type nodeType;
    /**
     * The action registered in the action registry. Only required for ACTION and HUMAN nodes.
     */
    private String actionName;
    private Map<String, Object> config;
    private String fromState;
    /**
     * success by default
     */
    private String toState;
    /**
     * some other states like failure, timeout, cancelled, etc.
     */
    private List<String> otherStates;
    private String stateFormula;
    private String subscribeTopic;
    private String publishTopic;
    private ApiCallConfig apiCallConfig;

    @Slf4j
    public static class JsonConverter implements AttributeConverter<List<StateFlow>, String> {

        private static final TypeReference<List<StateFlow>> MAP_TYPE_REFERENCE = new TypeReference<>() {};
        @Override
        public String convertToDatabaseColumn(List<StateFlow> stateFlows) {
            try {
                return JsonUtil.getObjectMapper().writeValueAsString(stateFlows);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to database column: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<StateFlow> convertToEntityAttribute(String s) {
            try {
                return JsonUtil.getObjectMapper().readValue(s, MAP_TYPE_REFERENCE);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to entity attribute: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
