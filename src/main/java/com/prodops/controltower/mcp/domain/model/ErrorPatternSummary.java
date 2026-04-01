package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record ErrorPatternSummary(
    String cluster,
    String namespace,
    String serviceOrWorkload,
    String executiveSummary,
    String operatorSummary,
    List<ErrorPatternMatch> matches,
    List<DeepLink> directLinks,
    Instant generatedAt,
    DataFreshness dataFreshness) {

  public record ErrorPatternMatch(
      String pattern,
      String signature,
      int occurrences,
      String example,
      List<String> podNames,
      DeepLink deepLink) {}
}
