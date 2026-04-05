package com.medai.vector.application;

import com.medai.shared.api.RetrievedContext;
import com.medai.vector.config.VectorServiceProperties;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class QdrantClientService {

    private final WebClient qdrantWebClient;
    private final VectorServiceProperties properties;

    public QdrantClientService(WebClient qdrantWebClient, VectorServiceProperties properties) {
        this.qdrantWebClient = qdrantWebClient;
        this.properties = properties;
    }

    public Mono<Void> ensureCollection(String collectionName) {
        var body = Map.of(
            "vectors", Map.of(
                "size", properties.getVectorSize(),
                "distance", "Cosine"));
        return qdrantWebClient.put()
            .uri("/collections/{collection}", collectionName)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .then()
            .onErrorResume(exception -> Mono.empty());
    }

    public Mono<Void> upsertPoints(String collectionName, List<Map<String, Object>> points) {
        return qdrantWebClient.put()
            .uri("/collections/{collection}/points?wait=true", collectionName)
            .bodyValue(Map.of("points", points))
            .retrieve()
            .bodyToMono(Map.class)
            .then();
    }

    public Mono<List<RetrievedContext>> search(String collectionName, List<Double> vector, int topK) {
        var body = Map.of(
            "vector", vector,
            "limit", topK,
            "with_payload", true);
        return qdrantWebClient.post()
            .uri("/collections/{collection}/points/search", collectionName)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(QdrantSearchResponse.class)
            .map(response -> response.result().stream()
                .map(result -> {
                    var payload = result.payload();
                    var content = String.valueOf(payload.getOrDefault("content", ""));
                    var rank = ((Number) payload.getOrDefault("chunkIndex", 0)).intValue();
                    return new RetrievedContext(
                        String.valueOf(result.id()),
                        content,
                        result.score(),
                        rank,
                        payload);
                })
                .toList());
    }

    public record QdrantSearchResponse(List<QdrantResult> result) {
    }

    public record QdrantResult(Object id, double score, Map<String, Object> payload) {
    }
}
