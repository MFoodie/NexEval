package com.nexeval.service;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.QuestionView;
import com.nexeval.dto.StartExamResponse;
import com.nexeval.model.ExamDefinition;
import com.nexeval.model.ExamPaperQuestion;
import com.nexeval.model.ExamSession;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionItem;
import com.nexeval.model.QuestionOption;
import com.nexeval.repository.ExamDefinitionRepository;
import com.nexeval.repository.ExamPaperQuestionRepository;
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

  private static final int DEFAULT_MAX_QUESTIONS = 10;
  private final ExamWebSocketHub webSocketHub;
  private final ExamDefinitionRepository examDefinitionRepository;
  private final ExamPaperQuestionRepository examPaperQuestionRepository;

  private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

  private final Map<String, List<QuestionItem>> examQuestionCache = new ConcurrentHashMap<>();

  public CatExamService(
    ExamWebSocketHub webSocketHub,
    ExamDefinitionRepository examDefinitionRepository,
    ExamPaperQuestionRepository examPaperQuestionRepository
  ) {
    this.webSocketHub = webSocketHub;
    this.examDefinitionRepository = examDefinitionRepository;
    this.examPaperQuestionRepository = examPaperQuestionRepository;
  }

  public StartExamResponse startSession(String userId, String examId) {
    ExamDefinition examDefinition = resolveExamDefinition(examId);
    List<QuestionItem> questionBank = getQuestionBank(examDefinition.getId());
    int maxQuestions = resolveMaxQuestions(examDefinition, questionBank.size());
    String sessionId = UUID.randomUUID().toString().replace("-", "");
    ExamSession session = new ExamSession(sessionId, userId, examDefinition.getId(), maxQuestions);
    sessions.put(sessionId, session);
    return new StartExamResponse(
      sessionId,
      session.getUserId(),
      session.getExamId(),
      session.getTheta(),
      session.getMaxQuestions()
    );
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

    QuestionItem question = requireQuestion(session.getExamId(), questionId);
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
    List<QuestionItem> questionBank = getQuestionBank(session.getExamId());
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

  private QuestionItem requireQuestion(String examId, String questionId) {
    List<QuestionItem> questionBank = getQuestionBank(examId);
    return questionBank.stream()
      .filter(item -> item.id().equals(questionId))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("Question not found: " + questionId));
  }

  private List<QuestionItem> getQuestionBank(String examId) {
    ExamDefinition examDefinition = resolveExamDefinition(examId);
    String resolvedExamId = examDefinition.getId();
    List<QuestionItem> cached = examQuestionCache.get(resolvedExamId);
    if (cached != null && !cached.isEmpty()) {
      return cached;
    }

    List<QuestionItem> loaded = loadExamQuestionBank(examDefinition);
    examQuestionCache.put(resolvedExamId, loaded);
    return loaded;
  }

  private List<QuestionItem> loadExamQuestionBank(ExamDefinition examDefinition) {
    List<ExamPaperQuestion> paperQuestions = examPaperQuestionRepository
      .findAllByPaper_IdOrderByDisplayOrderAsc(examDefinition.getPaper().getId());

    if (paperQuestions.isEmpty()) {
      throw new NoSuchElementException("Exam paper has no questions: " + examDefinition.getId());
    }

    return paperQuestions.stream()
      .map(ExamPaperQuestion::getQuestion)
      .map(this::toQuestionItem)
      .toList();
  }

  private ExamDefinition resolveExamDefinition(String examId) {
    if (examId == null || examId.isBlank()) {
      return examDefinitionRepository.findFirstByIsDefaultTrueAndActiveTrue()
        .orElseThrow(() -> new NoSuchElementException("Default exam is not configured"));
    }

    return examDefinitionRepository.findByIdAndActiveTrue(examId)
      .orElseThrow(() -> new NoSuchElementException("Exam not found: " + examId));
  }

  private int resolveMaxQuestions(ExamDefinition examDefinition, int questionCount) {
    int configured = examDefinition.getMaxQuestions();
    int fallback = Math.min(DEFAULT_MAX_QUESTIONS, questionCount);
    if (configured <= 0) {
      return fallback;
    }
    return Math.min(configured, questionCount);
  }

  private QuestionItem toQuestionItem(QuestionBank question) {
    List<String> options = question.getOptions().stream()
      .sorted(Comparator.comparingInt(QuestionOption::getOptionOrder))
      .map(QuestionOption::getOptionText)
      .toList();

    return new QuestionItem(
      question.getId(),
      question.getStem(),
      options,
      question.getAnswerKey(),
      question.getDifficulty()
    );
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
