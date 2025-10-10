package ai.demo.agent.base.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskSizeTest {

    @Test
    void testSizeComparisons() {
        assertTrue(TaskSize.MEDIUM.isLargerThan(TaskSize.SMALL));
        assertFalse(TaskSize.SMALL.isLargerThan(TaskSize.EXTRA_LARGE));
        assertTrue(TaskSize.LARGE.shouldConsiderSplitting());
        assertEquals(2, TaskSize.MEDIUM.getProcessingWeight());
        assertEquals(8, TaskSize.EXTRA_LARGE.getProcessingWeight());
        assertEquals(TaskSize.MEDIUM, TaskSize.getDefault());
    }

    @Test
    void testToStringContainsDescription() {
        String text = TaskSize.EXTRA_LARGE.toString();
        assertTrue(text.contains("EXTRA_LARGE"));
        assertTrue(text.contains("extensive"));
    }
}
