package com.youthnightschool.config;

import io.lettuce.core.SslOptions;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration. Uses {@code spring.data.redis.url} from application properties.
 * Provides a {@link RedisTemplate} with String key/value serialization.
 * Supports TLS via {@code app.redis.tls} and {@code app.redis.tls-reject-unauthorized}.
 */
@Configuration
public class RedisConfig {

  private final AppProperties appProperties;

  public RedisConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
    return builder -> {
      if (appProperties.getRedis().isTls()) {
        builder.useSsl();
        if (!appProperties.getRedis().isTlsRejectUnauthorized()) {
          builder.clientOptions(
              io.lettuce.core.ClientOptions.builder()
                  .sslOptions(SslOptions.builder().jdkSslProvider().build())
                  .build()
          );
        }
      }
    };
  }
}
