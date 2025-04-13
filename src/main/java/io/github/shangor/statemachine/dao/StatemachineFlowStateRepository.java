package io.github.shangor.statemachine.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StatemachineFlowStateRepository extends CrudRepository<StatemachineFlowStateEntity, String> {
    @Query("select s from StatemachineFlowStateEntity s where s.id.transactionId = ?1")
    List<StatemachineFlowStateEntity> findByTransactionId(String txnId);
}
