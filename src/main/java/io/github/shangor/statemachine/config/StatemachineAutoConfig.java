package io.github.shangor.statemachine.config;

import io.github.shangor.statemachine.state.ActionHandlers;
import io.github.shangor.statemachine.state.ActionNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
@Slf4j
public class StatemachineAutoConfig {
    @Bean
    @ConditionalOnMissingBean(ActionHandlers.class)
    public ActionHandlers defaultActionHandlers() {
        var ah =  new ActionHandlers();
        ah.registerActionHandler("test", new ActionNode() {
            @Override
            public Map<String, Object> action(Param input) {
                log.info("Test Agent: {}", input.getConfig());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("InterruptedException: {}", e.getMessage());
                }

                return input.getContext();
            }
        });
        return ah;
    }
}
