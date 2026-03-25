package com.idfcfirstbank.prodops.controltower.mcp.adapter.fixture;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.DashboardPort;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureDashboardAdapter implements DashboardPort {

  private final FixtureScenarioLoader loader;

  public FixtureDashboardAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<DashboardInfo> search(
      String cluster, String query, List<String> tags, String folder, int limit) {
    return loader.loadRepository().dashboards().stream()
        .filter(dashboard -> dashboard.cluster().equals(cluster))
        .filter(
            dashboard ->
                query == null
                    || query.isBlank()
                    || dashboard.title().toLowerCase().contains(query.toLowerCase()))
        .filter(dashboard -> tags == null || tags.isEmpty() || dashboard.tags().containsAll(tags))
        .filter(
            dashboard -> folder == null || folder.isBlank() || dashboard.folder().equals(folder))
        .limit(limit)
        .toList();
  }

  @Override
  public Optional<DashboardInfo> getByUid(String cluster, String dashboardUid) {
    return loader.loadRepository().dashboards().stream()
        .filter(dashboard -> dashboard.cluster().equals(cluster))
        .filter(dashboard -> dashboard.uid().equals(dashboardUid))
        .findFirst();
  }
}
