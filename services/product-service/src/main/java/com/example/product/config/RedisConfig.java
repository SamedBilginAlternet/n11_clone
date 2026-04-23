package com.example.product.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        var jsonSerializer = new GenericJackson2JsonRedisSerializer();
        var defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withCacheConfiguration("products:byId",
                        defaults.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("products:bySlug",
                        defaults.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("products:categories",
                        defaults.entryTtl(Duration.ofHours(24)))
                .build();
    }
}
