package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.util.List;

public record ReadOnlyContract(
    List<String> upstreamSystems,
    List<String> inScopeObjects,
    List<String> allowedVerbs,
    List<String> prohibitedActions,
    List<String> redactionRules,
    List<String> scopeRules) {}
