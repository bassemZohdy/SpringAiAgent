package ai.demo.agent.base.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

    @Test
    void testStatusFlags() {
        assertTrue(TaskStatus.CREATED.isPending());
        assertTrue(TaskStatus.ASSIGNED.isPending());
        assertTrue(TaskStatus.PROCESSING.isActive());
        assertTrue(TaskStatus.COMPLETED.isTerminal());
        assertTrue(TaskStatus.FAILED.isTerminal());
        assertTrue(TaskStatus.CANCELLED.isTerminal());
        assertFalse(TaskStatus.CREATED.isTerminal());
    }

    @Test
    void testTransitionRules() {
        assertTrue(TaskStatus.CREATED.canTransitionTo(TaskStatus.ASSIGNED));
        assertTrue(TaskStatus.ASSIGNED.canTransitionTo(TaskStatus.PROCESSING));
        assertTrue(TaskStatus.PROCESSING.canTransitionTo(TaskStatus.COMPLETED));
        assertFalse(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.CREATED));
        assertFalse(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.PROCESSING));
    }

    @Test
    void testToStringContainsDescription() {
        String text = TaskStatus.PROCESSING.toString();
        assertTrue(text.contains("PROCESSING"));
        assertTrue(text.contains("currently"));
    }
}
