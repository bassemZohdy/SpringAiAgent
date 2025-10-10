package ai.demo.agent.base.task;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A task that can be processed by agents with metadata, hierarchy, and attempt tracking.
 */
public interface Task {
    
    String getId();
    Instant getCreatedAt();
    String getDescription();
    TaskPriority getPriority();
    TaskStatus getStatus();
    TaskSize getSize();
    String getCompletionCriteria();
    double getProgress();
    Map<String, Object> getMetadata();
    Task getParent();
    List<Task> getSubTasks();
    List<TaskAttempt> getAttempts();
    int getMaxAttempts();
    TaskAttempt getCurrentAttempt();
    
    default boolean hasSubTasks() {
        return !getSubTasks().isEmpty();
    }
    
    default boolean isRoot() {
        return getParent() == null;
    }
    
    default int getAttemptCount() {
        return getAttempts().size();
    }
    
    default boolean hasRemainingAttempts() {
        return getAttemptCount() < getMaxAttempts();
    }
    
    default TaskAttempt getLastAttempt() {
        List<TaskAttempt> attempts = getAttempts();
        return attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
    }
    
    default boolean isCompleted() {
        return getStatus().isTerminal();
    }
    
    default boolean isInProgress() {
        return getStatus().isActive();
    }
    
    default boolean isPending() {
        return getStatus().isPending();
    }
}
