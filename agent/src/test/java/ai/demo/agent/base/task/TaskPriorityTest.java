package ai.demo.agent.base.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskPriorityTest {

    @Test
    void testPriorityOrdering() {
        assertTrue(TaskPriority.HIGH.isHigherThan(TaskPriority.NORMAL));
        assertFalse(TaskPriority.LOW.isHigherThan(TaskPriority.CRITICAL));
        assertEquals(TaskPriority.NORMAL, TaskPriority.getDefault());
    }

    @Test
    void testToStringContainsDescription() {
        String text = TaskPriority.CRITICAL.toString();
        assertTrue(text.contains("CRITICAL"));
        assertTrue(text.contains("immediate"));
    }
}
