package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.LogEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LogSignatureAnalyzer {

  private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
  private static final Pattern HEX_PATTERN = Pattern.compile("\\b[0-9a-f]{8,}\\b");

  public List<LogErrorSignature> summarize(List<LogEvent> events, Instant symptomOnset, int limit) {
    return events.stream().collect(Collectors.groupingBy(this::signatureFor)).entrySet().stream()
        .map(entry -> toSignature(entry.getKey(), entry.getValue(), symptomOnset))
        .sorted(Comparator.comparingInt(LogErrorSignature::count).reversed())
        .limit(limit)
        .toList();
  }

  public String dominantSignature(List<LogErrorSignature> signatures) {
    return signatures.stream().findFirst().map(LogErrorSignature::signature).orElse("none");
  }

  private LogErrorSignature toSignature(
      String signature, List<LogEvent> events, Instant symptomOnset) {
    List<String> traceIds =
        events.stream()
            .map(LogEvent::traceId)
            .filter(traceId -> traceId != null && !traceId.isBlank())
            .distinct()
            .limit(5)
            .toList();
    Instant firstSeen =
        events.stream().map(LogEvent::observedAt).min(Instant::compareTo).orElse(Instant.EPOCH);
    Instant lastSeen =
        events.stream().map(LogEvent::observedAt).max(Instant::compareTo).orElse(Instant.EPOCH);
    LogEvent sample =
        events.stream().max(Comparator.comparing(LogEvent::observedAt)).orElse(events.getFirst());
    boolean novel =
        symptomOnset != null
            && !firstSeen.isBefore(symptomOnset.minus(Duration.ofMinutes(15)))
            && !firstSeen.isAfter(symptomOnset.plus(Duration.ofMinutes(20)));
    double confidence = Math.min(0.95d, 0.35d + (events.size() * 0.07d) + (novel ? 0.1d : 0.0d));
    return new LogErrorSignature(
        signature,
        sample.severity(),
        events.size(),
        firstSeen,
        lastSeen,
        novel,
        sample.sanitizedMessage(),
        traceIds,
        sample.deepLink(),
        confidence);
  }

  private String signatureFor(LogEvent event) {
    if (event.exceptionSignature() != null && !event.exceptionSignature().isBlank()) {
      return event.exceptionSignature();
    }
    String normalized = NUMBER_PATTERN.matcher(event.sanitizedMessage()).replaceAll("<num>");
    normalized = HEX_PATTERN.matcher(normalized).replaceAll("<hex>");
    return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
  }
}
