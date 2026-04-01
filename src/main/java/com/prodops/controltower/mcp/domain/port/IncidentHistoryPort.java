package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.model.HistoricalIncident;
import java.util.List;

public interface IncidentHistoryPort {

  List<HistoricalIncident> listHistoricalIncidents(
      String cluster, String namespace, String serviceOrWorkload, int limit);
}
