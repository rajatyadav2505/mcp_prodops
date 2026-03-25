package com.idfcfirstbank.prodops.controltower.mcp.domain.port;

import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ClusterInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.HpaInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.IngressInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.LogExcerpt;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.NamespaceInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PdbInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.PodInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.ServiceInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WarningEvent;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadInfo;
import com.idfcfirstbank.prodops.controltower.mcp.domain.model.WorkloadKind;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface ClusterInventoryPort {

  List<ClusterInfo> listClusters();

  List<NamespaceInfo> listNamespaces(String cluster);

  List<WorkloadInfo> listWorkloads(
      String cluster, String namespace, WorkloadKind kind, String labelSelector);

  Optional<WorkloadInfo> getWorkload(
      String cluster, String namespace, String workloadName, WorkloadKind kind);

  List<PodInfo> listPodsForWorkload(String cluster, String namespace, WorkloadInfo workload);

  List<WarningEvent> listWarningEvents(
      String cluster, String namespace, String workloadName, Duration since);

  List<ServiceInfo> listServices(String cluster, String namespace);

  List<IngressInfo> listIngresses(String cluster, String namespace);

  Optional<HpaInfo> getHpa(String cluster, String namespace, String workloadName);

  Optional<PdbInfo> getPdb(String cluster, String namespace, String workloadName);

  Optional<LogExcerpt> getPodLogs(
      String cluster,
      String namespace,
      String podName,
      String container,
      int tailLines,
      Duration since);
}
