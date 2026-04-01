package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CausalEvidenceGraphBuilderTest {

  private final CausalEvidenceGraphBuilder builder = new CausalEvidenceGraphBuilder();

  @Test
  void buildsBoundedCrossPlaneGraph() {
    var graph =
        builder.build(
            ChangeAttributionEngineTest.paymentsIncidentContext(),
            Instant.parse("2026-03-25T10:12:00Z"),
            List.of(
                new LogErrorSignature(
                    "java.lang.IllegalStateException",
                    "ERROR",
                    3,
                    Instant.parse("2026-03-25T10:14:00Z"),
                    Instant.parse("2026-03-25T10:22:00Z"),
                    true,
                    "Settlement schema v4 cannot deserialize payoutConfig",
                    List.of("trace-pay-001"),
                    null,
                    0.82d)),
            List.of(
                new RootCauseCandidate(
                    "change-payments-pr-482",
                    CauseType.CHANGE_REGRESSION,
                    CausationClass.LIKELY_ROOT_CAUSE,
                    "payments-api",
                    "Schema rollout change",
                    "Version-aligned change with failing bootstrap traces.",
                    List.of(),
                    List.of(),
                    84.0d,
                    0.88d,
                    ChangeAttributionEngineTest.paymentsIncidentContext()
                        .bitbucketChanges()
                        .getFirst(),
                    List.of("payments-ledger"))),
            6);

    assertThat(graph.nodes()).isNotEmpty();
    assertThat(graph.nodes()).hasSizeLessThanOrEqualTo(6);
    assertThat(graph.edges()).isNotEmpty();
  }
}
