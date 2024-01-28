package org.spring.cloud.openfeign;

import feign.micrometer.FeignTargetObservationConvention;
import feign.micrometer.MicrometerObservationCapability;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservationAutoConfiguration.class,
                    FeignClientsConfiguration.class,
                    FeignAutoConfig.class));

    @Test
    void SLF4JLoggerFactoryBeanIsCreated() {
        runner
                .run(ctx -> assertThat(ctx.getBean(FeignLoggerFactory.class)).isInstanceOf(SLF4JLoggerFactory.class));
    }

    @Test
    void feignTargetObservationConventionBeanIsCreated() {
        runner
                .run(ctx -> assertThat(ctx.getBean(MicrometerObservationCapability.class))
                        .extracting(capability -> ReflectionTestUtils.getField(capability, "customFeignObservationConvention"))
                        .isInstanceOf(FeignTargetObservationConvention.class));
    }
}
