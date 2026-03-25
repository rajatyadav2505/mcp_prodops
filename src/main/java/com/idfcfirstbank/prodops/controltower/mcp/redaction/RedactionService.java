package com.idfcfirstbank.prodops.controltower.mcp.redaction;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RedactionService {

  private static final List<PatternReplacement> TOKEN_PATTERNS =
      List.of(
          new PatternReplacement(
              Pattern.compile("(?i)(authorization\\s*:\\s*bearer\\s+)[A-Za-z0-9._\\-+/=]+"),
              "$1[REDACTED]"),
          new PatternReplacement(Pattern.compile("(?i)(password\\s*[=:]\\s*)\\S+"), "$1[REDACTED]"),
          new PatternReplacement(Pattern.compile("(?i)(token\\s*[=:]\\s*)\\S+"), "$1[REDACTED]"),
          new PatternReplacement(Pattern.compile("(?i)(secret\\s*[=:]\\s*)\\S+"), "$1[REDACTED]"),
          new PatternReplacement(Pattern.compile("(?i)(apikey\\s*[=:]\\s*)\\S+"), "$1[REDACTED]"),
          new PatternReplacement(
              Pattern.compile("(?i)(jdbc:[^\\s]+://[^\\s:/]+:)[^@\\s]+@"), "$1[REDACTED]@"),
          new PatternReplacement(Pattern.compile("\\b[A-Za-z0-9+/]{32,}={0,2}\\b"), "[REDACTED]"));

  public List<String> redactLines(List<String> lines) {
    return lines.stream().map(this::redact).toList();
  }

  public String redact(String value) {
    String sanitized = value;
    for (PatternReplacement replacement : TOKEN_PATTERNS) {
      sanitized = replacement.pattern().matcher(sanitized).replaceAll(replacement.replacement());
    }
    return sanitized;
  }

  private record PatternReplacement(Pattern pattern, String replacement) {}
}
