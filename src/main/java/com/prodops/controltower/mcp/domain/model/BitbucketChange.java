package com.prodops.controltower.mcp.domain.model;

import java.time.Instant;
import java.util.List;

public record BitbucketChange(
    String changeId,
    String serviceId,
    String workspace,
    String repoSlug,
    String projectKey,
    String commitSha,
    String branch,
    String title,
    String description,
    List<String> labels,
    List<String> changedFiles,
    String author,
    List<String> reviewers,
    Instant committedAt,
    Instant mergedAt,
    BitbucketPullRequest pullRequest,
    List<BitbucketPipelineRun> pipelineRuns,
    List<DeepLink> deepLinks) {}
