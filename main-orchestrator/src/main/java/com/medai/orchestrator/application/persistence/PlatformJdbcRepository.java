package com.medai.orchestrator.application.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medai.orchestrator.domain.ConversationTurn;
import com.medai.orchestrator.domain.ToolExecutionResult;
import com.medai.shared.agent.AgentType;
import com.medai.shared.api.UserProfile;
import com.medai.shared.evaluation.EvaluationResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PlatformJdbcRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<UserProfile> findUserProfile(String userId) {
        var results = jdbcTemplate.query("""
            SELECT user_id, full_name, allergies, medications, insurance_provider, insurance_plan, notes
            FROM users
            WHERE user_id = ?
            """, this::mapUserProfile, userId);
        return results.stream().findFirst();
    }

    public void ensureSession(String sessionId, String userId) {
        jdbcTemplate.update("""
            INSERT INTO users(user_id, full_name, created_at, updated_at)
            VALUES(?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE updated_at = NOW()
            """, userId, userId);
        jdbcTemplate.update("""
            INSERT INTO sessions(session_id, user_id, created_at, updated_at)
            VALUES(?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE updated_at = NOW()
            """, sessionId, userId);
    }

    public List<ConversationTurn> recentMessages(String sessionId, int windowSize) {
        return jdbcTemplate.query("""
            SELECT role, content, created_at
            FROM (
                SELECT role, content, created_at
                FROM messages
                WHERE session_id = ?
                ORDER BY created_at DESC
                LIMIT ?
            ) recent_messages
            ORDER BY created_at ASC
            """, (resultSet, rowNum) -> new ConversationTurn(
            resultSet.getString("role"),
            resultSet.getString("content"),
            resultSet.getTimestamp("created_at").toInstant()
        ), sessionId, windowSize);
    }

    public void saveMessage(String sessionId, String userId, String role, String content, int tokenCount) {
        jdbcTemplate.update("""
            INSERT INTO messages(session_id, user_id, role, content, token_count, created_at)
            VALUES(?, ?, ?, ?, ?, NOW())
            """, sessionId, userId, role, content, tokenCount);
    }

    public void saveToolCalls(String sessionId, String userId, AgentType agentType, List<ToolExecutionResult> toolCalls) {
        for (var toolCall : toolCalls) {
            jdbcTemplate.update("""
                INSERT INTO tool_calls(session_id, user_id, agent_name, tool_name, tool_input, tool_output, created_at)
                VALUES(?, ?, ?, ?, ?, ?, NOW())
                """, sessionId, userId, agentType.name(), toolCall.tool(), toolCall.input(), toolCall.output());
        }
    }

    public void saveEvaluation(String sessionId, String userId, AgentType agentType, EvaluationResult evaluation) {
        jdbcTemplate.update("""
            INSERT INTO evaluations(session_id, user_id, agent_name, confidence_score, relevance_score, hallucination_flag, rationale, validation_notes, created_at)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """,
            sessionId,
            userId,
            agentType.name(),
            evaluation.confidenceScore(),
            evaluation.relevanceScore(),
            evaluation.hallucinationDetected(),
            evaluation.rationale(),
            safeJson(evaluation.validationNotes()));
    }

    public void saveAuditLog(String sessionId,
                             String userId,
                             String query,
                             AgentType agentType,
                             List<String> toolsUsed,
                             int tokensUsed,
                             double confidence,
                             boolean hallucinationFlag,
                             long responseTimeMs) {
        jdbcTemplate.update("""
            INSERT INTO audit_logs(session_id, user_id, query_text, agent_selected, tools_used, tokens_used, confidence_score, hallucination_flag, response_time_ms, created_at)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """,
            sessionId,
            userId,
            query,
            agentType.name(),
            safeJson(toolsUsed),
            tokensUsed,
            confidence,
            hallucinationFlag,
            responseTimeMs);
    }

    private UserProfile mapUserProfile(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserProfile(
            resultSet.getString("user_id"),
            resultSet.getString("full_name"),
            splitList(resultSet.getString("allergies")),
            splitList(resultSet.getString("medications")),
            resultSet.getString("insurance_provider"),
            resultSet.getString("insurance_plan"),
            resultSet.getString("notes"));
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isBlank())
            .toList();
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
