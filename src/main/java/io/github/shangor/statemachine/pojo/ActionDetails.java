package io.github.shangor.statemachine.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.shangor.statemachine.util.JsonUtil;
import jakarta.persistence.AttributeConverter;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
public class ActionDetails {
    private String publishTopic;
    private String subscribeTopic;
    private ApiCallConfig apiConfig;

    @Slf4j
    public static class ActionDetailsConverter implements AttributeConverter<ActionDetails, String> {
        @Override
        public String convertToDatabaseColumn(ActionDetails actionDetails) {
            try {
                return JsonUtil.getObjectMapper().writeValueAsString(actionDetails);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to database column: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        @Override
        public ActionDetails convertToEntityAttribute(String s) {
            try {
                return JsonUtil.getObjectMapper().readValue(s, ActionDetails.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert to entity attribute: {}!", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
