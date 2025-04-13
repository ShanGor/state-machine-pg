package io.github.shangor.statemachine.dao;

import io.github.shangor.statemachine.pojo.ActionDetails;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.Map;

/**
 * CREATE TABLE sm_txn_detail (
	id varchar(37) NOT NULL,
	use_case_id varchar(37) NOT NULL,
	current_state varchar(64) NULL,
	collected_states jsonb NULL,
	action_details jsonb NULL,
	domain_context jsonb NULL,
	creation_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	last_update_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT sm_txn_detail_pk PRIMARY KEY (id)
);
 */

@Table(name = "sm_txn_detail")
@Data
@Entity
public class StatemachineTransactionDetailEntity {
    @Id
    private String id;
    private String useCaseId;
    private String currentState;
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Boolean> collectedStates;
    @Convert(converter = ActionDetails.ActionDetailsConverter.class)
    private ActionDetails actionDetails;
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> domainContext;
    @CreationTimestamp
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;
}
