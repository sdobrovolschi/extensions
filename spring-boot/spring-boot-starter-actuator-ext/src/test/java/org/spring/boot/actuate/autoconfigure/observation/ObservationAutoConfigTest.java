package org.spring.boot.actuate.autoconfigure.observation;

import io.micrometer.core.instrument.observation.ErrorMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationAutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    CompositeMeterRegistryAutoConfiguration.class,
                    ObservationAutoConfig.class,
                    ObservationAutoConfiguration.class));

    @Test
    void errorMeterObservationHandlerBeanIsCreated() {
        runner
                .run(ctx -> assertThat(ctx.getBean(MeterObservationHandler.class)).isInstanceOf(ErrorMeterObservationHandler.class));
    }
}
