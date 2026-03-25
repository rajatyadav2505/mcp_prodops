package com.idfcfirstbank.prodops.controltower.mcp.observability;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import com.idfcfirstbank.prodops.controltower.mcp.policy.PolicyDeniedException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

  private final Cache<String, WindowCounter> cache;
  private final ProdOpsProperties properties;
  private final Clock clock;

  public RequestRateLimiter(ProdOpsProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    this.cache =
        Caffeine.newBuilder()
            .maximumSize(properties.cache().maxEntries())
            .expireAfterWrite(properties.rateLimit().window())
            .build();
  }

  public void assertAllowed(String identity, String operation) {
    Instant now = Instant.now(clock);
    String key = identity + ":" + operation;
    WindowCounter counter = cache.get(key, ignored -> new WindowCounter(now, 0));
    if (now.isAfter(counter.windowStart().plus(properties.rateLimit().window()))) {
      counter = new WindowCounter(now, 0);
    }
    int nextCount = counter.count() + 1;
    cache.put(key, new WindowCounter(counter.windowStart(), nextCount));
    if (nextCount > properties.rateLimit().requestsPerWindow()) {
      throw new PolicyDeniedException("Request rate exceeded configured policy window.");
    }
  }

  private record WindowCounter(Instant windowStart, int count) {}
}
