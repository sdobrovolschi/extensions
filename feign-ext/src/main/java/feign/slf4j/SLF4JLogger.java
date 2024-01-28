package feign.slf4j;

import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.System.lineSeparator;

public final class SLF4JLogger extends feign.Logger {

    private final Logger logger;
    private final ThreadLocal<MessageBuilder> message = new ThreadLocal<>();

    public SLF4JLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        if (logger.isTraceEnabled()) {
            message.set(new MessageBuilder(configKey));
            super.logRequest(configKey, logLevel, request);
        }
    }

    @Override
    protected Response logAndRebufferResponse(
            String configKey,
            Level logLevel,
            Response response,
            long elapsedTime) throws IOException {

        if (logger.isTraceEnabled()) {
            var resp = super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);

            logger.trace(message.get().toString());

            return resp;
        }
        return response;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        var builder = message.get();
        if (builder == null) {
            builder = new MessageBuilder(configKey);
            message.set(builder);
        }

        builder
                .append(lineSeparator())
                .append(format.formatted(args));
    }

    private final class MessageBuilder {

        String configKey;
        StringBuilder delegate = new StringBuilder();

        MessageBuilder(String configKey) {
            this.configKey = configKey;
        }

        MessageBuilder append(String string) {
            delegate.append(string);
            return this;
        }

        @Override
        public String toString() {
            return lineSeparator() + methodTag(configKey) + delegate;
        }
    }
}
