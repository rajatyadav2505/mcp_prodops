package com.prodops.controltower.mcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.stereotype.Component;

@Component
public class McpContentSupport {

  private final ObjectMapper objectMapper;

  public McpContentSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ReadResourceResult jsonResource(String uri, Object payload) {
    return new ReadResourceResult(
        java.util.List.of(new TextResourceContents(uri, "application/json", json(payload))));
  }

  public GetPromptResult prompt(String description, String text) {
    return new GetPromptResult(
        description, java.util.List.of(new PromptMessage(Role.USER, new TextContent(text))));
  }

  private String json(Object payload) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize MCP payload.", exception);
    }
  }
}
