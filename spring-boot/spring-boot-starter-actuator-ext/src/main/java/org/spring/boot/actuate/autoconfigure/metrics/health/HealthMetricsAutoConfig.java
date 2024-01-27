package org.spring.boot.actuate.autoconfigure.metrics.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.actuator.map-health-indicators-to-metrics
 */
@AutoConfiguration(after = {MetricsAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class})
@ConditionalOnBean(MeterRegistry.class)
public class HealthMetricsAutoConfig {

    public HealthMetricsAutoConfig(MeterRegistry registry, HealthEndpoint healthEndpoint) {
        Gauge.builder("health", healthEndpoint, this::getStatusCode).strongReference(true).register(registry);
    }

    private int getStatusCode(HealthEndpoint health) {
        var status = health.health().getStatus();
        if (Status.UP.equals(status)) {
            return 3;
        }
        if (Status.OUT_OF_SERVICE.equals(status)) {
            return 2;
        }
        if (Status.DOWN.equals(status)) {
            return 1;
        }
        return 0;
    }
}
