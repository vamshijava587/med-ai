package com.medai.orchestrator.application;

import com.medai.shared.agent.AgentType;
import com.medai.shared.api.KnowledgeSearchRequest;
import com.medai.shared.api.KnowledgeSearchResponse;
import com.medai.shared.api.RetrievedContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class VectorDataClient {

    private final WebClient vectorServiceWebClient;

    public VectorDataClient(WebClient vectorServiceWebClient) {
        this.vectorServiceWebClient = vectorServiceWebClient;
    }

    public Mono<List<RetrievedContext>> search(AgentType agentType, String query, int topK, boolean reRank) {
        return vectorServiceWebClient.post()
            .uri("/internal/knowledge/search")
            .bodyValue(new KnowledgeSearchRequest(agentType, query, topK, reRank))
            .retrieve()
            .bodyToMono(KnowledgeSearchResponse.class)
            .map(KnowledgeSearchResponse::matches)
            .onErrorReturn(List.of());
    }
}
