package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;

public record BitbucketPipelineRun(
    String pipelineId,
    String state,
    String result,
    String targetBranch,
    String targetCommit,
    boolean deployment,
    Instant createdAt,
    Instant completedAt,
    DeepLink deepLink) {}
