package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

/**
 * AI/LLM-based agent interface with transformation pipeline: TASK → PROMPT → CHAT_RESPONSE → RESULT
 */
public interface AiAgent<TASK extends Task, PROMPT, CHAT_RESPONSE, RESULT> extends TaskAgent<TASK, RESULT> {
    
    PROMPT transformToPrompt(TASK task);
    RESULT transformFromResponse(CHAT_RESPONSE response);
    
    default CHAT_RESPONSE call(PROMPT prompt) {
        throw new UnsupportedOperationException("call() method must be implemented by concrete agent classes");
    }
}
