package com.prodops.controltower.mcp.adapter.kubernetes;

import com.prodops.controltower.mcp.config.ProdOpsProperties;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live")
public class KubernetesClientFactory {

  private final ProdOpsProperties properties;
  private final Map<String, ApiClient> clients = new ConcurrentHashMap<>();

  public KubernetesClientFactory(ProdOpsProperties properties) {
    this.properties = properties;
  }

  public ApiClient clientFor(String cluster) {
    return clients.computeIfAbsent(cluster, this::createClient);
  }

  private ApiClient createClient(String clusterName) {
    ProdOpsProperties.ClusterProperties cluster =
        properties.clusters().stream()
            .filter(candidate -> candidate.name().equals(clusterName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Cluster is not configured for Kubernetes access."));
    try {
      if (cluster.kubernetes().inCluster()) {
        return ClientBuilder.cluster().build();
      }
      KubeConfig kubeConfig =
          KubeConfig.loadKubeConfig(
              Files.newBufferedReader(
                  Path.of(cluster.kubernetes().kubeconfig()), StandardCharsets.UTF_8));
      if (cluster.kubernetes().context() != null && !cluster.kubernetes().context().isBlank()) {
        kubeConfig.setContext(cluster.kubernetes().context());
      }
      return ClientBuilder.kubeconfig(kubeConfig).build();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to create Kubernetes client for " + clusterName, exception);
    }
  }
}
