package com.medai.orchestrator.application.advisor;

import com.medai.orchestrator.config.AppProperties;
import com.medai.orchestrator.domain.ResponseDetailLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenOptimizerAdvisorTest {

    @Test
    void shouldReturnDetailedForSmallPrompt() {
        var properties = new AppProperties();
        var advisor = new TokenOptimizerAdvisor(properties);

        assertThat(advisor.chooseDetailLevel(200)).isEqualTo(ResponseDetailLevel.DETAILED);
    }

    @Test
    void shouldReturnConciseForLargePrompt() {
        var properties = new AppProperties();
        var advisor = new TokenOptimizerAdvisor(properties);

        assertThat(advisor.chooseDetailLevel(5_000)).isEqualTo(ResponseDetailLevel.CONCISE);
    }
}
