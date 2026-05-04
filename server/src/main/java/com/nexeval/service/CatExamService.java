package com.nexeval.service;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.ExamAnswerView;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.QuestionView;
import com.nexeval.dto.StartExamResponse;
import com.nexeval.model.BlankQuestionBank;
import com.nexeval.model.ExamAnswer;
import com.nexeval.model.EssayQuestionBank;
import com.nexeval.model.ExamDefinition;
import com.nexeval.model.ExamPaperQuestion;
import com.nexeval.model.ExamSession;
import com.nexeval.model.JudgeQuestionBank;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionItem;
import com.nexeval.model.QuestionOption;
import com.nexeval.model.QuestionType;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.ExamDefinitionRepository;
import com.nexeval.repository.ExamPaperQuestionRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.ws.ExamWebSocketHub;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CatExamService {

  private static final Logger log = LoggerFactory.getLogger(CatExamService.class);

  private static final int DEFAULT_MAX_QUESTIONS = 10;
  private static final List<String> JUDGE_OPTIONS = List.of("true", "false");
  private final ExamWebSocketHub webSocketHub;
  private final ExamDefinitionRepository examDefinitionRepository;
  private final ExamPaperQuestionRepository examPaperQuestionRepository;
  private final QuestionBankRepository questionBankRepository;
  private final JudgeQuestionBankRepository judgeQuestionBankRepository;
  private final BlankQuestionBankRepository blankQuestionBankRepository;
  private final EssayQuestionBankRepository essayQuestionBankRepository;
  private final ExamAnswerRepository examAnswerRepository;

  private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

  private final Map<String, List<QuestionItem>> examQuestionCache = new ConcurrentHashMap<>();

  public CatExamService(
    ExamWebSocketHub webSocketHub,
    ExamDefinitionRepository examDefinitionRepository,
    ExamPaperQuestionRepository examPaperQuestionRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository,
    ExamAnswerRepository examAnswerRepository
  ) {
    this.webSocketHub = webSocketHub;
    this.examDefinitionRepository = examDefinitionRepository;
    this.examPaperQuestionRepository = examPaperQuestionRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
    this.examAnswerRepository = examAnswerRepository;
  }

  public StartExamResponse startSession(String userId, String sourceId) {
    String normalizedSourceId = normalizeSourceId(sourceId);
    List<QuestionItem> questionBank = getQuestionBank(normalizedSourceId);

    int maxQuestions;
    if (normalizedSourceId.isBlank()) {
      maxQuestions = resolveMaxQuestions(resolveExamDefinition(null), questionBank.size());
    } else if (isCourseSource(normalizedSourceId)) {
      maxQuestions = questionBank.size();
    } else {
      maxQuestions = resolveMaxQuestions(resolveExamDefinition(normalizedSourceId), questionBank.size());
    }

    String sessionId = UUID.randomUUID().toString().replace("-", "");
    ExamSession session = new ExamSession(sessionId, userId, normalizedSourceId, maxQuestions);
    sessions.put(sessionId, session);
    return new StartExamResponse(
      sessionId,
      session.getUserId(),
      session.getExamId(),
      session.getTheta(),
      session.getMaxQuestions()
    );
  }

  public List<QuestionView> getExamQuestions(String sessionId) {
    ExamSession session = requireSession(sessionId);
    return getQuestionBank(session.getExamId()).stream()
      .map(this::toQuestionView)
      .toList();
  }

  public Map<String, Object> getSessionState(String sessionId) {
    ExamSession session = requireSession(sessionId);
    return Map.of(
      "sessionId", session.getSessionId(),
      "examId", session.getExamId(),
      "theta", session.getTheta(),
      "answeredCount", session.getAnsweredCount(),
      "maxQuestions", session.getMaxQuestions(),
      "finished", session.isFinished(),
      "answeredQuestionIds", session.getAnsweredQuestionIds().stream().toList()
    );
  }

  public List<ExamAnswerView> getSessionAnswers(String sessionId) {
    try {
      return examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc(sessionId).stream()
        .map(this::toAnswerView)
        .toList();
    } catch (DataAccessException ex) {
      // Table may not exist or DB is not ready; log and return empty list so
      // frontend can continue without crashing the websocket request.
      log.warn("Failed to load session answers for sessionId={}: {}", sessionId, ex.getMessage());
      return List.of();
    }
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
    String answerText = request.selectedOption().trim();

    QuestionItem question = requireQuestion(session.getExamId(), questionId);
    boolean scoreEnabled = question.scorable();
    boolean correct = scoreEnabled && matchesAnswer(question.answerKey(), answerText);

    session.markAnswered(question.id(), correct, question.difficulty(), scoreEnabled);
    saveAnswer(session, question, answerText, scoreEnabled, correct);

    boolean reportedCorrect = scoreEnabled ? correct : true;
    webSocketHub.pushToExam(sessionId, Map.of(
      "type", "EVENT",
      "event", "ANSWER_UPDATED",
      "payload", Map.of(
        "sessionId", sessionId,
        "questionId", question.id(),
        "correct", reportedCorrect,
        "theta", session.getTheta(),
        "answeredCount", session.getAnsweredCount(),
        "finished", session.isFinished()
      )
    ));
    return new AnswerResponse(reportedCorrect, session.getTheta(), session.getAnsweredCount(), session.isFinished());
  }

  private QuestionItem pickNextQuestion(ExamSession session) {
    List<QuestionItem> questionBank = getQuestionBank(session.getExamId());
    Set<String> answered = session.getAnsweredQuestionIds();

    return questionBank.stream()
      .filter(item -> !answered.contains(item.id()))
      .min(Comparator.comparingDouble(item -> Math.abs(item.difficulty() - session.getTheta())))
      .orElse(null);
  }

  private void saveAnswer(
    ExamSession session,
    QuestionItem question,
    String answerText,
    boolean scoreEnabled,
    boolean correct
  ) {
    try {
      Optional<ExamAnswer> existing = examAnswerRepository
        .findFirstBySessionIdAndQuestionId(session.getSessionId(), question.id());
      ExamAnswer answer = existing.orElseGet(ExamAnswer::new);

      answer.setSessionId(session.getSessionId());
      answer.setUserId(session.getUserId());
      String courseNo = normalizeSourceId(session.getExamId());
      answer.setCourseNo(courseNo.isBlank() ? null : courseNo);
      answer.setQuestionId(question.id());
      answer.setQuestionType(question.type());
      answer.setAnswerText(answerText);
      answer.setCorrect(scoreEnabled ? correct : null);
      answer.setAnsweredAt(Instant.now());

      if (answer.getId() == null) {
        answer.setReviewed(false);
      }

      examAnswerRepository.save(answer);
    } catch (DataAccessException ex) {
      // Avoid bubbling DB errors to caller; the session state has already
      // been updated in-memory. Log and continue so UI stays responsive.
      log.warn("Failed to persist answer for sessionId={}, questionId={}: {}",
        session.getSessionId(), question.id(), ex.getMessage());
    }
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
    String sourceId = normalizeSourceId(examId);
    String cacheKey = sourceId.isBlank() ? "DEFAULT_EXAM" : sourceId;

    List<QuestionItem> cached = examQuestionCache.get(cacheKey);
    if (cached != null && !cached.isEmpty()) {
      return cached;
    }

    List<QuestionItem> loaded = loadQuestionBankBySource(sourceId);
    examQuestionCache.put(cacheKey, loaded);
    return loaded;
  }

  private List<QuestionItem> loadQuestionBankBySource(String sourceId) {
    String normalizedSourceId = normalizeSourceId(sourceId);

    if (normalizedSourceId.isBlank()) {
      return loadExamQuestionBank(resolveExamDefinition(null));
    }

    List<QuestionItem> courseQuestions = loadCourseQuestionBank(normalizedSourceId);
    if (!courseQuestions.isEmpty()) {
      return courseQuestions;
    }

    return loadExamQuestionBank(resolveExamDefinition(normalizedSourceId));
  }

  private boolean isCourseSource(String sourceId) {
    String normalizedSourceId = normalizeSourceId(sourceId);
    return !normalizedSourceId.isBlank() && (
      questionBankRepository.existsByActiveTrueAndCno(normalizedSourceId)
        || judgeQuestionBankRepository.existsByActiveTrueAndCno(normalizedSourceId)
        || blankQuestionBankRepository.existsByActiveTrueAndCno(normalizedSourceId)
        || essayQuestionBankRepository.existsByActiveTrueAndCno(normalizedSourceId)
    );
  }

  private List<QuestionItem> loadCourseQuestionBank(String courseNo) {
    List<QuestionItem> items = new ArrayList<>();

    List<QuestionBank> choiceQuestions = questionBankRepository.findAllByActiveTrueAndCno(courseNo);
    items.addAll(choiceQuestions.stream()
      .sorted(Comparator.comparingDouble(QuestionBank::getDifficulty).thenComparing(QuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    List<JudgeQuestionBank> judgeQuestions = judgeQuestionBankRepository.findAllByActiveTrueAndCno(courseNo);
    items.addAll(judgeQuestions.stream()
      .sorted(Comparator.comparingDouble(JudgeQuestionBank::getDifficulty).thenComparing(JudgeQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    List<BlankQuestionBank> blankQuestions = blankQuestionBankRepository.findAllByActiveTrueAndCno(courseNo);
    items.addAll(blankQuestions.stream()
      .sorted(Comparator.comparingDouble(BlankQuestionBank::getDifficulty).thenComparing(BlankQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    List<EssayQuestionBank> essayQuestions = essayQuestionBankRepository.findAllByActiveTrueAndCno(courseNo);
    items.addAll(essayQuestions.stream()
      .sorted(Comparator.comparingDouble(EssayQuestionBank::getDifficulty).thenComparing(EssayQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    return items.stream()
      .sorted(Comparator.comparingInt((QuestionItem item) -> typeOrder(item.type()))
        .thenComparingDouble(QuestionItem::difficulty)
        .thenComparing(QuestionItem::id))
      .toList();
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
      question.getDifficulty(),
      QuestionType.CHOICE,
      true
    );
  }

  private QuestionItem toQuestionItem(JudgeQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      JUDGE_OPTIONS,
      question.isAnswerKey() ? "true" : "false",
      question.getDifficulty(),
      QuestionType.JUDGE,
      true
    );
  }

  private QuestionItem toQuestionItem(BlankQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      List.of(),
      question.getAnswerKey(),
      question.getDifficulty(),
      QuestionType.BLANK,
      true
    );
  }

  private QuestionItem toQuestionItem(EssayQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      List.of(),
      "",
      question.getDifficulty(),
      QuestionType.ESSAY,
      false
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
    return new QuestionView(
      item.id(),
      item.stem(),
      item.options(),
      item.difficulty(),
      item.type().name().toLowerCase()
    );
  }

  private ExamAnswerView toAnswerView(ExamAnswer answer) {
    return new ExamAnswerView(
      answer.getQuestionId(),
      answer.getAnswerText(),
      answer.getQuestionType().name().toLowerCase(),
      answer.getCorrect()
    );
  }

  private int typeOrder(QuestionType type) {
    return switch (type) {
      case CHOICE -> 1;
      case JUDGE -> 2;
      case BLANK -> 3;
      case ESSAY -> 4;
    };
  }

  private boolean matchesAnswer(String answerKey, String answerText) {
    return Objects.equals(normalizeAnswer(answerKey), normalizeAnswer(answerText));
  }

  private String normalizeAnswer(String answer) {
    if (answer == null) {
      return "";
    }
    return answer.trim().replaceAll("\\s+", " ").toLowerCase();
  }

  private String normalizeSourceId(String sourceId) {
    return sourceId == null ? "" : sourceId.trim();
  }
}
