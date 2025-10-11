package ai.demo.agent.chat;

import ai.demo.agent.base.Agent;
import ai.demo.agent.metrics.ChatAgentMetrics;

import java.util.concurrent.CompletableFuture;

public interface ChatAgent<REQUEST, RESPONSE> extends Agent {

    CompletableFuture<RESPONSE> chat(REQUEST request);

    default void onConversationStarted(REQUEST request) {}

    default void onConversationCompleted(REQUEST request, RESPONSE response) {}

    default void onConversationFailed(REQUEST request, Throwable error) {}

    @Override
    ChatAgentMetrics getMetrics();
}