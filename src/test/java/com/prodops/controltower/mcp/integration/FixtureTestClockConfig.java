package com.prodops.controltower.mcp.integration;

import com.prodops.controltower.mcp.TestFixtures;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class FixtureTestClockConfig {

  @Bean
  @Primary
  Clock testClock() {
    return TestFixtures.fixedClock();
  }
}
