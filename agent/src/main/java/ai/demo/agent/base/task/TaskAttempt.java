package ai.demo.agent.base.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * A single task processing attempt with timing, results, and learnings.
 */
public interface TaskAttempt {
    
    String getId();
    int getNumber();
    Instant getStartTime();
    Instant getEndTime();
    Duration getDuration();
    boolean isSuccessful();
    boolean isInProgress();
    String getResult();
    String getErrorMessage();
    String getExceptionDetails();
    String getLearnings();
    Map<String, Object> getMetadata();
    String getAgentId();
    String getAgentName();
    String getProcessingContext();
}
