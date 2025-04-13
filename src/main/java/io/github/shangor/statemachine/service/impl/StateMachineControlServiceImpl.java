package io.github.shangor.statemachine.service.impl;

import io.github.shangor.statemachine.dao.StateMachineControlEntity;
import io.github.shangor.statemachine.dao.StateMachineControlRepository;
import io.github.shangor.statemachine.service.StateMachineControlService;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class StateMachineControlServiceImpl implements StateMachineControlService {
    private final StateMachineControlRepository repo;
    @Override
    public Flux<StateMachineControlEntity> loadAll() {
        return Flux.create(sink -> ConcurrentUtil.unblockFlux(() -> {
            repo.findAll().forEach(sink::next);
            sink.complete();
        }));
    }
}
