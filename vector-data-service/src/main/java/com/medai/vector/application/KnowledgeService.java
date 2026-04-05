package com.medai.vector.application;

import com.medai.shared.api.IngestDocumentsRequest;
import com.medai.shared.api.IngestDocumentsResponse;
import com.medai.shared.api.KnowledgeSearchRequest;
import com.medai.shared.api.KnowledgeSearchResponse;
import com.medai.shared.api.RetrievedContext;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class KnowledgeService {

    private final SmartChunkingService smartChunkingService;
    private final EmbeddingGateway embeddingGateway;
    private final QdrantClientService qdrantClientService;

    public KnowledgeService(SmartChunkingService smartChunkingService,
                            EmbeddingGateway embeddingGateway,
                            QdrantClientService qdrantClientService) {
        this.smartChunkingService = smartChunkingService;
        this.embeddingGateway = embeddingGateway;
        this.qdrantClientService = qdrantClientService;
    }

    public Mono<IngestDocumentsResponse> ingest(IngestDocumentsRequest request) {
        var collection = request.agentType().collectionName();
        return qdrantClientService.ensureCollection(collection)
            .thenMany(Flux.fromIterable(request.documents()))
            .flatMap(document -> Flux.fromIterable(smartChunkingService.chunk(document)))
            .flatMap(chunk -> embeddingGateway.embed(chunk.content())
                .map(vector -> toPoint(chunk, vector)))
            .collectList()
            .flatMap(points -> qdrantClientService.upsertPoints(collection, points)
                .thenReturn(new IngestDocumentsResponse(request.agentType(), points.size(), points.size())));
    }

    public Mono<KnowledgeSearchResponse> search(KnowledgeSearchRequest request) {
        var collection = request.agentType().collectionName();
        return qdrantClientService.ensureCollection(collection)
            .then(embeddingGateway.embed(request.query()))
            .flatMap(vector -> qdrantClientService.search(collection, vector, request.topK() <= 0 ? 5 : request.topK()))
            .map(results -> request.reRank() ? rerank(request.query(), results) : results)
            .map(results -> new KnowledgeSearchResponse(request.agentType(), results));
    }

    private Map<String, Object> toPoint(SmartChunkingService.ChunkedDocument chunk, List<Double> vector) {
        var payload = new HashMap<String, Object>();
        payload.put("content", chunk.content());
        payload.put("chunkIndex", chunk.chunkIndex());
        payload.put("frequencyScore", chunk.frequencyScore());
        if (chunk.metadata() != null) {
            payload.putAll(chunk.metadata());
        }
        return Map.of(
            "id", chunk.id(),
            "vector", vector,
            "payload", payload);
    }

    private List<RetrievedContext> rerank(String query, List<RetrievedContext> results) {
        Set<String> queryTerms = new HashSet<>(List.of(query.toLowerCase().split("\\s+")));
        var ordered = results.stream()
            .sorted(Comparator.comparingDouble(result -> -combinedScore(queryTerms, result)))
            .toList();
        var reranked = new java.util.ArrayList<RetrievedContext>(ordered.size());
        int rank = 1;
        for (var result : ordered) {
            reranked.add(new RetrievedContext(result.id(), result.content(), result.score(), rank, result.metadata()));
            rank++;
        }
        return reranked;
    }

    private double combinedScore(Set<String> queryTerms, RetrievedContext result) {
        var lexicalScore = queryTerms.stream()
            .filter(term -> result.content().toLowerCase().contains(term))
            .count();
        var frequencyScore = ((Number) result.metadata().getOrDefault("frequencyScore", 0.0d)).doubleValue();
        return result.score() + lexicalScore + (frequencyScore / 100.0d);
    }

}
