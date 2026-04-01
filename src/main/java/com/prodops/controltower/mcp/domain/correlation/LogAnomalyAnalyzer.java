package com.prodops.controltower.mcp.domain.correlation;

import com.prodops.controltower.mcp.domain.model.LogErrorSignature;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LogAnomalyAnalyzer {

  public Assessment analyze(
      int recentCount,
      int baselineCount,
      double recentMinutes,
      double baselineMinutes,
      List<LogErrorSignature> signatures) {
    double recentPerMinute = recentMinutes <= 0.0d ? recentCount : recentCount / recentMinutes;
    double baselinePerMinute =
        baselineMinutes <= 0.0d ? baselineCount : baselineCount / baselineMinutes;
    double anomalyRatio =
        baselinePerMinute <= 0.0d
            ? (recentPerMinute <= 0.0d ? 1.0d : recentPerMinute)
            : recentPerMinute / baselinePerMinute;
    return new Assessment(recentPerMinute, baselinePerMinute, anomalyRatio, signatures);
  }

  public record Assessment(
      double recentPerMinute,
      double baselinePerMinute,
      double anomalyRatio,
      List<LogErrorSignature> dominantRecentSignatures) {}
}
