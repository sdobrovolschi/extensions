package org.spring.cloud.openfeign;

import feign.Feign;
import feign.micrometer.FeignTargetObservationConvention;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = FeignAutoConfiguration.class)
@ConditionalOnClass(Feign.class)
public class FeignAutoConfig {

    @Bean
    FeignLoggerFactory feignLoggerFactory() {
        return new SLF4JLoggerFactory();
    }

    @Bean
    MicrometerObservationCapability micrometerObservationCapability(ObservationRegistry registry) {
        return new MicrometerObservationCapability(registry, new FeignTargetObservationConvention());
    }
}
