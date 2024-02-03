package org.spring.cloud.gateway.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.http.server.reactive.observation.ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE;

class ObservationRoutePredicateHandlerMappingTest {

    PathRoutePredicateFactory predicateFactory = new PathRoutePredicateFactory();
    ObservationRoutePredicateHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        var route = Route.async().id("test").uri("http://localhost")
                .predicate(predicateFactory.apply(c -> c.setPatterns(List.of("/test/{id}"))))
                .build();

        handlerMapping = new ObservationRoutePredicateHandlerMapping(
                null, () -> Flux.just(route).hide(), new GlobalCorsProperties(), new MockEnvironment());
    }

    @Test
    void pathPatternIsSetToObservationContext() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test/1"));

        var observationContext = new ServerRequestObservationContext(
                exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
        exchange.getAttributes().put(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);

        var route = handlerMapping.lookupRoute(exchange);

        StepVerifier.create(route.map(Route::getId))
                .expectNext("test")
                .verifyComplete();

        assertThat(exchange)
                .extracting(e -> e.getAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE), type(ServerRequestObservationContext.class))
                .extracting(ServerRequestObservationContext::getPathPattern)
                .isEqualTo("/test/{id}");
    }
}
