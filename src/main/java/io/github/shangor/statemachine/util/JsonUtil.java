package io.github.shangor.statemachine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JsonUtil {
    private static ObjectMapper objectMapper = null;

    public JsonUtil(ObjectMapper objectMapper) {
        JsonUtil.objectMapper = objectMapper;
    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            throw new RuntimeException("ObjectMapper is not initialized!");
        }
        return objectMapper;
    }
}
