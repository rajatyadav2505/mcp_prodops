package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.time.Duration;
import java.time.Instant;

public record DataFreshness(
    Instant generatedAt, Instant sourceObservedAt, Duration staleness, boolean cached) {}
