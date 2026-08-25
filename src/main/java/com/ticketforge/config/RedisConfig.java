package com.ticketforge.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

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

    @Bean
    public ObjectMapper redisObjectMapper() {
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
    public GenericJackson2JsonRedisSerializer redisJsonSerializer(ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer) {

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
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
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager fallbackCacheManager() {
        log.info("Configuring In-Memory ConcurrentMapCacheManager fallback for local testing");
        return new ConcurrentMapCacheManager(
                CACHE_SYSTEM_STATUS, CACHE_SEATS, CACHE_SEAT, CACHE_WAITLIST, CACHE_RESERVATIONS
        );
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "ticketforge.redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
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
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(2)
                .setRetryInterval(1000);

        if (redisPassword != null && !redisPassword.isBlank()) {
            singleServer.setPassword(redisPassword);
        }

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            log.warn("Could not connect to Redis at {}: {}. Redisson will initialize when Redis is reachable.", address, e.getMessage());
            return null;
        }
    }
}
