package com.prodops.controltower.mcp.domain.model;

public record SloTarget(
    String name, String objective, String threshold, String measurementWindow) {}
