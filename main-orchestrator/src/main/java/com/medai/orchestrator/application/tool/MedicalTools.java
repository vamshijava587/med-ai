package com.medai.orchestrator.application.tool;

import com.medai.orchestrator.application.VectorDataClient;
import com.medai.orchestrator.application.persistence.PlatformJdbcRepository;
import com.medai.shared.agent.AgentType;
import java.time.Duration;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class MedicalTools {

    private final ToolContextHolder toolContextHolder;
    private final VectorDataClient vectorDataClient;
    private final PlatformJdbcRepository repository;

    public MedicalTools(ToolContextHolder toolContextHolder,
                        VectorDataClient vectorDataClient,
                        PlatformJdbcRepository repository) {
        this.toolContextHolder = toolContextHolder;
        this.vectorDataClient = vectorDataClient;
        this.repository = repository;
    }

    @Tool(description = "Search the symptom knowledge base for grounded symptom explanations and possible causes.")
    public String search_symptoms(String query) {
        return summarizeMatches(AgentType.SYMPTOM, query);
    }

    @Tool(description = "Check medication interaction concerns using grounded medication knowledge.")
    public String check_drug_interaction(String medications) {
        return summarizeMatches(AgentType.MEDICATION, medications);
    }

    @Tool(description = "Look up the current user's insurance provider and plan details.")
    public String get_insurance_info() {
        var context = requireContext();
        return repository.findUserProfile(context.userId())
            .map(profile -> "Insurance provider: " + profile.insuranceProvider() + ", plan: " + profile.insurancePlan())
            .orElse("No insurance information is available for the current user.");
    }

    @Tool(description = "Estimate urgency for the symptoms to support triage.")
    public String triage_urgency(String symptoms) {
        var normalized = symptoms.toLowerCase();
        if (normalized.contains("chest pain") || normalized.contains("difficulty breathing") || normalized.contains("unconscious")) {
            return "Urgency level: EMERGENCY. Recommend immediate emergency evaluation.";
        }
        if (normalized.contains("high fever") || normalized.contains("severe pain") || normalized.contains("persistent vomiting")) {
            return "Urgency level: URGENT. Recommend same-day clinical assessment.";
        }
        return "Urgency level: STANDARD. Recommend primary care follow-up with symptom monitoring.";
    }

    @Tool(description = "Search the active agent collection in the vector retrieval service.")
    public String vector_search_rag(String query) {
        return summarizeMatches(requireContext().agentType(), query);
    }

    private String summarizeMatches(AgentType agentType, String query) {
        var matches = vectorDataClient.search(agentType, query, 5, true)
            .blockOptional(Duration.ofSeconds(10))
            .orElseGet(java.util.List::of);
        if (matches.isEmpty()) {
            return "No grounded knowledge was found for this query.";
        }
        var builder = new StringBuilder();
        for (var match : matches) {
            builder.append("- score=")
                .append(match.score())
                .append(" content=")
                .append(match.content())
                .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private com.medai.orchestrator.domain.AgentExecutionContext requireContext() {
        var context = toolContextHolder.get();
        if (context == null) {
            throw new IllegalStateException("Tool context is not available.");
        }
        return context;
    }
}
