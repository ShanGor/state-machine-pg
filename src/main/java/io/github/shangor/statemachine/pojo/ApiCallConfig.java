
package io.github.shangor.statemachine.pojo;

import lombok.Data;

import java.util.Map;

@Data
public class ApiCallConfig {
    private String url;
    private String method;
    private String body;
    private Map<String, String> headers;
    private boolean async;
    /**
     * if 0 means no need to retry. every 5 seconds retry once
     */
    private int retryTimes;
    /**
     * if 0 means no need to replay. every 5 minutes replay once
     */
    private int replayTimes;
}
