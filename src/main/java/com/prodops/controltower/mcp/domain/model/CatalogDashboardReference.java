package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record CatalogDashboardReference(
    String serviceId, String displayName, List<String> dashboardUids) {}
