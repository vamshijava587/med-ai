package com.medai.shared.api;

import java.util.Map;

public record RetrievedContext(
    String id,
    String content,
    double score,
    int rank,
    Map<String, Object> metadata
) {
}
