package com.prodops.controltower.mcp.domain.model;

import java.util.List;
import java.util.Map;

public record NetworkPolicyInfo(
    String cluster,
    String namespace,
    String name,
    Map<String, String> podSelector,
    List<String> policyTypes,
    int ingressRuleCount,
    int egressRuleCount,
    boolean defaultDenyIngress,
    boolean defaultDenyEgress) {

  public NetworkPolicyInfo {
    podSelector = podSelector == null ? Map.of() : Map.copyOf(podSelector);
    policyTypes = policyTypes == null ? List.of() : List.copyOf(policyTypes);
  }
}
