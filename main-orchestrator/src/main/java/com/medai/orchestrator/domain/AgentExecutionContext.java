package com.medai.orchestrator.domain;

import com.medai.shared.agent.AgentType;
import com.medai.shared.api.RetrievedContext;
import com.medai.shared.api.UserProfile;
import java.util.List;
import java.util.Map;

public record AgentExecutionContext(
    String userId,
    String sessionId,
    AgentType agentType,
    UserProfile userProfile,
    List<ConversationTurn> history,
    List<RetrievedContext> retrievedContext,
    ResponseDetailLevel responseDetailLevel,
    Map<String, Object> requestMetadata
) {
}
