FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
COPY src/ src/
COPY config/ config/
COPY fixtures/ fixtures/

RUN chmod +x mvnw
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError" \
    JAVA_OPTS="" \
    PRODOPS_BIND_ADDRESS="0.0.0.0"

WORKDIR /app

RUN groupadd --system prodops && useradd --system --gid prodops --home-dir /app --shell /usr/sbin/nologin prodops

COPY --from=build /workspace/target/prodops-control-tower-mcp-*.jar /app/prodops-control-tower-mcp.jar
COPY --from=build /workspace/config /app/config
COPY --from=build /workspace/fixtures /app/fixtures

RUN chown -R prodops:prodops /app

EXPOSE 8080 8081

USER prodops

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/prodops-control-tower-mcp.jar"]
