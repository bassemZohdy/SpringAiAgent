package ai.demo.springagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAgentApplication.class, args);
    }
}