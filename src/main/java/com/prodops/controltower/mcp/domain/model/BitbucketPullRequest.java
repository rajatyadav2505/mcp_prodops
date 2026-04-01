package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record BitbucketPullRequest(
    String pullRequestId,
    String title,
    String description,
    String sourceBranch,
    String destinationBranch,
    String author,
    List<String> reviewers,
    List<String> labels,
    Instant createdAt,
    Instant updatedAt,
    Instant mergedAt,
    DeepLink deepLink) {}
