package com.medai.orchestrator.application.advisor;

import com.medai.orchestrator.domain.ConversationTurn;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MessageChatMemoryAdvisor {

    public String format(List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return "No prior conversation history is available.";
        }
        return history.stream()
            .map(turn -> turn.role() + ": " + turn.content())
            .collect(Collectors.joining("\n"));
    }
}
