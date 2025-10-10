package ai.demo.agent.base;

/**
 * Agent lifecycle states for state machine management.
 */
public enum AgentState {
    
    CREATED("Agent has been created but not started"),
    STARTING("Agent is starting up"),
    STARTED("Agent is running and can process tasks"),
    PAUSING("Agent is pausing"),
    PAUSED("Agent is paused and not processing tasks"),
    STOPPING("Agent is stopping"),
    STOPPED("Agent has been stopped"),
    ERROR("Agent is in an error state"),
    RESETTING("Agent is being reset");
    
    private final String description;
    
    AgentState(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canProcessTasks() {
        return this == STARTED;
    }
    
    public boolean isTransitional() {
        return this == STARTING || this == PAUSING || this == STOPPING || this == RESETTING;
    }
    
    public boolean isTerminal() {
        return this == STOPPED || this == ERROR;
    }
    
    public boolean canTransitionTo(AgentState targetState) {
        switch (this) {
            case CREATED:
                return targetState == STARTING || targetState == ERROR;
            case STARTING:
                return targetState == STARTED || targetState == ERROR;
            case STARTED:
                return targetState == PAUSING || targetState == STOPPING || targetState == ERROR;
            case PAUSING:
                return targetState == PAUSED || targetState == ERROR;
            case PAUSED:
                return targetState == STARTING || targetState == STOPPING || targetState == ERROR;
            case STOPPING:
                return targetState == STOPPED || targetState == ERROR;
            case STOPPED:
                return targetState == RESETTING || targetState == ERROR;
            case ERROR:
                return targetState == RESETTING;
            case RESETTING:
                return targetState == CREATED || targetState == ERROR;
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return name() + ": " + description;
    }
}
