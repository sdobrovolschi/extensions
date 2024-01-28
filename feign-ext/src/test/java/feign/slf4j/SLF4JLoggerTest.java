package feign.slf4j;

import feign.Logger.Level;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static feign.Logger.Level.*;
import static feign.Request.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SLF4JLoggerTest {

    SLF4JLogger logger = new SLF4JLogger(feign.Logger.class);

    @Nested
    @DisplayName("TRACE")
    @SpringBootTest(classes = Config.class)
    @ExtendWith(OutputCaptureExtension.class)
    @ActiveProfiles("trace")
    class Trace {

        @ParameterizedTest(name = "{0}")
        @MethodSource("levelArgumentsProvider")
        void level(Level level, String log, CapturedOutput output) throws Exception {
            logger.logRequest(CONFIG_KEY, level, REQUEST);
            logger.logAndRebufferResponse(CONFIG_KEY, level, RESPONSE, 12);

            assertThat(output.getOut()).contains(log);
        }

        static Stream<Arguments> levelArgumentsProvider() {
            return Stream.of(
                    arguments(BASIC,
                            """
                                    feign.Logger                             :\s
                                    [Api#get]\s
                                    ---> GET http://api.example.com/api HTTP/1.1
                                    <--- HTTP/1.1 200 OK (12ms)
                                    """),
                    arguments(HEADERS,
                            """
                                    feign.Logger                             :\s
                                    [Api#get]\s
                                    ---> GET http://api.example.com/api HTTP/1.1
                                    Accept: application/json
                                    ---> END HTTP (0-byte body)
                                    <--- HTTP/1.1 200 OK (12ms)
                                    content-length: 62
                                    content-type: application/json
                                    <--- END HTTP (13-byte body)
                                    """),
                    arguments(FULL,
                            """
                                    feign.Logger                             :\s
                                    [Api#get]\s
                                    ---> GET http://api.example.com/api HTTP/1.1
                                    Accept: application/json
                                    ---> END HTTP (0-byte body)
                                    <--- HTTP/1.1 200 OK (12ms)
                                    content-length: 62
                                    content-type: application/json
                                                                        
                                    {"value":"1"}
                                    <--- END HTTP (13-byte body)
                                    """)
            );
        }
    }

    @Nested
    @DisplayName("INFO")
    @SpringBootTest(classes = Config.class)
    @ExtendWith(OutputCaptureExtension.class)
    @ActiveProfiles("info")
    class Info {

        @Test
        @DisplayName("BASIC")
        void basic(CapturedOutput output) throws Exception {
            logger.logRequest(CONFIG_KEY, BASIC, REQUEST);
            logger.logAndRebufferResponse(CONFIG_KEY, BASIC, RESPONSE, 12);

            assertThat(output.getOut()).isEmpty();
        }
    }

    @Configuration
    static class Config {

    }

    static final String CONFIG_KEY = "Api#get()";
    static final Request REQUEST = new RequestTemplate()
            .method(GET)
            .target("http://api.example.com")
            .uri("/api")
            .header("Accept", "application/json")
            .resolve(emptyMap())
            .request();
    static final Response RESPONSE = Response.builder()
            .status(200)
            .reason("OK")
            .request(REQUEST)
            .headers(Map.of(
                    "Content-Type", List.of("application/json"),
                    "Content-Length", List.of("62")
            ))
            .body("{\"value\":\"1\"}", UTF_8)
            .build();
}
