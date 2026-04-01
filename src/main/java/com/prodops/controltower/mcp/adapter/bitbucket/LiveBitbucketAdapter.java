package com.prodops.controltower.mcp.adapter.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.BitbucketChange;
import com.prodops.controltower.mcp.domain.model.BitbucketChangeQuery;
import com.prodops.controltower.mcp.domain.model.BitbucketPipelineRun;
import com.prodops.controltower.mcp.domain.model.BitbucketPullRequest;
import com.prodops.controltower.mcp.domain.model.DeepLink;
import com.prodops.controltower.mcp.domain.model.EvidenceSource;
import com.prodops.controltower.mcp.domain.port.BitbucketPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("live")
public class LiveBitbucketAdapter implements BitbucketPort {

  private final ProdOpsProperties properties;
  private final RestClient.Builder restClientBuilder;

  public LiveBitbucketAdapter(ProdOpsProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClientBuilder = restClientBuilder;
  }

  @Override
  public List<BitbucketChange> listChanges(BitbucketChangeQuery query) {
    ClusterBitbucket config =
        clusterBitbucket(query.cluster(), query.workspace(), query.repoSlug());
    if (config.baseUrl().isBlank() || config.workspace().isBlank() || config.repoSlug().isBlank()) {
      return List.of();
    }
    PullRequestResponse response =
        client(config)
            .get()
            .uri(searchUri(config, query))
            .retrieve()
            .body(PullRequestResponse.class);
    if (response == null || response.values() == null) {
      return List.of();
    }
    return response.values().stream()
        .filter(pr -> pr.state() != null && "MERGED".equalsIgnoreCase(pr.state()))
        .filter(
            pr ->
                query.branch() == null
                    || query.branch().isBlank()
                    || query.branch().equals(pr.source().branch().name()))
        .filter(pr -> !mergedAt(pr).isBefore(query.start()))
        .filter(pr -> !mergedAt(pr).isAfter(query.end()))
        .limit(query.limit())
        .map(pr -> toChange(config, query, pr))
        .toList();
  }

  private BitbucketChange toChange(
      ClusterBitbucket config, BitbucketChangeQuery query, PullRequestValue pullRequest) {
    String commitSha =
        Optional.ofNullable(pullRequest.mergeCommit())
            .map(PullRequestCommit::hash)
            .orElseGet(
                () ->
                    Optional.ofNullable(pullRequest.source())
                        .map(Source::commit)
                        .map(PullRequestCommit::hash)
                        .orElse(""));
    List<String> changedFiles = diffStat(config, pullRequest.id());
    List<BitbucketPipelineRun> pipelineRuns =
        statuses(config, commitSha, pullRequest.source().branch().name());
    DeepLink prLink =
        new DeepLink(
            "Bitbucket PR " + pullRequest.id(),
            EvidenceSource.BITBUCKET,
            pullRequest.links().html().href(),
            "Merged pull request");
    DeepLink commitLink =
        new DeepLink(
            "Bitbucket commit " + abbreviate(commitSha),
            EvidenceSource.BITBUCKET,
            config.browseBaseUrl() + "/commits/" + commitSha,
            "Commit details");
    return new BitbucketChange(
        "pr-" + pullRequest.id(),
        query.serviceId(),
        config.workspace(),
        config.repoSlug(),
        config.projectKey(),
        commitSha,
        pullRequest.source().branch().name(),
        pullRequest.title(),
        Optional.ofNullable(pullRequest.description()).orElse(""),
        List.of(),
        changedFiles,
        pullRequest.author().displayName(),
        Optional.ofNullable(pullRequest.reviewers()).orElse(List.of()).stream()
            .map(User::displayName)
            .toList(),
        Optional.ofNullable(pullRequest.source())
            .map(Source::commit)
            .map(PullRequestCommit::date)
            .orElse(mergedAt(pullRequest)),
        mergedAt(pullRequest),
        new BitbucketPullRequest(
            String.valueOf(pullRequest.id()),
            pullRequest.title(),
            Optional.ofNullable(pullRequest.description()).orElse(""),
            pullRequest.source().branch().name(),
            pullRequest.destination().branch().name(),
            pullRequest.author().displayName(),
            Optional.ofNullable(pullRequest.reviewers()).orElse(List.of()).stream()
                .map(User::displayName)
                .toList(),
            List.of(),
            pullRequest.createdOn(),
            pullRequest.updatedOn(),
            mergedAt(pullRequest),
            prLink),
        pipelineRuns,
        List.of(prLink, commitLink));
  }

  private List<String> diffStat(ClusterBitbucket config, long pullRequestId) {
    DiffStatResponse response =
        client(config)
            .get()
            .uri(
                config.baseUrl()
                    + "/2.0/repositories/"
                    + config.workspace()
                    + "/"
                    + config.repoSlug()
                    + "/pullrequests/"
                    + pullRequestId
                    + "/diffstat")
            .retrieve()
            .body(DiffStatResponse.class);
    if (response == null || response.values() == null) {
      return List.of();
    }
    return response.values().stream()
        .map(value -> value.newFile() != null ? value.newFile().path() : value.oldFile().path())
        .distinct()
        .limit(25)
        .toList();
  }

  private List<BitbucketPipelineRun> statuses(
      ClusterBitbucket config, String commitSha, String branch) {
    if (commitSha == null || commitSha.isBlank()) {
      return List.of();
    }
    StatusResponse response =
        client(config)
            .get()
            .uri(
                config.baseUrl()
                    + "/2.0/repositories/"
                    + config.workspace()
                    + "/"
                    + config.repoSlug()
                    + "/commit/"
                    + commitSha
                    + "/statuses")
            .retrieve()
            .body(StatusResponse.class);
    if (response == null || response.values() == null) {
      return List.of();
    }
    return response.values().stream()
        .map(
            status ->
                new BitbucketPipelineRun(
                    status.key(),
                    Optional.ofNullable(status.state()).map(State::name).orElse("UNKNOWN"),
                    Optional.ofNullable(status.state()).map(State::resultName).orElse("UNKNOWN"),
                    branch,
                    commitSha,
                    status.name() != null && status.name().toLowerCase().contains("deploy"),
                    status.createdOn(),
                    status.updatedOn(),
                    new DeepLink(
                        status.name(),
                        EvidenceSource.BITBUCKET,
                        Optional.ofNullable(status.url()).orElse(config.browseBaseUrl()),
                        "Build or deployment status")))
        .toList();
  }

  private String searchUri(ClusterBitbucket config, BitbucketChangeQuery query) {
    String mergedFilter = "state=\"MERGED\" AND updated_on>=\"" + query.start() + "\"";
    return UriComponentsBuilder.fromHttpUrl(config.baseUrl())
        .path("/2.0/repositories/{workspace}/{repo}/pullrequests")
        .queryParam("pagelen", query.limit())
        .queryParam("sort", "-updated_on")
        .queryParam("q", mergedFilter)
        .buildAndExpand(config.workspace(), config.repoSlug())
        .toUriString();
  }

  private RestClient client(ClusterBitbucket config) {
    RestClient.Builder builder =
        restClientBuilder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (!config.bearerToken().isBlank()) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.bearerToken());
    }
    return builder.build();
  }

  private ClusterBitbucket clusterBitbucket(String cluster, String workspace, String repoSlug) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(
            candidate ->
                new ClusterBitbucket(
                    candidate.bitbucket().baseUrl(),
                    resolveSecret(candidate.bitbucket().bearerTokenRef()),
                    workspace == null || workspace.isBlank()
                        ? candidate.bitbucket().workspace()
                        : workspace,
                    candidate.bitbucket().projectKey(),
                    repoSlug == null || repoSlug.isBlank()
                        ? candidate.bitbucket().repoSlug()
                        : repoSlug,
                    candidate.bitbucket().browseBaseUrl().isBlank()
                        ? candidate.bitbucket().baseUrl()
                        : candidate.bitbucket().browseBaseUrl()))
        .orElseThrow(
            () -> new IllegalArgumentException("Cluster is not configured for Bitbucket access."));
  }

  private Instant mergedAt(PullRequestValue value) {
    return Optional.ofNullable(value.updatedOn()).orElse(Instant.EPOCH);
  }

  private String abbreviate(String commitSha) {
    return commitSha == null || commitSha.length() <= 8 ? commitSha : commitSha.substring(0, 8);
  }

  private String resolveSecret(String reference) {
    return reference == null || reference.isBlank() ? "" : System.getenv(reference);
  }

  private record ClusterBitbucket(
      String baseUrl,
      String bearerToken,
      String workspace,
      String projectKey,
      String repoSlug,
      String browseBaseUrl) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PullRequestResponse(List<PullRequestValue> values) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PullRequestValue(
      long id,
      String title,
      String description,
      String state,
      User author,
      List<User> reviewers,
      Source source,
      Destination destination,
      @JsonProperty("merge_commit") PullRequestCommit mergeCommit,
      @JsonProperty("created_on") Instant createdOn,
      @JsonProperty("updated_on") Instant updatedOn,
      PullRequestLinks links) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record User(@JsonProperty("display_name") String displayName) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Source(Branch branch, PullRequestCommit commit) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Destination(Branch branch) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Branch(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PullRequestCommit(String hash, Instant date) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PullRequestLinks(HtmlLink html) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record HtmlLink(String href) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DiffStatResponse(List<DiffStatValue> values) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DiffStatValue(
      @JsonProperty("new") FileRef newFile, @JsonProperty("old") FileRef oldFile) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FileRef(String path) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record StatusResponse(List<StatusValue> values) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record StatusValue(
      String key,
      String name,
      State state,
      String url,
      @JsonProperty("created_on") Instant createdOn,
      @JsonProperty("updated_on") Instant updatedOn) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record State(String name, @JsonProperty("result_name") String resultName) {}
}
