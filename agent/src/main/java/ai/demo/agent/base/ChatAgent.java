package ai.demo.agent.base;

import java.util.concurrent.CompletableFuture;

/**
 * Agent specialization focused on conversational interactions with end users.
 *
 * @param <REQUEST>  the inbound chat message or payload type
 * @param <RESPONSE> the response payload type
 */
public interface ChatAgent<REQUEST, RESPONSE> extends Agent {

    CompletableFuture<RESPONSE> chat(REQUEST request);

    default void onConversationStarted(REQUEST request) {
    }

    default void onConversationCompleted(REQUEST request, RESPONSE response) {
    }

    default void onConversationFailed(REQUEST request, Throwable error) {
    }
}
