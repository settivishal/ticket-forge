package com.ticketforge.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketforge.event.RedisEventSubscriber;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    public static final String CACHE_SYSTEM_STATUS = "ticketforge:system_status";
    public static final String CACHE_SEATS = "ticketforge:seats";
    public static final String CACHE_SEAT = "ticketforge:seat";
    public static final String CACHE_WAITLIST = "ticketforge:waitlist";
    public static final String CACHE_RESERVATIONS = "ticketforge:reservations";

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSsl;

    @Value("${ticketforge.redis.enabled:auto}")
    private String redisEnabled;

    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    @Bean
    public RedisSerializer<Object> redisJsonSerializer() {
        return new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (redisUrl != null && !redisUrl.isBlank()) {
            String uriStr = redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")
                    ? redisUrl
                    : (redisSsl ? "rediss://" : "redis://") + redisUrl;
            RedisURI uri = RedisURI.create(uriStr);
            RedisConfiguration redisConfig = LettuceConnectionFactory.createRedisConfiguration(uri);
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
            if (redisSsl || uriStr.startsWith("rediss://")) {
                clientConfig.useSsl();
            }
            clientConfig.commandTimeout(Duration.ofMillis(3000));
            var factory = new LettuceConnectionFactory(redisConfig, clientConfig.build());
            factory.afterPropertiesSet();
            return factory;
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost != null && !redisHost.isBlank() ? redisHost : "localhost");
        config.setPort(redisPort > 0 ? redisPort : 6379);
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.setPassword(RedisPassword.of(redisPassword));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
        if (redisSsl) {
            clientConfig.useSsl();
        }
        clientConfig.commandTimeout(Duration.ofMillis(3000));
        var factory = new LettuceConnectionFactory(config, clientConfig.build());
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            @Autowired(required = false) RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> redisJsonSerializer) {
        if (connectionFactory == null) {
            return null;
        }

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<String> stringSerializer = RedisSerializer.string();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        if (!isRedisAvailable()) {
            return createInMemoryCacheManager();
        }

        RedisConnectionFactory connectionFactory = redisConnectionFactory();
        if (connectionFactory == null) {
            return createInMemoryCacheManager();
        }

        try {
            RedisSerializer<Object> redisJsonSerializer = redisJsonSerializer();
            RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer));

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put(CACHE_SYSTEM_STATUS, defaultCacheConfig.entryTtl(Duration.ofSeconds(10)));
            cacheConfigurations.put(CACHE_SEATS, defaultCacheConfig.entryTtl(Duration.ofSeconds(60)));
            cacheConfigurations.put(CACHE_SEAT, defaultCacheConfig.entryTtl(Duration.ofSeconds(60)));
            cacheConfigurations.put(CACHE_WAITLIST, defaultCacheConfig.entryTtl(Duration.ofSeconds(10)));
            cacheConfigurations.put(CACHE_RESERVATIONS, defaultCacheConfig.entryTtl(Duration.ofSeconds(30)));

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to initialize RedisCacheManager, operating with In-Memory fallback: {}", e.getMessage());
            return createInMemoryCacheManager();
        }
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failure on key '{}' in cache '{}': {}. Falling back to source.", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failure on key '{}' in cache '{}': {}. Proceeding.", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failure on key '{}' in cache '{}': {}. Proceeding.", key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failure in cache '{}': {}. Proceeding.", cache.getName(), exception.getMessage());
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "ticketforge.redis.pubsub.enabled", havingValue = "true", matchIfMissing = false)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Autowired(required = false) RedisConnectionFactory connectionFactory,
            RedisEventSubscriber eventSubscriber) {
        if (!isRedisAvailable() || connectionFactory == null) {
            return null;
        }
        try {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(eventSubscriber, new ChannelTopic("ticketforge:events"));
            return container;
        } catch (Exception e) {
            log.warn("Could not start RedisMessageListenerContainer: {}", e.getMessage());
            return null;
        }
    }

    private CacheManager createInMemoryCacheManager() {
        log.info("Using In-Memory ConcurrentMapCacheManager (zero network latency, non-blocking)");
        return new ConcurrentMapCacheManager(
                CACHE_SYSTEM_STATUS, CACHE_SEATS, CACHE_SEAT, CACHE_WAITLIST, CACHE_RESERVATIONS
        );
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "ticketforge.redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        if (!isRedisAvailable()) {
            log.info("Redis is offline. Redisson distributed locks will operate using local JVM ReentrantLock fallback.");
            return null;
        }

        Config config = new Config();
        String address;
        if (redisUrl != null && !redisUrl.isBlank()) {
            address = redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")
                    ? redisUrl
                    : (redisSsl ? "rediss://" : "redis://") + redisUrl;
        } else {
            String scheme = redisSsl ? "rediss://" : "redis://";
            address = scheme + redisHost + ":" + redisPort;
        }

        log.info("Configuring Redisson client connected to {}", address);
        var singleServer = config.useSingleServer()
                .setAddress(address)
                .setConnectTimeout(2000)
                .setTimeout(2000)
                .setRetryAttempts(1)
                .setRetryInterval(500);

        if (redisPassword != null && !redisPassword.isBlank()) {
            singleServer.setPassword(redisPassword);
        }

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            log.warn("Could not connect to Redis at {}: {}. Operating with local locking.", address, e.getMessage());
            return null;
        }
    }

    private boolean isRedisAvailable() {
        if ("false".equalsIgnoreCase(redisEnabled)) {
            return false;
        }
        try {
            String host = (redisHost != null && !redisHost.isBlank()) ? redisHost : "localhost";
            int port = redisPort > 0 ? redisPort : 6379;
            if (redisUrl != null && !redisUrl.isBlank()) {
                URI uri = URI.create(redisUrl.replace("rediss://", "http://").replace("redis://", "http://"));
                if (uri.getHost() != null) {
                    host = uri.getHost();
                    port = uri.getPort() > 0 ? uri.getPort() : (redisUrl.startsWith("rediss://") ? 6380 : 6379);
                }
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 800);
                return true;
            }
        } catch (Exception e) {
            log.info("Redis is not reachable at {}:{}. Operating with In-Memory Caching & Local Locking.", redisHost, redisPort);
            return false;
        }
    }
}
