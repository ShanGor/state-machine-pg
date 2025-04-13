package io.github.shangor.statemachine.service;

import io.github.shangor.statemachine.pojo.ConsumerRecord;

import java.util.List;
import java.util.Set;

public interface PubSubService {
    void subscribe(List<String> topics);
    void publishEvent(String topic, String eventId, String event);
    List<ConsumerRecord<String, String>> poll();
    List<ConsumerRecord<String, String>> poll(List<String> topics);
    List<ConsumerRecord<String, String>> poll(Set<String> topics);

    void acknowledge(String topic, String eventId);
}
