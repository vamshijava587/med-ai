package com.medai.orchestrator.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

@Service
public class PromptTemplateService {

    private final ResourceLoader resourceLoader;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String render(String classpathLocation, Map<String, Object> attributes) {
        var template = loadTemplate(classpathLocation);
        var st = new ST(template, '$', '$');
        attributes.forEach(st::add);
        return st.render();
    }

    public String loadTemplate(String classpathLocation) {
        return templateCache.computeIfAbsent(classpathLocation, key -> {
            try (var inputStream = resourceLoader.getResource("classpath:" + key).getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            catch (IOException exception) {
                throw new IllegalStateException("Unable to load prompt template " + key, exception);
            }
        });
    }
}
