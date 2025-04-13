package io.github.shangor.statemachine.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

/**
 *
 * CREATE TABLE sm_pub_sub (
 * 	 topic varchar(127) NOT NULL,
 * 	 "data" text not NULL,
 * 	 processed char(1) not NULL,
 * 	 creation_time timestamp null default NOW(),
 * 	 last_update_time timestamp null default NOW(),
 * 	 id varchar(37) not NULL
 * );
 *
 * CREATE INDEX sm_pub_sub_id_idx ON sm_pub_sub (id);
 * CREATE INDEX sm_pub_sub_processed_idx ON sm_pub_sub (processed);
 */
@Entity
@Table(name = "sm_pub_sub")
@Data
public class PubSubEntity {
    @Id
    private String id;
    private String topic;
    private String data;
    private String processed; // N/Y/F, F for in the flight

    @CreationTimestamp
    private Timestamp creationTime;
    @UpdateTimestamp
    private Timestamp lastUpdateTime;


}
