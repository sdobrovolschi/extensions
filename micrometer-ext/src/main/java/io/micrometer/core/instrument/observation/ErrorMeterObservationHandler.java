package io.micrometer.core.instrument.observation;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;

import java.util.List;

import static java.util.stream.Collectors.toList;

public final class ErrorMeterObservationHandler implements MeterObservationHandler<Observation.Context> {

    private final MeterRegistry meterRegistry;
    private final MeterObservationHandler<Observation.Context> delegate;

    public ErrorMeterObservationHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.delegate = new DefaultMeterObservationHandler(meterRegistry);
    }

    @Override
    public void onStart(Observation.Context context) {
        delegate.onStart(context);
    }

    @Override
    public void onStop(Observation.Context context) {
        List<Tag> tags = createTags(context);
        tags.add(Tag.of("error", getErrorValue(context)));
        tags.add(Tag.of("message", getErrorMessage(context)));

        Timer.Sample sample = context.getRequired(Timer.Sample.class);
        sample.stop(Timer.builder(context.getName()).tags(tags).register(meterRegistry));

        LongTaskTimer.Sample longTaskSample = context.getRequired(LongTaskTimer.Sample.class);
        longTaskSample.stop();
    }

    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
        delegate.onEvent(event, context);
    }

    private String getErrorValue(Observation.Context context) {
        var error = context.getError();
        return error == null ? "none" : error.getClass().getSimpleName();
    }

    private String getErrorMessage(Observation.Context context) {
        var error = context.getError();
        return error == null ? "none" : error.getMessage();
    }

    private List<Tag> createTags(Observation.Context context) {
        return context.getLowCardinalityKeyValues().stream()
                .map(keyValue -> Tag.of(keyValue.getKey(), keyValue.getValue()))
                .collect(toList());
    }
}
