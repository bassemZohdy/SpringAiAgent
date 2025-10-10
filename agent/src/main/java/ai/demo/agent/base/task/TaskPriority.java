package ai.demo.agent.base.task;

/**
 * Task priority levels for scheduling and resource allocation.
 */
public enum TaskPriority {
    
    LOW("Low priority - can be deferred"),
    NORMAL("Normal priority - standard processing"),
    HIGH("High priority - expedited processing"),
    CRITICAL("Critical priority - immediate processing required");
    
    private final String description;
    
    TaskPriority(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isHigherThan(TaskPriority other) {
        return this.ordinal() > other.ordinal();
    }
    
    public static TaskPriority getDefault() {
        return NORMAL;
    }
    
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
