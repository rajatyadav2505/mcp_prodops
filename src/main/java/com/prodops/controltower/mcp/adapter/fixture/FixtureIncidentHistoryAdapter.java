package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.port.IncidentHistoryPort;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureIncidentHistoryAdapter implements IncidentHistoryPort {

  private final FixtureScenarioLoader loader;

  public FixtureIncidentHistoryAdapter(FixtureScenarioLoader loader) {
    this.loader = loader;
  }

  @Override
  public List<HistoricalIncident> listHistoricalIncidents(
      String cluster, String namespace, String serviceOrWorkload, int limit) {
    return loader.loadRepository().historicalIncidents().stream()
        .filter(
            incident ->
                incident.serviceId() == null || incident.serviceId().equals(serviceOrWorkload))
        .sorted(Comparator.comparing(HistoricalIncident::occurredAt).reversed())
        .limit(limit)
        .toList();
  }
}
