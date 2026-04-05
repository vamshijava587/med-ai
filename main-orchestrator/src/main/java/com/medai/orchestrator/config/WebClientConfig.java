package com.medai.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient vectorServiceWebClient(WebClient.Builder builder, AppProperties properties) {
        return builder.baseUrl(properties.getVector().getBaseUrl()).build();
    }
}
