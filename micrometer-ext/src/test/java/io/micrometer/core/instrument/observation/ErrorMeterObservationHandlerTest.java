package io.micrometer.core.instrument.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMeterObservationHandlerTest {

    ObservationHandler<Observation.Context> handler;
    MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = createMeterRegistry();
        handler = new ErrorMeterObservationHandler(meterRegistry);
    }

    private MeterRegistry createMeterRegistry() {
        return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }

    @Test
    void metersStart() {
        var context = new ContextTestBuilder().build();

        handler.onStart(context);

        assertThat(getMeters("testName.active"))
                .extracting(Meter::getId)
                .flatExtracting(Meter.Id::getTags)
                .asInstanceOf(InstanceOfAssertFactories.list(Tag.class))
                .containsExactlyInAnyOrder(Tag.of("lcTag", "foo"));

        assertThat(getMeters("testName")).isEmpty();
    }

    @Test
    void metersStops() {
        var context = new ContextTestBuilder()
                .withDefaultMeters()
                .build();

        handler.onStop(context);

        assertThat(getMeters("testName.active"))
                .extracting(Meter::getId)
                .flatExtracting(Meter.Id::getTags)
                .asInstanceOf(InstanceOfAssertFactories.list(Tag.class))
                .containsExactlyInAnyOrder(Tag.of("lcTag", "foo"));

        assertThat(getMeters("testName"))
                .extracting(Meter::getId)
                .flatExtracting(Meter.Id::getTags)
                .asInstanceOf(InstanceOfAssertFactories.list(Tag.class))
                .containsExactlyInAnyOrder(
                        Tag.of("lcTag", "foo"),
                        Tag.of("error", "IOException"),
                        Tag.of("message", "simulated"));
    }

    private List<Meter> getMeters(String meterName) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(meterName))
                .toList();
    }

    class ContextTestBuilder {

        Observation.Context context;

        ContextTestBuilder() {
            context = createContext();
        }

        Observation.Context createContext() {
            var context = new Observation.Context();
            context.setName("testName");
            context.setContextualName("testContextualName");
            context.setError(new IOException("simulated"));
            context.addLowCardinalityKeyValue(KeyValue.of("lcTag", "foo"));
            context.addHighCardinalityKeyValue(KeyValue.of("hcTag", "bar"));
            context.put("contextKey", "contextValue");

            return context;
        }

        ContextTestBuilder withDefaultMeters() {
            var longTaskSample = LongTaskTimer.builder(context.getName() + ".active")
                    .tags(List.of(Tag.of("lcTag", "foo")))
                    .register(meterRegistry)
                    .start();
            context.put(LongTaskTimer.Sample.class, longTaskSample);

            var sample = Timer.start(meterRegistry);
            context.put(Timer.Sample.class, sample);

            return this;
        }

        Observation.Context build() {
            return context;
        }
    }
}
