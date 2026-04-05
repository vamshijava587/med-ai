package com.medai.orchestrator.application.tool;

import com.medai.orchestrator.domain.AgentExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class ToolContextHolder {

    private final ThreadLocal<AgentExecutionContext> currentContext = new ThreadLocal<>();

    public void set(AgentExecutionContext context) {
        currentContext.set(context);
    }

    public AgentExecutionContext get() {
        return currentContext.get();
    }

    public void clear() {
        currentContext.remove();
    }
}
