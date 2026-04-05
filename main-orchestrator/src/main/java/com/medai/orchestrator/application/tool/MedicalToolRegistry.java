package com.medai.orchestrator.application.tool;

import com.medai.orchestrator.domain.AgentExecutionContext;
import com.medai.orchestrator.domain.ToolExecutionResult;
import com.medai.orchestrator.domain.ToolRequest;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class MedicalToolRegistry {

    private final MedicalTools medicalTools;
    private final ToolContextHolder toolContextHolder;
    private final Map<String, Method> methods = new HashMap<>();

    public MedicalToolRegistry(MedicalTools medicalTools, ToolContextHolder toolContextHolder) {
        this.medicalTools = medicalTools;
        this.toolContextHolder = toolContextHolder;
    }

    @PostConstruct
    void registerTools() {
        for (var method : MedicalTools.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                methods.put(method.getName(), method);
            }
        }
    }

    public Mono<List<ToolExecutionResult>> executeToolCalls(List<ToolRequest> toolCalls, AgentExecutionContext context) {
        return Flux.fromIterable(toolCalls)
            .flatMap(toolCall -> Mono.fromCallable(() -> invoke(toolCall, context)).subscribeOn(Schedulers.boundedElastic()))
            .collectList();
    }

    private ToolExecutionResult invoke(ToolRequest request, AgentExecutionContext context) throws Exception {
        var method = methods.get(request.tool());
        if (method == null) {
            return new ToolExecutionResult(request.tool(), request.input(), "Unknown tool requested.");
        }
        toolContextHolder.set(context);
        try {
            var output = switch (method.getParameterCount()) {
                case 0 -> String.valueOf(method.invoke(medicalTools));
                case 1 -> String.valueOf(method.invoke(medicalTools, request.input()));
                default -> "Unsupported tool signature.";
            };
            return new ToolExecutionResult(request.tool(), request.input(), output);
        }
        finally {
            toolContextHolder.clear();
        }
    }
}
