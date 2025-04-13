package io.github.shangor.statemachine.dao;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Map;

/**
 * CREATE TABLE sm_txn_hist (
    id varchar(37) not null,
    use_case_id varchar(37) NOT NULL,
    transaction_id varchar(37) not NULL,
	event_name varchar(128) not null,
    state varchar(64),
	payload jsonb NULL,
	creation_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	last_update_time timestamp NULL DEFAULT CURRENT_TIMESTAMP
);
 */
@Table(name = "sm_txn_hist")
@Entity
@Data
public class StateMachineTransactionHistoryEntity {
    @Id
    private String id;
    private String useCaseId;
    private String transactionId;
    private String eventName;
    private String state;
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> payload;
    private Timestamp creationTime;
    private Timestamp lastUpdateTime;
}
