package ai.demo.agent.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AgentMetrics {

    private final AtomicLong operationsProcessed = new AtomicLong(0);
    private final AtomicLong operationsSucceeded = new AtomicLong(0);
    private final AtomicLong operationsFailed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
    private final AtomicLong minProcessingTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong(0);
    private final AtomicReference<Instant> lastOperationStartTime = new AtomicReference<>();
    private final AtomicReference<Instant> lastOperationEndTime = new AtomicReference<>();
    private final Instant agentStartTime;

    protected AgentMetrics() {
        this.agentStartTime = Instant.now();
    }

    public void recordOperationStarted() {
        lastOperationStartTime.set(Instant.now());
    }

    public void recordOperationSucceeded(long processingTimeNanos) {
        operationsProcessed.incrementAndGet();
        operationsSucceeded.incrementAndGet();
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinMaxProcessingTime(processingTimeNanos);
        lastOperationEndTime.set(Instant.now());
    }

    public void recordOperationFailed(long processingTimeNanos) {
        operationsProcessed.incrementAndGet();
        operationsFailed.incrementAndGet();
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinMaxProcessingTime(processingTimeNanos);
        lastOperationEndTime.set(Instant.now());
    }

    private void updateMinMaxProcessingTime(long processingTimeNanos) {
        minProcessingTimeNanos.accumulateAndGet(processingTimeNanos, Long::min);
        maxProcessingTimeNanos.accumulateAndGet(processingTimeNanos, Long::max);
    }

    public long getOperationsProcessed() {
        return operationsProcessed.get();
    }

    public long getOperationsSucceeded() {
        return operationsSucceeded.get();
    }

    public long getOperationsFailed() {
        return operationsFailed.get();
    }

    public double getSuccessRate() {
        long total = operationsProcessed.get();
        return total > 0 ? (double) operationsSucceeded.get() / total : 0.0;
    }

    public double getFailureRate() {
        long total = operationsProcessed.get();
        return total > 0 ? (double) operationsFailed.get() / total : 0.0;
    }

    public Duration getAverageProcessingTime() {
        long total = operationsProcessed.get();
        if (total > 0) {
            long avgNanos = totalProcessingTimeNanos.get() / total;
            return Duration.ofNanos(avgNanos);
        }
        return Duration.ZERO;
    }

    public Duration getMinProcessingTime() {
        long min = minProcessingTimeNanos.get();
        return min == Long.MAX_VALUE ? Duration.ZERO : Duration.ofNanos(min);
    }

    public Duration getMaxProcessingTime() {
        return Duration.ofNanos(maxProcessingTimeNanos.get());
    }

    public Duration getTotalProcessingTime() {
        return Duration.ofNanos(totalProcessingTimeNanos.get());
    }

    public Instant getLastOperationStartTime() {
        return lastOperationStartTime.get();
    }

    public Instant getLastOperationEndTime() {
        return lastOperationEndTime.get();
    }

    public Duration getUptime() {
        return Duration.between(agentStartTime, Instant.now());
    }

    public double getThroughput() {
        long total = operationsProcessed.get();
        if (total > 0) {
            Duration uptime = getUptime();
            double uptimeSeconds = uptime.toMillis() / 1000.0;
            return uptimeSeconds > 0 ? total / uptimeSeconds : 0.0;
        }
        return 0.0;
    }

    public void reset() {
        operationsProcessed.set(0);
        operationsSucceeded.set(0);
        operationsFailed.set(0);
        totalProcessingTimeNanos.set(0);
        minProcessingTimeNanos.set(Long.MAX_VALUE);
        maxProcessingTimeNanos.set(0);
        lastOperationStartTime.set(null);
        lastOperationEndTime.set(null);
    }

    public String getSummary() {
        return String.format(
            "%s{processed=%d, succeeded=%d, failed=%d, successRate=%.2f%%, " +
            "avgTime=%s, minTime=%s, maxTime=%s, throughput=%.2f ops/sec, uptime=%s}",
            this.getClass().getSimpleName(),
            getOperationsProcessed(),
            getOperationsSucceeded(),
            getOperationsFailed(),
            getSuccessRate() * 100,
            getAverageProcessingTime(),
            getMinProcessingTime(),
            getMaxProcessingTime(),
            getThroughput(),
            getUptime()
        );
    }

    @Override
    public String toString() {
        return getSummary();
    }
}