package com.prodops.controltower.mcp.domain.port;

import com.prodops.controltower.mcp.domain.model.TraceSearchQuery;
import com.prodops.controltower.mcp.domain.model.TraceSummary;
import java.util.List;
import java.util.Optional;

public interface JaegerTracePort {

  List<TraceSummary> searchTraces(TraceSearchQuery query);

  Optional<TraceSummary> getTrace(String cluster, String traceId);
}
