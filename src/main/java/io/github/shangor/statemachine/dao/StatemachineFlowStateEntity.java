package io.github.shangor.statemachine.dao;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 CREATE TABLE sm_flow_status (
 	txn_id varchar(37) NOT NULL,
    use_case_id varchar(37) NOT NULL,
 	node_id varchar(37) NOT NULL,
 	state varchar(64) NULL,
 	context jsonb NULL,
 	creation_time timestamp DEFAULT NOW() NULL,
 	last_update_time timestamp DEFAULT NOW() NULL
 );

 CREATE INDEX sm_flow_status_txn_id_idx ON sm_flow_status (txn_id);
 CREATE UNIQUE INDEX sm_flow_status_unique_id_idx ON sm_flow_status (txn_id, use_case_id, node_id);
 */
@Entity
@Table(name = "sm_flow_status")
@Data
public class StatemachineFlowStateEntity {
    @EmbeddedId
    private Id id;
    private String state;
    private String context;

    @Column(updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private Timestamp creationTime;

    @UpdateTimestamp
    private Timestamp lastUpdateTime;

    @Data
    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "txn_id")
        private String transactionId;
        private String useCaseId;
        private String nodeId;
    }
}
