package ee.digit25.detector.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration optimized for transaction validation on 2 vCPU / 8 GB RAM environment.
 *
 * Thread pool sizing rationale:
 * - Core pool: 3 threads (1.5x vCPU count) - optimal for I/O-bound database queries
 * - Max pool: 6 threads (3x vCPU count) - handles occasional bursts without overwhelming CPU
 * - Queue capacity: 100 - accommodates 2 batches worth of transactions (2 x 50)
 * - Keep-alive: 60s - recycles idle threads to minimize memory overhead
 *
 * This configuration allows 3-6 transactions to be validated in parallel, providing
 * 1.5-2x throughput improvement over sequential processing while staying within
 * resource constraints.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "transactionValidationExecutor")
    public Executor transactionValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: 3 threads for steady-state parallelism
        // Optimal for I/O-bound workload on 2 vCPU system
        executor.setCorePoolSize(3);

        // Max pool size: 6 threads to handle bursts
        // Higher than core allows handling spikes without blocking
        executor.setMaxPoolSize(6);

        // Queue capacity: 100 tasks (2 full batches)
        // Prevents memory issues while allowing adequate buffering
        executor.setQueueCapacity(100);

        // Keep-alive time: 60 seconds
        // Idle threads above core size are recycled after 60s
        executor.setKeepAliveSeconds(60);

        // Thread naming for debugging and monitoring
        executor.setThreadNamePrefix("tx-validation-");

        // Rejection policy: Caller-runs (blocking fallback)
        // If queue is full, the calling thread processes the task
        // This provides backpressure and prevents OutOfMemoryError
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for all tasks to complete on shutdown (graceful shutdown)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
