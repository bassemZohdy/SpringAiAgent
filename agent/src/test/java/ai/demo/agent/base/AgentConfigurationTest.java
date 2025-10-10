package ai.demo.agent.base;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

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
            .properties(Map.of("extra.key", 123))
            .build();

        assertEquals("Test instructions", config.getInstructions());
        assertEquals(Duration.ofSeconds(10), config.getTaskTimeout());
        assertEquals(5, config.getMaxConcurrentTasks());
        assertFalse(config.isMetricsEnabled());
        assertEquals("test.value", config.getProperty("test.key"));
        assertEquals(123, config.getProperty("extra.key", 0));
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

    @Test
    public void testPropertyTypeMismatchAndRemoval() {
        AgentConfiguration config = AgentConfiguration.builder()
            .property("string.key", "value")
            .property("string.key", null) // removal
            .property("number.key", 10)
            .build();

        assertNull(config.getProperty("string.key"));
        assertEquals(10, config.getProperty("number.key", 0));
        assertEquals("fallback", config.getProperty("number.key", "fallback"));
        assertNull(config.getProperty("missing", (String) null));
    }
}
