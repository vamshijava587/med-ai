package com.medai.shared.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

public record DocumentChunkRequest(
    @NotBlank String sourceId,
    @NotBlank String content,
    @Positive int chunkSizeTokens,
    @PositiveOrZero int overlapTokens,
    Map<String, Object> metadata
) {
    public DocumentChunkRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
