package org.spring.cloud.openfeign;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.FeignLoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignClientsConfiguration.class, FeignAutoConfig.class));

    @Test
    void SLF4JLoggerFactoryBeanIsCreated() {
        runner
                .run(ctx -> assertThat(ctx.getBean(FeignLoggerFactory.class)).isInstanceOf(SLF4JLoggerFactory.class));
    }
}
