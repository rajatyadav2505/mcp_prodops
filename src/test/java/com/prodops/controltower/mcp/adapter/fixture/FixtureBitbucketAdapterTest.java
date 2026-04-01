package com.prodops.controltower.mcp.adapter.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodops.controltower.mcp.TestFixtures;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixtureBitbucketAdapterTest {

  @Test
  void loadsFixtureChangesWithinTheRequestedWindow() {
    FixtureBitbucketAdapter adapter = new FixtureBitbucketAdapter(loader());

    var changes =
        adapter.listChanges(
            new BitbucketChangeQuery(
                "payments-dev",
                "payments-api",
                "prodops",
                "payments-api",
                null,
                Instant.parse("2026-03-24T22:30:00Z"),
                Instant.parse("2026-03-25T00:00:00Z"),
                10));

    assertThat(changes).isNotEmpty();
    assertThat(changes).anyMatch(change -> change.commitSha().startsWith("3b2f1a9"));
  }

  private FixtureScenarioLoader loader() {
    return new FixtureScenarioLoader(
        TestFixtures.prodOpsProperties(
            Path.of("src/test/resources/config/service-catalog-test.yaml"),
            Path.of("src/test/resources/config/risk-weights-test.yaml"),
            Path.of("src/test/resources/fixtures"),
            List.of("scenario_fixture_smoke")),
        new ObjectMapper().findAndRegisterModules());
  }
}
