package com.medai.orchestrator.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Routing routing = new Routing();
    private Evaluation evaluation = new Evaluation();
    private Memory memory = new Memory();
    private Tokens tokens = new Tokens();
    private Vector vector = new Vector();
    private Pricing pricing = new Pricing();
    private Map<String, AgentDefinition> agents = new HashMap<>();

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public Tokens getTokens() {
        return tokens;
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
    }

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    public Pricing getPricing() {
        return pricing;
    }

    public void setPricing(Pricing pricing) {
        this.pricing = pricing;
    }

    public Map<String, AgentDefinition> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, AgentDefinition> agents) {
        this.agents = agents;
    }

    public static class Routing {
        private String primaryProvider = "OLLAMA";
        private String fallbackProvider = "OPENAI";

        public String getPrimaryProvider() {
            return primaryProvider;
        }

        public void setPrimaryProvider(String primaryProvider) {
            this.primaryProvider = primaryProvider;
        }

        public String getFallbackProvider() {
            return fallbackProvider;
        }

        public void setFallbackProvider(String fallbackProvider) {
            this.fallbackProvider = fallbackProvider;
        }
    }

    public static class Evaluation {
        private double confidenceThreshold = 0.70;
        private double hallucinationThreshold = 0.55;

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public double getHallucinationThreshold() {
            return hallucinationThreshold;
        }

        public void setHallucinationThreshold(double hallucinationThreshold) {
            this.hallucinationThreshold = hallucinationThreshold;
        }
    }

    public static class Memory {
        private int windowSize = 12;

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }
    }

    public static class Tokens {
        private int conciseThreshold = 3200;
        private int balancedThreshold = 1600;
        private int maxToolIterations = 3;

        public int getConciseThreshold() {
            return conciseThreshold;
        }

        public void setConciseThreshold(int conciseThreshold) {
            this.conciseThreshold = conciseThreshold;
        }

        public int getBalancedThreshold() {
            return balancedThreshold;
        }

        public void setBalancedThreshold(int balancedThreshold) {
            this.balancedThreshold = balancedThreshold;
        }

        public int getMaxToolIterations() {
            return maxToolIterations;
        }

        public void setMaxToolIterations(int maxToolIterations) {
            this.maxToolIterations = maxToolIterations;
        }
    }

    public static class Vector {
        private String baseUrl = "http://vector-data-service:8082";
        private int topK = 5;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }

    public static class Pricing {
        private double ollamaInputPerThousand = 0.0;
        private double ollamaOutputPerThousand = 0.0;
        private double openAiInputPerThousand = 0.005;
        private double openAiOutputPerThousand = 0.015;

        public double getOllamaInputPerThousand() {
            return ollamaInputPerThousand;
        }

        public void setOllamaInputPerThousand(double ollamaInputPerThousand) {
            this.ollamaInputPerThousand = ollamaInputPerThousand;
        }

        public double getOllamaOutputPerThousand() {
            return ollamaOutputPerThousand;
        }

        public void setOllamaOutputPerThousand(double ollamaOutputPerThousand) {
            this.ollamaOutputPerThousand = ollamaOutputPerThousand;
        }

        public double getOpenAiInputPerThousand() {
            return openAiInputPerThousand;
        }

        public void setOpenAiInputPerThousand(double openAiInputPerThousand) {
            this.openAiInputPerThousand = openAiInputPerThousand;
        }

        public double getOpenAiOutputPerThousand() {
            return openAiOutputPerThousand;
        }

        public void setOpenAiOutputPerThousand(double openAiOutputPerThousand) {
            this.openAiOutputPerThousand = openAiOutputPerThousand;
        }
    }

    public static class AgentDefinition {
        private String systemPrompt;
        private String guardrailsPrompt;
        private String toolInstructionsPrompt;
        private String collection;
        private String displayName;

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getGuardrailsPrompt() {
            return guardrailsPrompt;
        }

        public void setGuardrailsPrompt(String guardrailsPrompt) {
            this.guardrailsPrompt = guardrailsPrompt;
        }

        public String getToolInstructionsPrompt() {
            return toolInstructionsPrompt;
        }

        public void setToolInstructionsPrompt(String toolInstructionsPrompt) {
            this.toolInstructionsPrompt = toolInstructionsPrompt;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
