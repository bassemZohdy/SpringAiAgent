package ai.demo.agent.base;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentMemoryTest {

    @Test
    void testRecordAndRetrieveEntries() {
        AgentMemory memory = new AgentMemory(5);
        assertTrue(memory.isEmpty());

        memory.recordExecution("task-1", "result-1", true, 10, "learning-1");
        memory.recordExecution("task-2", "result-2", false, 20, "learning-2");

        assertEquals(2, memory.size());
        assertFalse(memory.isEmpty());

        List<AgentMemory.MemoryEntry> entries = memory.getEntries();
        assertEquals(2, entries.size());

        AgentMemory.MemoryEntry first = entries.get(0);
        assertTrue(first.isSuccess());
        assertEquals("task-1", first.getTaskDescription());
        assertEquals("result-1", first.getResultDescription());

        AgentMemory.MemoryEntry second = entries.get(entries.size() - 1);
        assertFalse(second.isSuccess());
        assertEquals("learning-2", second.getLearnings());

        assertEquals(2, memory.getRecentEntries(5).size());
        assertEquals(1, memory.getRecentEntries(1).size());
    }

    @Test
    void testFindEntriesAndStats() {
        AgentMemory memory = new AgentMemory(10);
        memory.recordExecution("Alpha task", "result", true, 10, "learning alpha");
        memory.recordExecution("Beta task", "result", false, 20, "beta error");
        memory.recordExecution("Gamma", "result", true, 30, null);

        assertEquals(2, memory.findEntries("task").size());
        assertEquals(1, memory.findEntries("error").size());
        assertTrue(memory.findEntries("missing").isEmpty());

        AgentMemory.MemoryStats stats = memory.getStats();
        assertEquals(3, stats.getTotalEntries());
        assertEquals(2, stats.getSuccessfulTasks());
        assertEquals(1, stats.getFailedTasks());
        assertTrue(stats.getAverageProcessingTimeNanos() > 0);
        assertFalse(stats.hasSummary());
    }

    @Test
    void testCompactAndClear() {
        AgentMemory memory = new AgentMemory(6);
        assertEquals(10, memory.getMaxCapacity());
        for (int i = 0; i < 8; i++) {
            memory.recordExecution("task-" + i, "result-" + i, i % 2 == 0, i, null);
        }

        assertTrue(memory.size() <= memory.getMaxCapacity());
        assertNotNull(memory.toString());

        memory.setSummary("Summary text");
        memory.compact();
        assertTrue(memory.size() <= memory.getMaxCapacity());
        assertEquals("Summary text", memory.getSummary());

        memory.clear();
        assertTrue(memory.isEmpty());
        assertNull(memory.getSummary());
    }
}
