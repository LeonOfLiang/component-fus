package com.sztus.framework.component.fus.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import com.sztus.framework.component.cache.core.BeanContextAware;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author daniel
 * @date 2020/11/30
 */
@Configuration
public class PrimaryRedisConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.jedis.pool")
    @Scope(value = "prototype")
    public GenericObjectPoolConfig redisPool() {
        return new GenericObjectPoolConfig();
    }

    private StringRedisTemplate getRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setValueSerializer(new GenericFastJsonRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.primary.reader")
    @Primary
    public RedisStandaloneConfiguration primaryReaderRedisConfig() {
        return new RedisStandaloneConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.primary")
    public RedisListenerProperty getPrimaryRedisListenerProperty() {
        return new RedisListenerProperty();
    }

    @Bean
    @Primary
    public JedisConnectionFactory primaryReaderFactory() {
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().usePooling()
                .poolConfig(redisPool())
                .build();
        return new JedisConnectionFactory(primaryReaderRedisConfig(), jedisClientConfiguration);
    }

    @Bean(name = "primaryReaderRedisTemplate")
    @Primary
    public StringRedisTemplate primaryReaderRedisTemplate() {
        StringRedisTemplate template = getRedisTemplate();
        template.setConnectionFactory(primaryReaderFactory());
        return template;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis.primary.writer")
    public RedisStandaloneConfiguration primaryWriterRedisConfig() {
        return new RedisStandaloneConfiguration();
    }

    @Bean
    public JedisConnectionFactory primaryWriterFactory() {
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().usePooling()
                .poolConfig(redisPool())
                .build();
        return new JedisConnectionFactory(primaryWriterRedisConfig(), jedisClientConfiguration);
    }

    @Bean(name = "primaryWriterRedisTemplate")
    public StringRedisTemplate primaryWriterRedisTemplate() {
        StringRedisTemplate template = getRedisTemplate();
        template.setConnectionFactory(primaryWriterFactory());
        return template;
    }

    /**
     * config set notify-keyspace-events "K$lshz"    ??????Redis????????????
     * config set notify-keyspace-events ""          ??????Redis????????????
     * config get notify-keyspace-events             ??????Redis????????????
     */
    @Bean
    public RedisMessageListenerContainer primaryContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(primaryReaderFactory());
        RedisListenerProperty redisListenerProperty = getPrimaryRedisListenerProperty();
        String beanName = redisListenerProperty.getListenerName();
        String listenerKey = redisListenerProperty.getListenerKey();
        if (StringUtils.isNoneBlank(listenerKey, beanName)) {
            MessageListener messageListener = BeanContextAware.getBean(beanName, MessageListener.class);
            if (messageListener != null) {
                container.addMessageListener(messageListener, new PatternTopic(listenerKey));
            }
        }

        return container;
    }
}