package com.nexeval.service;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.ExamAnswerDetailView;
import com.nexeval.dto.ExamAttemptView;
import com.nexeval.dto.ExamAnswerView;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.QuestionView;
import com.nexeval.dto.StartExamResponse;
import com.nexeval.model.BlankQuestionBank;
import com.nexeval.model.ExamAnswer;
import com.nexeval.model.ExamAttempt;
import com.nexeval.model.ExamAttemptStatus;
import com.nexeval.model.EssayQuestionBank;
import com.nexeval.model.ExamDefinition;
import com.nexeval.model.ExamPaperQuestion;
import com.nexeval.model.ExamSession;
import com.nexeval.model.JudgeQuestionBank;
import com.nexeval.model.PracticePaper;
import com.nexeval.model.PracticePaperQuestion;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionItem;
import com.nexeval.model.QuestionOption;
import com.nexeval.model.QuestionType;
import com.nexeval.model.SessionMode;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.ExamAttemptRepository;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.ExamDefinitionRepository;
import com.nexeval.repository.ExamPaperQuestionRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.PracticePaperQuestionRepository;
import com.nexeval.repository.PracticePaperRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.ws.ExamWebSocketHub;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
  private static final int DEFAULT_EXAM_DURATION_MINUTES = 60;
  private static final List<String> JUDGE_OPTIONS = List.of("true", "false");
  private final ExamWebSocketHub webSocketHub;
  private final ExamDefinitionRepository examDefinitionRepository;
  private final ExamPaperQuestionRepository examPaperQuestionRepository;
  private final PracticePaperRepository practicePaperRepository;
  private final PracticePaperQuestionRepository practicePaperQuestionRepository;
  private final QuestionBankRepository questionBankRepository;
  private final JudgeQuestionBankRepository judgeQuestionBankRepository;
  private final BlankQuestionBankRepository blankQuestionBankRepository;
  private final EssayQuestionBankRepository essayQuestionBankRepository;
  private final ExamAnswerRepository examAnswerRepository;
  private final ExamAttemptRepository examAttemptRepository;

  private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

  private final Map<String, List<QuestionItem>> examQuestionCache = new ConcurrentHashMap<>();

  public CatExamService(
    ExamWebSocketHub webSocketHub,
    ExamDefinitionRepository examDefinitionRepository,
    ExamPaperQuestionRepository examPaperQuestionRepository,
    PracticePaperRepository practicePaperRepository,
    PracticePaperQuestionRepository practicePaperQuestionRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository,
    ExamAnswerRepository examAnswerRepository,
    ExamAttemptRepository examAttemptRepository
  ) {
    this.webSocketHub = webSocketHub;
    this.examDefinitionRepository = examDefinitionRepository;
    this.examPaperQuestionRepository = examPaperQuestionRepository;
    this.practicePaperRepository = practicePaperRepository;
    this.practicePaperQuestionRepository = practicePaperQuestionRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
    this.examAnswerRepository = examAnswerRepository;
    this.examAttemptRepository = examAttemptRepository;
  }

  public StartExamResponse startSession(String userId, String sourceId) {
    return startPracticeSession(userId, sourceId);
  }

  public StartExamResponse startPracticeSession(String userId, String courseNo) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    PracticePaper practicePaper = resolvePracticePaper(normalizedCourseNo);

    String sourceId = normalizedCourseNo;
    String paperId = null;
    List<QuestionItem> questionBank;

    if (practicePaper != null) {
      paperId = practicePaper.getId();
      sourceId = paperId;
      questionBank = loadPracticeQuestionBank(paperId);
    } else if (!normalizedCourseNo.isBlank() && isCourseSource(normalizedCourseNo)) {
      questionBank = loadCourseQuestionBank(normalizedCourseNo);
    } else {
      questionBank = loadExamQuestionBank(resolveExamDefinition(null));
    }

    int maxQuestions = questionBank.size();
    ExamSession session = createSession(
      userId,
      normalizedCourseNo,
      sourceId,
      maxQuestions,
      SessionMode.PRACTICE,
      paperId,
      null
    );

    return toStartResponse(session);
  }

  public StartExamResponse startExamSession(String userId, String courseNo) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    if (normalizedCourseNo.isBlank()) {
      throw new IllegalArgumentException("courseNo is required for exam sessions");
    }
    if (!isCourseSource(normalizedCourseNo)) {
      throw new IllegalArgumentException("No questions configured for course: " + normalizedCourseNo);
    }
    ExamDefinition definition = resolveExamDefinition(null);
    List<QuestionItem> questionBank = loadCourseQuestionBank(normalizedCourseNo);
    String paperId = null;
    int maxQuestions = resolveMaxQuestions(definition, questionBank.size());

    int durationMinutes = definition.getDurationMinutes() > 0
      ? definition.getDurationMinutes()
      : DEFAULT_EXAM_DURATION_MINUTES;
    int timeLimitSeconds = durationMinutes * 60;

    ExamSession session = createSession(
      userId,
      normalizedCourseNo,
      definition.getId(),
      maxQuestions,
      SessionMode.EXAM,
      paperId,
      timeLimitSeconds
    );

    return toStartResponse(session);
  }

  public List<QuestionView> getExamQuestions(String sessionId) {
    ExamSession session = requireSession(sessionId);
    refreshSessionStatus(session);
    return getQuestionBank(session).stream()
      .map(this::toQuestionView)
      .toList();
  }

  public Map<String, Object> getSessionState(String sessionId) {
    ExamSession session = requireSession(sessionId);
    refreshSessionStatus(session);
    long remainingSeconds = -1;
    if (session.getMode() == SessionMode.EXAM) {
      remainingSeconds = Math.max(0, session.getRemainingSeconds(Instant.now()));
    }
    Integer timeLimitSeconds = session.getTimeLimitSeconds();

    Map<String, Object> state = new LinkedHashMap<>();
    state.put("sessionId", session.getSessionId());
    state.put("examId", session.getExamId());
    state.put("courseNo", session.getCourseNo());
    state.put("mode", session.getMode().name().toLowerCase());
    state.put("theta", session.getTheta());
    state.put("answeredCount", session.getAnsweredCount());
    state.put("maxQuestions", session.getMaxQuestions());
    state.put("finished", session.isFinished());
    state.put("timeLimitSeconds", timeLimitSeconds == null ? -1 : timeLimitSeconds);
    state.put("remainingSeconds", remainingSeconds);
    state.put("answeredQuestionIds", session.getAnsweredQuestionIds().stream().toList());
    return state;
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

  public Map<String, Object> finishSession(String sessionId) {
    ExamSession session = requireSession(sessionId);
    session.submit(Instant.now());
    updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, Instant.now());

    return Map.of(
      "sessionId", session.getSessionId(),
      "finished", session.isFinished(),
      "submittedAt", session.getSubmittedAt() == null ? "" : session.getSubmittedAt().toString()
    );
  }

  public List<ExamAttemptView> getExamAttempts(String courseNo, String userId) {
    try {
      List<ExamAttempt> attempts;
      String normalizedCourseNo = normalizeSourceId(courseNo);
      if (!normalizedCourseNo.isBlank()) {
        attempts = examAttemptRepository
          .findAllByCourseNoAndUserIdAndModeOrderByStartedAtDesc(normalizedCourseNo, userId, SessionMode.EXAM);
      } else {
        attempts = examAttemptRepository.findAllByUserIdAndModeOrderByStartedAtDesc(userId, SessionMode.EXAM);
      }

      return attempts.stream()
        .map(this::toAttemptView)
        .toList();
    } catch (DataAccessException ex) {
      log.warn("Failed to load exam attempts for userId={}, courseNo={}: {}", userId, courseNo, ex.getMessage());
      return List.of();
    }
  }

  public List<ExamAnswerDetailView> getAttemptAnswers(String sessionId) {
    try {
      return examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc(sessionId).stream()
        .map(this::toAnswerDetailView)
        .toList();
    } catch (DataAccessException ex) {
      log.warn("Failed to load attempt answers for sessionId={}: {}", sessionId, ex.getMessage());
      return List.of();
    }
  }

  public ExamAnswerDetailView reviewAnswer(Long answerId, Integer score, String reviewNote, String reviewerId) {
    ExamAnswer answer = examAnswerRepository.findById(answerId)
      .orElseThrow(() -> new NoSuchElementException("Answer not found: " + answerId));

    answer.setScore(score);
    answer.setReviewNote(reviewNote == null || reviewNote.isBlank() ? null : reviewNote.trim());
    answer.setReviewerId(reviewerId == null || reviewerId.isBlank() ? null : reviewerId.trim());
    answer.setReviewed(true);
    answer.setReviewedAt(Instant.now());

    ExamAnswer saved = examAnswerRepository.save(answer);
    return toAnswerDetailView(saved);
  }

  public NextQuestionResponse getNextQuestion(String sessionId) {
    ExamSession session = requireSession(sessionId);

    refreshSessionStatus(session);

    if (session.isFinished()) {
      return buildFinishedResponse(session);
    }

    QuestionItem next = pickNextQuestion(session);
    if (next == null) {
      session.finish();
      updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, session.getSubmittedAt());
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

    refreshSessionStatus(session);
    if (session.isFinished()) {
      throw new IllegalStateException("Session already finished.");
    }

    String questionId = request.questionId().trim();
    String answerText = request.selectedOption().trim();

    QuestionItem question = requireQuestion(session, questionId);
    boolean scoreEnabled = question.scorable();
    boolean correct = scoreEnabled && matchesAnswer(question.answerKey(), answerText);

    session.markAnswered(question.id(), correct, question.difficulty(), scoreEnabled);
    saveAnswer(session, question, answerText, scoreEnabled, correct);

    if (session.isFinished()) {
      session.submit(Instant.now());
      updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, session.getSubmittedAt());
    }

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
    List<QuestionItem> questionBank = getQuestionBank(session);
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
      String courseNo = normalizeSourceId(session.getCourseNo());
      answer.setCourseNo(courseNo.isBlank() ? null : courseNo);
      answer.setQuestionId(question.id());
      answer.setQuestionType(question.type());
      answer.setAnswerText(answerText);
      answer.setCorrect(scoreEnabled ? correct : null);
      if (scoreEnabled) {
        answer.setScore(correct ? 1 : 0);
        answer.setReviewed(true);
        answer.setReviewNote(null);
        answer.setReviewerId(null);
        answer.setReviewedAt(null);
      } else {
        answer.setScore(null);
        answer.setReviewed(false);
      }
      answer.setAnsweredAt(Instant.now());

      if (answer.getId() == null) {
        answer.setReviewed(scoreEnabled);
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

  private QuestionItem requireQuestion(ExamSession session, String questionId) {
    List<QuestionItem> questionBank = getQuestionBank(session);
    return questionBank.stream()
      .filter(item -> item.id().equals(questionId))
      .findFirst()
      .orElseThrow(() -> new NoSuchElementException("Question not found: " + questionId));
  }

  private List<QuestionItem> getQuestionBank(ExamSession session) {
    String sourceId = normalizeSourceId(session.getExamId());
    String cacheKey = session.getMode().name() + ":" + (sourceId.isBlank() ? "DEFAULT" : sourceId);
    if (session.getMode() == SessionMode.EXAM) {
      String courseNo = normalizeSourceId(session.getCourseNo());
      if (!courseNo.isBlank() && isCourseSource(courseNo)) {
        cacheKey = session.getMode().name() + ":COURSE:" + courseNo;
      }
    }

    List<QuestionItem> cached = examQuestionCache.get(cacheKey);
    if (cached != null && !cached.isEmpty()) {
      return cached;
    }

    List<QuestionItem> loaded = loadQuestionBankForSession(session);
    examQuestionCache.put(cacheKey, loaded);
    return loaded;
  }

  private List<QuestionItem> loadQuestionBankForSession(ExamSession session) {
    if (session.getMode() == SessionMode.EXAM) {
      String courseNo = normalizeSourceId(session.getCourseNo());
      if (!courseNo.isBlank() && isCourseSource(courseNo)) {
        return loadCourseQuestionBank(courseNo);
      }
      return loadExamQuestionBank(resolveExamDefinition(session.getExamId()));
    }

    if (session.getPaperId() != null && !session.getPaperId().isBlank()) {
      return loadPracticeQuestionBank(session.getPaperId());
    }

    String courseNo = normalizeSourceId(session.getCourseNo());
    if (!courseNo.isBlank() && isCourseSource(courseNo)) {
      return loadCourseQuestionBank(courseNo);
    }

    PracticePaper fallbackPaper = resolvePracticePaper(courseNo);
    if (fallbackPaper != null) {
      return loadPracticeQuestionBank(fallbackPaper.getId());
    }

    return loadExamQuestionBank(resolveExamDefinition(null));
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

  private List<QuestionItem> loadPracticeQuestionBank(String paperId) {
    List<PracticePaperQuestion> paperQuestions = practicePaperQuestionRepository
      .findAllByPaper_IdOrderByDisplayOrderAsc(paperId);

    if (paperQuestions.isEmpty()) {
      throw new NoSuchElementException("Practice paper has no questions: " + paperId);
    }

    return paperQuestions.stream()
      .map(PracticePaperQuestion::getQuestion)
      .map(this::toQuestionItem)
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

  private PracticePaper resolvePracticePaper(String courseNo) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    if (!normalizedCourseNo.isBlank()) {
      Optional<PracticePaper> byCourse = practicePaperRepository
        .findFirstByActiveTrueAndCourseNo(normalizedCourseNo);
      if (byCourse.isPresent()) {
        return byCourse.get();
      }
    }

    return practicePaperRepository.findFirstByActiveTrueAndCourseNoIsNull().orElse(null);
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

  private ExamSession createSession(
    String userId,
    String courseNo,
    String sourceId,
    int maxQuestions,
    SessionMode mode,
    String paperId,
    Integer timeLimitSeconds
  ) {
    String sessionId = UUID.randomUUID().toString().replace("-", "");
    Instant startedAt = Instant.now();
    ExamSession session = new ExamSession(
      sessionId,
      userId,
      courseNo,
      normalizeSourceId(sourceId),
      maxQuestions,
      mode,
      paperId,
      startedAt,
      timeLimitSeconds
    );
    sessions.put(sessionId, session);
    persistAttempt(session);
    return session;
  }

  private StartExamResponse toStartResponse(ExamSession session) {
    return new StartExamResponse(
      session.getSessionId(),
      session.getUserId(),
      session.getExamId(),
      session.getTheta(),
      session.getMaxQuestions(),
      session.getMode().name().toLowerCase(),
      session.getTimeLimitSeconds(),
      session.getStartedAt() == null ? null : session.getStartedAt().toString()
    );
  }

  private void persistAttempt(ExamSession session) {
    try {
      ExamAttempt attempt = new ExamAttempt();
      attempt.setSessionId(session.getSessionId());
      attempt.setUserId(session.getUserId());
      attempt.setCourseNo(normalizeSourceId(session.getCourseNo()));
      attempt.setMode(session.getMode());
      attempt.setPaperId(session.getPaperId());
      attempt.setStartedAt(session.getStartedAt() == null ? Instant.now() : session.getStartedAt());
      attempt.setTimeLimitSeconds(session.getTimeLimitSeconds());
      attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
      examAttemptRepository.save(attempt);
    } catch (DataAccessException ex) {
      log.warn("Failed to persist attempt for sessionId={}: {}", session.getSessionId(), ex.getMessage());
    }
  }

  private void updateAttemptStatus(ExamSession session, ExamAttemptStatus status, Instant submittedAt) {
    try {
      Optional<ExamAttempt> attempt = examAttemptRepository.findFirstBySessionId(session.getSessionId());
      if (attempt.isEmpty()) {
        return;
      }
      ExamAttempt value = attempt.get();
      value.setStatus(status);
      if (submittedAt != null) {
        value.setSubmittedAt(submittedAt);
      }
      examAttemptRepository.save(value);
    } catch (DataAccessException ex) {
      log.warn("Failed to update attempt status for sessionId={}: {}", session.getSessionId(), ex.getMessage());
    }
  }

  private void refreshSessionStatus(ExamSession session) {
    if (session.isFinished()) {
      return;
    }
    if (session.getMode() == SessionMode.EXAM && session.isExpired(Instant.now())) {
      session.submit(Instant.now());
      updateAttemptStatus(session, ExamAttemptStatus.EXPIRED, session.getSubmittedAt());
    }
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

  private ExamAttemptView toAttemptView(ExamAttempt attempt) {
    return new ExamAttemptView(
      attempt.getSessionId(),
      attempt.getUserId(),
      attempt.getCourseNo(),
      attempt.getMode().name().toLowerCase(),
      attempt.getStatus().name().toLowerCase(),
      attempt.getStartedAt() == null ? null : attempt.getStartedAt().toString(),
      attempt.getSubmittedAt() == null ? null : attempt.getSubmittedAt().toString(),
      attempt.getTimeLimitSeconds()
    );
  }

  private ExamAnswerDetailView toAnswerDetailView(ExamAnswer answer) {
    return new ExamAnswerDetailView(
      answer.getId(),
      answer.getQuestionId(),
      resolveQuestionStem(answer),
      answer.getQuestionType().name().toLowerCase(),
      answer.getAnswerText(),
      answer.getCorrect(),
      answer.getScore(),
      answer.isReviewed(),
      answer.getReviewNote()
    );
  }

  private String resolveQuestionStem(ExamAnswer answer) {
    if (answer.getQuestionType() == null) {
      return "";
    }

    return switch (answer.getQuestionType()) {
      case CHOICE -> questionBankRepository.findById(answer.getQuestionId())
        .map(QuestionBank::getStem)
        .orElse("");
      case JUDGE -> judgeQuestionBankRepository.findById(answer.getQuestionId())
        .map(JudgeQuestionBank::getStem)
        .orElse("");
      case BLANK -> blankQuestionBankRepository.findById(answer.getQuestionId())
        .map(BlankQuestionBank::getStem)
        .orElse("");
      case ESSAY -> essayQuestionBankRepository.findById(answer.getQuestionId())
        .map(EssayQuestionBank::getStem)
        .orElse("");
    };
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
