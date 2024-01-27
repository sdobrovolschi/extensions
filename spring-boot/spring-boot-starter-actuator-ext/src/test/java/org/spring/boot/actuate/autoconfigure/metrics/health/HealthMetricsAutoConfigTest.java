package org.spring.boot.actuate.autoconfigure.metrics.health;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.spring.boot.actuate.autoconfigure.metrics.MetricsRun;
import org.spring.boot.actuate.autoconfigure.metrics.health.HealthMetricsAutoConfig;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HealthMetricsAutoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
            .withConfiguration(AutoConfigurations.of(HealthEndpointAutoConfiguration.class, HealthMetricsAutoConfig.class));

    @Test
    void autoConfiguredHealthIsInstrumented() {
        this.contextRunner.run((context) -> {
            var registry = context.getBean(MeterRegistry.class);
            registry.get("health").meter();
        });
    }

    @Test
    void healthInstrumentationCanBeDisabled() {
        this.contextRunner.withPropertyValues("management.metrics.enable.health=false").run((context) -> {
            var registry = context.getBean(MeterRegistry.class);
            assertThat(registry.find("health").meter()).isNull();
        });
    }
}
