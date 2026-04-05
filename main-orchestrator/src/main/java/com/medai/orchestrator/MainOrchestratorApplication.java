package com.medai.orchestrator;

import com.medai.orchestrator.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class MainOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainOrchestratorApplication.class, args);
    }
}
