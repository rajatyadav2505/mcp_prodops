package com.idfcfirstbank.prodops.controltower.mcp.domain.model;

import java.util.List;

public record DashboardPanel(String title, String description, List<String> targets) {}
