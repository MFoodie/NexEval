package com.nexeval.service;

import com.nexeval.model.ExamSession.IrtAnswerRecord;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IrtCatService {

  private static final double THETA_MIN = -3.0;
  private static final double THETA_MAX = 3.0;
  private static final int NODE_COUNT = 35;
  private static final double MIN_DISCRIMINATION = 0.1;

  public record IrtEstimate(double theta, double standardError) {}

  public double probability(double theta, double discriminationA, double difficultyB) {
    double a = Math.max(MIN_DISCRIMINATION, discriminationA);
    double exponent = -a * (theta - difficultyB);
    return 1.0 / (1.0 + Math.exp(exponent));
  }

  public double information(double theta, double discriminationA, double difficultyB) {
    double p = probability(theta, discriminationA, difficultyB);
    return discriminationA * discriminationA * p * (1.0 - p);
  }

  public IrtEstimate estimateThetaEap(List<IrtAnswerRecord> history) {
    if (history == null || history.isEmpty()) {
      return new IrtEstimate(0.0, 9.99);
    }

    double step = (THETA_MAX - THETA_MIN) / (NODE_COUNT - 1);
    double[] nodes = new double[NODE_COUNT];
    for (int i = 0; i < NODE_COUNT; i++) {
      nodes[i] = THETA_MIN + i * step;
    }

    double[] logPosterior = new double[NODE_COUNT];
    double maxLog = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < NODE_COUNT; i++) {
      double theta = nodes[i];
      double logLikelihood = 0.0;
      for (IrtAnswerRecord record : history) {
        double p = probability(theta, record.discriminationA(), record.difficultyB());
        p = clamp(p, 1e-8, 1.0 - 1e-8);
        logLikelihood += record.correct() ? Math.log(p) : Math.log(1.0 - p);
      }
      double logPrior = -0.5 * theta * theta;
      double value = logLikelihood + logPrior;
      logPosterior[i] = value;
      if (value > maxLog) {
        maxLog = value;
      }
    }

    double sumWeight = 0.0;
    double sumTheta = 0.0;
    for (int i = 0; i < NODE_COUNT; i++) {
      double weight = Math.exp(logPosterior[i] - maxLog);
      sumWeight += weight;
      sumTheta += nodes[i] * weight;
    }

    double thetaHat = sumWeight == 0.0 ? 0.0 : sumTheta / sumWeight;
    double info = 0.0;
    for (IrtAnswerRecord record : history) {
      info += information(thetaHat, record.discriminationA(), record.difficultyB());
    }
    double standardError = info > 0.0 ? 1.0 / Math.sqrt(info) : 9.99;
    return new IrtEstimate(thetaHat, standardError);
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
