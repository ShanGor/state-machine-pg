package io.github.shangor.statemachine.dao;

import org.springframework.data.repository.CrudRepository;

public interface StateMachineTransactionHistoryRepository extends CrudRepository<StateMachineTransactionHistoryEntity, Long> {
}
