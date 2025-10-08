package ai.demo.springagent.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("provider")
public class ProviderHealthIndicator implements HealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(ProviderHealthIndicator.class);

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Override
    public Health health() {
        try {
            // Try a quick GET /models OpenAI-compatible endpoint
            String modelsUrl = baseUrl.endsWith("/") ? baseUrl + "models" : baseUrl + "/models";
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(modelsUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET();
            if (!apiKey.isBlank()) {
                b.header("Authorization", "Bearer " + apiKey);
            }
            HttpRequest req = b.build();
            HttpResponse<Void> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() >= 200 && resp.statusCode() < 500) {
                return Health.up()
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("status", resp.statusCode())
                        .build();
            }
            return Health.down()
                    .withDetail("baseUrl", baseUrl)
                    .withDetail("status", resp.statusCode())
                    .build();
        } catch (Exception e) {
            log.debug("Provider health check failed", e);
            return Health.down(e)
                    .withDetail("baseUrl", baseUrl)
                    .build();
        }
    }
}

