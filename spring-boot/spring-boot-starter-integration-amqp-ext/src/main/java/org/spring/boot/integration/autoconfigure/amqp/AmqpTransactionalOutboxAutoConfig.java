package org.spring.boot.integration.autoconfigure.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.amqp.core.MessageProperties.CONTENT_TYPE_JSON;
import static org.springframework.amqp.support.AmqpHeaders.CONTENT_ENCODING;
import static org.springframework.amqp.support.AmqpHeaders.CONTENT_TYPE;

@AutoConfiguration(after = IntegrationAutoConfiguration.class, before = RabbitAutoConfiguration.class)
public class AmqpTransactionalOutboxAutoConfig {

    @Bean
    IntegrationFlow messageRelay(MessageChannel messageBrokerOutboundChannel, AmqpTemplate amqpTemplate) {
        return IntegrationFlow.from(messageBrokerOutboundChannel)
                .handle(Amqp.outboundAdapter(amqpTemplate).exchangeName("messages").headersMappedLast(true))
                .get();
    }

    @Bean
    RabbitTemplateConfigurer rabbitTemplateConfigurer(
            RabbitProperties properties,
            ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers) {

        var configurer = new RabbitTemplateConfigurer(properties);
        configurer.setMessageConverter(new SimpleMessageConverter());
        configurer.setRetryTemplateCustomizers(retryTemplateCustomizers.orderedStream().toList());

        return configurer;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObjectMapper.class)
    static class JacksonConfig {

        @Bean
        Serializer<? super Message<?>> serializer(ObjectMapper objectMapper) {
            return (message, outputStream) -> objectMapper.writeValue(outputStream, message.getPayload());
        }

        @Bean
        Deserializer<? extends Message<?>> deserializer() {
            return inputStream -> MessageBuilder.withPayload(inputStream.readAllBytes())
                    .setHeader(CONTENT_ENCODING, UTF_8)
                    .setHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .build();
        }
    }
}
