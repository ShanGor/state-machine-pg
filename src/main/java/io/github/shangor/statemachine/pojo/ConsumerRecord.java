package io.github.shangor.statemachine.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ConsumerRecord<T,V> {
    private String id;
    private T topic;
    private V value;
}
