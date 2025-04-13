package io.github.shangor.statemachine.dao;

import io.github.shangor.statemachine.state.StateFlow;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

/**
 * CREATE TABLE sm_control (
	use_case_id varchar(37) NOT NULL,
	use_case_name varchar(64) NULL,
	detail jsonb NULL,
	creation_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	last_update_time timestamp NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT sm_control_pk PRIMARY KEY (use_case_id)
);
 */
@Data
@Table(name = "sm_control")
@Entity
public class StatemachineControlEntity {
    @Id
    private String useCaseId;
    private String useCaseName;
    @Convert(converter = StateFlow.JsonConverter.class)
    private List<StateFlow> detail;



    @CreationTimestamp
    private Timestamp creationTime;

    @UpdateTimestamp
    private Timestamp lastUpdateTime;



}
