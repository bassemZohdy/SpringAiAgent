package ai.demo.springagent.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads variables from a .env.local (or .env) file into Spring Environment for local dev.
 * Priority: very early so values are available to configuration binding.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Only apply in dev profile to avoid surprising prod behavior
        if (!environment.acceptsProfiles(p -> p.test("dev"))) {
            return;
        }

        File envFile = findEnvFile();
        if (envFile == null || !envFile.isFile()) {
            return;
        }

        Map<String, Object> values = parseEnvFile(envFile);
        if (values.isEmpty()) {
            return;
        }

        // Add as lowest precedence so real env vars still win
        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        environment.getPropertySources().addLast(source);
    }

    private File findEnvFile() {
        // Search current and parent directories for .env.local, then .env
        String[] candidates = new String[]{
                ".env.local",
                "../.env.local",
                "../../.env.local",
                ".env",
                "../.env",
                "../../.env"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        return null;
    }

    private Map<String, Object> parseEnvFile(File file) {
        Map<String, Object> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int idx = trimmed.indexOf('=');
                if (idx <= 0) continue;
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                // Strip optional surrounding quotes
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

