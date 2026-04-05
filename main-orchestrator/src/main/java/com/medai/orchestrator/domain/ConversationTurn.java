package com.medai.orchestrator.domain;

import java.time.Instant;

public record ConversationTurn(
    String role,
    String content,
    Instant createdAt
) {
}
