package ai.demo.agent.base;

/**
 * Specialized exception for agent operations, providing context about
 * the agent's state during the error.
 */
public class AgentException extends Exception {

    private final String agentId;
    private final AgentState currentState;
    private final AgentState attemptedState;
    
    /**
     * Constructs a new agent exception with the specified detail message.
     * 
     * @param message the detail message
     */
    public AgentException(String message) {
        super(message);
        this.agentId = null;
        this.currentState = null;
        this.attemptedState = null;
    }
    
    /**
     * Constructs a new agent exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.agentId = null;
        this.currentState = null;
        this.attemptedState = null;
    }
    
    /**
     * Constructs a new agent exception with context about the agent's state.
     * 
     * @param message the detail message
     * @param agentId the ID of the agent that encountered the error
     * @param currentState the current state of the agent
     */
    public AgentException(String message, String agentId, AgentState currentState) {
        super(message);
        this.agentId = agentId;
        this.currentState = currentState;
        this.attemptedState = null;
    }
    
    /**
     * Constructs a new agent exception for state transition errors.
     * 
     * @param message the detail message
     * @param agentId the ID of the agent that encountered the error
     * @param currentState the current state of the agent
     * @param attemptedState the state the agent was trying to transition to
     */
    public AgentException(String message, String agentId, AgentState currentState, AgentState attemptedState) {
        super(message);
        this.agentId = agentId;
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }
    
    /**
     * Constructs a new agent exception with cause and state context.
     * 
     * @param message the detail message
     * @param cause the cause
     * @param agentId the ID of the agent that encountered the error
     * @param currentState the current state of the agent
     */
    public AgentException(String message, Throwable cause, String agentId, AgentState currentState) {
        super(message, cause);
        this.agentId = agentId;
        this.currentState = currentState;
        this.attemptedState = null;
    }
    
    /**
     * Check if this is a state transition error.
     *
     * @return true if this error occurred during a state transition
     */
    public boolean isStateTransitionError() {
        return attemptedState != null;
    }

    public String getAgentId() { return agentId; }
    public AgentState getCurrentState() { return currentState; }
    public AgentState getAttemptedState() { return attemptedState; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        
        if (agentId != null) {
            sb.append(" [Agent: ").append(agentId).append("]");
        }
        
        if (currentState != null) {
            sb.append(" [Current State: ").append(currentState).append("]");
        }
        
        if (attemptedState != null) {
            sb.append(" [Attempted State: ").append(attemptedState).append("]");
        }
        
        return sb.toString();
    }
}
