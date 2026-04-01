package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.ErrorPatternSummary;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LogPatternAnalyzer {

  private static final Map<String, List<String>> DEFAULT_PATTERNS =
      Map.of(
          "oom", List.of("outofmemory", "oomkilled", "java heap space"),
          "connection_refused", List.of("connection refused", "connectexception"),
          "timeout", List.of("timeout", "timed out", "deadline exceeded"),
          "illegal_state", List.of("illegalstateexception", "illegal state"),
          "null_pointer", List.of("nullpointerexception", "null pointer"),
          "ssl", List.of("sslhandshakeexception", "certificate", "pkix"));

  public List<ErrorPatternSummary.ErrorPatternMatch> summarize(List<LogEvent> events, int limit) {
    Map<String, List<LogEvent>> matches = new LinkedHashMap<>();
    for (LogEvent event : events) {
      String normalized =
          (event.sanitizedMessage() == null ? event.message() : event.sanitizedMessage())
              .toLowerCase(Locale.ROOT);
      for (Map.Entry<String, List<String>> pattern : DEFAULT_PATTERNS.entrySet()) {
        if (pattern.getValue().stream().anyMatch(normalized::contains)) {
          matches.computeIfAbsent(pattern.getKey(), ignored -> new ArrayList<>()).add(event);
        }
      }
    }
    return matches.entrySet().stream()
        .sorted(
            Comparator.comparingInt(
                    (Map.Entry<String, List<LogEvent>> entry) -> entry.getValue().size())
                .reversed())
        .limit(limit)
        .map(entry -> toMatch(entry.getKey(), entry.getValue()))
        .toList();
  }

  private ErrorPatternSummary.ErrorPatternMatch toMatch(String pattern, List<LogEvent> events) {
    LogEvent example = events.getFirst();
    return new ErrorPatternSummary.ErrorPatternMatch(
        pattern,
        example.exceptionSignature() == null || example.exceptionSignature().isBlank()
            ? pattern
            : example.exceptionSignature(),
        events.size(),
        example.sanitizedMessage(),
        events.stream()
            .map(LogEvent::podName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList(),
        example.deepLink() == null
            ? null
            : new DeepLink(
                pattern, EvidenceSource.KIBANA, example.deepLink().url(), "Kibana log view"));
  }
}
