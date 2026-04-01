package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.CausationClass;
import com.prodops.controltower.mcp.domain.model.CauseType;
import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.model.IncidentFingerprint;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.RootCauseCandidate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncidentFingerprintEngineTest {

  private final IncidentFingerprintEngine engine = new IncidentFingerprintEngine();

  @Test
  void matchesHistoricalIncidentUsingCrossPlaneFingerprint() {
    IncidentFingerprint current =
        engine.fingerprint(
            ChangeAttributionEngineTest.checkoutIncidentContext(),
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
                    0.7d)),
            new RootCauseCandidate(
                "dependency-failure",
                CauseType.DEPENDENCY_FAILURE,
                CausationClass.LIKELY_ROOT_CAUSE,
                "merchant-profile",
                "Downstream dependency failed first.",
                "Jaeger shows merchant-profile failing first.",
                List.of(),
                List.of(),
                72.0d,
                0.82d,
                null,
                List.of("merchant-profile")));

    List<HistoricalIncident> historical =
        List.of(
            new HistoricalIncident(
                "hist-merchant-profile",
                "Checkout latency due to merchant profile outage",
                Instant.parse("2026-01-12T09:10:00Z"),
                "checkout-api",
                CauseType.DEPENDENCY_FAILURE,
                new IncidentFingerprint(
                    "error_rate_ratio:high,latency_slo_ratio:high",
                    List.of("merchant-profile returned HTTP 503"),
                    "merchant-profile/GET /merchant-profile/{id}",
                    "ready=4/4,restarts=0",
                    "checkout-api->merchant-profile",
                    CauseType.DEPENDENCY_FAILURE),
                "Merchant profile failed first.",
                List.of()));

    assertThat(engine.match(current, historical, 3)).hasSize(1);
    assertThat(engine.match(current, historical, 3).getFirst().similarity())
        .isGreaterThanOrEqualTo(0.6d);
  }
}
