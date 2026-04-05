package com.medai.vector.application;

import com.medai.shared.api.DocumentChunkRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartChunkingServiceTest {

    private final SmartChunkingService smartChunkingService = new SmartChunkingService();

    @Test
    void shouldCreateMultipleChunksForLongDocument() {
        var content = "alpha ".repeat(300) + "beta ".repeat(50);
        var request = new DocumentChunkRequest("doc-1", content, 100, 20, Map.of("source", "unit-test"));

        var chunks = smartChunkingService.chunk(request);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.getFirst().metadata()).containsEntry("source", "unit-test");
    }
}
