package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record CatalogRunbookReference(
    String serviceId, String displayName, List<String> runbookLinks) {}
