package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.util.List;

public record CatalogSloReference(
    String serviceId, String displayName, List<SloTarget> sloTargets) {}
