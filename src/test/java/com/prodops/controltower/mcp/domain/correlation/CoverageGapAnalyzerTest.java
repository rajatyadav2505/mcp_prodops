package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CoverageGapAnalyzerTest {

  private final CoverageGapAnalyzer analyzer = new CoverageGapAnalyzer();

  @Test
  void reportsMissingRepoVersionAndTraceCoverage() {
    var context = ChangeAttributionEngineTest.checkoutIncidentContext();
    var gaps = analyzer.analyze(context, context.analysisTime());

    assertThat(gaps).isNotEmpty();
    assertThat(gaps).anyMatch(gap -> gap.type().name().equals("LOG_FIELDS_INSUFFICIENT"));
  }
}
