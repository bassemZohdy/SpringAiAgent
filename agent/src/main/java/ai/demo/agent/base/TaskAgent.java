package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

import java.util.concurrent.CompletableFuture;

/**
 * Agent specialization that manages discrete tasks and produces results.
 *
 * @param <TASK>   the type of task/input handled by the agent
 * @param <RESULT> the type of output produced by the agent
 */
public interface TaskAgent<TASK extends Task, RESULT> extends Agent {

    CompletableFuture<RESULT> process(TASK task);

    default void onTaskStarted(TASK task) {
    }

    default void onTaskCompleted(TASK task, RESULT result) {
    }

    default void onTaskFailed(TASK task, Throwable error) {
    }
}
