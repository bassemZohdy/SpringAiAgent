package ai.demo.agent.chat;

import ai.demo.agent.base.Agent;
import ai.demo.agent.base.AgentConfiguration;
import ai.demo.agent.base.AgentException;
import ai.demo.agent.base.AgentMemory;
import ai.demo.agent.base.AgentState;
import ai.demo.agent.metrics.ChatAgentMetrics;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for chat-oriented agents with lifecycle, metrics and execution infrastructure.
 */
public abstract class BaseChatAgent<REQUEST, RESPONSE> implements ChatAgent<REQUEST, RESPONSE>, Agent {

    private final String agentId;
    private final String agentName;
    private final String version;
    private final Instant createdAt;
    private final AgentConfiguration configuration;
    private final List<String> capabilities;

    private final AtomicReference<AgentState> state = new AtomicReference<>(AgentState.CREATED);
    private final Object stateLock = new Object();

    private volatile ExecutorService executor;
    private final ChatAgentMetrics metrics;
    private final AgentMemory memory;

    protected BaseChatAgent(String agentName, String version, AgentConfiguration configuration,
                            List<String> capabilities) {
        this.agentId = UUID.randomUUID().toString();
        this.agentName = Objects.requireNonNull(agentName);
        this.version = Objects.requireNonNull(version);
        this.createdAt = Instant.now();
        this.configuration = Objects.requireNonNull(configuration);
        this.capabilities = List.copyOf(Objects.requireNonNull(capabilities));
        this.metrics = new ChatAgentMetrics();
        this.memory = new AgentMemory();
    }

    protected BaseChatAgent(String agentName, String version, List<String> capabilities) {
        this(agentName, version, AgentConfiguration.defaultConfiguration(), capabilities);
    }

    protected abstract RESPONSE doChat(REQUEST request) throws Exception;

    @Override
    public CompletableFuture<RESPONSE> chat(REQUEST request) {
        if (!isRunning()) {
            return CompletableFuture.failedFuture(new AgentException("Agent is not running", agentId, state.get()));
        }
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            metrics.recordOperationStarted();
            onConversationStarted(request);
            try {
                RESPONSE result = doChat(request);
                long processingTime = System.nanoTime() - startTime;
                // Token estimates are left to concrete classes; record basic message stats
                metrics.recordMessageProcessed(processingTime, 0, 0, true);
                onConversationCompleted(request, result);
                // Treat each chat call as a single message exchange
                metrics.recordConversationCompleted(1, 1, 1);
                return result;
            } catch (Exception e) {
                long processingTime = System.nanoTime() - startTime;
                metrics.recordMessageProcessingFailed(processingTime, 0, true);
                onConversationFailed(request, e);
                metrics.recordConversationAbandoned();
                throw new RuntimeException("Chat processing failed", e);
            }
        }, executor);
    }

    // Agent identity
    @Override public String getAgentId() { return agentId; }
    @Override public String getAgentName() { return agentName; }
    @Override public String getVersion() { return version; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public String getInstructions() { return configuration.getInstructions(); }
    @Override public List<String> getCapabilities() { return capabilities; }
    @Override public AgentConfiguration getConfiguration() { return configuration; }
    @Override public AgentState getState() { return state.get(); }
    @Override public boolean isRunning() { return state.get() == AgentState.STARTED; }
    @Override public boolean isPaused() { return state.get() == AgentState.PAUSED; }

    // Lifecycle
    @Override
    public void start() throws AgentException {
        synchronized (stateLock) {
            if (state.get() == AgentState.STARTED) return;
            state.set(AgentState.STARTING);
            try {
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "ChatAgent-" + agentName + "-" + agentId.substring(0, 8));
                        t.setDaemon(true);
                        return t;
                    });
                }
                state.set(AgentState.STARTED);
            } catch (Exception e) {
                state.set(AgentState.ERROR);
                throw new AgentException("Failed to start agent: " + e.getMessage(), e, agentId, state.get());
            }
        }
    }

    @Override
    public void pause() throws AgentException {
        synchronized (stateLock) {
            if (state.get() != AgentState.STARTED) return;
            state.set(AgentState.PAUSING);
            state.set(AgentState.PAUSED);
        }
    }

    @Override
    public void stop() throws AgentException {
        synchronized (stateLock) {
            if (state.get() == AgentState.STOPPED) return;
            state.set(AgentState.STOPPING);
            try {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                    executor.awaitTermination(configuration.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
                }
                state.set(AgentState.STOPPED);
            } catch (Exception e) {
                state.set(AgentState.ERROR);
                throw new AgentException("Failed to stop agent: " + e.getMessage(), e, agentId, state.get());
            }
        }
    }

    @Override
    public void reset() throws AgentException {
        synchronized (stateLock) {
            try {
                metrics.reset();
                memory.clear();
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
                executor = null;
                state.set(AgentState.CREATED);
            } catch (Exception e) {
                state.set(AgentState.ERROR);
                throw new AgentException("Failed to reset agent: " + e.getMessage(), e, agentId, state.get());
            }
        }
    }

    // Metrics and memory
    @Override public ChatAgentMetrics getMetrics() { return metrics; }
    @Override public AgentMemory getMemory() { return memory; }
    @Override public void clearMemory() { memory.clear(); }
    @Override public void compactMemory() { memory.compact(); }

    // Utility for tests
    protected ExecutorService getExecutor() { return executor; }

    // Convenience getters (used by tests)
    public long getConversationsAbandoned() { return metrics.getConversationsAbandoned(); }
}
