package ai.demo.springagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionMappingService.class);
    
    private final Map<String, String> threadToSessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToThreadMap = new ConcurrentHashMap<>();
    
    /**
     * Get or create a session ID for the given thread ID
     * @param threadId The thread ID from the request
     * @return The corresponding session ID for the model
     */
    public String getOrCreateSessionId(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            String sessionId = generateSessionId();
            logger.debug("Generated new session ID for null/empty thread: {}", sessionId);
            return sessionId;
        }
        
        String sessionId = threadToSessionMap.get(threadId);
        if (sessionId == null) {
            sessionId = generateSessionId();
            threadToSessionMap.put(threadId, sessionId);
            sessionToThreadMap.put(sessionId, threadId);
            logger.info("Created new session mapping: threadId={} -> sessionId={}", threadId, sessionId);
        } else {
            logger.debug("Retrieved existing session mapping: threadId={} -> sessionId={}", threadId, sessionId);
        }
        
        return sessionId;
    }
    
    /**
     * Get the thread ID for a given session ID
     * @param sessionId The session ID
     * @return The corresponding thread ID, or null if not found
     */
    public String getThreadId(String sessionId) {
        return sessionToThreadMap.get(sessionId);
    }
    
    /**
     * Get the session ID for a given thread ID
     * @param threadId The thread ID
     * @return The corresponding session ID, or null if not found
     */
    public String getSessionId(String threadId) {
        return threadToSessionMap.get(threadId);
    }
    
    /**
     * Remove the mapping for a thread ID
     * @param threadId The thread ID to remove
     * @return True if the mapping was removed, false if it didn't exist
     */
    public boolean removeThreadMapping(String threadId) {
        String sessionId = threadToSessionMap.remove(threadId);
        if (sessionId != null) {
            sessionToThreadMap.remove(sessionId);
            logger.info("Removed session mapping: threadId={} -> sessionId={}", threadId, sessionId);
            return true;
        }
        return false;
    }
    
    /**
     * Get all current mappings
     * @return A copy of the thread to session mappings
     */
    public Map<String, String> getAllMappings() {
        return Map.copyOf(threadToSessionMap);
    }
    
    /**
     * Clear all mappings
     */
    public void clearAllMappings() {
        int size = threadToSessionMap.size();
        threadToSessionMap.clear();
        sessionToThreadMap.clear();
        logger.info("Cleared all session mappings ({} entries)", size);
    }
    
    /**
     * Generate a unique session ID
     * @return A new session ID
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Get statistics about current mappings
     * @return Statistics as a formatted string
     */
    public String getStatistics() {
        int mappingCount = threadToSessionMap.size();
        return String.format("SessionMappingService: %d active mappings", mappingCount);
    }
}