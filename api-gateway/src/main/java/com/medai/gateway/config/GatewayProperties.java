package com.medai.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gateway-specific properties.
 * JWT validation is now delegated to auth-service via RemoteAuthJwtDecoder.
 * The auth.service.* properties are bound directly via @Value in RemoteAuthJwtDecoder.
 */
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private String orchestratorUrl = "http://localhost:8081";

    public String getOrchestratorUrl() { return orchestratorUrl; }
    public void setOrchestratorUrl(String orchestratorUrl) { this.orchestratorUrl = orchestratorUrl; }
}
