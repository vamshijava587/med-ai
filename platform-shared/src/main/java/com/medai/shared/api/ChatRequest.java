package com.medai.shared.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ChatRequest(
    @NotBlank String userId,
    @NotBlank String sessionId,
    @NotBlank String message,
    @NotNull Map<String, Object> metadata
) {
    public ChatRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
