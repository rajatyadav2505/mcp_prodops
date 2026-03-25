package com.prodops.controltower.mcp.adapter.grafana;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.prodops.controltower.mcp.config.ProdOpsProperties;
import com.prodops.controltower.mcp.domain.model.DashboardInfo;
import com.prodops.controltower.mcp.domain.model.DashboardPanel;
import com.prodops.controltower.mcp.domain.port.DashboardPort;
import java.time.Clock;
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
public class LiveGrafanaAdapter implements DashboardPort {

  private final ProdOpsProperties properties;
  private final RestClient.Builder restClientBuilder;
  private final Cache<String, DashboardInfo> dashboardCache;

  public LiveGrafanaAdapter(
      ProdOpsProperties properties, RestClient.Builder restClientBuilder, Clock clock) {
    this.properties = properties;
    this.restClientBuilder = restClientBuilder;
    this.dashboardCache =
        Caffeine.newBuilder()
            .maximumSize(properties.cache().maxEntries())
            .expireAfterWrite(properties.cache().dashboardTtl())
            .build();
  }

  @Override
  public List<DashboardInfo> search(
      String cluster, String query, List<String> tags, String folder, int limit) {
    ClusterGrafana config = clusterGrafana(cluster);
    SearchResponse[] response =
        client(config)
            .get()
            .uri(buildSearchUri(config.baseUrl(), query, tags))
            .retrieve()
            .body(SearchResponse[].class);
    if (response == null) {
      return List.of();
    }
    return List.of(response).stream()
        .filter(item -> folder == null || folder.isBlank() || folder.equals(item.folderTitle()))
        .limit(limit)
        .map(
            item ->
                new DashboardInfo(
                    cluster,
                    item.uid(),
                    item.title(),
                    item.folderTitle(),
                    item.tags() == null ? List.of() : item.tags(),
                    config.baseUrl() + item.url(),
                    List.of(),
                    List.of(),
                    List.of()))
        .toList();
  }

  @Override
  public Optional<DashboardInfo> getByUid(String cluster, String dashboardUid) {
    String cacheKey = cluster + ":" + dashboardUid;
    DashboardInfo cached = dashboardCache.getIfPresent(cacheKey);
    if (cached != null) {
      return Optional.of(cached);
    }
    ClusterGrafana config = clusterGrafana(cluster);
    DashboardResponse response =
        client(config)
            .get()
            .uri(config.baseUrl() + "/api/dashboards/uid/" + dashboardUid)
            .retrieve()
            .body(DashboardResponse.class);
    if (response == null || response.dashboard() == null || response.meta() == null) {
      return Optional.empty();
    }
    DashboardInfo dashboard =
        new DashboardInfo(
            cluster,
            response.dashboard().uid(),
            response.dashboard().title(),
            response.meta().folderTitle(),
            response.meta().tags() == null ? List.of() : response.meta().tags(),
            config.baseUrl() + response.meta().url(),
            Optional.ofNullable(response.dashboard().panels()).orElse(List.of()).stream()
                .map(
                    panel ->
                        new DashboardPanel(panel.title(), panel.description(), panel.targets()))
                .toList(),
            Optional.ofNullable(response.dashboard().templating())
                .map(Templating::list)
                .orElse(List.of())
                .stream()
                .map(Variable::name)
                .toList(),
            List.of(response.dashboard().datasource()));
    dashboardCache.put(cacheKey, dashboard);
    return Optional.of(dashboard);
  }

  private RestClient client(ClusterGrafana config) {
    RestClient.Builder builder =
        restClientBuilder.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    if (!config.bearerToken().isBlank()) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.bearerToken());
    }
    return builder.build();
  }

  private ClusterGrafana clusterGrafana(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(
            candidate ->
                new ClusterGrafana(
                    candidate.grafana().baseUrl(),
                    resolveSecret(candidate.grafana().bearerTokenRef())))
        .orElseThrow(
            () -> new IllegalArgumentException("Cluster is not configured for Grafana access."));
  }

  private String resolveSecret(String reference) {
    return reference == null || reference.isBlank() ? "" : System.getenv(reference);
  }

  private String buildSearchUri(String baseUrl, String query, List<String> tags) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(baseUrl).path("/api/search").queryParam("query", query);
    if (tags != null) {
      tags.forEach(tag -> builder.queryParam("tag", tag));
    }
    return builder.toUriString();
  }

  private record ClusterGrafana(String baseUrl, String bearerToken) {}

  private record SearchResponse(
      String uid, String title, String url, List<String> tags, String folderTitle) {}

  private record DashboardResponse(Meta meta, Dashboard dashboard) {}

  private record Meta(String folderTitle, String url, List<String> tags) {}

  private record Dashboard(
      String uid, String title, String datasource, List<Panel> panels, Templating templating) {}

  private record Panel(String title, String description, List<String> targets) {}

  private record Templating(List<Variable> list) {}

  private record Variable(String name) {}
}
