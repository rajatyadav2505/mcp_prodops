package com.prodops.controltower.mcp.domain.model;

import java.util.List;

public record CausalEvidenceGraph(List<EvidenceNode> nodes, List<EvidenceEdge> edges) {}
