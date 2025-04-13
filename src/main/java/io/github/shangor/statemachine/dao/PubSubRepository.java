package io.github.shangor.statemachine.dao;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PubSubRepository extends CrudRepository<PubSubEntity, String> {
    @Modifying
    @Query(value = "update sm_pub_sub set processed = 'Y' where id = ?1", nativeQuery = true)
    void acknowledge(String eventId);
}
