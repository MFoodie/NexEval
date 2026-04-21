package com.nexeval.service;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.QuestionView;
import com.nexeval.dto.StartExamResponse;
import com.nexeval.model.ExamSession;
import com.nexeval.model.QuestionItem;
import com.nexeval.ws.ExamWebSocketHub;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CatExamService {

  private static final int MAX_QUESTIONS = 10;
  private final ExamWebSocketHub webSocketHub;

  private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

  private final List<QuestionItem> questionBank = List.of(
    new QuestionItem("Q001", "What is 6 + 7?", List.of("11", "12", "13", "14"), "13", -2.2),
    new QuestionItem("Q002", "What is 9 * 8?", List.of("62", "72", "81", "64"), "72", -1.7),
    new QuestionItem("Q003", "Solve x in 2x + 5 = 19.", List.of("6", "7", "8", "9"), "7", -1.1),
    new QuestionItem("Q004", "Derivative of x^2 is:", List.of("x", "2x", "x^2", "2"), "2x", -0.6),
    new QuestionItem("Q005", "Which data structure is FIFO?", List.of("Stack", "Queue", "Tree", "Heap"), "Queue", -0.2),
    new QuestionItem("Q006", "SQL keyword used to filter rows:", List.of("ORDER", "WHERE", "GROUP", "INDEX"), "WHERE", 0.1),
    new QuestionItem("Q007", "Binary search average time complexity:", List.of("O(n)", "O(log n)", "O(n log n)", "O(1)"), "O(log n)", 0.3),
    new QuestionItem("Q008", "If p -> q and p is true, then q is:", List.of("True", "False", "Unknown", "Both"), "True", 0.7),
    new QuestionItem("Q009", "First normal form (1NF) mainly requires:", List.of("No null values", "Atomic column values", "No duplicate rows", "Foreign keys"), "Atomic column values", 1.0),
    new QuestionItem("Q010", "JVM stands for:", List.of("Java Verified Model", "Java Virtual Machine", "Joint Vector Memory", "Java Vendor Module"), "Java Virtual Machine", 1.3),
    new QuestionItem("Q011", "For REST update with idempotence, common method is:", List.of("POST", "PATCH", "PUT", "CONNECT"), "PUT", 1.4),
    new QuestionItem("Q012", "Integral of 2x dx is:", List.of("x^2 + C", "2x + C", "x + C", "x^3 + C"), "x^2 + C", 1.6),
    new QuestionItem("Q013", "Gradient descent is used to:", List.of("Maximize variance", "Minimize loss", "Encode labels", "Sort arrays"), "Minimize loss", 1.9),
    new QuestionItem("Q014", "Under network partition in CAP theorem, systems usually trade off:", List.of("Consistency or Availability", "Latency or Throughput", "Memory or CPU", "Storage or Network"), "Consistency or Availability", 2.2),
    new QuestionItem("Q015", "In IRT/CAT, theta usually denotes:", List.of("Question count", "Latent ability level", "Exam duration", "Difficulty variance"), "Latent ability level", 2.5)
  );

  public CatExamService(ExamWebSocketHub webSocketHub) {
    this.webSocketHub = webSocketHub;
  }

  public StartExamResponse startSession(String userId) {
    String sessionId = UUID.randomUUID().toString().replace("-", "");
    ExamSession session = new ExamSession(sessionId, userId, MAX_QUESTIONS);
    sessions.put(sessionId, session);
    return new StartExamResponse(sessionId, session.getUserId(), session.getTheta(), MAX_QUESTIONS);
  }

  public NextQuestionResponse getNextQuestion(String sessionId) {
    ExamSession session = requireSession(sessionId);

    if (session.isFinished()) {
      return buildFinishedResponse(session);
    }

    QuestionItem next = pickNextQuestion(session);
    if (next == null) {
      session.finish();
      return buildFinishedResponse(session);
    }

    return new NextQuestionResponse(
      session.getSessionId(),
      session.getAnsweredCount(),
      session.getMaxQuestions(),
      session.getTheta(),
      false,
      toQuestionView(next)
    );
  }

  public AnswerResponse submitAnswer(String sessionId, AnswerRequest request) {
    ExamSession session = requireSession(sessionId);

    String questionId = request.questionId().trim();
    String selectedOption = request.selectedOption().trim();

    QuestionItem question = requireQuestion(questionId);
    boolean correct = question.answerKey().equalsIgnoreCase(selectedOption);

    session.markAnswered(question.id(), correct, question.difficulty());

    webSocketHub.pushToExam(sessionId, Map.of(
      "type", "EVENT",
      "event", "ANSWER_UPDATED",
      "payload", Map.of(
        "sessionId", sessionId,
        "questionId", question.id(),
        "correct", correct,
        "theta", session.getTheta(),
        "answeredCount", session.getAnsweredCount(),
        "finished", session.isFinished()
      )
    ));

    return new AnswerResponse(correct, session.getTheta(), session.getAnsweredCount(), session.isFinished());
  }

  private QuestionItem pickNextQuestion(ExamSession session) {
    Set<String> answered = session.getAnsweredQuestionIds();

    return questionBank.stream()
      .filter(item -> !answered.contains(item.id()))
      .min(Comparator.comparingDouble(item -> Math.abs(item.difficulty() - session.getTheta())))
      .orElse(null);
  }

  private ExamSession requireSession(String sessionId) {
    ExamSession session = sessions.get(sessionId);
    if (session == null) {
      throw new NoSuchElementException("Session not found: " + sessionId);
    }
    return session;
  }

  private QuestionItem requireQuestion(String questionId) {
    return questionBank.stream()
      .filter(item -> item.id().equals(questionId))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("Question not found: " + questionId));
  }

  private NextQuestionResponse buildFinishedResponse(ExamSession session) {
    return new NextQuestionResponse(
      session.getSessionId(),
      session.getAnsweredCount(),
      session.getMaxQuestions(),
      session.getTheta(),
      true,
      null
    );
  }

  private QuestionView toQuestionView(QuestionItem item) {
    return new QuestionView(item.id(), item.stem(), item.options(), item.difficulty());
  }
}
