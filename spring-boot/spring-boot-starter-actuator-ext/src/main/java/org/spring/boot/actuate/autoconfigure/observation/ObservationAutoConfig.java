package org.spring.boot.actuate.autoconfigure.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.ErrorMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = ObservationAutoConfiguration.class)
@ConditionalOnClass(ObservationRegistry.class)
public class ObservationAutoConfig {

    @Bean
    MeterObservationHandler<Observation.Context> meterObservationHandler(MeterRegistry meterRegistry) {
        return new ErrorMeterObservationHandler(meterRegistry);
    }
}
