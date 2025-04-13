package io.github.shangor.statemachine.service.impl;

import io.github.shangor.statemachine.dao.StatemachineControlEntity;
import io.github.shangor.statemachine.dao.StatemachineControlRepository;
import io.github.shangor.statemachine.service.StateMachineControlService;
import io.github.shangor.statemachine.util.ConcurrentUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class StateMachineControlServiceImpl implements StateMachineControlService {
    private final StatemachineControlRepository repo;
    @Override
    public Flux<StatemachineControlEntity> loadAll() {
        return Flux.create(sink -> ConcurrentUtil.unblockFlux(() -> {
            repo.findAll().forEach(sink::next);
            sink.complete();
        }));
    }
}
