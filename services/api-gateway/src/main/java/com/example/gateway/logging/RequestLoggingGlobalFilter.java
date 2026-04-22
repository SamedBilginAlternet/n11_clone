package com.example.gateway.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFlux equivalent of the per-service RequestLoggingFilter. Assigns the
 * client-visible correlation ID at the edge so every downstream service gets
 * it propagated via the X-Correlation-Id header. Reactive: we can't use MDC
 * naively across thread hops, so we only set it around the synchronous log
 * calls here; downstream services set their own MDC in their servlet filter.
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String cid = correlationId;

        // Propagate to downstream + echo to client.
        ServerHttpRequest mutated = request.mutate()
                .header(CORRELATION_ID_HEADER, cid)
                .build();
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set(CORRELATION_ID_HEADER, cid);

        withMdc(cid, () ->
                log.info("→ {} {}", mutated.getMethod(), mutated.getURI().getPath()));

        long start = System.currentTimeMillis();
        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    withMdc(cid, () -> {
                        Integer status = response.getStatusCode() != null
                                ? response.getStatusCode().value() : null;
                        log.info("← {} {} status={} | {} ms",
                                mutated.getMethod(), mutated.getURI().getPath(), status, duration);
                    });
                });
    }

    private static void withMdc(String cid, Runnable r) {
        MDC.put(MDC_KEY, cid);
        try { r.run(); } finally { MDC.remove(MDC_KEY); }
    }

    @Override
    public int getOrder() {
        // Run first — before routing so we catch every request.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
