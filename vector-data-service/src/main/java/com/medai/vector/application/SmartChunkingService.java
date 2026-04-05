package com.medai.vector.application;

import com.medai.shared.api.DocumentChunkRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SmartChunkingService {

    public List<ChunkedDocument> chunk(DocumentChunkRequest request) {
        var tokens = tokenize(request.content());
        var frequencies = frequencyMap(tokens);
        var chunks = new ArrayList<ChunkedDocument>();
        var size = Math.max(20, request.chunkSizeTokens());
        var overlap = Math.max(0, request.overlapTokens());
        int chunkIndex = 0;
        for (int start = 0; start < tokens.size(); start += Math.max(1, size - overlap)) {
            int end = Math.min(tokens.size(), start + size);
            var window = tokens.subList(start, end);
            if (window.isEmpty()) {
                continue;
            }
            var content = String.join(" ", window);
            var frequencyScore = window.stream()
                .mapToDouble(token -> frequencies.getOrDefault(token, 0))
                .sum();
            chunks.add(new ChunkedDocument(
                request.sourceId() + "-" + chunkIndex,
                content,
                chunkIndex,
                frequencyScore,
                request.metadata()));
            chunkIndex++;
            if (end == tokens.size()) {
                break;
            }
        }
        return chunks;
    }

    private List<String> tokenize(String content) {
        return java.util.Arrays.stream(content.split("\\s+"))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .toList();
    }

    private Map<String, Integer> frequencyMap(List<String> tokens) {
        var frequencies = new HashMap<String, Integer>();
        for (var token : tokens) {
            frequencies.merge(token.toLowerCase(), 1, Integer::sum);
        }
        return frequencies;
    }

    public record ChunkedDocument(
        String id,
        String content,
        int chunkIndex,
        double frequencyScore,
        Map<String, Object> metadata
    ) {
    }
}
