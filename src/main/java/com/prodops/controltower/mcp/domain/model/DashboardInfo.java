package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record DashboardInfo(
    String cluster,
    String uid,
    String title,
    String folder,
    List<String> tags,
    String url,
    List<DashboardPanel> panels,
    List<String> variables,
    List<String> datasourceHints) {}
