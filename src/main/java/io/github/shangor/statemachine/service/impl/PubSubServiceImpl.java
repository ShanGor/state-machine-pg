package io.github.shangor.statemachine.service.impl;

import io.github.shangor.statemachine.dao.PubSubEntity;
import io.github.shangor.statemachine.dao.PubSubRepository;
import io.github.shangor.statemachine.exception.MachineStopping;
import io.github.shangor.statemachine.pojo.ConsumerRecord;
import io.github.shangor.statemachine.service.PubSubService;
import io.github.shangor.statemachine.task.MainFlowTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PubSubServiceImpl implements PubSubService {
    private final PubSubRepository repo;
    private static final String SQL_LOOP = "select * from sm_pub_sub where processed='N' and topic=ANY(?) limit 20 for update skip locked";

    private static final Set<String> topics = new HashSet<>();

    private final DataSource dataSource;
    private static final MachineStopping machineStopping = new MachineStopping("Statemachine is stopping");

    @Override
    public void subscribe(List<String> topics) {
        PubSubServiceImpl.topics.addAll(topics);
    }

    @Override
    @Transactional
    public void publishEvent(String topic, String eventId, String event) {
        var o = new PubSubEntity();
        o.setId(eventId);
        o.setTopic(topic);
        o.setData(event);
        o.setProcessed("N");
        repo.save(o);
    }

    @Override
    public List<ConsumerRecord<String, String>> poll() {
        return poll(topics);
    }

    @Override
    public List<ConsumerRecord<String, String>> poll(List<String> topics) {
        return poll(new HashSet<>(topics));
    }

    @Override
    public List<ConsumerRecord<String, String>> poll(Set<String> topics) {
        List<ConsumerRecord<String, String>> result = new LinkedList<>();
        try(var conn = dataSource.getConnection()) {
            var autoCommit = conn.getAutoCommit();
            var readOnly = conn.isReadOnly();
            try {
                conn.setAutoCommit(false);
                conn.setReadOnly(false);
                conn.beginRequest();
                try(var stmt = conn.prepareStatement(SQL_LOOP);
                    var updateStmt = conn.prepareStatement("update sm_pub_sub set processed='F' where id=?")) {
                    stmt.setArray(1, conn.createArrayOf("VARCHAR", topics.toArray()));
                    if (MainFlowTask.stopping) {
                        throw machineStopping;
                    }

                    var rs = stmt.executeQuery();
                    var found = false;
                    while(rs.next()) {
                        var id = rs.getString("id");
                        var topic = rs.getString("topic");
                        var data = rs.getString("data");
                        if (topics.contains(topic)) {

                            result.add(ConsumerRecord.<String, String>builder().id(id).topic(topic).value(data).build());
                            updateStmt.setString(1, id);
                            updateStmt.addBatch();
                            found = true;
                        }
                    }

                    if (found) {
                        if (MainFlowTask.stopping) {
                            result.clear();
                            throw machineStopping;
                        }

                        updateStmt.executeBatch();
                    }

                } catch (MachineStopping e) {
                    log.info("Machine stopping..");
                } finally {
                    conn.endRequest();
                }

            } finally {
                if (!conn.isClosed()) {
                    conn.setAutoCommit(autoCommit);
                    conn.setReadOnly(readOnly);
                }
            }
            if (result.isEmpty()) {
                Thread.sleep(100);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("InterruptedException: {}", e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional
    public void acknowledge(String topic, String eventId) {
        repo.acknowledge(eventId);
    }
}
