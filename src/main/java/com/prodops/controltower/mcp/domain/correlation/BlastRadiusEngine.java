package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.BlastRadiusImpact;
import com.prodops.controltower.mcp.domain.model.IngressInfo;
import com.prodops.controltower.mcp.domain.model.ServiceInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BlastRadiusEngine {

  public List<BlastRadiusImpact> estimate(
      String namespace,
      String serviceOrWorkload,
      List<ServiceInfo> services,
      List<IngressInfo> ingresses,
      boolean scaleConstrained) {
    List<BlastRadiusImpact> impacts = new ArrayList<>();
    List<ServiceInfo> matchingServices =
        services.stream()
            .filter(
                service ->
                    service.name().contains(serviceOrWorkload)
                        || service.selector().values().stream()
                            .anyMatch(v -> v.contains(serviceOrWorkload)))
            .toList();
    Set<String> matchingServiceNames =
        matchingServices.stream().map(ServiceInfo::name).collect(Collectors.toSet());
    List<IngressInfo> affectedIngresses =
        ingresses.stream()
            .filter(
                ingress ->
                    ingress.backendServices().stream().anyMatch(matchingServiceNames::contains))
            .toList();

    if (!matchingServices.isEmpty()) {
      impacts.add(
          new BlastRadiusImpact(
              "service-mesh-surface",
              "Kubernetes Services route traffic to the affected workload selectors.",
              0.72d,
              List.of(namespace),
              matchingServices.stream().map(ServiceInfo::name).toList()));
    }
    if (!affectedIngresses.isEmpty()) {
      impacts.add(
          new BlastRadiusImpact(
              "user-facing-ingress",
              "Ingress backends reference Services selected by the affected workload.",
              0.81d,
              List.of(namespace),
              affectedIngresses.stream().map(IngressInfo::name).toList()));
    }
    if (scaleConstrained) {
      impacts.add(
          new BlastRadiusImpact(
              "capacity-buffer",
              "Scaling is constrained, increasing the likelihood of broader spillover under sustained load.",
              0.69d,
              List.of(namespace),
              List.of(serviceOrWorkload)));
    }
    if (impacts.isEmpty()) {
      impacts.add(
          new BlastRadiusImpact(
              "contained-namespace-scope",
              "Only namespace-local evidence was available, so impact is intentionally conservative.",
              0.38d,
              List.of(namespace),
              List.of(serviceOrWorkload)));
    }
    return impacts;
  }
}
