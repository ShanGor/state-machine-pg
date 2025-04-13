package io.github.shangor.statemachine.dao;

import org.springframework.data.repository.CrudRepository;

public interface StatemachineTransactionHistoryRepository extends CrudRepository<StateMachineTransactionHistoryEntity, Long> {
}
