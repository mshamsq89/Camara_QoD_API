package com.camaraproject.qod.config;

import com.camaraproject.qod.model.SessionInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, SessionInfo> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        // =======================================================
        // == THE FIX: REGISTER THE JAVA 8+ TIME MODULE       ==
        // =======================================================
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // This enables OffsetDateTime serialization

        Jackson2JsonRedisSerializer<SessionInfo> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, SessionInfo.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, SessionInfo> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, SessionInfo> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
