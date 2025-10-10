package ai.demo.agent.base;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Memory system for agents to store task execution outcomes, context, and learnings.
 * This is separate from AgentState which tracks lifecycle states.
 * Memory stores the agent's experience and can be compacted/summarized.
 */
public class AgentMemory {
    
    private final ConcurrentLinkedQueue<MemoryEntry> entries;
    private final AtomicLong nextEntryId;
    private final int maxEntries;
    private volatile String summary;
    
    /**
     * Create a new agent memory with default capacity.
     */
    public AgentMemory() {
        this(1000);
    }
    
    /**
     * Create a new agent memory with specified capacity.
     * 
     * @param maxEntries maximum number of entries to keep in memory
     */
    public AgentMemory(int maxEntries) {
        this.entries = new ConcurrentLinkedQueue<>();
        this.nextEntryId = new AtomicLong(1);
        this.maxEntries = Math.max(10, maxEntries);
        this.summary = null;
    }
    
    /**
     * Record a task execution outcome in memory.
     * 
     * @param task the task that was executed
     * @param result the result of the task execution (can be null if failed)
     * @param success whether the task execution was successful
     * @param processingTimeNanos the time taken to process the task
     * @param learnings any learnings or insights from this execution
     * @param <T> the task type
     * @param <R> the result type
     */
    public <T, R> void recordExecution(T task, R result, boolean success, 
                                      long processingTimeNanos, String learnings) {
        MemoryEntry entry = new MemoryEntry(
            nextEntryId.getAndIncrement(),
            Instant.now(),
            task != null ? task.toString() : "null",
            result != null ? result.toString() : "null",
            success,
            processingTimeNanos,
            learnings
        );
        
        entries.offer(entry);
        
        // Auto-compact if we exceed capacity
        if (entries.size() > maxEntries) {
            compact();
        }
    }
    
    /**
     * Get all memory entries in chronological order.
     * 
     * @return immutable list of memory entries
     */
    public List<MemoryEntry> getEntries() {
        return List.copyOf(entries);
    }
    
    /**
     * Get recent memory entries (last N entries).
     * 
     * @param limit maximum number of recent entries to return
     * @return list of recent memory entries
     */
    public List<MemoryEntry> getRecentEntries(int limit) {
        List<MemoryEntry> allEntries = getEntries();
        int size = allEntries.size();
        int startIndex = Math.max(0, size - limit);
        return allEntries.subList(startIndex, size);
    }
    
    /**
     * Get entries that match a specific pattern or contain keywords.
     * 
     * @param keyword keyword to search for in task or result descriptions
     * @return list of matching memory entries
     */
    public List<MemoryEntry> findEntries(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String searchTerm = keyword.toLowerCase();
        return getEntries().stream()
            .filter(entry -> entry.getTaskDescription().toLowerCase().contains(searchTerm) ||
                           entry.getResultDescription().toLowerCase().contains(searchTerm) ||
                           (entry.getLearnings() != null && 
                            entry.getLearnings().toLowerCase().contains(searchTerm)))
            .collect(Collectors.toList());
    }
    
    /**
     * Get the current memory summary.
     * 
     * @return the memory summary, or null if no summary has been created
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Set a summary of the agent's memory/experience.
     * This is typically generated during memory compacting.
     * 
     * @param summary the memory summary
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    /**
     * Get memory statistics.
     * 
     * @return memory statistics object
     */
    public MemoryStats getStats() {
        List<MemoryEntry> allEntries = getEntries();
        long successful = allEntries.stream().mapToLong(e -> e.isSuccess() ? 1 : 0).sum();
        long failed = allEntries.size() - successful;
        
        double avgProcessingTime = allEntries.stream()
            .mapToLong(MemoryEntry::getProcessingTimeNanos)
            .average()
            .orElse(0.0);
        
        return new MemoryStats(
            allEntries.size(),
            successful,
            failed,
            avgProcessingTime,
            summary != null
        );
    }
    
    /**
     * Compact memory by removing older entries and optionally creating a summary.
     * This keeps the most recent entries and creates space for new ones.
     */
    public void compact() {
        // Keep only the most recent 70% of entries
        int targetSize = Math.max(1, (int) (maxEntries * 0.7));
        
        List<MemoryEntry> allEntries = getEntries();
        if (allEntries.size() <= targetSize) {
            return; // No need to compact
        }
        
        // Clear all entries and add back the most recent ones
        entries.clear();
        List<MemoryEntry> recentEntries = new ArrayList<>(
            allEntries.subList(allEntries.size() - targetSize, allEntries.size())
        );
        entries.addAll(recentEntries);
    }
    
    /**
     * Clear all memory entries and summary.
     */
    public void clear() {
        entries.clear();
        summary = null;
    }
    
    /**
     * Get the current number of memory entries.
     * 
     * @return the number of entries in memory
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * Check if memory is empty.
     * 
     * @return true if memory contains no entries
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    /**
     * Get the maximum capacity of this memory.
     * 
     * @return the maximum number of entries this memory can hold
     */
    public int getMaxCapacity() {
        return maxEntries;
    }
    
    @Override
    public String toString() {
        return String.format("AgentMemory{entries=%d, maxCapacity=%d, hasSummary=%s}", 
                           entries.size(), maxEntries, summary != null);
    }
    
    /**
     * Individual memory entry representing a single task execution outcome.
     */
    @Getter
    public static class MemoryEntry {
        private final long id;
        private final Instant timestamp;
        private final String taskDescription;
        private final String resultDescription;
        private final boolean success;
        private final long processingTimeNanos;
        private final String learnings;

        public MemoryEntry(long id, Instant timestamp, String taskDescription,
                           String resultDescription, boolean success,
                           long processingTimeNanos, String learnings) {
            this.id = id;
            this.timestamp = timestamp;
            this.taskDescription = taskDescription;
            this.resultDescription = resultDescription;
            this.success = success;
            this.processingTimeNanos = processingTimeNanos;
            this.learnings = learnings;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryEntry{id=%d, timestamp=%s, success=%s, task='%s', result='%s'}", 
                               id, timestamp, success, taskDescription, resultDescription);
        }
    }
    
    /**
     * Statistics about the agent's memory.
     */
    @Getter
    public static class MemoryStats {
        private final int totalEntries;
        private final long successfulTasks;
        private final long failedTasks;
        private final double averageProcessingTimeNanos;
        private final boolean hasSummary;

        public MemoryStats(int totalEntries, long successfulTasks, long failedTasks,
                           double averageProcessingTimeNanos, boolean hasSummary) {
            this.totalEntries = totalEntries;
            this.successfulTasks = successfulTasks;
            this.failedTasks = failedTasks;
            this.averageProcessingTimeNanos = averageProcessingTimeNanos;
            this.hasSummary = hasSummary;
        }

        public boolean hasSummary() {
            return hasSummary;
        }

        public double getSuccessRate() {
            return totalEntries > 0 ? (double) successfulTasks / totalEntries : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{entries=%d, successful=%d, failed=%d, successRate=%.2f%%, hasSummary=%s}",
                               totalEntries, successfulTasks, failedTasks, getSuccessRate() * 100, hasSummary);
        }
    }
}
