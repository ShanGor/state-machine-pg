package io.github.shangor.statemachine.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public class HttpUtil {
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_CALLBACK_POD_IP = "X-Callback-Pod-IP";

    public static Optional<String> tryGetHeader(String headerName, MultiValueMap<String, String> headers) {
        var value = headers.getFirst(headerName);
        if (value == null) {
            value = headers.getFirst(headerName.toLowerCase(Locale.ROOT));
            if (value == null) {
                value = headers.getFirst(headerName.toUpperCase(Locale.ROOT));
            }
        }
        if (value != null) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    /**
     * TODO: retry and replay logic;
     * @param url
     * @param body
     * @param async
     * @return
     */
    public static Optional<String> post(String url, String body, boolean async) {
        try {
            var resp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                    .send(HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .uri(URI.create(url)).build(),
                            HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                if (async) {
                    log.info("Successfully issued async request to {}", url);
                } else {
                    log.info("Successfully called API {}, will process the result soon..", url);
                    return Optional.of(resp.body());
                }
            } else {
                log.error("Failed to send request to {}: {} {}", url, resp.statusCode(), resp.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to get response from {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }
}
