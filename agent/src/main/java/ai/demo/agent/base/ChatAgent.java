package ai.demo.agent.base;

import java.util.concurrent.CompletableFuture;

/**
 * Agent specialization focused on conversational interactions with end users.
 *
 * <p>ChatAgents are designed for interactive, dialogue-based interactions where the
 * context flows naturally between turns. Unlike TaskAgents which handle discrete
 * units of work, ChatAgents maintain conversational context and state.
 *
 * <p>Examples: customer service bots, personal assistants, tutors, interviewers,
 * role-playing agents, interactive guides, etc.
 *
 * @param <REQUEST>  the inbound chat message or payload type
 * @param <RESPONSE> the response payload type
 */
public interface ChatAgent<REQUEST, RESPONSE> extends Agent {

    /**
     * Process a conversational request and return a response asynchronously.
     *
     * <p>Unlike task processing, chat interactions are typically stateful and may
     * depend on conversation history, context, and session information. The agent
     * should maintain appropriate conversational state across multiple calls.
     *
     * @param request the chat message or conversational input (must not be null)
     * @return CompletableFuture that completes with the response or fails with an exception
     * @throws IllegalArgumentException if request is null
     * @throws AgentException if agent is not in a state to handle conversations
     */
    CompletableFuture<RESPONSE> chat(REQUEST request);

    /**
     * Called when a new conversation session starts.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * session initialization, context setup, or greeting logic.
     *
     * @param request the initial request that started the conversation
     */
    default void onConversationStarted(REQUEST request) {
        // Default implementation does nothing
    }

    /**
     * Called when a conversational turn completes successfully.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * context updates, conversation logging, or turn-specific processing.
     *
     * @param request  the user's request that was processed
     * @param response the agent's response
     */
    default void onConversationCompleted(REQUEST request, RESPONSE response) {
        // Default implementation does nothing
    }

    /**
     * Called when a conversational turn fails during processing.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * error recovery, fallback responses, or conversation error handling.
     *
     * @param request the user's request that failed
     * @param error   the exception that caused the failure
     */
    default void onConversationFailed(REQUEST request, Throwable error) {
        // Default implementation does nothing
    }
}
