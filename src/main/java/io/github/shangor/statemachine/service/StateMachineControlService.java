
package io.github.shangor.statemachine.service;

import io.github.shangor.statemachine.dao.StateMachineControlEntity;
import reactor.core.publisher.Flux;


public interface StateMachineControlService {
    Flux<StateMachineControlEntity> loadAll();
}
