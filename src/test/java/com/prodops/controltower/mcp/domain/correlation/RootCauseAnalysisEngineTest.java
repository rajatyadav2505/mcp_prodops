package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.ObservabilityCoverageGap;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RootCauseAnalysisEngineTest {

  private final ChangeAttributionEngine changeAttributionEngine = new ChangeAttributionEngine();
  private final RootCauseAnalysisEngine engine = new RootCauseAnalysisEngine();
  private final CoverageGapAnalyzer coverageGapAnalyzer = new CoverageGapAnalyzer();

  @Test
  void prefersDependencyFailureOverRecentChangeWhenJaegerShowsDifferentRootFailure() {
    var context = ChangeAttributionEngineTest.checkoutIncidentContext();
    List<LogErrorSignature> signatures =
        List.of(
            new LogErrorSignature(
                "merchant-profile returned HTTP 503",
                "ERROR",
                2,
                Instant.parse("2026-03-25T10:18:00Z"),
                Instant.parse("2026-03-25T10:24:00Z"),
                true,
                "merchant-profile returned HTTP 503 during customer risk lookup",
                List.of("trace-checkout-001"),
                null,
                0.7d));

    var changeAssessment =
        changeAttributionEngine.assess(
            context, Instant.parse("2026-03-25T10:14:00Z"), signatures, RiskWeights.defaults());
    List<ObservabilityCoverageGap> gaps =
        coverageGapAnalyzer.analyze(context, Instant.parse("2026-03-25T10:14:00Z"));
    var analysis =
        engine.assess(context, signatures, changeAssessment, gaps, RiskWeights.defaults());

    assertThat(analysis.primarySuspect()).isNotNull();
    assertThat(analysis.primarySuspect().causeType().name()).isEqualTo("DEPENDENCY_FAILURE");
    assertThat(analysis.primarySuspect().entity()).isEqualTo("merchant-profile");
    assertThat(analysis.alternateSuspects())
        .anyMatch(candidate -> candidate.causeType().name().equals("CHANGE_REGRESSION"));
  }
}
