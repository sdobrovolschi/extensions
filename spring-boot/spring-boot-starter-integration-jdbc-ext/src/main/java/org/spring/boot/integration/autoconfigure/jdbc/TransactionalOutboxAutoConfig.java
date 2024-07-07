package org.spring.boot.integration.autoconfigure.jdbc;

import org.postgresql.PGConnection;
import org.postgresql.jdbc.PgConnection;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.dsl.QueueChannelSpec;
import org.springframework.integration.jdbc.channel.PostgresChannelMessageTableSubscriber;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.springframework.integration.dsl.MessageChannels.queue;

/**
 * <a href="https://microservices.io/patterns/data/transactional-outbox.html">Pattern: Transactional Outbox</a>
 */
@AutoConfiguration(after = IntegrationAutoConfiguration.class)
public class TransactionalOutboxAutoConfig {

    private static final String OUTBOX_TABLE_PREFIX = "OUTBOX_";
    private static final String GROUP_ID = "message-relay";

    @Bean
    @ConditionalOnBean(DataSource.class)
    JdbcChannelMessageStore channelMessageStore(
            DataSource dataSource,
            ObjectProvider<Serializer<? super Message<?>>> serializer,
            ObjectProvider<Deserializer<? extends Message<?>>> deserializer,
            JdbcChannelMessageStoreCustomizer customizer) {

        var messageStore = new JdbcChannelMessageStore(dataSource);
        messageStore.setCheckDatabaseOnStart(false);
        messageStore.setTablePrefix(OUTBOX_TABLE_PREFIX);

        serializer.ifAvailable(messageStore::setSerializer);
        deserializer.ifAvailable(messageStore::setDeserializer);

        customizer.customize(messageStore);

        return messageStore;
    }

    @Configuration(proxyBeanMethods = false)
    static class OutboxConfig {

        @Bean
        QueueChannelSpec events(ChannelMessageStore channelMessageStore) {
            return queue("events", channelMessageStore, GROUP_ID);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MessageRelayConfig {

        /**
         * <a href="https://docs.spring.io/spring-integration/reference/6.2/jdbc/message-store.html#postgresql-push">PostgreSQL: Receiving Push Notifications</a>
         * <a href="https://www.postgresql.org/docs/current/sql-notify.html">NOTIFY command</a>
         * <a href="https://www.postgresql.org/docs/current/sql-listen.html">LISTEN command</a>
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(PGConnection.class)
        static class PostgresMessageRelayConfig {

            @Bean
            JdbcChannelMessageStoreCustomizer channelMessageStoreCustomizer() {
                return channelMessageStore -> channelMessageStore
                        .setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
            }

            @Bean
            PostgresSubscribableChannel messageBrokerOutboundChannel(
                    PostgresChannelMessageTableSubscriber subscriber,
                    JdbcChannelMessageStore channelMessageStore,
                    PlatformTransactionManager transactionManager) {

                return new PostgresSubscribableChannel(channelMessageStore,
                        GROUP_ID,
                        subscriber,
                        new CurrentThreadExecutor(),
                        transactionManager);
            }

            @Bean
            PostgresChannelMessageTableSubscriber subscriber(
                    DataSourceProperties properties,
                    AsyncTaskExecutor messageRelayTaskExecutor) {

                var subscriber = new PostgresChannelMessageTableSubscriber(() ->
                        DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword())
                                .unwrap(PgConnection.class), OUTBOX_TABLE_PREFIX);
                subscriber.setTaskExecutor(messageRelayTaskExecutor);

                return subscriber;
            }
        }

        @Bean
        AsyncTaskExecutor messageRelayTaskExecutor() {
            return new ThreadPoolTaskExecutorBuilder()
                    .corePoolSize(1)
                    .maxPoolSize(1)
                    .queueCapacity(1)
                    .keepAlive(Duration.ZERO)
                    .threadNamePrefix("message-relay-")
                    .build();
        }
    }

    @FunctionalInterface
    interface JdbcChannelMessageStoreCustomizer {

        void customize(JdbcChannelMessageStore channelMessageStore);
    }

    private static final class PostgresSubscribableChannel extends AbstractSubscribableChannel
            implements PostgresChannelMessageTableSubscriber.Subscription {

        private static final LogAccessor LOGGER = new LogAccessor(PostgresSubscribableChannel.class);

        private static final Optional<?> FALLBACK_STUB = Optional.of(new Object());

        private final JdbcChannelMessageStore jdbcChannelMessageStore;

        private final Object groupId;

        private final PostgresChannelMessageTableSubscriber messageTableSubscriber;

        private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

        private final TransactionTemplate transactionTemplate;

        private final Executor executor;

        private RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

        private ErrorHandler errorHandler = ReflectionUtils::rethrowRuntimeException;

        private volatile boolean hasHandlers;


        public PostgresSubscribableChannel(
                JdbcChannelMessageStore jdbcChannelMessageStore,
                                           Object groupId,
                                           PostgresChannelMessageTableSubscriber messageTableSubscriber,
                Executor executor,
                PlatformTransactionManager transactionManager) {

            this.jdbcChannelMessageStore = jdbcChannelMessageStore;
            this.groupId = groupId;
            this.messageTableSubscriber = messageTableSubscriber;
            this.executor = executor;
            this.transactionTemplate = new TransactionTemplate(transactionManager);
        }

        public void setRetryTemplate(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
        }

        public void setErrorHandler(ErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        @Override
        public boolean subscribe(MessageHandler handler) {
            boolean subscribed = super.subscribe(handler);
            if (this.dispatcher.getHandlerCount() == 1) {
                this.messageTableSubscriber.subscribe(this);
                this.hasHandlers = true;
            }
            return subscribed;
        }

        @Override
        public boolean unsubscribe(MessageHandler handle) {
            boolean unsubscribed = super.unsubscribe(handle);
            if (this.dispatcher.getHandlerCount() == 0) {
                this.messageTableSubscriber.unsubscribe(this);
                this.hasHandlers = false;
            }
            return unsubscribed;
        }

        @Override
        protected MessageDispatcher getDispatcher() {
            return this.dispatcher;
        }

        @Override
        protected boolean doSend(Message<?> message, long timeout) {
            this.jdbcChannelMessageStore.addMessageToGroup(this.groupId, message);
            return true;
        }

        @Override
        public void notifyUpdate() {
            this.executor.execute(() -> {
                Optional<?> dispatchedMessage;
                do {
                    dispatchedMessage = pollAndDispatchMessage();
                } while (dispatchedMessage.isPresent());
            });
        }

        private Optional<?> pollAndDispatchMessage() {
            try {
                return doPollAndDispatchMessage();
            }
            catch (Exception ex) {
                try {
                    this.errorHandler.handleError(ex);
                }
                catch (Exception ex1) {
                    LOGGER.error(ex, "Exception during message dispatch");
                }
                return FALLBACK_STUB;
            }
        }

        private Optional<?> doPollAndDispatchMessage() {
            if (this.hasHandlers) {
                if (this.transactionTemplate != null) {
                    return this.retryTemplate.execute(context ->
                            this.transactionTemplate.execute(status ->
                                    pollMessage()
                                            .filter(message -> {
                                                if (!this.hasHandlers) {
                                                    status.setRollbackOnly();
                                                    return false;
                                                }
                                                return true;
                                            })
                                            .map(this::dispatch)));
                }
                else {
                    return pollMessage()
                            .map(message -> this.retryTemplate.execute(context -> dispatch(message)));
                }
            }
            return Optional.empty();
        }

        private Optional<Message<?>> pollMessage() {
            return Optional.ofNullable(this.jdbcChannelMessageStore.pollMessageFromGroup(this.groupId));
        }

        private Message<?> dispatch(Message<?> message) {
            this.dispatcher.dispatch(message);
            return message;
        }

        @Override
        public String getRegion() {
            return this.jdbcChannelMessageStore.getRegion();
        }

        @Override
        public Object getGroupId() {
            return this.groupId;
        }
    }

    private static final class CurrentThreadExecutor implements Executor {

        @Override
        public void execute(Runnable r) {
            r.run();
        }
    }
}
