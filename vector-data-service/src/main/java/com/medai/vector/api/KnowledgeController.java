package com.medai.vector.api;

import com.medai.shared.api.IngestDocumentsRequest;
import com.medai.shared.api.IngestDocumentsResponse;
import com.medai.shared.api.KnowledgeSearchRequest;
import com.medai.shared.api.KnowledgeSearchResponse;
import com.medai.vector.application.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/ingest")
    public Mono<IngestDocumentsResponse> ingest(@Valid @RequestBody IngestDocumentsRequest request) {
        return knowledgeService.ingest(request);
    }

    @PostMapping("/search")
    public Mono<KnowledgeSearchResponse> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return knowledgeService.search(request);
    }
}
