package org.spring.cloud.openfeign;

import feign.Logger;
import feign.slf4j.SLF4JLogger;
import org.springframework.cloud.openfeign.FeignLoggerFactory;

public final class SLF4JLoggerFactory implements FeignLoggerFactory {

    @Override
    public Logger create(Class<?> type) {
        return new SLF4JLogger(type);
    }
}
