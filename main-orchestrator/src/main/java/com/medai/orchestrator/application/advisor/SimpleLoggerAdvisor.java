package com.medai.orchestrator.application.advisor;

import com.medai.orchestrator.domain.AgentExecutionResult;
import com.medai.shared.evaluation.EvaluationResult;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimpleLoggerAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);

    public void logExecution(String sessionId, AgentExecutionResult execution, EvaluationResult evaluation, Duration latency) {
        log.info(
            "session={} agent={} provider={} inputTokens={} outputTokens={} cost={} confidence={} hallucination={} latencyMs={}",
            sessionId,
            execution.agentType(),
            execution.provider(),
            execution.inputTokens(),
            execution.outputTokens(),
            execution.estimatedCost(),
            evaluation.confidenceScore(),
            evaluation.hallucinationDetected(),
            latency.toMillis());
    }
}
