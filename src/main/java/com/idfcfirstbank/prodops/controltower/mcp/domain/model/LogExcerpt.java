package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record LogExcerpt(
    String podName, String container, Instant collectedAt, List<String> lines, boolean truncated) {}
