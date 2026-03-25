package com.prodops.controltower.mcp.domain.model;

public record ObjectReference(String cluster, String namespace, String kind, String name) {}
