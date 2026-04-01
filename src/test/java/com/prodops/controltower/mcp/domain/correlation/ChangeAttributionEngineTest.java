package com.prodops.controltower.mcp.domain.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.BitbucketPipelineRun;
import com.prodops.controltower.mcp.domain.model.BitbucketPullRequest;
import com.prodops.controltower.mcp.domain.model.DataFreshness;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.model.IncidentContext;
import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import com.prodops.controltower.mcp.domain.model.RiskLevel;
import com.prodops.controltower.mcp.domain.model.ServiceCatalogEntry;
import com.prodops.controltower.mcp.domain.model.SloTarget;
import com.prodops.controltower.mcp.domain.model.TraceDependencyEdge;
import com.prodops.controltower.mcp.domain.model.TraceSpanSummary;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadHealth;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.scoring.RiskWeights;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChangeAttributionEngineTest {

  private final ChangeAttributionEngine engine = new ChangeAttributionEngine();

  @Test
  void ranksVersionAlignedChangeAsLeadingSuspect() {
    IncidentContext context = paymentsIncidentContext();
    List<LogErrorSignature> signatures =
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
                0.82d));

    ChangeAttributionEngine.ChangeAssessment assessment =
        engine.assess(
            context, Instant.parse("2026-03-25T10:12:00Z"), signatures, RiskWeights.defaults());

    assertThat(assessment.candidates()).isNotEmpty();
    assertThat(assessment.candidates().getFirst().offendingChange().changeId())
        .isEqualTo("payments-pr-482");
    assertThat(assessment.candidates().getFirst().score()).isGreaterThanOrEqualTo(68.0d);
  }

  @Test
  void penalizesRecentChangeWhenJaegerShowsDifferentServiceFailingFirst() {
    IncidentContext context = checkoutIncidentContext();
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

    ChangeAttributionEngine.ChangeAssessment assessment =
        engine.assess(
            context, Instant.parse("2026-03-25T10:14:00Z"), signatures, RiskWeights.defaults());

    assertThat(assessment.candidates()).isNotEmpty();
    assertThat(assessment.candidates().getFirst().score()).isLessThan(50.0d);
    assertThat(assessment.candidates().getFirst().weakeningEvidenceIds())
        .anyMatch(reason -> reason.contains("different service failing first"));
  }

  static IncidentContext paymentsIncidentContext() {
    WorkloadInfo workload =
        new WorkloadInfo(
            "prodops-uat",
            "payments-uat",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            Map.of("app.kubernetes.io/version", "2026.03.25-4b91d1c"),
            Map.of("app", "payments-api"),
            4,
            2,
            Instant.parse("2026-03-25T07:00:00Z"),
            Instant.parse("2026-03-25T10:10:00Z"),
            "cards-platform",
            "critical");
    WorkloadHealth health =
        new WorkloadHealth(
            "prodops-uat",
            "payments-uat",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            4,
            2,
            List.of(),
            6,
            Duration.ofMinutes(22),
            List.of(),
            List.of(),
            List.of(),
            84.0d,
            RiskLevel.CRITICAL,
            com.prodops.controltower.mcp.domain.model.HealthVerdict.UNHEALTHY,
            Instant.parse("2026-03-25T10:34:00Z"),
            new DataFreshness(
                Instant.parse("2026-03-25T10:34:00Z"),
                Instant.parse("2026-03-25T10:32:00Z"),
                Duration.ofMinutes(2),
                false));
    BitbucketChange leadingChange =
        new BitbucketChange(
            "payments-pr-482",
            "payments-api",
            "prodops",
            "payments-api",
            "PAY",
            "4b91d1c9aa770f8d2a66f9c5dd18f70f93e40012",
            "release/payments-api",
            "Enable settlement schema v4 for settlement binding",
            "Moves the settlement binder to schema v4 and updates config validation.",
            List.of("schema"),
            List.of(
                "services/payments-api/src/main/java/com/prodops/payments/config/SettlementSchemaBinder.java"),
            "priya.shah",
            List.of("anand.k"),
            Instant.parse("2026-03-25T10:01:00Z"),
            Instant.parse("2026-03-25T10:08:00Z"),
            new BitbucketPullRequest(
                "482",
                "Enable settlement schema v4 for settlement binding",
                "Schema change",
                "feature/schema-v4-rollout",
                "release/payments-api",
                "priya.shah",
                List.of("anand.k"),
                List.of("schema"),
                Instant.parse("2026-03-25T09:25:00Z"),
                Instant.parse("2026-03-25T10:08:00Z"),
                Instant.parse("2026-03-25T10:08:00Z"),
                null),
            List.of(
                new BitbucketPipelineRun(
                    "pipe-9041",
                    "COMPLETED",
                    "FAILED",
                    "release/payments-api",
                    "4b91d1c9aa770f8d2a66f9c5dd18f70f93e40012",
                    true,
                    Instant.parse("2026-03-25T10:09:00Z"),
                    Instant.parse("2026-03-25T10:11:00Z"),
                    null)),
            List.of(
                new DeepLink(
                    "PR 482",
                    EvidenceSource.BITBUCKET,
                    "https://bitbucket.example.internal/payments/pr/482",
                    "Bitbucket pull request")));
    TraceSummary trace =
        new TraceSummary(
            "trace-pay-001",
            "payments-api",
            "POST /payments",
            Instant.parse("2026-03-25T10:14:05Z"),
            Duration.ofMillis(820),
            true,
            "payments-api",
            "bootstrap-config",
            Duration.ofMillis(780),
            List.of(
                new TraceSpanSummary(
                    "span-pay-bootstrap",
                    null,
                    "payments-api",
                    "bootstrap-config",
                    Instant.parse("2026-03-25T10:14:05Z"),
                    Duration.ofMillis(780),
                    true,
                    false,
                    "payments-api-74c6d85d97-5r9vq",
                    "2026.03.25-4b91d1c",
                    Map.of("error", "true"))),
            List.of(),
            List.of(
                new TraceDependencyEdge(
                    "payments-api", "payments-ledger", Duration.ofMillis(60), false)),
            null,
            "2026.03.25-4b91d1c",
            "payments-api-74c6d85d97-5r9vq");
    ServiceCatalogEntry catalogEntry =
        new ServiceCatalogEntry(
            "payments-api",
            "Payments API",
            "prodops-uat",
            "payments-uat",
            "payments-api",
            WorkloadKind.DEPLOYMENT,
            "payments-api",
            "critical",
            "cards-platform",
            List.of("payments-latency"),
            List.of(new SloTarget("latency", "99.9", "250ms", "PT30D")),
            List.of("https://runbooks.example.internal/payments-api"),
            Map.of(
                "error_rate", "clamp_max(0.82,1)",
                "latency", "clamp_max(0.91,1)",
                "cpu", "clamp_max(0.47,1)",
                "memory", "clamp_max(0.52,1)"),
            "prodops",
            "PAY",
            "payments-api",
            List.of("services/payments-api"),
            "payments-api",
            List.of("POST /payments"),
            "service.name:payments-api",
            "payments-*",
            List.of("app.kubernetes.io/version"),
            List.of("app.kubernetes.io/version"),
            List.of("payments-api"),
            List.of("payments-ledger"));
    return new IncidentContext(
        "prodops-uat",
        "payments-uat",
        "payments-api",
        workload,
        health,
        List.of(
            new WarningEvent(
                "prodops-uat",
                "payments-uat",
                "BackOff",
                "Back-off restarting failed container",
                "Pod",
                "payments-api",
                8,
                Instant.parse("2026-03-25T10:12:00Z"),
                Instant.parse("2026-03-25T10:28:00Z"))),
        catalogEntry,
        List.of(leadingChange),
        List.of(),
        List.of(trace),
        List.<HistoricalIncident>of(),
        Instant.parse("2026-03-25T10:34:00Z"),
        health.dataFreshness());
  }

  static IncidentContext checkoutIncidentContext() {
    WorkloadInfo workload =
        new WorkloadInfo(
            "prodops-uat",
            "checkout-uat",
            "checkout-api",
            WorkloadKind.DEPLOYMENT,
            Map.of("app.kubernetes.io/version", "2026.03.25-7d00aa1"),
            Map.of("app", "checkout-api"),
            4,
            4,
            Instant.parse("2026-03-25T06:00:00Z"),
            Instant.parse("2026-03-25T10:02:00Z"),
            "checkout-platform",
            "critical");
    WorkloadHealth health =
        new WorkloadHealth(
            "prodops-uat",
            "checkout-uat",
            "checkout-api",
            WorkloadKind.DEPLOYMENT,
            4,
            4,
            List.of(),
            0,
            Duration.ofMinutes(31),
            List.of(),
            List.of(),
            List.of(),
            69.0d,
            RiskLevel.HIGH,
            com.prodops.controltower.mcp.domain.model.HealthVerdict.DEGRADED,
            Instant.parse("2026-03-25T10:33:00Z"),
            new DataFreshness(
                Instant.parse("2026-03-25T10:33:00Z"),
                Instant.parse("2026-03-25T10:31:00Z"),
                Duration.ofMinutes(2),
                false));
    BitbucketChange innocentChange =
        new BitbucketChange(
            "checkout-pr-211",
            "checkout-api",
            "prodops",
            "checkout-api",
            "CHK",
            "cafe11223344556677889900aabbccddeeff0011",
            "release/checkout-api",
            "Refactor checkout banner copy",
            "UI response message cleanup for low-funds banner handling.",
            List.of("ux"),
            List.of(
                "services/checkout-api/src/main/java/com/prodops/checkout/web/BannerCopyFormatter.java"),
            "megha.s",
            List.of("arpit.r"),
            Instant.parse("2026-03-25T10:05:00Z"),
            Instant.parse("2026-03-25T10:09:00Z"),
            null,
            List.of(
                new BitbucketPipelineRun(
                    "pipe-2211",
                    "COMPLETED",
                    "SUCCESSFUL",
                    "release/checkout-api",
                    "cafe11223344556677889900aabbccddeeff0011",
                    true,
                    Instant.parse("2026-03-25T10:10:00Z"),
                    Instant.parse("2026-03-25T10:13:00Z"),
                    null)),
            List.of());
    TraceSummary trace =
        new TraceSummary(
            "trace-checkout-001",
            "checkout-api",
            "POST /checkout",
            Instant.parse("2026-03-25T10:18:03Z"),
            Duration.ofMillis(1240),
            true,
            "merchant-profile",
            "GET /merchant-profile/{id}",
            Duration.ofMillis(1170),
            List.of(
                new TraceSpanSummary(
                    "span-merchant-profile",
                    "span-checkout-root",
                    "merchant-profile",
                    "GET /merchant-profile/{id}",
                    Instant.parse("2026-03-25T10:18:03.050Z"),
                    Duration.ofMillis(1100),
                    true,
                    false,
                    "merchant-profile-6d5fcb76df-s8k8x",
                    "2026.03.25-bb44219",
                    Map.of("error", "true"))),
            List.of(),
            List.of(
                new TraceDependencyEdge(
                    "checkout-api", "merchant-profile", Duration.ofMillis(1100), true)),
            null,
            "2026.03.25-7d00aa1",
            "checkout-api-5cd4bd5c88-x2p4m");
    ServiceCatalogEntry catalogEntry =
        new ServiceCatalogEntry(
            "checkout-api",
            "Checkout API",
            "prodops-uat",
            "checkout-uat",
            "checkout-api",
            WorkloadKind.DEPLOYMENT,
            "checkout-api",
            "critical",
            "checkout-platform",
            List.of("checkout-latency"),
            List.of(),
            List.of(),
            Map.of(
                "error_rate", "clamp_max(0.61,1)",
                "latency", "clamp_max(0.88,1)",
                "cpu", "clamp_max(0.29,1)",
                "memory", "clamp_max(0.31,1)"),
            "prodops",
            "CHK",
            "checkout-api",
            List.of("services/checkout-api"),
            "checkout-api",
            List.of("POST /checkout"),
            "service.name:checkout-api",
            "checkout-*",
            List.of("app.kubernetes.io/version"),
            List.of("app.kubernetes.io/version"),
            List.of("checkout-api"),
            List.of("merchant-profile"));
    return new IncidentContext(
        "prodops-uat",
        "checkout-uat",
        "checkout-api",
        workload,
        health,
        List.of(),
        catalogEntry,
        List.of(innocentChange),
        List.of(),
        List.of(trace),
        List.<HistoricalIncident>of(),
        Instant.parse("2026-03-25T10:33:00Z"),
        health.dataFreshness());
  }
}
