package com.idfcfirstbank.prodops.controltower.mcp.domain.port;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.DashboardInfo;
import java.util.List;
import java.util.Optional;

public interface DashboardPort {

  List<DashboardInfo> search(
      String cluster, String query, List<String> tags, String folder, int limit);

  Optional<DashboardInfo> getByUid(String cluster, String dashboardUid);
}
