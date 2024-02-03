package org.spring.cloud.gateway.handler;

import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.http.server.reactive.observation.ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE;

public final class ObservationRoutePredicateHandlerMapping extends RoutePredicateHandlerMapping {

    private static final String ALL_PATHS = "/**";

    public ObservationRoutePredicateHandlerMapping(
            FilteringWebHandler webHandler,
            RouteLocator routeLocator,
            GlobalCorsProperties globalCorsProperties,
            Environment environment) {

        super(webHandler, routeLocator, globalCorsProperties, environment);
    }

    @Override
    protected Mono<Route> lookupRoute(ServerWebExchange exchange) {
        return super.lookupRoute(exchange)
                .map(route -> {
                    ServerRequestObservationContext context = exchange.getRequiredAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
                    context.setPathPattern(getPathPattern(route));

                    return route;
                });
    }

    /**
     * Copied from {@link org.springframework.cloud.gateway.filter.cors.CorsGatewayFilterApplicationListener#getPathPredicate(Route)}
     */
    private String getPathPattern(Route route) {
        var predicate = route.getPredicate();
        var pathPatterns = new AtomicReference<String>();
        predicate.accept(p -> {
            if (p.getConfig() instanceof PathRoutePredicateFactory.Config pathConfig) {
                if (!pathConfig.getPatterns().isEmpty()) {
                    pathPatterns.compareAndSet(null, pathConfig.getPatterns().get(0));
                }
            }
        });
        if (pathPatterns.get() != null) {
            return pathPatterns.get();
        }
        return ALL_PATHS;
    }
}
