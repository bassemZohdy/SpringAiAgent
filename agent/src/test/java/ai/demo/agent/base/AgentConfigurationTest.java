package ai.demo.agent.base;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

public class AgentConfigurationTest {

    @Test
    public void testDefaultConfiguration() {
        AgentConfiguration config = AgentConfiguration.defaultConfiguration();
        
        assertNotNull(config);
        assertTrue(config.isMetricsEnabled());
        assertEquals(Duration.ofMinutes(5), config.getTaskTimeout());
        assertEquals(Duration.ofSeconds(30), config.getShutdownTimeout());
        assertEquals(1, config.getMaxConcurrentTasks());
    }

    @Test
    public void testBuilderPattern() {
        AgentConfiguration config = AgentConfiguration.builder()
            .instructions("Test instructions")
            .taskTimeout(Duration.ofSeconds(10))
            .maxConcurrentTasks(5)
            .enableMetrics(false)
            .property("test.key", "test.value")
            .build();
        
        assertEquals("Test instructions", config.getInstructions());
        assertEquals(Duration.ofSeconds(10), config.getTaskTimeout());
        assertEquals(5, config.getMaxConcurrentTasks());
        assertFalse(config.isMetricsEnabled());
        assertEquals("test.value", config.getProperty("test.key"));
    }

    @Test
    public void testPropertyAccess() {
        AgentConfiguration config = AgentConfiguration.builder()
            .property("string.key", "string.value")
            .property("integer.key", 42)
            .build();
        
        assertEquals("string.value", config.getProperty("string.key", "default"));
        assertEquals(42, config.getProperty("integer.key", 0));
        assertEquals("default", config.getProperty("missing.key", "default"));
        
        assertTrue(config.getProperty("string.key", String.class).isPresent());
        assertTrue(config.getProperty("integer.key", Integer.class).isPresent());
        assertFalse(config.getProperty("missing.key", String.class).isPresent());
    }
}