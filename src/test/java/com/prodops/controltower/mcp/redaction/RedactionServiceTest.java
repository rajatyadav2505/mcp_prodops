package com.prodops.controltower.mcp.redaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RedactionServiceTest {

  private final RedactionService service = new RedactionService();

  @Test
  void redactsCredentialLikeValuesAndSecretsInBulk() {
    String input =
        """
                authorization: bearer eyJhbGciOiJSUzI1NiIsImtpZCI6InNlY3JldCJ9.eyJzdWIiOiJhbGljZSJ9.sig
                password=superSecret
                token: token-value
                secret: hidden-value
                apikey=key-value
                jdbc:mysql://reader:databaseSecret@db.internal:3306/app
                QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo1MjM0NTY=
                """;

    String redacted = service.redact(input);

    assertThat(redacted).contains("authorization: bearer [REDACTED]");
    assertThat(redacted).contains("password=[REDACTED]");
    assertThat(redacted).contains("token: [REDACTED]");
    assertThat(redacted).contains("secret: [REDACTED]");
    assertThat(redacted).contains("apikey=[REDACTED]");
    assertThat(redacted).contains("jdbc:mysql://reader:[REDACTED]@db.internal:3306/app");
    assertThat(redacted)
        .doesNotContain(
            "superSecret", "databaseSecret", "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo1MjM0NTY=");
  }

  @Test
  void redactsEveryLineInACollection() {
    List<String> redacted = service.redactLines(List.of("token=abc123", "safe line"));

    assertThat(redacted).containsExactly("token=[REDACTED]", "safe line");
  }
}
