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
    event_type varchar(32) not null,
    state varchar(64),
	payload jsonb NULL,
	creation_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	last_update_time timestamp NULL DEFAULT CURRENT_TIMESTAMP
);
 CREATE INDEX sm_txn_hist_transaction_id_idx ON sm_txn_hist USING btree (transaction_id);

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
    private String eventType;
    private String state;
    private String payload;
    private Timestamp creationTime;
    private Timestamp lastUpdateTime;
}
