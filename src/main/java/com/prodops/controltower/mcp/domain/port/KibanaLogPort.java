package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.model.LogEvent;
import com.prodops.controltower.mcp.domain.model.LogSearchQuery;
import java.util.List;

public interface KibanaLogPort {

  List<LogEvent> searchLogs(LogSearchQuery query);
}
