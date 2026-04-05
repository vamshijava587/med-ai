package com.medai.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofHours(1))
        .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return resolveKey(exchange.getRequest())
            .defaultIfEmpty("anonymous")
            .flatMap(key -> {
                var bucket = cache.get(key, ignored -> newBucket());
                if (bucket.tryConsume(1)) {
                    return chain.filter(exchange);
                }
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            });
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<String> resolveKey(ServerHttpRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> context.getAuthentication().getName())
            .switchIfEmpty(Mono.justOrEmpty(request.getRemoteAddress()).map(address -> address.getAddress().getHostAddress()));
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(15))))
            .build();
    }
}
