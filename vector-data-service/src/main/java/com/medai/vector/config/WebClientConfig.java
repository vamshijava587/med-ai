package com.medai.vector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient qdrantWebClient(WebClient.Builder builder, VectorServiceProperties properties) {
        return builder.baseUrl(properties.getQdrantUrl()).build();
    }
}
