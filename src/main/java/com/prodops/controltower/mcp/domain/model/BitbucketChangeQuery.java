package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record BitbucketChangeQuery(
    String cluster,
    String serviceId,
    String workspace,
    String repoSlug,
    String branch,
    Instant start,
    Instant end,
    int limit) {}
