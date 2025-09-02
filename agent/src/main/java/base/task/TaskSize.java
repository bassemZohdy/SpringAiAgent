package ai.demo.agent.base.task;

/**
 * Task complexity/size classification for resource planning and splitting decisions.
 */
public enum TaskSize {
    
    SMALL("Small task - quick completion, minimal resources"),
    MEDIUM("Medium task - moderate complexity and resources"),
    LARGE("Large task - complex processing, significant resources"),
    EXTRA_LARGE("Extra-large task - extensive processing, may need splitting");
    
    private final String description;
    
    TaskSize(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isLargerThan(TaskSize other) {
        return this.ordinal() > other.ordinal();
    }
    
    public boolean shouldConsiderSplitting() {
        return this == LARGE || this == EXTRA_LARGE;
    }
    
    public int getProcessingWeight() {
        switch (this) {
            case SMALL:
                return 1;
            case MEDIUM:
                return 2;
            case LARGE:
                return 4;
            case EXTRA_LARGE:
                return 8;
            default:
                return 1;
        }
    }
    
    public static TaskSize getDefault() {
        return MEDIUM;
    }
    
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}