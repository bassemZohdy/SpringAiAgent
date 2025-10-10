package ai.demo.agent.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentExceptionTest {

    @Test
    void testSimpleConstructors() {
        AgentException exception = new AgentException("Basic error");
        assertNull(exception.getAgentId());
        assertNull(exception.getCurrentState());
        assertNull(exception.getAttemptedState());
        assertFalse(exception.isStateTransitionError());
    }

    @Test
    void testStateAwareConstructors() {
        AgentException withState = new AgentException("Stateful", "agent-1", AgentState.STARTED);
        assertEquals("agent-1", withState.getAgentId());
        assertEquals(AgentState.STARTED, withState.getCurrentState());
        assertNull(withState.getAttemptedState());
        assertFalse(withState.isStateTransitionError());

        AgentException transition = new AgentException(
            "Transition", "agent-2", AgentState.CREATED, AgentState.STARTED
        );
        assertEquals("agent-2", transition.getAgentId());
        assertEquals(AgentState.CREATED, transition.getCurrentState());
        assertEquals(AgentState.STARTED, transition.getAttemptedState());
        assertTrue(transition.isStateTransitionError());
    }

    @Test
    void testToStringContainsContext() {
        AgentException exception = new AgentException(
            "Failure", new IllegalStateException("boom"), "agent-3", AgentState.ERROR
        );
        String text = exception.toString();
        assertTrue(text.contains("agent-3"));
        assertTrue(text.contains("ERROR"));
    }
}
