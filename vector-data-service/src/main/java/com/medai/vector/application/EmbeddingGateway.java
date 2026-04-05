package com.medai.vector.application;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class EmbeddingGateway {

    private final ApplicationContext applicationContext;

    public EmbeddingGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Mono<List<Double>> embed(String text) {
        return Mono.fromCallable(() -> invokeEmbed(text))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private List<Double> invokeEmbed(String text) throws Exception {
        var beanName = applicationContext.containsBean("ollamaEmbeddingModel") ? "ollamaEmbeddingModel" : "openAiEmbeddingModel";
        var bean = applicationContext.getBean(beanName);
        Method method = bean.getClass().getMethod("embed", String.class);
        var result = method.invoke(bean, text);
        if (result instanceof float[] floats) {
            var values = new ArrayList<Double>(floats.length);
            for (float value : floats) {
                values.add((double) value);
            }
            return values;
        }
        if (result instanceof double[] doubles) {
            var values = new ArrayList<Double>(doubles.length);
            for (double value : doubles) {
                values.add(value);
            }
            return values;
        }
        if (result instanceof List<?> list) {
            return list.stream().map(value -> ((Number) value).doubleValue()).toList();
        }
        throw new IllegalStateException("Unsupported embedding result type: " + result.getClass().getName());
    }
}
