package ai.demo.agent.base.task;

/**
 * Task lifecycle states for processing management.
 */
public enum TaskStatus {
    
    CREATED("Task has been created but not yet assigned"),
    ASSIGNED("Task has been assigned to an agent"),
    PROCESSING("Task is currently being processed"),
    COMPLETED("Task has been completed successfully"),
    FAILED("Task processing failed"),
    CANCELLED("Task was cancelled");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    public boolean isActive() {
        return this == PROCESSING;
    }
    
    public boolean isPending() {
        return this == CREATED || this == ASSIGNED;
    }
    
    public boolean canTransitionTo(TaskStatus target) {
        switch (this) {
            case CREATED:
                return target == ASSIGNED || target == CANCELLED;
            case ASSIGNED:
                return target == PROCESSING || target == CANCELLED;
            case PROCESSING:
                return target == COMPLETED || target == FAILED || target == CANCELLED;
            case COMPLETED:
            case FAILED:
            case CANCELLED:
                return false;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
