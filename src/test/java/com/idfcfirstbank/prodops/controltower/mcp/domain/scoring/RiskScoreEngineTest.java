package com.idfcfirstbank.prodops.controltower.mcp.domain.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;

class RiskScoreEngineTest {

  private final RiskScoreEngine engine = new RiskScoreEngine();

  @Test
  void scoresADegradedSignalAsCriticalAtTheUpperBound() {
    RiskAssessment assessment =
        engine.assess(
            new RiskSignal(5, 10, 0, 1.0d, 1.0d, 1.0d, 1.0d, 1.0d, 1.0d, 1.0d),
            RiskWeights.defaults());

    assertThat(assessment.score()).isEqualTo(100.0d);
    assertThat(assessment.level()).isEqualTo(RiskLevel.CRITICAL);
  }

  @Test
  void keepsSparseSignalsInTheLowBand() {
    RiskAssessment assessment =
        engine.assess(
            new RiskSignal(0, 0, 720, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
            RiskWeights.defaults());

    assertThat(assessment.score()).isLessThan(35.0d);
    assertThat(assessment.level()).isEqualTo(RiskLevel.LOW);
  }
}
