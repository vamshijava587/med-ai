package com.medai.orchestrator.application;

import com.medai.orchestrator.domain.AgentExecutionResult;
import com.medai.shared.evaluation.EvaluationResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class PlatformMetricsService {

    private final MeterRegistry meterRegistry;

    public PlatformMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(AgentExecutionResult execution, EvaluationResult evaluation) {
        Counter.builder("medai_requests_total")
            .tag("agent", execution.agentType().name())
            .tag("provider", execution.provider().name())
            .register(meterRegistry)
            .increment();

        DistributionSummary.builder("medai_tokens_input")
            .baseUnit("tokens")
            .tag("agent", execution.agentType().name())
            .register(meterRegistry)
            .record(execution.inputTokens());

        DistributionSummary.builder("medai_tokens_output")
            .baseUnit("tokens")
            .tag("agent", execution.agentType().name())
            .register(meterRegistry)
            .record(execution.outputTokens());

        DistributionSummary.builder("medai_confidence_score")
            .tag("agent", execution.agentType().name())
            .register(meterRegistry)
            .record(evaluation.confidenceScore());

        Counter.builder("medai_hallucination_flags_total")
            .tag("agent", execution.agentType().name())
            .register(meterRegistry)
            .increment(evaluation.hallucinationDetected() ? 1.0 : 0.0);
    }
}
