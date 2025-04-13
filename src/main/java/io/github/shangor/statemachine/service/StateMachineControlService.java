
package io.github.shangor.statemachine.service;

import io.github.shangor.statemachine.dao.StatemachineControlEntity;
import reactor.core.publisher.Flux;


public interface StateMachineControlService {
    Flux<StatemachineControlEntity> loadAll();
}
