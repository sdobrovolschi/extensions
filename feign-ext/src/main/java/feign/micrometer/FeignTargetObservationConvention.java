package feign.micrometer;

import io.micrometer.common.KeyValues;
import io.micrometer.common.docs.KeyName;

import static feign.micrometer.FeignTargetObservationConvention.TargetTags.TARGET_NAME;

public final class FeignTargetObservationConvention implements FeignObservationConvention {

    private static final DefaultFeignObservationConvention DELEGATE = DefaultFeignObservationConvention.INSTANCE;

    @Override
    public String getName() {
        return DELEGATE.getName();
    }

    @Override
    public String getContextualName(FeignContext context) {
        return DELEGATE.getContextualName(context);
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(FeignContext context) {
        var keyValues = DELEGATE.getLowCardinalityKeyValues(context);


        if (context.getCarrier() != null) {
            keyValues = keyValues.and(TARGET_NAME.withValue(context.getCarrier().requestTemplate().feignTarget().name()));
        }
        return keyValues;
    }

    enum TargetTags implements KeyName {

        TARGET_NAME {
            @Override
            public String asString() {
                return "net.peer.name";
            }
        }
    }
}
