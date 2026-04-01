package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import com.prodops.controltower.mcp.domain.port.BitbucketPort;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureBitbucketAdapter implements BitbucketPort {

  private final FixtureScenarioLoader loader;

  public FixtureBitbucketAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<BitbucketChange> listChanges(BitbucketChangeQuery query) {
    return loader.loadRepository().bitbucketChanges().stream()
        .filter(
            change ->
                query.serviceId() == null
                    || query.serviceId().isBlank()
                    || query.serviceId().equals(change.serviceId()))
        .filter(
            change ->
                query.workspace() == null
                    || query.workspace().isBlank()
                    || query.workspace().equals(change.workspace()))
        .filter(
            change ->
                query.repoSlug() == null
                    || query.repoSlug().isBlank()
                    || query.repoSlug().equals(change.repoSlug()))
        .filter(
            change ->
                query.branch() == null
                    || query.branch().isBlank()
                    || query.branch().equals(change.branch()))
        .filter(
            change -> {
              java.time.Instant time =
                  change.mergedAt() == null ? change.committedAt() : change.mergedAt();
              return (query.start() == null || !time.isBefore(query.start()))
                  && (query.end() == null || !time.isAfter(query.end()));
            })
        .sorted(
            Comparator.comparing(
                    (BitbucketChange change) ->
                        change.mergedAt() == null ? change.committedAt() : change.mergedAt())
                .reversed())
        .limit(query.limit())
        .toList();
  }
}
