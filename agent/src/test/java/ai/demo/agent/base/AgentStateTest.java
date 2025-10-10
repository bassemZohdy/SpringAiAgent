package ai.demo.agent.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentStateTest {

    @Test
    void testStateDescriptions() {
        assertEquals("Agent has been created but not started", AgentState.CREATED.getDescription());
        assertTrue(AgentState.STARTING.isTransitional());
        assertTrue(AgentState.STOPPED.isTerminal());
        assertFalse(AgentState.STARTED.isTerminal());
        assertTrue(AgentState.STARTED.canProcessTasks());
        assertFalse(AgentState.PAUSED.canProcessTasks());
        assertTrue(AgentState.RESETTING.isTransitional());
        assertTrue(AgentState.ERROR.isTerminal());
    }

    @Test
    void testValidTransitions() {
        assertTrue(AgentState.CREATED.canTransitionTo(AgentState.STARTING));
        assertTrue(AgentState.STARTING.canTransitionTo(AgentState.STARTED));
        assertTrue(AgentState.STARTED.canTransitionTo(AgentState.PAUSING));
        assertTrue(AgentState.PAUSED.canTransitionTo(AgentState.STARTING));
        assertTrue(AgentState.PAUSED.canTransitionTo(AgentState.STOPPING));
        assertTrue(AgentState.STOPPED.canTransitionTo(AgentState.RESETTING));
        assertTrue(AgentState.ERROR.canTransitionTo(AgentState.RESETTING));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(AgentState.STARTED.canTransitionTo(AgentState.CREATED));
        assertFalse(AgentState.STOPPED.canTransitionTo(AgentState.STARTED));
        assertFalse(AgentState.ERROR.canTransitionTo(AgentState.STARTED));
    }

    @Test
    void testToStringIncludesDescription() {
        String text = AgentState.STARTED.toString();
        assertTrue(text.contains("STARTED"));
        assertTrue(text.contains("Agent is running"));
    }
}
