package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

/**
 * AI/LLM-based agent interface with transformation pipeline: TASK → PROMPT → CHAT_RESPONSE → RESULT
 *
 * <p>AiAgents bridge structured task processing with large language model interactions.
 * They handle the complex transformation between domain-specific tasks and LLM-compatible
 * prompts, then transform LLM responses back into structured results.
 *
 * <p>This interface defines a clear pipeline:
 * <ol>
 *   <li>Transform domain task into LLM prompt</li>
 *   <li>Execute LLM call via {@code call()} method</li>
 *   <li>Transform LLM response back into domain result</li>
 * </ol>
 *
 * <p>Examples: code review agents, document summarizers, content generators,
 * data analysis assistants, translation services, etc.
 *
 * @param <TASK>          the domain-specific task type (must extend Task)
 * @param <PROMPT>        the prompt type compatible with the LLM
 * @param <CHAT_RESPONSE> the raw response type from the LLM
 * @param <RESULT>        the domain-specific result type
 */
public interface AiAgent<TASK extends Task, PROMPT, CHAT_RESPONSE, RESULT> extends TaskAgent<TASK, RESULT> {

    /**
     * Transform a domain-specific task into an LLM-compatible prompt.
     *
     * <p>This method handles the critical task-to-prompt transformation, including
     * formatting instructions, context injection, and prompt engineering.
     *
     * @param task the domain task to transform (must not be null)
     * @return prompt suitable for LLM processing
     * @throws IllegalArgumentException if task is null
     * @throws AiTransformationException if transformation fails
     */
    PROMPT transformToPrompt(TASK task);

    /**
     * Transform a raw LLM response back into a domain-specific result.
     *
     * <p>This method handles response parsing, validation, error handling,
     * and conversion from the LLM's native format to the expected domain format.
     *
     * @param response the raw response from the LLM (must not be null)
     * @return structured domain result
     * @throws IllegalArgumentException if response is null
     * @throws AiTransformationException if transformation fails
     */
    RESULT transformFromResponse(CHAT_RESPONSE response);

    /**
     * Execute the LLM call with the given prompt.
     *
     * <p>This method must be implemented by concrete agent classes to handle
     * the actual LLM interaction, including API calls, error handling, retries,
     * and response processing.
     *
     * <p>The default implementation throws UnsupportedOperationException to ensure
     * that concrete classes provide their own implementation.
     *
     * @param prompt the formatted prompt for the LLM (must not be null)
     * @return raw response from the LLM
     * @throws IllegalArgumentException if prompt is null
     * @throws AiExecutionException if LLM call fails
     */
    default CHAT_RESPONSE call(PROMPT prompt) {
        throw new UnsupportedOperationException(
            "call() method must be implemented by concrete agent classes to handle LLM interactions"
        );
    }

    /**
     * Creates a default implementation of the AI transformation pipeline.
     *
     * <p>This utility method orchestrates the full pipeline:
     * task → prompt → LLM call → response → result. Classes can use this
     * method in their {@link BaseAgent#doProcess(Task)} implementation.
     *
     * @param task the task to process
     * @return result of the AI pipeline processing
     * @throws RuntimeException if any step in the pipeline fails
     */
    default RESULT processWithAiPipeline(TASK task) {
        try {
            // Step 1: Transform task to prompt
            PROMPT prompt = transformToPrompt(task);

            // Step 2: Execute LLM call
            CHAT_RESPONSE response = call(prompt);

            // Step 3: Transform response back to result
            return transformFromResponse(response);

        } catch (Exception e) {
            throw new RuntimeException("AI pipeline processing failed", e);
        }
    }
}
