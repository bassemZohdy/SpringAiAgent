package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

import java.util.concurrent.CompletableFuture;

/**
 * Agent specialization that manages discrete tasks and produces results.
 *
 * <p>TaskAgents are designed for structured, goal-oriented work where each input
 * represents a discrete unit of work with well-defined completion criteria.
 *
 * <p>Examples: data processing tasks, automation workflows, document analysis,
 * code generation, file conversions, etc.
 *
 * @param <TASK>   the type of task/input handled by the agent (must extend Task)
 * @param <RESULT> the type of output produced by the agent
 */
public interface TaskAgent<TASK extends Task, RESULT> extends Agent {

    /**
     * Process a single task and return the result asynchronously.
     *
     * <p>The task will be processed according to the agent's current state and
     * configuration. The returned CompletableFuture will complete when the task
     * is finished, either successfully or with an exception.
     *
     * @param task the task to process (must not be null)
     * @return CompletableFuture that completes with the result or fails with an exception
     * @throws IllegalArgumentException if task is null
     * @throws AgentException if agent is not in a state to process tasks
     */
    CompletableFuture<RESULT> process(TASK task);

    /**
     * Called when a task begins processing.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * setup operations, logging, or custom tracking.
     *
     * @param task the task that started processing
     */
    default void onTaskStarted(TASK task) {
        // Default implementation does nothing
    }

    /**
     * Called when a task completes successfully.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * cleanup operations, result post-processing, or custom event handling.
     *
     * @param task   the task that completed
     * @param result the result produced by the task
     */
    default void onTaskCompleted(TASK task, RESULT result) {
        // Default implementation does nothing
    }

    /**
     * Called when a task fails during processing.
     *
     * <p>Default implementation does nothing. Subclasses can override to perform
     * error logging, recovery operations, or custom error handling.
     *
     * @param task  the task that failed
     * @param error the exception that caused the failure
     */
    default void onTaskFailed(TASK task, Throwable error) {
        // Default implementation does nothing
    }
}
