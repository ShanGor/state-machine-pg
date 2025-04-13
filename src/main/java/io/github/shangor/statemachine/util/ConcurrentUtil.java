package io.github.shangor.statemachine.util;


import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ConcurrentUtil {
    private static final TimeBasedEpochGenerator uuidGenerator = Generators.timeBasedEpochGenerator();
    /**
     * Currently the virtual thread got some issue, let's create this unified interface, so we can change the implementation later when virtual thread is matured.
     */
    public static void unblockFlux(Runnable sinkRunnable) {
        CompletableFuture.runAsync(sinkRunnable);
    }

    public static void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable);
    }

    /**
     * Mono and Flux is not supposed to be blocked, to avoid problem, we need to block it in a new thread. Here we play a trick to use virtual thread to get the result.
     * Only valid in Java 19+;
     */
    public static <T> T block(Mono<T> mono) {
        var ref = new AtomicReference<T>();
        try {
            CompletableFuture.runAsync(() -> {
                ref.set(mono.block());
            }).join();
            return ref.get();
        } catch (Exception e) {
            log.error("monoBlock error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static UUID uuidV7() {
        return uuidGenerator.generate();
    }
}
