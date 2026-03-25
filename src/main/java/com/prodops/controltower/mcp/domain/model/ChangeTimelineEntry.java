package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record ChangeTimelineEntry(
    Instant timestamp, String category, String description, String source) {}
