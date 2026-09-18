package org.fl.flowledger.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Configuration
public class RedisConfig {

    @Bean
    @Profile("dev")
    CommandLineRunner testRedis(StringRedisTemplate redisTemplate) {
        return args -> {
            redisTemplate.opsForValue().set("flowledger:test", "ok");

            String value = redisTemplate
                    .opsForValue()
                    .get("flowledger:test");

            log.info("Redis connectivity check: {}", value);
        };
    }
}
