# Claude Code Setup

## Stdio mode

Build the jar first:

```bash
./mvnw -B -DskipTests package
```

Example MCP server entry:

```json
{
  "mcpServers": {
    "prodops-control-tower-mcp": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/target/prodops-control-tower-mcp-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "SPRING_PROFILES_ACTIVE": "fixture,stdio"
      }
    }
  }
}
```

## HTTP mode

Run the server:

```bash
SPRING_PROFILES_ACTIVE=fixture,http ./mvnw spring-boot:run
```

Point the client at:

- MCP URL: `http://127.0.0.1:8080/mcp`

For remote deployment, use `live,http`, enable JWT, and configure approved origins.
