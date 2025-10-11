package ai.demo.agent.task;

import ai.demo.agent.base.Agent;
import ai.demo.agent.metrics.TaskAgentMetrics;
import ai.demo.agent.base.task.Task;

import java.util.concurrent.CompletableFuture;

public interface TaskAgent<TASK extends Task, RESULT> extends Agent {

    CompletableFuture<RESULT> process(TASK task);

    default void onTaskStarted(TASK task) {}

    default void onTaskCompleted(TASK task, RESULT result) {}

    default void onTaskFailed(TASK task, Throwable error) {}

    @Override
    TaskAgentMetrics getMetrics();
}