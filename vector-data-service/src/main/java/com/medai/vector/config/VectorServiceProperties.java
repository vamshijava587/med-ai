package com.medai.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vector")
public class VectorServiceProperties {

    private String qdrantUrl = "http://localhost:6333";
    private int vectorSize = 768;
    private int defaultChunkSizeTokens = 220;
    private int defaultChunkOverlapTokens = 40;

    public String getQdrantUrl() { return qdrantUrl; }
    public void setQdrantUrl(String qdrantUrl) { this.qdrantUrl = qdrantUrl; }

    public int getVectorSize() { return vectorSize; }
    public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }

    public int getDefaultChunkSizeTokens() { return defaultChunkSizeTokens; }
    public void setDefaultChunkSizeTokens(int defaultChunkSizeTokens) { this.defaultChunkSizeTokens = defaultChunkSizeTokens; }

    public int getDefaultChunkOverlapTokens() { return defaultChunkOverlapTokens; }
    public void setDefaultChunkOverlapTokens(int defaultChunkOverlapTokens) { this.defaultChunkOverlapTokens = defaultChunkOverlapTokens; }
}
