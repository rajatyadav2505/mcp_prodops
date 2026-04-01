package com.prodops.controltower.mcp.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class FixturePaymentsTestClockConfig {

  @Bean
  @Primary
  Clock testClock() {
    return Clock.fixed(Instant.parse("2026-03-24T23:55:00Z"), ZoneOffset.UTC);
  }
}
