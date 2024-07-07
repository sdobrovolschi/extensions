package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
class TestConfig {

    @TestConfiguration(proxyBeanMethods = false)
    @Slf4j
    static class RabbitConfig {

        @Bean
        Jackson2JsonMessageConverter jsonMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    }
}
