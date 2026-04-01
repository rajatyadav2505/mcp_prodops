package com.prodops.controltower.mcp.adapter.history;

import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import com.prodops.controltower.mcp.domain.port.IncidentHistoryPort;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live")
public class EmptyIncidentHistoryAdapter implements IncidentHistoryPort {

  @Override
  public List<HistoricalIncident> listHistoricalIncidents(
      String cluster, String namespace, String serviceOrWorkload, int limit) {
    return List.of();
  }
}
