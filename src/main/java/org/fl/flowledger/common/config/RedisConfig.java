package org.fl.flowledger.common.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    CommandLineRunner testRedis(StringRedisTemplate redisTemplate) {
        return args -> {
            redisTemplate.opsForValue().set("flowledger:test", "ok");

            String value = redisTemplate
                    .opsForValue()
                    .get("flowledger:test");

            System.out.println("Redis test: " + value);
        };
    }
}