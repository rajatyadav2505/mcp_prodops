package com.prodops.controltower.mcp.adapter.fixture;

import com.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.prodops.controltower.mcp.domain.model.HpaInfo;
import com.prodops.controltower.mcp.domain.model.IngressInfo;
import com.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.prodops.controltower.mcp.domain.model.NetworkPolicyInfo;
import com.prodops.controltower.mcp.domain.model.PdbInfo;
import com.prodops.controltower.mcp.domain.model.PodInfo;
import com.prodops.controltower.mcp.domain.model.RolloutRevision;
import com.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.prodops.controltower.mcp.domain.model.WarningEvent;
import com.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fixture")
public class FixtureClusterInventoryAdapter implements ClusterInventoryPort {

  private final FixtureScenarioLoader loader;
  private final Clock clock;

  public FixtureClusterInventoryAdapter(FixtureScenarioLoader loader, Clock clock) {
    this.loader = loader;
    this.clock = clock;
  }

  @Override
  public List<ClusterInfo> listClusters() {
    return loader.loadRepository().clusters().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                ClusterInfo::name, cluster -> cluster, (left, right) -> left))
        .values()
        .stream()
        .toList();
  }

  @Override
  public List<NamespaceInfo> listNamespaces(String cluster) {
    return loader.loadRepository().namespaces().stream()
        .filter(namespace -> namespace.cluster().equals(cluster))
        .toList();
  }

  @Override
  public List<WorkloadInfo> listWorkloads(
      String cluster, String namespace, WorkloadKind kind, String labelSelector) {
    return loader.loadRepository().workloads().stream()
        .filter(workload -> workload.cluster().equals(cluster))
        .filter(workload -> workload.namespace().equals(namespace))
        .filter(workload -> kind == null || workload.kind() == kind)
        .filter(
            workload ->
                labelSelector == null
                    || labelSelector.isBlank()
                    || workload.labels().toString().contains(labelSelector))
        .toList();
  }

  @Override
  public Optional<WorkloadInfo> getWorkload(
      String cluster, String namespace, String workloadName, WorkloadKind kind) {
    return loader.loadRepository().workloads().stream()
        .filter(workload -> workload.cluster().equals(cluster))
        .filter(workload -> workload.namespace().equals(namespace))
        .filter(workload -> workload.name().equals(workloadName))
        .filter(workload -> workload.kind() == kind)
        .findFirst();
  }

  @Override
  public List<PodInfo> listPodsForWorkload(
      String cluster, String namespace, WorkloadInfo workload) {
    return loader.loadRepository().pods().stream()
        .filter(pod -> pod.cluster().equals(cluster))
        .filter(pod -> pod.namespace().equals(namespace))
        .filter(pod -> pod.ownerReference().name().equals(workload.name()))
        .toList();
  }

  @Override
  public List<WarningEvent> listWarningEvents(
      String cluster, String namespace, String workloadName, Duration since) {
    Instant referenceTime = loader.loadRepository().referenceTime();
    Instant cutoff =
        (referenceTime == null || referenceTime.equals(Instant.EPOCH)
                ? Instant.now(clock)
                : referenceTime)
            .minus(since);
    return loader.loadRepository().warningEvents().stream()
        .filter(event -> event.cluster().equals(cluster))
        .filter(event -> namespace == null || event.namespace().equals(namespace))
        .filter(event -> workloadName == null || event.involvedName().equals(workloadName))
        .filter(event -> !event.lastTimestamp().isBefore(cutoff))
        .toList();
  }

  @Override
  public List<ServiceInfo> listServices(String cluster, String namespace) {
    return loader.loadRepository().services().stream()
        .filter(service -> service.cluster().equals(cluster))
        .filter(service -> service.namespace().equals(namespace))
        .toList();
  }

  @Override
  public List<IngressInfo> listIngresses(String cluster, String namespace) {
    return loader.loadRepository().ingresses().stream()
        .filter(ingress -> ingress.cluster().equals(cluster))
        .filter(ingress -> ingress.namespace().equals(namespace))
        .toList();
  }

  @Override
  public List<NetworkPolicyInfo> listNetworkPolicies(String cluster, String namespace) {
    return loader.loadRepository().networkPolicies().stream()
        .filter(policy -> policy.cluster().equals(cluster))
        .filter(policy -> policy.namespace().equals(namespace))
        .toList();
  }

  @Override
  public Optional<HpaInfo> getHpa(String cluster, String namespace, String workloadName) {
    return loader.loadRepository().hpas().stream()
        .filter(hpa -> hpa.cluster().equals(cluster))
        .filter(hpa -> hpa.namespace().equals(namespace))
        .filter(hpa -> hpa.name().equals(workloadName))
        .findFirst();
  }

  @Override
  public Optional<PdbInfo> getPdb(String cluster, String namespace, String workloadName) {
    return loader.loadRepository().pdbs().stream()
        .filter(pdb -> pdb.cluster().equals(cluster))
        .filter(pdb -> pdb.namespace().equals(namespace))
        .filter(pdb -> pdb.name().equals(workloadName))
        .findFirst();
  }

  @Override
  public List<RolloutRevision> listRolloutRevisions(
      String cluster, String namespace, String workloadName, WorkloadKind workloadKind) {
    List<RolloutRevision> explicit =
        loader.loadRepository().rolloutRevisions().stream()
            .filter(revision -> revision.cluster().equals(cluster))
            .filter(revision -> revision.namespace().equals(namespace))
            .filter(revision -> revision.workloadName().equals(workloadName))
            .filter(revision -> revision.workloadKind() == workloadKind)
            .toList();
    if (!explicit.isEmpty()) {
      return explicit;
    }
    return loader.loadRepository().workloads().stream()
        .filter(workload -> workload.cluster().equals(cluster))
        .filter(workload -> workload.namespace().equals(namespace))
        .filter(workload -> workload.name().equals(workloadName))
        .filter(workload -> workload.kind() == workloadKind)
        .findFirst()
        .map(
            workload ->
                List.of(
                    new RolloutRevision(
                        cluster,
                        namespace,
                        workloadName,
                        workloadKind,
                        workload.revision(),
                        workload.image(),
                        versionTag(workload.labels()),
                        workload.desiredReplicas(),
                        workload.readyReplicas(),
                        workload.readyReplicas() != null
                                && workload.desiredReplicas() != null
                                && workload.readyReplicas() >= workload.desiredReplicas()
                            ? "complete"
                            : "incomplete",
                        workload.updatedAt() == null
                            ? workload.createdAt()
                            : workload.updatedAt())))
        .orElse(List.of());
  }

  @Override
  public Optional<LogExcerpt> getPodLogs(
      String cluster,
      String namespace,
      String podName,
      String container,
      int tailLines,
      Duration since) {
    return loader.loadRepository().logs().stream()
        .filter(log -> log.podName().equals(podName))
        .filter(log -> log.container().equals(container))
        .findFirst();
  }

  private String versionTag(java.util.Map<String, String> labels) {
    return java.util.List.of("app.kubernetes.io/version", "release", "image-tag").stream()
        .map(labels::get)
        .filter(value -> value != null && !value.isBlank())
        .findFirst()
        .orElse(null);
  }
}
