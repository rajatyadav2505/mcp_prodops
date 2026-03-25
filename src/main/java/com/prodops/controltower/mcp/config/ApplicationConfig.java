package com.prodops.controltower.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  @Qualifier("yamlObjectMapper")
  ObjectMapper yamlObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
        .factory(new YAMLFactory())
        .findModulesViaServiceLoader(true)
        .build();
  }

  @Bean
  CacheManager cacheManager(ProdOpsProperties properties) {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    manager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(properties.cache().maxEntries())
            .expireAfterWrite(properties.cache().dashboardTtl()));
    return manager;
  }

  @Bean
  RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }
}
