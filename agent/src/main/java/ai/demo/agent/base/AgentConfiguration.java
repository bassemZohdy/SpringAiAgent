package ai.demo.agent.base;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration container for agent settings and properties.
 * Provides type-safe access to configuration values with defaults.
 */
public class AgentConfiguration {
    
    private final Map<String, Object> properties;
    private final String instructions;
    private final Duration taskTimeout;
    private final int maxConcurrentTasks;
    private final boolean enableMetrics;
    private final Duration shutdownTimeout;
    
    private AgentConfiguration(Builder builder) {
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        this.instructions = builder.instructions;
        this.taskTimeout = builder.taskTimeout;
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.enableMetrics = builder.enableMetrics;
        this.shutdownTimeout = builder.shutdownTimeout;
    }
    
    /**
     * Get all configuration properties.
     * 
     * @return immutable map of properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * Get a property value.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Get a property value with type safety.
     * 
     * @param key the property key
     * @param type the expected type
     * @param <T> the type parameter
     * @return optional containing the typed value, or empty if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Get a property value with a default fallback.
     * 
     * @param key the property key
     * @param defaultValue the default value if not found
     * @param <T> the type parameter
     * @return the property value or default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (defaultValue != null && !defaultValue.getClass().isInstance(value)) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }
    
    /**
     * Get the agent instructions.
     * 
     * @return the agent instructions, or null if not set
     */
    public String getInstructions() {
        return instructions;
    }
    
    /**
     * Get the task timeout duration.
     * 
     * @return the task timeout
     */
    public Duration getTaskTimeout() {
        return taskTimeout;
    }
    
    /**
     * Get the maximum number of concurrent tasks (for future use).
     * 
     * @return the max concurrent tasks
     */
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
    
    /**
     * Check if metrics collection is enabled.
     * 
     * @return true if metrics are enabled
     */
    public boolean isMetricsEnabled() {
        return enableMetrics;
    }
    
    /**
     * Get the shutdown timeout duration.
     * 
     * @return the shutdown timeout
     */
    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }
    
    @Override
    public String toString() {
        return String.format("AgentConfiguration{properties=%d, instructions='%s', taskTimeout=%s, maxConcurrentTasks=%d, enableMetrics=%s, shutdownTimeout=%s}",
                properties.size(), instructions, taskTimeout, maxConcurrentTasks, enableMetrics, shutdownTimeout);
    }
    
    /**
     * Create a new builder for agent configuration.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a default configuration.
     * 
     * @return default agent configuration
     */
    public static AgentConfiguration defaultConfiguration() {
        return builder().build();
    }
    
    /**
     * Builder for creating agent configurations.
     */
    public static class Builder {
        private final Map<String, Object> properties = new HashMap<>();
        private String instructions;
        private Duration taskTimeout = Duration.ofMinutes(5);
        private int maxConcurrentTasks = 1;
        private boolean enableMetrics = true;
        private Duration shutdownTimeout = Duration.ofSeconds(30);
        
        /**
         * Set a custom property.
         * 
         * @param key the property key
         * @param value the property value
         * @return this builder
         */
        public Builder property(String key, Object value) {
            if (key != null) {
                if (value != null) {
                    properties.put(key, value);
                } else {
                    properties.remove(key);
                }
            }
            return this;
        }
        
        /**
         * Set multiple properties at once.
         * 
         * @param properties the properties to set
         * @return this builder
         */
        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }
        
        /**
         * Set the agent instructions.
         * 
         * @param instructions the agent instructions
         * @return this builder
         */
        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }
        
        /**
         * Set the task timeout.
         * 
         * @param taskTimeout the task timeout duration
         * @return this builder
         */
        public Builder taskTimeout(Duration taskTimeout) {
            this.taskTimeout = Objects.requireNonNull(taskTimeout, "Task timeout cannot be null");
            return this;
        }
        
        /**
         * Set the maximum concurrent tasks.
         * 
         * @param maxConcurrentTasks the maximum concurrent tasks
         * @return this builder
         */
        public Builder maxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = Math.max(1, maxConcurrentTasks);
            return this;
        }
        
        /**
         * Enable or disable metrics collection.
         * 
         * @param enableMetrics true to enable metrics
         * @return this builder
         */
        public Builder enableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
            return this;
        }
        
        /**
         * Set the shutdown timeout.
         * 
         * @param shutdownTimeout the shutdown timeout duration
         * @return this builder
         */
        public Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "Shutdown timeout cannot be null");
            return this;
        }
        
        /**
         * Build the agent configuration.
         * 
         * @return the configured agent configuration
         */
        public AgentConfiguration build() {
            return new AgentConfiguration(this);
        }
    }
}
