package feign.micrometer;

import feign.Feign;
import feign.RequestLine;
import feign.mock.HttpMethod;
import feign.mock.MockClient;
import feign.mock.MockTarget;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeignMicrometerTest {

    MeterRegistry metricsRegistry;
    Feign feign;

    @BeforeEach
    void setUp() {
        metricsRegistry = createMeterRegistry();
        feign = createFeign();
    }

    @Test
    void metricsIncludeTargetNameTag() {
        var api = feign.newInstance(new MockTarget<>(API.class));

        api.get();

        assertThat(getMetrics())
                .extracting(Meter::getId)
                .flatExtracting(Meter.Id::getTags)
                .asInstanceOf(InstanceOfAssertFactories.list(Tag.class))
                .contains(Tag.of("net.peer.name", API.class.getSimpleName()));
    }

    private MeterRegistry createMeterRegistry() {
        return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    }

    private Feign createFeign() {
        var observationRegistry = ObservationRegistry.create();

        observationRegistry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(metricsRegistry));

        return Feign.builder()
                .client(new MockClient().ok(HttpMethod.GET, "/get", "1234567890abcde"))
                .addCapability(new MicrometerObservationCapability(observationRegistry, new FeignTargetObservationConvention()))
                .build();
    }

    private List<Meter> getMetrics() {
        return metricsRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals("http.client.requests"))
                .toList();
    }

    interface API {

        @RequestLine("GET /get")
        String get();
    }
}
