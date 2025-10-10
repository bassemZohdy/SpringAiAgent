package ai.demo.springagent.config;

import ai.demo.agent.base.AgentConfiguration;
import ai.demo.springagent.agent.ChatCompletionAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration class for setting up agent framework beans.
 *
 * <p>This configuration creates the necessary beans for the new agent abstraction
 * framework, including ChatCompletionAgent instances and their configuration.
 */
@Configuration
public class AgentBeanConfiguration {

    /**
     * Create the default agent configuration.
     *
     * @param aiModelConfig AI model configuration
     * @param env Spring environment
     * @return configured AgentConfiguration
     */
    @Bean
    public AgentConfiguration defaultAgentConfiguration(Environment env) {
        String instructions = env.getProperty("agent.instructions",
                "You are a helpful AI assistant. Provide clear, accurate, and thoughtful responses.");

        return AgentConfiguration.builder()
                .instructions(instructions)
                .maxConcurrentTasks(1)
                .taskTimeout(java.time.Duration.ofSeconds(30))
                .shutdownTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Create the main ChatCompletionAgent bean.
     *
     * @param chatClient Spring AI ChatClient
     * @param agentConfig agent configuration
     * @return configured ChatCompletionAgent
     */
    @Bean
    @ConditionalOnProperty(name = "agent.enabled", havingValue = "true", matchIfMissing = true)
    public ChatCompletionAgent chatCompletionAgent(ChatClient chatClient, AgentConfiguration agentConfig) {
        return new ChatCompletionAgent(chatClient, agentConfig);
    }

    /**
     * Create a secondary agent for different use cases.
     * This demonstrates how to create multiple agents with different configurations.
     *
     * @param chatClient Spring AI ChatClient
     * @return specialized ChatCompletionAgent
     */
    @Bean
    @ConditionalOnProperty(name = "agent.specialized.enabled", havingValue = "true")
    public ChatCompletionAgent specializedChatAgent(ChatClient chatClient) {
        AgentConfiguration specializedConfig = AgentConfiguration.builder()
                .instructions("You are a specialized AI assistant focused on code analysis and debugging. " +
                             "Provide detailed technical explanations and help with programming tasks.")
                .maxConcurrentTasks(1)
                .taskTimeout(java.time.Duration.ofSeconds(45))
                .build();

        return new ChatCompletionAgent(chatClient, specializedConfig);
    }

    /**
     * Configuration for agent-specific settings.
     */
    @Configuration
    @ConditionalOnProperty(name = "agent.enabled", havingValue = "true", matchIfMissing = true)
    public static class AgentSettingsConfiguration {

        /**
         * Enable agent metrics collection.
         */
        @Bean
        public AgentMetricsCollector agentMetricsCollector() {
            return new AgentMetricsCollector();
        }

        /**
         * Enable agent memory management.
         */
        @Bean
        public AgentMemoryManager agentMemoryManager() {
            return new AgentMemoryManager();
        }
    }

    /**
     * Component for collecting and reporting agent metrics.
     */
    public static class AgentMetricsCollector {
        // Implementation for metrics collection
        // This could integrate with Micrometer, Prometheus, etc.
    }

    /**
     * Component for managing agent memory optimization.
     */
    public static class AgentMemoryManager {
        // Implementation for memory management
        // This could handle periodic memory compaction, cleanup, etc.
    }
}