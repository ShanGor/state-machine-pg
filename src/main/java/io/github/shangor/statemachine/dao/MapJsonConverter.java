package io.github.shangor.statemachine.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.shangor.statemachine.util.JsonUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Converter
public class MapJsonConverter implements AttributeConverter<Map<String, Object>, String> {
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {};
    @Override
    public String convertToDatabaseColumn(Map<String, Object> stringObjectMap) {
        try {
            return JsonUtil.getObjectMapper().writeValueAsString(stringObjectMap);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert to database column: {}!", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String s) {
        try {
            return JsonUtil.getObjectMapper().readValue(s, MAP_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert to entity attribute: {}!", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
