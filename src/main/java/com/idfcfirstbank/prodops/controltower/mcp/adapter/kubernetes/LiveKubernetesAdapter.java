package com.idfcfirstbank.prodops.controltower.mcp.adapter.kubernetes;

import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.HpaInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.IngressInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ObjectReference;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PdbInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PodInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WarningEvent;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadKind;
import com.idfcfirstbank.prodops.controltower.mcp.domain.port.ClusterInventoryPort;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AutoscalingV2Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.PolicyV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1CronJobList;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live")
public class LiveKubernetesAdapter implements ClusterInventoryPort {

  private final KubernetesClientFactory clientFactory;
  private final ProdOpsProperties properties;
  private final Clock clock;

  public LiveKubernetesAdapter(
      KubernetesClientFactory clientFactory, ProdOpsProperties properties, Clock clock) {
    this.clientFactory = clientFactory;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public List<ClusterInfo> listClusters() {
    return properties.clusters().stream()
        .filter(ProdOpsProperties.ClusterProperties::enabled)
        .map(
            cluster ->
                new ClusterInfo(
                    cluster.name(), cluster.environment(), "configured", "configured", Map.of()))
        .toList();
  }

  @Override
  public List<NamespaceInfo> listNamespaces(String cluster) {
    CoreV1Api api = new CoreV1Api(clientFactory.clientFor(cluster));
    try {
      V1NamespaceList namespaces = api.listNamespace().execute();
      return namespaces.getItems().stream()
          .map(
              namespace ->
                  new NamespaceInfo(
                      cluster,
                      namespace.getMetadata().getName(),
                      metadataLabels(namespace.getMetadata()),
                      metadataLabels(namespace.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(namespace.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard"),
                      "critical"
                          .equalsIgnoreCase(
                              metadataLabels(namespace.getMetadata())
                                  .getOrDefault(criticalityLabelKey(cluster), ""))))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list namespaces from Kubernetes.", exception);
    }
  }

  @Override
  public List<WorkloadInfo> listWorkloads(
      String cluster, String namespace, WorkloadKind kind, String labelSelector) {
    List<WorkloadInfo> workloads = new ArrayList<>();
    if (kind == null || kind == WorkloadKind.DEPLOYMENT) {
      workloads.addAll(listDeployments(cluster, namespace, labelSelector));
    }
    if (kind == null || kind == WorkloadKind.STATEFULSET) {
      workloads.addAll(listStatefulSets(cluster, namespace, labelSelector));
    }
    if (kind == null || kind == WorkloadKind.DAEMONSET) {
      workloads.addAll(listDaemonSets(cluster, namespace, labelSelector));
    }
    if (kind == null || kind == WorkloadKind.JOB) {
      workloads.addAll(listJobs(cluster, namespace, labelSelector));
    }
    if (kind == null || kind == WorkloadKind.CRONJOB) {
      workloads.addAll(listCronJobs(cluster, namespace, labelSelector));
    }
    return workloads;
  }

  @Override
  public Optional<WorkloadInfo> getWorkload(
      String cluster, String namespace, String workloadName, WorkloadKind kind) {
    return listWorkloads(cluster, namespace, kind, null).stream()
        .filter(workload -> workload.name().equals(workloadName))
        .findFirst();
  }

  @Override
  public List<PodInfo> listPodsForWorkload(
      String cluster, String namespace, WorkloadInfo workload) {
    CoreV1Api api = new CoreV1Api(clientFactory.clientFor(cluster));
    try {
      V1PodList pods =
          api.listNamespacedPod(namespace).labelSelector(selector(workload.selector())).execute();
      return pods.getItems().stream().map(pod -> toPod(cluster, namespace, pod)).toList();
    } catch (ApiException exception) {
      throw new IllegalStateException(
          "Failed to list pods for workload " + workload.name(), exception);
    }
  }

  @Override
  public List<WarningEvent> listWarningEvents(
      String cluster, String namespace, String workloadName, Duration since) {
    CoreV1Api api = new CoreV1Api(clientFactory.clientFor(cluster));
    try {
      CoreV1EventList eventList =
          namespace == null
              ? api.listEventForAllNamespaces().execute()
              : api.listNamespacedEvent(namespace).execute();
      List<CoreV1Event> items = eventList.getItems();
      Instant cutoff = Instant.now(clock).minus(since);
      return items.stream()
          .filter(event -> "Warning".equalsIgnoreCase(event.getType()))
          .filter(
              event ->
                  workloadName == null || workloadName.equals(event.getInvolvedObject().getName()))
          .filter(event -> !eventTimestamp(event).isBefore(cutoff))
          .map(
              event ->
                  new WarningEvent(
                      cluster,
                      namespace,
                      event.getReason(),
                      event.getMessage(),
                      event.getInvolvedObject().getKind(),
                      event.getInvolvedObject().getName(),
                      Optional.ofNullable(event.getCount()).orElse(1),
                      Optional.ofNullable(event.getFirstTimestamp())
                          .map(ts -> ts.toInstant())
                          .orElse(Instant.EPOCH),
                      Optional.ofNullable(event.getLastTimestamp())
                          .map(ts -> ts.toInstant())
                          .orElse(Instant.EPOCH)))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to read Kubernetes warning events.", exception);
    }
  }

  @Override
  public List<ServiceInfo> listServices(String cluster, String namespace) {
    CoreV1Api api = new CoreV1Api(clientFactory.clientFor(cluster));
    try {
      return api.listNamespacedService(namespace).execute().getItems().stream()
          .map(
              service ->
                  new ServiceInfo(
                      cluster,
                      namespace,
                      service.getMetadata().getName(),
                      service.getSpec().getType(),
                      Optional.ofNullable(service.getSpec().getSelector()).orElse(Map.of())))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes Services.", exception);
    }
  }

  @Override
  public List<IngressInfo> listIngresses(String cluster, String namespace) {
    NetworkingV1Api api = new NetworkingV1Api(clientFactory.clientFor(cluster));
    try {
      return api.listNamespacedIngress(namespace).execute().getItems().stream()
          .map(
              ingress ->
                  new IngressInfo(
                      cluster,
                      namespace,
                      ingress.getMetadata().getName(),
                      Optional.ofNullable(ingress.getSpec().getRules()).orElse(List.of()).stream()
                          .map(rule -> rule.getHost())
                          .toList(),
                      Optional.ofNullable(ingress.getSpec().getRules()).orElse(List.of()).stream()
                          .flatMap(
                              rule ->
                                  Optional.ofNullable(rule.getHttp())
                                      .map(http -> http.getPaths())
                                      .orElse(List.of())
                                      .stream())
                          .map(path -> path.getBackend().getService().getName())
                          .toList()))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes Ingress resources.", exception);
    }
  }

  @Override
  public Optional<HpaInfo> getHpa(String cluster, String namespace, String workloadName) {
    AutoscalingV2Api api = new AutoscalingV2Api(clientFactory.clientFor(cluster));
    try {
      return api.listNamespacedHorizontalPodAutoscaler(namespace).execute().getItems().stream()
          .filter(hpa -> workloadName.equals(hpa.getMetadata().getName()))
          .findFirst()
          .map(
              hpa ->
                  new HpaInfo(
                      cluster,
                      namespace,
                      workloadName,
                      hpa.getSpec().getMinReplicas(),
                      hpa.getSpec().getMaxReplicas(),
                      hpa.getStatus().getCurrentReplicas(),
                      hpa.getStatus().getDesiredReplicas() != null
                          && hpa.getStatus().getCurrentReplicas() != null
                          && hpa.getStatus().getCurrentReplicas()
                              >= hpa.getSpec().getMaxReplicas()));
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to read Kubernetes HPA resources.", exception);
    }
  }

  @Override
  public Optional<PdbInfo> getPdb(String cluster, String namespace, String workloadName) {
    PolicyV1Api api = new PolicyV1Api(clientFactory.clientFor(cluster));
    try {
      return api.listNamespacedPodDisruptionBudget(namespace).execute().getItems().stream()
          .filter(pdb -> workloadName.equals(pdb.getMetadata().getName()))
          .findFirst()
          .map(
              pdb ->
                  new PdbInfo(
                      cluster,
                      namespace,
                      pdb.getMetadata().getName(),
                      String.valueOf(pdb.getSpec().getMinAvailable()),
                      String.valueOf(pdb.getSpec().getMaxUnavailable()),
                      Optional.ofNullable(pdb.getStatus().getDisruptionsAllowed()).orElse(0)));
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to read Kubernetes PDB resources.", exception);
    }
  }

  @Override
  public Optional<LogExcerpt> getPodLogs(
      String cluster,
      String namespace,
      String podName,
      String container,
      int tailLines,
      Duration since) {
    PodLogs podLogs = new PodLogs(clientFactory.clientFor(cluster));
    try (InputStream stream =
            podLogs.streamNamespacedPodLog(namespace, podName, container, tailLines, null, false);
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      List<String> lines = reader.lines().limit(tailLines).toList();
      return Optional.of(
          new LogExcerpt(podName, container, Instant.now(), lines, lines.size() >= tailLines));
    } catch (ApiException | IOException exception) {
      throw new IllegalStateException("Failed to read pod logs from Kubernetes.", exception);
    }
  }

  private List<WorkloadInfo> listDeployments(
      String cluster, String namespace, String labelSelector) {
    AppsV1Api api = new AppsV1Api(clientFactory.clientFor(cluster));
    try {
      V1DeploymentList deployments =
          api.listNamespacedDeployment(namespace).labelSelector(labelSelector).execute();
      return deployments.getItems().stream()
          .map(
              deployment ->
                  new WorkloadInfo(
                      cluster,
                      namespace,
                      deployment.getMetadata().getName(),
                      WorkloadKind.DEPLOYMENT,
                      metadataLabels(deployment.getMetadata()),
                      Optional.ofNullable(deployment.getSpec().getSelector().getMatchLabels())
                          .orElse(Map.of()),
                      deployment.getSpec().getReplicas(),
                      Optional.ofNullable(deployment.getStatus().getReadyReplicas()).orElse(0),
                      deployment.getMetadata().getCreationTimestamp().toInstant(),
                      Optional.ofNullable(deployment.getStatus().getConditions())
                          .orElse(List.of())
                          .stream()
                          .map(condition -> condition.getLastUpdateTime().toInstant())
                          .max(Instant::compareTo)
                          .orElse(deployment.getMetadata().getCreationTimestamp().toInstant()),
                      metadataLabels(deployment.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(deployment.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard")))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes Deployments.", exception);
    }
  }

  private List<WorkloadInfo> listStatefulSets(
      String cluster, String namespace, String labelSelector) {
    AppsV1Api api = new AppsV1Api(clientFactory.clientFor(cluster));
    try {
      V1StatefulSetList sets =
          api.listNamespacedStatefulSet(namespace).labelSelector(labelSelector).execute();
      return sets.getItems().stream()
          .map(
              set ->
                  new WorkloadInfo(
                      cluster,
                      namespace,
                      set.getMetadata().getName(),
                      WorkloadKind.STATEFULSET,
                      metadataLabels(set.getMetadata()),
                      Optional.ofNullable(set.getSpec().getSelector().getMatchLabels())
                          .orElse(Map.of()),
                      set.getSpec().getReplicas(),
                      Optional.ofNullable(set.getStatus().getReadyReplicas()).orElse(0),
                      set.getMetadata().getCreationTimestamp().toInstant(),
                      set.getMetadata().getCreationTimestamp().toInstant(),
                      metadataLabels(set.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(set.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard")))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes StatefulSets.", exception);
    }
  }

  private List<WorkloadInfo> listJobs(String cluster, String namespace, String labelSelector) {
    BatchV1Api api = new BatchV1Api(clientFactory.clientFor(cluster));
    try {
      V1JobList jobs = api.listNamespacedJob(namespace).labelSelector(labelSelector).execute();
      return jobs.getItems().stream()
          .map(
              job ->
                  new WorkloadInfo(
                      cluster,
                      namespace,
                      job.getMetadata().getName(),
                      WorkloadKind.JOB,
                      metadataLabels(job.getMetadata()),
                      Optional.ofNullable(job.getSpec().getSelector())
                          .map(selector -> selector.getMatchLabels())
                          .orElse(Map.of()),
                      1,
                      Optional.ofNullable(job.getStatus().getSucceeded()).orElse(0),
                      job.getMetadata().getCreationTimestamp().toInstant(),
                      job.getMetadata().getCreationTimestamp().toInstant(),
                      metadataLabels(job.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(job.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard")))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes Jobs.", exception);
    }
  }

  private List<WorkloadInfo> listDaemonSets(
      String cluster, String namespace, String labelSelector) {
    AppsV1Api api = new AppsV1Api(clientFactory.clientFor(cluster));
    try {
      V1DaemonSetList daemonSets =
          api.listNamespacedDaemonSet(namespace).labelSelector(labelSelector).execute();
      return daemonSets.getItems().stream()
          .map(
              daemonSet ->
                  new WorkloadInfo(
                      cluster,
                      namespace,
                      daemonSet.getMetadata().getName(),
                      WorkloadKind.DAEMONSET,
                      metadataLabels(daemonSet.getMetadata()),
                      Optional.ofNullable(daemonSet.getSpec().getSelector().getMatchLabels())
                          .orElse(Map.of()),
                      Optional.ofNullable(daemonSet.getStatus().getDesiredNumberScheduled())
                          .orElse(0),
                      Optional.ofNullable(daemonSet.getStatus().getNumberReady()).orElse(0),
                      daemonSet.getMetadata().getCreationTimestamp().toInstant(),
                      daemonSet.getMetadata().getCreationTimestamp().toInstant(),
                      metadataLabels(daemonSet.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(daemonSet.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard")))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes DaemonSets.", exception);
    }
  }

  private List<WorkloadInfo> listCronJobs(String cluster, String namespace, String labelSelector) {
    BatchV1Api api = new BatchV1Api(clientFactory.clientFor(cluster));
    try {
      V1CronJobList cronJobs =
          api.listNamespacedCronJob(namespace).labelSelector(labelSelector).execute();
      return cronJobs.getItems().stream()
          .map(
              cronJob ->
                  new WorkloadInfo(
                      cluster,
                      namespace,
                      cronJob.getMetadata().getName(),
                      WorkloadKind.CRONJOB,
                      metadataLabels(cronJob.getMetadata()),
                      Optional.ofNullable(cronJob.getSpec().getJobTemplate())
                          .map(template -> template.getSpec())
                          .map(spec -> spec.getSelector())
                          .map(selector -> selector.getMatchLabels())
                          .orElse(Map.of()),
                      1,
                      Optional.ofNullable(cronJob.getStatus())
                          .map(status -> status.getActive())
                          .map(List::size)
                          .orElse(0),
                      cronJob.getMetadata().getCreationTimestamp().toInstant(),
                      cronJob.getMetadata().getCreationTimestamp().toInstant(),
                      metadataLabels(cronJob.getMetadata())
                          .getOrDefault(teamLabelKey(cluster), "unknown"),
                      metadataLabels(cronJob.getMetadata())
                          .getOrDefault(criticalityLabelKey(cluster), "standard")))
          .toList();
    } catch (ApiException exception) {
      throw new IllegalStateException("Failed to list Kubernetes CronJobs.", exception);
    }
  }

  private PodInfo toPod(String cluster, String namespace, V1Pod pod) {
    List<V1ContainerStatus> statuses =
        Optional.ofNullable(pod.getStatus().getContainerStatuses()).orElse(List.of());
    int restartCount =
        statuses.stream()
            .mapToInt(status -> Optional.ofNullable(status.getRestartCount()).orElse(0))
            .sum();
    List<String> containerStates =
        statuses.stream().map(status -> status.getName() + ":" + state(status)).toList();
    String lastTerminationReason =
        statuses.stream()
            .map(
                status ->
                    Optional.ofNullable(status.getLastState())
                        .map(state -> state.getTerminated())
                        .map(terminated -> terminated.getReason())
                        .orElse(null))
            .filter(reason -> reason != null)
            .findFirst()
            .orElse("unknown");
    V1OwnerReference ownerReference =
        Optional.ofNullable(pod.getMetadata().getOwnerReferences()).orElse(List.of()).stream()
            .findFirst()
            .orElse(new V1OwnerReference().kind("Pod").name(pod.getMetadata().getName()));
    return new PodInfo(
        cluster,
        namespace,
        pod.getMetadata().getName(),
        pod.getStatus().getPhase(),
        statuses.stream().allMatch(status -> Boolean.TRUE.equals(status.getReady())),
        restartCount,
        containerStates,
        lastTerminationReason,
        pod.getMetadata().getCreationTimestamp().toInstant(),
        new ObjectReference(
            cluster, namespace, ownerReference.getKind(), ownerReference.getName()));
  }

  private String selector(Map<String, String> selector) {
    return selector.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(","));
  }

  private Map<String, String> metadataLabels(V1ObjectMeta metadata) {
    return Optional.ofNullable(metadata.getLabels()).orElse(Map.of());
  }

  private String state(V1ContainerStatus status) {
    if (status.getState() == null) {
      return "unknown";
    }
    if (status.getState().getRunning() != null) {
      return "running";
    }
    if (status.getState().getWaiting() != null) {
      return Optional.ofNullable(status.getState().getWaiting().getReason()).orElse("waiting");
    }
    if (status.getState().getTerminated() != null) {
      return Optional.ofNullable(status.getState().getTerminated().getReason())
          .orElse("terminated");
    }
    return "unknown";
  }

  private Instant eventTimestamp(CoreV1Event event) {
    return Optional.ofNullable(event.getLastTimestamp())
        .map(ts -> ts.toInstant())
        .or(() -> Optional.ofNullable(event.getEventTime()).map(ts -> ts.toInstant()))
        .or(() -> Optional.ofNullable(event.getFirstTimestamp()).map(ts -> ts.toInstant()))
        .orElse(Instant.EPOCH);
  }

  private String teamLabelKey(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(ProdOpsProperties.ClusterProperties::teamLabelKey)
        .orElse("owner-team");
  }

  private String criticalityLabelKey(String cluster) {
    return properties.clusters().stream()
        .filter(candidate -> candidate.name().equals(cluster))
        .findFirst()
        .map(ProdOpsProperties.ClusterProperties::criticalityLabelKey)
        .orElse("criticality");
  }
}
