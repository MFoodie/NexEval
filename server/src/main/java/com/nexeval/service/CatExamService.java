package com.nexeval.service;

import com.nexeval.dto.AvailablePaperView;
import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.CatKnowledgeInsightsResponse;
import com.nexeval.dto.ExamAnswerDetailView;
import com.nexeval.dto.ExamAttemptView;
import com.nexeval.dto.ExamAnswerView;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.ScoreAppealRequest;
import com.nexeval.dto.ScoreAppealView;
import com.nexeval.dto.QuestionSearchView;
import com.nexeval.dto.QuestionView;
import com.nexeval.dto.StartExamResponse;
import com.nexeval.model.BlankQuestionBank;
import com.nexeval.model.ExamAnswer;
import com.nexeval.model.ExamAttempt;
import com.nexeval.model.ExamAttemptStatus;
import com.nexeval.model.EssayQuestionBank;
import com.nexeval.model.ExamDefinition;
import com.nexeval.model.ExamPaper;
import com.nexeval.model.ExamPaperQuestion;
import com.nexeval.model.ExamSession;
import com.nexeval.model.JudgeQuestionBank;
import com.nexeval.model.JudgeQuestionMedia;
import com.nexeval.model.PaperPublish;
import com.nexeval.model.PaperQuestionItem;
import com.nexeval.model.PracticePaper;
import com.nexeval.model.PracticePaperQuestion;
import com.nexeval.model.ScoreAppeal;
import com.nexeval.model.ScoreAppealStatus;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionItem;
import com.nexeval.model.QuestionOption;
import com.nexeval.model.QuestionType;
import com.nexeval.model.TeacherProfile;
import com.nexeval.model.SessionMode;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.ExamAttemptRepository;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.ExamDefinitionRepository;
import com.nexeval.repository.ExamPaperQuestionRepository;
import com.nexeval.repository.ExamPaperRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.JudgeQuestionMediaRepository;
import com.nexeval.repository.PaperPublishRepository;
import com.nexeval.repository.PaperQuestionItemRepository;
import com.nexeval.repository.PracticePaperQuestionRepository;
import com.nexeval.repository.PracticePaperRepository;
import com.nexeval.repository.ScRecordRepository;
import com.nexeval.repository.ScoreAppealRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.ws.ExamWebSocketHub;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatExamService {

  private static final Logger log = LoggerFactory.getLogger(CatExamService.class);

  private static final int DEFAULT_EXAM_DURATION_MINUTES = 60;
  private static final int CAT_MAX_QUESTIONS = 20;
  private static final double CAT_DIFFICULTY_STEP = 0.5;
  private static final double CAT_DIFFICULTY_EPSILON = 1e-9;
  private static final List<String> JUDGE_OPTIONS = List.of("true", "false");
  private final ExamWebSocketHub webSocketHub;
  private final ExamDefinitionRepository examDefinitionRepository;
  private final ExamPaperQuestionRepository examPaperQuestionRepository;
  private final PracticePaperRepository practicePaperRepository;
  private final PracticePaperQuestionRepository practicePaperQuestionRepository;
  private final QuestionBankRepository questionBankRepository;
  private final JudgeQuestionBankRepository judgeQuestionBankRepository;
  private final JudgeQuestionMediaRepository judgeQuestionMediaRepository;
  private final BlankQuestionBankRepository blankQuestionBankRepository;
  private final EssayQuestionBankRepository essayQuestionBankRepository;
  private final ExamAnswerRepository examAnswerRepository;
  private final ExamAttemptRepository examAttemptRepository;
  private final ScoreAppealRepository scoreAppealRepository;
  private final TeacherProfileRepository teacherProfileRepository;
  private final ExamPaperRepository examPaperRepository;
  private final PaperQuestionItemRepository paperQuestionItemRepository;
  private final PaperPublishRepository paperPublishRepository;
  private final ScRecordRepository scRecordRepository;
  private final AiGradingService aiGradingService;
  private final AiPracticeService aiPracticeService;
  private final AiWeaknessService aiWeaknessService;
  private final IrtCatService irtCatService;

  private final Map<String, ExamSession> sessions = new ConcurrentHashMap<>();

  private final Map<String, List<QuestionItem>> examQuestionCache = new ConcurrentHashMap<>();

  private final Map<String, Set<String>> recentCatQuestionIds = new ConcurrentHashMap<>();

  public CatExamService(
    ExamWebSocketHub webSocketHub,
    ExamDefinitionRepository examDefinitionRepository,
    ExamPaperQuestionRepository examPaperQuestionRepository,
    PracticePaperRepository practicePaperRepository,
    PracticePaperQuestionRepository practicePaperQuestionRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    JudgeQuestionMediaRepository judgeQuestionMediaRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository,
    ExamAnswerRepository examAnswerRepository,
    ExamAttemptRepository examAttemptRepository,
    ScoreAppealRepository scoreAppealRepository,
    TeacherProfileRepository teacherProfileRepository,
    ExamPaperRepository examPaperRepository,
    PaperQuestionItemRepository paperQuestionItemRepository,
    PaperPublishRepository paperPublishRepository,
    ScRecordRepository scRecordRepository,
    AiGradingService aiGradingService,
    AiPracticeService aiPracticeService,
    AiWeaknessService aiWeaknessService,
    IrtCatService irtCatService
  ) {
    this.webSocketHub = webSocketHub;
    this.examDefinitionRepository = examDefinitionRepository;
    this.examPaperQuestionRepository = examPaperQuestionRepository;
    this.practicePaperRepository = practicePaperRepository;
    this.practicePaperQuestionRepository = practicePaperQuestionRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.judgeQuestionMediaRepository = judgeQuestionMediaRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
    this.examAnswerRepository = examAnswerRepository;
    this.examAttemptRepository = examAttemptRepository;
    this.scoreAppealRepository = scoreAppealRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.examPaperRepository = examPaperRepository;
    this.paperQuestionItemRepository = paperQuestionItemRepository;
    this.paperPublishRepository = paperPublishRepository;
    this.scRecordRepository = scRecordRepository;
    this.aiGradingService = aiGradingService;
    this.aiPracticeService = aiPracticeService;
    this.aiWeaknessService = aiWeaknessService;
    this.irtCatService = irtCatService;
  }

  public StartExamResponse startSession(String userId, String sourceId) {
    return startPracticeSession(userId, sourceId);
  }

  public StartExamResponse startPracticeSession(String userId, String courseNo) {
    return startPracticeSession(userId, courseNo, "中", 10);
  }

  public StartExamResponse startPracticeSession(String userId, String courseNo, String difficultyLevel, Integer questionCount) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    if (normalizedCourseNo.isBlank()) {
      throw new IllegalArgumentException("courseNo is required for practice sessions");
    }

    List<QuestionItem> allCourseQuestions = loadAllCourseQuestionBank(normalizedCourseNo);
    if (allCourseQuestions.isEmpty()) {
      throw new NoSuchElementException("No practice questions configured for course: " + normalizedCourseNo);
    }

    PracticeDifficulty practiceDifficulty = PracticeDifficulty.fromLabel(difficultyLevel);
    int requestedCount = normalizePracticeCount(questionCount, allCourseQuestions.size());
    List<QuestionItem> selectedQuestions = selectPracticeQuestions(
      normalizedCourseNo,
      practiceDifficulty,
      requestedCount,
      allCourseQuestions
    );

    int maxQuestions = selectedQuestions.size();
    ExamSession session = createSession(
      userId,
      normalizedCourseNo,
      normalizedCourseNo,
      maxQuestions,
      SessionMode.PRACTICE,
      null,
      null
    );

    examQuestionCache.put(session.getSessionId(), selectedQuestions);

    return toStartResponse(session);
  }

  public StartExamResponse startCatSession(String userId, String courseNo) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    if (normalizedCourseNo.isBlank()) {
      throw new IllegalArgumentException("courseNo is required for CAT sessions");
    }
    if (!isCourseSource(normalizedCourseNo)) {
      throw new IllegalArgumentException("No questions configured for course: " + normalizedCourseNo);
    }

    List<QuestionItem> catBank = loadCatQuestionBank(normalizedCourseNo);
    if (catBank.isEmpty()) {
      throw new IllegalArgumentException("No choice questions configured for course: " + normalizedCourseNo);
    }

    int maxQuestions = Math.min(CAT_MAX_QUESTIONS, catBank.size());
    ExamSession session = createSession(
      userId,
      normalizedCourseNo,
      normalizedCourseNo,
      maxQuestions,
      SessionMode.CAT,
      null,
      null
    );

    String cacheKey = SessionMode.CAT.name() + ":SESSION:" + session.getSessionId();
    examQuestionCache.put(cacheKey, catBank);
    recentCatQuestionIds.put(
      session.getSessionId(),
      loadLatestCompletedCatQuestionIds(userId, normalizedCourseNo)
    );
    return toStartResponse(session);
  }

  public StartExamResponse startExamSession(String userId, String courseNo, String definitionId) {
    String normalizedCourseNo = normalizeSourceId(courseNo);
    String normalizedDefinitionId = normalizeSourceId(definitionId);

    if (!normalizedDefinitionId.isBlank()) {
      ExamDefinition definition = resolveExamDefinition(normalizedDefinitionId);
      List<QuestionItem> questionBank = loadPaperQuestions(definition.getPaper().getId());
      int maxQuestions = questionBank.size();
      int durationMinutes = definition.getDurationMinutes() > 0 ? definition.getDurationMinutes() : DEFAULT_EXAM_DURATION_MINUTES;

      ExamSession session = createSession(userId, normalizedCourseNo, definition.getId(), maxQuestions,
        SessionMode.EXAM, definition.getPaper().getId(), durationMinutes * 60);
      cacheQuestionsForSession(session, questionBank);
      return toStartResponse(session);
    }

    if (normalizedCourseNo.isBlank()) {
      throw new IllegalArgumentException("courseNo is required for exam sessions");
    }
    if (!isCourseSource(normalizedCourseNo)) {
      throw new IllegalArgumentException("No questions configured for course: " + normalizedCourseNo);
    }
    ExamDefinition definition = resolveExamDefinition(null);
    List<QuestionItem> questionBank = loadCourseQuestionBank(normalizedCourseNo);
    int maxQuestions = questionBank.size();

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
      null,
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
    state.put("standardError", session.getStandardError());
    state.put("answeredCount", session.getAnsweredCount());
    state.put("maxQuestions", session.getMaxQuestions());
    state.put("finished", session.isFinished());
    state.put("timeLimitSeconds", timeLimitSeconds == null ? -1 : timeLimitSeconds);
    state.put("remainingSeconds", remainingSeconds);
    state.put("answeredQuestionIds", session.getAnsweredQuestionIds().stream().toList());
    state.put("growthPoints", buildGrowthPoints(session));
    return state;
  }

  private List<Map<String, Object>> buildGrowthPoints(ExamSession session) {
    List<ExamSession.IrtAnswerRecord> history = session.getIrtHistory();
    if (history == null || history.isEmpty()) {
      return List.of();
    }

    List<Map<String, Object>> points = new ArrayList<>(history.size());
    List<ExamSession.IrtAnswerRecord> prefix = new ArrayList<>(history.size());
    for (int i = 0; i < history.size(); i++) {
      prefix.add(history.get(i));
      IrtCatService.IrtEstimate estimate = irtCatService.estimateThetaEap(prefix);
      points.add(Map.of(
        "questionNo", i + 1,
        "score", thetaToPercent(estimate.theta()),
        "questionId", history.get(i).questionId(),
        "difficulty", history.get(i).questionDifficulty(),
        "correct", history.get(i).correct()
      ));
    }
    return points;
  }

  private int thetaToPercent(double theta) {
    double normalized = ((theta + 3.0) / 6.0) * 100.0;
    int score = (int) Math.round(normalized);
    return Math.max(0, Math.min(100, score));
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
    // mark session finished
    session.submit(Instant.now());

    // Auto-grade objective questions (choice/judge/blank) on final submission
    try {
      List<ExamAnswer> answers = examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc(sessionId);
      boolean changed = false;
      for (ExamAnswer a : answers) {
        if (a.getQuestionType() == null) continue;
        switch (a.getQuestionType()) {
          case CHOICE, JUDGE, BLANK -> {
            Integer max = resolveQuestionMaxScore(a.getQuestionType(), a.getQuestionId());
            int score = 0;
            if (a.getCorrect() != null && a.getCorrect()) {
              score = max == null ? 1 : Math.max(0, max);
            } else {
              score = 0;
            }
            a.setScore(score);
            a.setReviewed(true);
            a.setReviewedAt(Instant.now());
            changed = true;
          }
          default -> {
            // skip essay; will be reviewed separately
          }
        }
      }
      if (changed) {
        examAnswerRepository.saveAll(answers);
      }
    } catch (DataAccessException ex) {
      log.warn("Failed to auto-grade objective answers for sessionId={}: {}", sessionId, ex.getMessage());
    }

    // persist attempt status as submitted
    updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, Instant.now());

    return Map.of(
      "sessionId", session.getSessionId(),
      "finished", session.isFinished(),
      "submittedAt", session.getSubmittedAt() == null ? "" : session.getSubmittedAt().toString(),
      "totalScore", resolveAttemptTotalScore(session.getSessionId())
    );
  }

  public List<ExamAttemptView> getExamAttempts(String mode, String courseNo, String userId) {
    try {
      List<ExamAttempt> attempts;
      String normalizedMode = normalizeSourceId(mode).toUpperCase();
      SessionMode sessionMode;
      try {
        sessionMode = normalizedMode.isBlank() ? SessionMode.EXAM : SessionMode.valueOf(normalizedMode);
      } catch (IllegalArgumentException ex) {
        sessionMode = SessionMode.EXAM;
      }

      String normalizedCourseNo = normalizeSourceId(courseNo);
      if (!normalizedCourseNo.isBlank()) {
        attempts = examAttemptRepository
          .findAllByCourseNoAndUserIdAndModeOrderByStartedAtDesc(normalizedCourseNo, userId, sessionMode);
      } else {
        attempts = examAttemptRepository.findAllByUserIdAndModeOrderByStartedAtDesc(userId, sessionMode);
      }

      return attempts.stream()
        .map(this::toAttemptView)
        .toList();
    } catch (DataAccessException ex) {
      log.warn("Failed to load exam attempts for userId={}, courseNo={}, mode={}: {}", userId, courseNo, mode, ex.getMessage());
      return List.of();
    }
  }

  public List<ScoreAppealView> getScoreAppeals() {
    try {
      return scoreAppealRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toScoreAppealView)
        .toList();
    } catch (DataAccessException ex) {
      log.warn("Failed to load score appeals: {}", ex.getMessage());
      return List.of();
    }
  }

  public ScoreAppealView createScoreAppeal(ScoreAppealRequest request) {
    String userId = normalizeSourceId(request.userId());
    String courseNo = normalizeSourceId(request.courseNo());
    String reason = request.reason() == null ? "" : request.reason().trim();
    if (userId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }
    if (courseNo.isBlank()) {
      throw new IllegalArgumentException("courseNo is required");
    }

    ExamAttempt attempt = resolveLatestSubmittedExamAttempt(userId, courseNo);
    if (attempt == null) {
      throw new NoSuchElementException("No submitted exam found for this course");
    }

    scoreAppealRepository.findFirstBySessionIdAndStatus(attempt.getSessionId(), ScoreAppealStatus.PENDING)
      .ifPresent(existing -> {
        throw new IllegalStateException("该考试已存在待处理的复核申请");
      });

    ScoreAppeal appeal = new ScoreAppeal();
    appeal.setSessionId(attempt.getSessionId());
    appeal.setUserId(userId);
    appeal.setCourseNo(courseNo);
    appeal.setReason(reason.isBlank() ? null : reason);
    appeal.setStatus(ScoreAppealStatus.PENDING);
    appeal.setCreatedAt(Instant.now());
    scoreAppealRepository.save(appeal);
    return toScoreAppealView(appeal);
  }

  public ScoreAppealView reviewScoreAppeal(Long appealId, boolean approved, String reviewerId, String handledNote) {
    ScoreAppeal appeal = scoreAppealRepository.findById(appealId)
      .orElseThrow(() -> new NoSuchElementException("Appeal not found: " + appealId));
    if (appeal.getStatus() != ScoreAppealStatus.PENDING) {
      throw new IllegalStateException("该复核申请已处理");
    }

    String normalizedReviewerId = normalizeSourceId(reviewerId);
    String note = handledNote == null ? "" : handledNote.trim();

    if (approved) {
      zeroEssayScores(appeal.getSessionId());
      appeal.setStatus(ScoreAppealStatus.APPROVED);
    } else {
      appeal.setStatus(ScoreAppealStatus.REJECTED);
    }

    appeal.setHandledAt(Instant.now());
    appeal.setHandledBy(normalizedReviewerId.isBlank() ? null : normalizedReviewerId);
    appeal.setHandledNote(note.isBlank() ? null : note);
    ScoreAppeal saved = scoreAppealRepository.save(appeal);
    return toScoreAppealView(saved);
  }

  private ExamAttempt resolveLatestSubmittedExamAttempt(String userId, String courseNo) {
    List<ExamAttempt> attempts = examAttemptRepository
      .findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(userId, courseNo, SessionMode.EXAM);

    for (ExamAttempt attempt : attempts) {
      if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
        continue;
      }
      return attempt;
    }

    return null;
  }

  private void zeroEssayScores(String sessionId) {
    try {
      List<ExamAnswer> essayAnswers = examAnswerRepository.findAllBySessionIdAndQuestionType(sessionId, QuestionType.ESSAY);
      for (ExamAnswer answer : essayAnswers) {
        answer.setScore(0);
        answer.setReviewed(true);
        answer.setReviewedAt(Instant.now());
      }
      examAnswerRepository.saveAll(essayAnswers);
    } catch (DataAccessException ex) {
      log.warn("Failed to zero essay scores for sessionId={}: {}", sessionId, ex.getMessage());
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
    Integer cappedScore = capReviewScore(score, answer);
    answer.setScore(cappedScore);
    answer.setReviewNote(null);
    answer.setReviewerId(reviewerId == null || reviewerId.isBlank() ? null : reviewerId.trim());
    answer.setReviewed(true);
    answer.setReviewedAt(Instant.now());

    ExamAnswer saved = examAnswerRepository.save(answer);
    return toAnswerDetailView(saved);
  }

  public ExamAnswerDetailView aiReviewAnswer(Long answerId, String reviewerId) {
    ExamAnswer answer = examAnswerRepository.findById(answerId)
      .orElseThrow(() -> new NoSuchElementException("Answer not found: " + answerId));
    if (answer.getQuestionType() != QuestionType.ESSAY) {
      throw new IllegalArgumentException("仅支持大题 AI 批改");
    }

    String reviewer = normalizeSourceId(reviewerId);
    if (reviewer.isBlank()) {
      throw new IllegalArgumentException("reviewerId is required");
    }
    if (!isVipTeacher(reviewer)) {
      throw new IllegalStateException("仅 VIP 教师可使用 AI 批改");
    }

    EssayQuestionBank question = essayQuestionBankRepository.findById(answer.getQuestionId())
      .orElseThrow(() -> new NoSuchElementException("Essay question not found: " + answer.getQuestionId()));

    AiGradeResult aiResult = aiGradingService.gradeEssay(
      question.getStem(),
      question.getPoints(),
      question.getStandardAnswer(),
      question.getScoringRubric(),
      answer.getAnswerText(),
      answer.getAnswerImagePath(),
      question.getImagePath()
    );

    int cappedScore = capReviewScore(aiResult.score(), answer);
    answer.setScore(cappedScore);
    answer.setReviewNote(null);
    answer.setAiReviewLog(aiResult.aiLog());
    answer.setReviewerId(reviewer);
    answer.setReviewed(aiResult.confidence() >= aiGradingService.getConfidenceThreshold());
    answer.setReviewedAt(Instant.now());

    ExamAnswer saved = examAnswerRepository.save(answer);
    return toAnswerDetailView(saved);
  }

  public ExamAnswerDetailView studentAiReviewAnswer(Long answerId, String userId) {
    ExamAnswer answer = examAnswerRepository.findById(answerId)
      .orElseThrow(() -> new NoSuchElementException("Answer not found: " + answerId));
    if (answer.getQuestionType() != QuestionType.ESSAY) {
      throw new IllegalArgumentException("仅支持大题 AI 批改");
    }

    String normalizedUserId = normalizeSourceId(userId);
    if (normalizedUserId.isBlank()) {
      throw new IllegalArgumentException("userId is required");
    }
    if (!Objects.equals(normalizedUserId, normalizeSourceId(answer.getUserId()))) {
      throw new IllegalStateException("无权评估该答案");
    }

    ExamSession session = requireSession(answer.getSessionId());
    if (session.getMode() != SessionMode.PRACTICE) {
      throw new IllegalStateException("仅练习模式支持学生 AI 评估");
    }
    if (!session.isFinished()) {
      throw new IllegalStateException("请先交卷后再进行 AI 评估");
    }

    EssayQuestionBank question = essayQuestionBankRepository.findById(answer.getQuestionId())
      .orElseThrow(() -> new NoSuchElementException("Essay question not found: " + answer.getQuestionId()));

    AiGradeResult aiResult = aiGradingService.gradeEssay(
      question.getStem(),
      question.getPoints(),
      question.getStandardAnswer(),
      question.getScoringRubric(),
      answer.getAnswerText(),
      answer.getAnswerImagePath(),
      question.getImagePath()
    );

    int cappedScore = capReviewScore(aiResult.score(), answer);
    answer.setScore(cappedScore);
    answer.setReviewNote(null);
    answer.setAiReviewLog(aiResult.aiLog());
    answer.setReviewerId(normalizedUserId);
    answer.setReviewed(true);
    answer.setReviewedAt(Instant.now());

    ExamAnswer saved = examAnswerRepository.save(answer);
    return toAnswerDetailView(saved);
  }

  public Map<String, Object> generateCatReport(
    String sessionId,
    String courseNo,
    String courseName,
    Integer estimatedScore,
    Integer systemPrecision,
    Integer answeredCount,
    Integer maxQuestions
  ) {
    String normalizedSessionId = sessionId == null ? "" : sessionId.trim();
    if (normalizedSessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId is required");
    }

    int safeEstimated = clampPercent(estimatedScore);
    int safePrecision = clampPercent(systemPrecision);
    List<ExamAnswerDetailView> answers = getAttemptAnswers(normalizedSessionId);
    int safeAnswered = answers.size();
    int safeMax = Math.max(1, maxQuestions == null ? 20 : maxQuestions);
    String safeCourseNo = normalizeSourceId(courseNo);
    String safeCourseName = courseName == null ? "" : courseName.trim();

    String summary = aiPracticeService.generateCatSummary(
      safeCourseNo,
      safeCourseName,
      safeEstimated,
      safePrecision,
      safeAnswered,
      safeMax,
      answers
    );

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sessionId", normalizedSessionId);
    payload.put("reportText", summary);
    payload.put("estimatedScore", safeEstimated);
    payload.put("systemPrecision", safePrecision);
    payload.put("answeredCount", safeAnswered);
    payload.put("maxQuestions", safeMax);
    return payload;
  }

  public Map<String, Object> generateCatKnowledgeInsights(
    String sessionId,
    String courseNo,
    String courseName
  ) {
    String normalizedSessionId = sessionId == null ? "" : sessionId.trim();
    if (normalizedSessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId is required");
    }

    List<ExamAnswerDetailView> answers = getAttemptAnswers(normalizedSessionId);
    CatKnowledgeInsightsResponse insights = aiWeaknessService.generateCatKnowledgeInsights(
      normalizeSourceId(courseNo),
      courseName == null ? "" : courseName.trim(),
      answers
    );

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sessionId", normalizedSessionId);
    payload.put("masteryPoints", insights.masteryPoints() == null ? List.of() : insights.masteryPoints());
    payload.put("weakPoints", insights.weakPoints() == null ? List.of() : insights.weakPoints());
    return payload;
  }

  private Integer capReviewScore(Integer score, ExamAnswer answer) {
    if (score == null) {
      return null;
    }
    int normalizedScore = Math.max(0, score);
    Integer maxScore = resolveQuestionMaxScore(answer.getQuestionType(), answer.getQuestionId());
    if (maxScore == null || maxScore <= 0) {
      return normalizedScore;
    }
    return Math.min(normalizedScore, maxScore);
  }

  private boolean isVipTeacher(String userId) {
    return teacherProfileRepository.findFirstById(userId)
      .map(TeacherProfile::isVip)
      .orElse(false);
  }

  public List<QuestionSearchView> searchQuestions(String cno, String questionType, String difficulty, String keyword) {
    List<QuestionSearchView> results = new ArrayList<>();
    String normalizedCno = cno == null ? "" : cno.trim();
    String normalizedType = questionType == null ? "" : questionType.trim().toUpperCase();
    String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
    String normalizedDifficulty = difficulty == null ? "" : difficulty.trim();

    if (normalizedType.isEmpty() || normalizedType.equals("CHOICE")) {
      List<QuestionBank> questions = normalizedCno.isEmpty()
        ? questionBankRepository.findAllByActiveTrue()
        : questionBankRepository.findAllByActiveTrueAndCno(normalizedCno);
      for (QuestionBank q : questions) {
        if (matchesSearch(q.getStem(), normalizedKeyword) && matchesDifficulty(String.valueOf(q.getDifficulty()), normalizedDifficulty)) {
          results.add(new QuestionSearchView(q.getId(), truncateStem(q.getStem()), "CHOICE", formatDifficulty(q.getDifficulty()), q.getPoints(), q.getCno() == null ? "" : q.getCno()));
        }
      }
    }

    if (normalizedType.isEmpty() || normalizedType.equals("JUDGE")) {
      List<JudgeQuestionBank> questions = normalizedCno.isEmpty()
        ? judgeQuestionBankRepository.findAll()
        : judgeQuestionBankRepository.findAllByActiveTrueAndCno(normalizedCno);
      for (JudgeQuestionBank q : questions) {
        if (!q.isActive()) continue;
        if (matchesSearch(q.getStem(), normalizedKeyword) && matchesDifficulty(String.valueOf(q.getDifficulty()), normalizedDifficulty)) {
          results.add(new QuestionSearchView(q.getId(), truncateStem(q.getStem()), "JUDGE", formatDifficulty(q.getDifficulty()), q.getPoints(), q.getCno() == null ? "" : q.getCno()));
        }
      }
    }

    if (normalizedType.isEmpty() || normalizedType.equals("BLANK")) {
      List<BlankQuestionBank> questions = normalizedCno.isEmpty()
        ? blankQuestionBankRepository.findAll()
        : blankQuestionBankRepository.findAllByActiveTrueAndCno(normalizedCno);
      for (BlankQuestionBank q : questions) {
        if (!q.isActive()) continue;
        if (matchesSearch(q.getStem(), normalizedKeyword) && matchesDifficulty(String.valueOf(q.getDifficulty()), normalizedDifficulty)) {
          results.add(new QuestionSearchView(q.getId(), truncateStem(q.getStem()), "BLANK", formatDifficulty(q.getDifficulty()), q.getPoints(), q.getCno() == null ? "" : q.getCno()));
        }
      }
    }

    if (normalizedType.isEmpty() || normalizedType.equals("ESSAY")) {
      List<EssayQuestionBank> questions = normalizedCno.isEmpty()
        ? essayQuestionBankRepository.findAll()
        : essayQuestionBankRepository.findAllByActiveTrueAndCno(normalizedCno);
      for (EssayQuestionBank q : questions) {
        if (!q.isActive()) continue;
        if (matchesSearch(q.getStem(), normalizedKeyword) && matchesDifficulty(String.valueOf(q.getDifficulty()), normalizedDifficulty)) {
          results.add(new QuestionSearchView(q.getId(), truncateStem(q.getStem()), "ESSAY", formatDifficulty(q.getDifficulty()), q.getPoints(), q.getCno() == null ? "" : q.getCno()));
        }
      }
    }

    return results;
  }

  private boolean matchesSearch(String stem, String keyword) {
    if (keyword.isEmpty()) return true;
    return stem != null && stem.toLowerCase().contains(keyword);
  }

  private boolean matchesDifficulty(String diffStr, String filter) {
    if (filter.isEmpty()) return true;
    try {
      double d = Double.parseDouble(diffStr);
      return switch (filter) {
        case "easy" -> d <= 2.0;
        case "medium" -> d > 2.0 && d <= 3.5;
        case "hard" -> d > 3.5;
        default -> true;
      };
    } catch (NumberFormatException e) {
      return true;
    }
  }

  private String formatDifficulty(double d) {
    if (d <= 2.0) return "易";
    if (d <= 3.5) return "中";
    return "难";
  }

  private String truncateStem(String stem) {
    if (stem == null) return "";
    return stem.length() > 80 ? stem.substring(0, 80) + "..." : stem;
  }

  @Transactional
  public Map<String, Object> createExamPaper(String teacherEid, String paperName, String description,
                                              Integer durationMinutes, String questionIdsJson,
                                              String classListJson) {
    String paperId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String definitionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

    ExamPaper paper = new ExamPaper();
    paper.setId(paperId);
    paper.setName(paperName.trim());
    paper.setActive(true);
    examPaperRepository.save(paper);

    int duration = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 60;

    ExamDefinition definition = new ExamDefinition();
    definition.setId(definitionId);
    definition.setName(paperName.trim());
    definition.setDescription(description != null ? description.trim() : "");
    definition.setDurationMinutes(duration);
    definition.setActive(true);
    definition.setDefault(false);
    definition.setPaper(paper);
    examDefinitionRepository.save(definition);

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode items = mapper.readTree(questionIdsJson);
      int maxQuestions = 0;
      for (int i = 0; i < items.size(); i++) {
        com.fasterxml.jackson.databind.JsonNode item = items.get(i);
        PaperQuestionItem pqi = new PaperQuestionItem();
        pqi.setPaperId(paperId);
        pqi.setQuestionId(item.path("questionId").asText());
        pqi.setQuestionType(item.path("questionType").asText().toUpperCase());
        pqi.setDisplayOrder(i + 1);
        paperQuestionItemRepository.save(pqi);
        maxQuestions++;
      }
      definition.setMaxQuestions(maxQuestions);
      examDefinitionRepository.save(definition);
    } catch (Exception e) {
      throw new IllegalArgumentException("题目列表解析失败: " + e.getMessage());
    }

    int publishCount = 0;
    String normalizedClassList = classListJson != null ? classListJson.trim() : "";
    if (!normalizedClassList.isEmpty()) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode classItems = mapper.readTree(normalizedClassList);
        for (int i = 0; i < classItems.size(); i++) {
          JsonNode cls = classItems.get(i);
          PaperPublish pub = new PaperPublish();
          pub.setDefinitionId(definitionId);
          pub.setCno(cls.path("cno").asText().trim());
          pub.setEid(cls.path("eid").asText().trim());
          paperPublishRepository.save(pub);
          publishCount++;
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("教学班列表解析失败: " + e.getMessage());
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("paperId", paperId);
    result.put("definitionId", definitionId);
    result.put("name", paperName.trim());
    result.put("publishCount", publishCount);
    result.put("message", "试卷创建成功");
    return result;
  }

  public NextQuestionResponse getNextQuestion(String sessionId) {
    ExamSession session = requireSession(sessionId);

    refreshSessionStatus(session);

    if (session.getMode() == SessionMode.CAT && shouldFinishCat(session)) {
      session.finish();
      updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, session.getSubmittedAt());
      return buildFinishedResponse(session);
    }

    if (session.isFinished()) {
      return buildFinishedResponse(session);
    }

    QuestionItem next = pickNextQuestion(session);
    if (next == null) {
      if (session.getMode() == SessionMode.CAT) {
        session.finish();
        updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, session.getSubmittedAt());
        return buildFinishedResponse(session);
      }
      return new NextQuestionResponse(
        session.getSessionId(),
        session.getAnsweredCount(),
        session.getMaxQuestions(),
        session.getTheta(),
        session.getStandardError(),
        false,
        null
      );
    }

    return new NextQuestionResponse(
      session.getSessionId(),
      session.getAnsweredCount(),
      session.getMaxQuestions(),
      session.getTheta(),
      session.getStandardError(),
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
    String answerText = normalizeAnswerText(request.selectedOption());
    String answerImagePath = normalizeAnswerImagePath(request.answerImagePath());

    QuestionItem question = requireQuestion(session, questionId);
    if (session.getMode() == SessionMode.CAT && question.type() != QuestionType.CHOICE) {
      throw new IllegalStateException("CAT sessions only accept choice questions.");
    }
    validateAnswerPayload(question, answerText, answerImagePath);
    boolean scoreEnabled = question.scorable();
    boolean correct = scoreEnabled && matchesAnswer(question.answerKey(), answerText);

    boolean alreadyAnswered = session.getAnsweredQuestionIds().contains(question.id());
    session.markAnswered(question.id(), correct, question.difficulty(), scoreEnabled && session.getMode() != SessionMode.CAT);
    if (session.getMode() == SessionMode.CAT && scoreEnabled && !alreadyAnswered) {
      session.recordIrtAnswer(
        question.id(),
        question.difficulty(),
        question.discriminationA(),
        effectiveCatDifficultyB(question),
        correct
      );
      updateCatEstimate(session);
      if (shouldFinishCat(session)) {
        session.finish();
        updateAttemptStatus(session, ExamAttemptStatus.SUBMITTED, session.getSubmittedAt());
      }
    }
    saveAnswer(session, question, answerText, answerImagePath, scoreEnabled, correct);

    boolean reportedCorrect = scoreEnabled ? correct : true;
    webSocketHub.pushToExam(sessionId, Map.of(
      "type", "EVENT",
      "event", "ANSWER_UPDATED",
      "payload", Map.of(
        "sessionId", sessionId,
        "questionId", question.id(),
        "correct", reportedCorrect,
        "theta", session.getTheta(),
        "standardError", session.getStandardError(),
        "answeredCount", session.getAnsweredCount(),
        "finished", session.isFinished()
      )
    ));
    return new AnswerResponse(
      reportedCorrect,
      session.getTheta(),
      session.getStandardError(),
      session.getAnsweredCount(),
      session.isFinished()
    );
  }

  private QuestionItem pickNextQuestion(ExamSession session) {
    List<QuestionItem> questionBank = getQuestionBank(session);
    Set<String> answered = session.getAnsweredQuestionIds();

    if (session.getMode() == SessionMode.CAT) {
      return pickNextCatQuestion(session, questionBank);
    }

    return questionBank.stream()
      .filter(item -> !answered.contains(item.id()))
      .min(Comparator.comparingDouble(item -> Math.abs(item.difficulty() - session.getTheta())))
      .orElse(null);
  }

  private QuestionItem pickNextCatQuestion(ExamSession session, List<QuestionItem> questionBank) {
    if (questionBank == null || questionBank.isEmpty()) {
      return null;
    }

    Set<String> answered = session.getAnsweredQuestionIds();
    Set<String> used = session.getUsedQuestionIds();
    List<ExamSession.IrtAnswerRecord> history = session.getIrtHistory();
    ExamSession.IrtAnswerRecord latestAnswer = history.isEmpty()
      ? null
      : history.get(history.size() - 1);

    double theta = session.getTheta();
    List<QuestionItem> available = questionBank.stream()
      .filter(item -> item.scorable() && item.type() == QuestionType.CHOICE)
      .filter(item -> !answered.contains(item.id()) && !used.contains(item.id()))
      .toList();
    if (available.isEmpty()) {
      return null;
    }

    double targetDifficulty = 3.0;
    if (latestAnswer != null) {
      double previousDifficulty = latestAnswer.questionDifficulty();
      targetDifficulty = previousDifficulty
        + (latestAnswer.correct() ? CAT_DIFFICULTY_STEP : -CAT_DIFFICULTY_STEP);
    }

    Set<String> recentIds = recentCatQuestionIds.getOrDefault(session.getSessionId(), Set.of());
    QuestionItem selected = selectCatCandidate(available, targetDifficulty, theta, recentIds);
    session.markQuestionUsed(selected.id());
    return selected;
  }

  private QuestionItem selectCatCandidate(
    List<QuestionItem> candidates,
    double targetDifficulty,
    double theta,
    Set<String> recentIds
  ) {
    double bestDistance = Double.POSITIVE_INFINITY;
    boolean bestIsRecent = true;
    double bestInformation = -1.0;
    List<QuestionItem> bestCandidates = new ArrayList<>();

    for (QuestionItem item : candidates) {
      double distance = Math.abs(item.difficulty() - targetDifficulty);
      boolean isRecent = recentIds.contains(item.id());
      double information = irtCatService.information(
        theta,
        item.discriminationA(),
        effectiveCatDifficultyB(item)
      );

      if (distance < bestDistance - CAT_DIFFICULTY_EPSILON
        || (Math.abs(distance - bestDistance) <= CAT_DIFFICULTY_EPSILON
          && bestIsRecent && !isRecent)
        || (Math.abs(distance - bestDistance) <= CAT_DIFFICULTY_EPSILON
          && bestIsRecent == isRecent
          && information > bestInformation + CAT_DIFFICULTY_EPSILON)) {
        bestDistance = distance;
        bestIsRecent = isRecent;
        bestInformation = information;
        bestCandidates.clear();
        bestCandidates.add(item);
      } else if (Math.abs(distance - bestDistance) <= CAT_DIFFICULTY_EPSILON
        && bestIsRecent == isRecent
        && Math.abs(information - bestInformation) <= CAT_DIFFICULTY_EPSILON) {
        bestCandidates.add(item);
      }
    }

    return bestCandidates.get(ThreadLocalRandom.current().nextInt(bestCandidates.size()));
  }

  private double effectiveCatDifficultyB(QuestionItem item) {
    if (item == null) {
      return 0.0;
    }
    // Legacy questions often kept difficulty_b at its 0.00 default. The
    // display difficulty is consistently populated on the project's 1-5 scale.
    return Math.max(-2.0, Math.min(2.0, item.difficulty() - 3.0));
  }

  private void updateCatEstimate(ExamSession session) {
    IrtCatService.IrtEstimate estimate = irtCatService.estimateThetaEap(session.getIrtHistory());
    session.updateIrtEstimate(estimate.theta(), estimate.standardError());
  }

  private boolean shouldFinishCat(ExamSession session) {
    return session.getAnsweredCount() >= session.getMaxQuestions();
  }

  private void saveAnswer(
    ExamSession session,
    QuestionItem question,
    String answerText,
    String answerImagePath,
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
      answer.setAnswerImagePath(answerImagePath);
      answer.setCorrect(scoreEnabled ? correct : null);
      int maxScore = Math.max(0, question.points());
      if (scoreEnabled) {
        answer.setScore(correct ? maxScore : 0);
        answer.setReviewed(true);
        answer.setReviewNote(null);
        answer.setReviewerId(null);
        answer.setReviewedAt(Instant.now());
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
      String paperId = session.getPaperId();
      if (paperId != null && !paperId.isBlank()) {
        cacheKey = session.getMode().name() + ":PAPER:" + session.getSessionId();
      } else {
        String courseNo = normalizeSourceId(session.getCourseNo());
        if (!courseNo.isBlank() && isCourseSource(courseNo)) {
          cacheKey = session.getMode().name() + ":COURSE:" + courseNo;
        }
      }
    } else if (session.getMode() == SessionMode.PRACTICE) {
      // Practice sessions must not reuse another session's generated bank.
      // Use sessionId so difficulty/questionCount can produce independent result sets.
      cacheKey = session.getMode().name() + ":SESSION:" + session.getSessionId();
    } else if (session.getMode() == SessionMode.CAT) {
      cacheKey = session.getMode().name() + ":SESSION:" + session.getSessionId();
    }

    // Log cache decisions to help diagnose cross-course reuse
    try {
      log.info("getQuestionBank sessionId={} mode={} examId={} courseNo={} cacheKey={}",
        session.getSessionId(), session.getMode(), session.getExamId(), session.getCourseNo(), cacheKey);
    } catch (Exception ignore) {
      // best-effort logging
    }

    List<QuestionItem> cached = examQuestionCache.get(cacheKey);
    if (cached != null && !cached.isEmpty()) {
      log.info("getQuestionBank cache hit: key={} size={}", cacheKey, cached.size());
      return cached;
    }

    List<QuestionItem> loaded = loadQuestionBankForSession(session);
    examQuestionCache.put(cacheKey, loaded);
    return loaded;
  }

  private List<QuestionItem> loadQuestionBankForSession(ExamSession session) {
    if (session.getMode() == SessionMode.PRACTICE) {
      List<QuestionItem> selected = examQuestionCache.get(session.getSessionId());
      if (selected != null && !selected.isEmpty()) {
        log.info("loadQuestionBankForSession: using selected practice session bank for sessionId={} size={}", session.getSessionId(), selected.size());
        return selected;
      }
    }

    if (session.getMode() == SessionMode.CAT) {
      String courseNo = normalizeSourceId(session.getCourseNo());
      log.info("loadQuestionBankForSession: using CAT bank for course={}", courseNo);
      return loadCatQuestionBank(courseNo);
    }

    if (session.getMode() == SessionMode.EXAM) {
      String paperId = session.getPaperId();
      if (paperId != null && !paperId.isBlank()) {
        log.info("loadQuestionBankForSession: using paper questions for paperId={}", paperId);
        return loadPaperQuestions(paperId);
      }
      String courseNo = normalizeSourceId(session.getCourseNo());
      if (!courseNo.isBlank() && isCourseSource(courseNo)) {
        log.info("loadQuestionBankForSession: using course question bank for course={}", courseNo);
        return loadCourseQuestionBank(courseNo);
      }
      log.info("loadQuestionBankForSession: using exam paper for examId={}", session.getExamId());
      return loadExamQuestionBank(resolveExamDefinition(session.getExamId()));
    }

    String courseNo = normalizeSourceId(session.getCourseNo());
    if (!courseNo.isBlank() && isCourseSource(courseNo)) {
      log.info("loadQuestionBankForSession: fallback course question bank for course={}", courseNo);
      return loadCourseQuestionBank(courseNo);
    }

    return loadExamQuestionBank(resolveExamDefinition(null));
  }

  private List<QuestionItem> loadAllCourseQuestionBank(String courseNo) {
    List<QuestionItem> items = new ArrayList<>();

    items.addAll(questionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .sorted(Comparator.comparingDouble(QuestionBank::getDifficulty).thenComparing(QuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    items.addAll(judgeQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .sorted(Comparator.comparingDouble(JudgeQuestionBank::getDifficulty).thenComparing(JudgeQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    items.addAll(blankQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .sorted(Comparator.comparingDouble(BlankQuestionBank::getDifficulty).thenComparing(BlankQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    items.addAll(essayQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .sorted(Comparator.comparingDouble(EssayQuestionBank::getDifficulty).thenComparing(EssayQuestionBank::getId))
      .map(this::toQuestionItem)
      .toList());

    return items.stream()
      .sorted(Comparator.comparingInt((QuestionItem item) -> typeOrder(item.type()))
        .thenComparingDouble(QuestionItem::difficulty)
        .thenComparing(QuestionItem::id))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<QuestionItem> selectPracticeQuestions(
    String courseNo,
    PracticeDifficulty practiceDifficulty,
    int questionCount,
    List<QuestionItem> allCourseQuestions
  ) {
    List<QuestionItem> bandCandidates = filterPracticeDifficultyBand(allCourseQuestions, practiceDifficulty);
    List<QuestionItem> workingPool = bandCandidates.isEmpty() ? new ArrayList<>(allCourseQuestions) : new ArrayList<>(bandCandidates);
    int cappedCount = Math.max(1, Math.min(questionCount, allCourseQuestions.size()));

    List<AiPracticeService.PracticeQuestionCandidate> candidates = workingPool.stream()
      .limit(40)
      .map(item -> new AiPracticeService.PracticeQuestionCandidate(
        item.id(),
        item.type().name().toLowerCase(),
        item.difficulty(),
        item.points(),
        trimStem(item.stem())
      ))
      .toList();

    List<String> aiSelectedIds = aiPracticeService.selectPracticeQuestionIds(
      courseNo,
      practiceDifficulty.label,
      cappedCount,
      candidates
    );

    Map<String, QuestionItem> itemById = allCourseQuestions.stream()
      .collect(Collectors.toMap(QuestionItem::id, item -> item, (left, right) -> left, LinkedHashMap::new));

    List<QuestionItem> selected = new ArrayList<>();
    for (String id : aiSelectedIds) {
      QuestionItem item = itemById.get(id);
      if (item != null && selected.stream().noneMatch(existing -> existing.id().equals(item.id()))) {
        selected.add(item);
      }
      if (selected.size() >= cappedCount) {
        break;
      }
    }

    if (selected.size() < cappedCount) {
      List<QuestionItem> fallback = new ArrayList<>(workingPool);
      Collections.shuffle(fallback, ThreadLocalRandom.current());
      for (QuestionItem item : fallback) {
        if (selected.stream().anyMatch(existing -> existing.id().equals(item.id()))) {
          continue;
        }
        selected.add(item);
        if (selected.size() >= cappedCount) {
          break;
        }
      }
    }

    if (selected.size() < cappedCount) {
      List<QuestionItem> backup = new ArrayList<>(allCourseQuestions);
      Collections.shuffle(backup, ThreadLocalRandom.current());
      for (QuestionItem item : backup) {
        if (selected.stream().anyMatch(existing -> existing.id().equals(item.id()))) {
          continue;
        }
        selected.add(item);
        if (selected.size() >= cappedCount) {
          break;
        }
      }
    }

    return selected;
  }

  private List<QuestionItem> filterPracticeDifficultyBand(List<QuestionItem> questions, PracticeDifficulty difficulty) {
    if (questions == null || questions.isEmpty()) {
      return List.of();
    }

    List<QuestionItem> sorted = questions.stream()
      .sorted(Comparator.comparingDouble(QuestionItem::difficulty).thenComparing(QuestionItem::id))
      .toList();

    if (sorted.size() <= 3) {
      return new ArrayList<>(sorted);
    }

    int third = Math.max(1, sorted.size() / 3);
    return switch (difficulty) {
      case EASY -> new ArrayList<>(sorted.subList(0, Math.min(third, sorted.size())));
      case MEDIUM -> new ArrayList<>(sorted.subList(Math.min(third, sorted.size()), Math.min(third * 2, sorted.size())));
      case HARD -> new ArrayList<>(sorted.subList(Math.min(third * 2, sorted.size()), sorted.size()));
    };
  }

  private int normalizePracticeCount(Integer questionCount, int maxAvailable) {
    int requested = questionCount == null ? 10 : questionCount;
    if (requested < 1) {
      requested = 1;
    }
    return Math.min(requested, Math.max(1, maxAvailable));
  }

  private int clampPercent(Integer value) {
    int raw = value == null ? 0 : value;
    return Math.max(0, Math.min(100, raw));
  }

  private String trimStem(String stem) {
    String text = stem == null ? "" : stem.trim();
    if (text.length() <= 120) {
      return text;
    }
    return text.substring(0, 120);
  }

  private enum PracticeDifficulty {
    EASY("易"),
    MEDIUM("中"),
    HARD("难");

    private final String label;

    PracticeDifficulty(String label) {
      this.label = label;
    }

    private static PracticeDifficulty fromLabel(String label) {
      String normalized = label == null ? "" : label.trim().toLowerCase();
      if (normalized.isBlank()) {
        return MEDIUM;
      }
      return switch (normalized) {
        case "easy", "easy-level", "low", "易", "简单" -> EASY;
        case "hard", "high", "难", "困难" -> HARD;
        default -> MEDIUM;
      };
    }
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

    List<QuestionItem> ordered = items.stream()
      .sorted(Comparator.comparingInt((QuestionItem item) -> typeOrder(item.type()))
        .thenComparingDouble(QuestionItem::difficulty)
        .thenComparing(QuestionItem::id))
      .collect(Collectors.toCollection(ArrayList::new));

    return prioritizeImageQuestion(ordered);
  }

  private List<QuestionItem> loadCatQuestionBank(String courseNo) {
    return questionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .map(this::toQuestionItem)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private Set<String> loadLatestCompletedCatQuestionIds(String userId, String courseNo) {
    try {
      List<ExamAttempt> previousAttempts = examAttemptRepository
        .findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(userId, courseNo, SessionMode.CAT);
      Optional<ExamAttempt> latestCompleted = previousAttempts.stream()
        .filter(attempt -> attempt.getStatus() == ExamAttemptStatus.SUBMITTED
          || attempt.getStatus() == ExamAttemptStatus.EXPIRED)
        .findFirst();
      if (latestCompleted.isEmpty()) {
        return Set.of();
      }

      return examAnswerRepository
        .findAllBySessionIdOrderByAnsweredAtAsc(latestCompleted.get().getSessionId())
        .stream()
        .map(ExamAnswer::getQuestionId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    } catch (DataAccessException ex) {
      log.warn("Failed to load previous CAT questions for userId={}, courseNo={}: {}",
        userId, courseNo, ex.getMessage());
      return Set.of();
    }
  }

  private List<QuestionItem> loadPracticeQuestionBank(String paperId, String courseNo) {
    List<PracticePaperQuestion> paperQuestions = practicePaperQuestionRepository
      .findAllByPaper_IdOrderByDisplayOrderAsc(paperId);

    if (paperQuestions.isEmpty()) {
      throw new NoSuchElementException("Practice paper has no questions: " + paperId);
    }

    List<QuestionItem> items = paperQuestions.stream()
      .map(PracticePaperQuestion::getQuestion)
      .map(this::toQuestionItem)
      .collect(Collectors.toCollection(ArrayList::new));

    return ensurePracticeQuestionBankHasImage(items, courseNo);
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

  private QuestionItem toQuestionItem(QuestionBank question) {
    List<String> options = question.getOptions().stream()
      .sorted(Comparator.comparingInt(QuestionOption::getOptionOrder))
      .map(QuestionOption::getOptionText)
      .toList();

    return new QuestionItem(
      question.getId(),
      question.getStem(),
      question.getImagePath(),
      question.getImageMode(),
      options,
      question.getAnswerKey(),
      question.getDifficulty(),
      question.getDifficultyB(),
      question.getDiscriminationA(),
      QuestionType.CHOICE,
      question.getPoints(),
      true
    );
  }

  private QuestionItem toQuestionItem(JudgeQuestionBank question) {
    JudgeQuestionMedia media = findJudgeQuestionMediaSafely(question.getId());
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      media != null ? media.getImagePath() : null,
      media != null ? media.getImageMode() : null,
      JUDGE_OPTIONS,
      question.isAnswerKey() ? "true" : "false",
      question.getDifficulty(),
      question.getDifficultyB(),
      question.getDiscriminationA(),
      QuestionType.JUDGE,
      question.getPoints(),
      true
    );
  }

  private QuestionItem toQuestionItem(BlankQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      question.getImagePath(),
      question.getImageMode(),
      List.of(),
      question.getAnswerKey(),
      question.getDifficulty(),
      question.getDifficultyB(),
      question.getDiscriminationA(),
      QuestionType.BLANK,
      question.getPoints(),
      true
    );
  }

  private QuestionItem toQuestionItem(EssayQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      question.getImagePath(),
      question.getImageMode(),
      List.of(),
      "",
      question.getDifficulty(),
      question.getDifficultyB(),
      question.getDiscriminationA(),
      QuestionType.ESSAY,
      question.getPoints(),
      false
    );
  }

  private JudgeQuestionMedia findJudgeQuestionMediaSafely(String questionId) {
    if (questionId == null || questionId.isBlank()) {
      return null;
    }
    try {
      return judgeQuestionMediaRepository.findById(questionId).orElse(null);
    } catch (RuntimeException ex) {
      log.debug("judge question media unavailable for {}", questionId, ex);
      return null;
    }
  }

  private NextQuestionResponse buildFinishedResponse(ExamSession session) {
    return new NextQuestionResponse(
      session.getSessionId(),
      session.getAnsweredCount(),
      session.getMaxQuestions(),
      session.getTheta(),
      session.getStandardError(),
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

  private void cacheQuestionsForSession(ExamSession session, List<QuestionItem> questions) {
    examQuestionCache.put(session.getSessionId(), questions);
  }

  private List<QuestionItem> loadPaperQuestions(String paperId) {
    List<PaperQuestionItem> items = paperQuestionItemRepository.findAllByPaperIdOrderByDisplayOrderAsc(paperId);
    if (items.isEmpty()) {
      throw new NoSuchElementException("Paper has no questions: " + paperId);
    }
    return items.stream()
      .map(pqi -> loadQuestionByIdAndType(pqi.getQuestionId(), pqi.getQuestionType()))
      .toList();
  }

  private QuestionItem loadQuestionByIdAndType(String questionId, String questionType) {
    return switch (questionType) {
      case "CHOICE" -> questionBankRepository.findById(questionId)
        .map(this::toQuestionItem)
        .orElseThrow(() -> new NoSuchElementException("Choice question not found: " + questionId));
      case "JUDGE" -> judgeQuestionBankRepository.findById(questionId)
        .map(this::toQuestionItem)
        .orElseThrow(() -> new NoSuchElementException("Judge question not found: " + questionId));
      case "BLANK" -> blankQuestionBankRepository.findById(questionId)
        .map(this::toQuestionItem)
        .orElseThrow(() -> new NoSuchElementException("Blank question not found: " + questionId));
      case "ESSAY" -> essayQuestionBankRepository.findById(questionId)
        .map(this::toQuestionItem)
        .orElseThrow(() -> new NoSuchElementException("Essay question not found: " + questionId));
      default -> throw new IllegalArgumentException("Unknown question type: " + questionType);
    };
  }

  public List<AvailablePaperView> getAvailablePapers(String sno, String userId) {
    if (sno == null || sno.trim().isEmpty()) {
      return List.of();
    }
    String normalizedUserId = userId == null ? "" : userId.trim();
    List<ScRecordRepository.StudentClassRow> classes = scRecordRepository.findStudentClasses(sno.trim());
    List<AvailablePaperView> results = new ArrayList<>();
    for (ScRecordRepository.StudentClassRow row : classes) {
      List<PaperPublish> publishes = paperPublishRepository.findAllByCnoAndEid(row.getCno(), row.getEid());
      for (PaperPublish pub : publishes) {
        examDefinitionRepository.findByIdAndActiveTrue(pub.getDefinitionId()).ifPresent(def -> {
          String paperId = def.getPaper().getId();
          if (!normalizedUserId.isEmpty()) {
            List<ExamAttempt> completed = examAttemptRepository.findAllByUserIdAndPaperIdAndStatus(
              normalizedUserId, paperId, ExamAttemptStatus.SUBMITTED);
            if (!completed.isEmpty()) {
              return;
            }
          }
          int questionCount = paperQuestionItemRepository.findAllByPaperIdOrderByDisplayOrderAsc(paperId).size();
          results.add(new AvailablePaperView(
            def.getId(), paperId, def.getName(),
            def.getDescription() != null ? def.getDescription() : "",
            def.getDurationMinutes(), questionCount, row.getCno(), row.getEid()
          ));
        });
      }
    }
    return results;
  }

  private StartExamResponse toStartResponse(ExamSession session) {
    return new StartExamResponse(
      session.getSessionId(),
      session.getUserId(),
      session.getExamId(),
      session.getTheta(),
      session.getStandardError(),
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
      item.imagePath(),
      item.imageMode(),
      item.options(),
      item.difficulty(),
      item.type().name().toLowerCase()
    );
  }

  private List<QuestionItem> prioritizeImageQuestion(List<QuestionItem> items) {
    List<QuestionItem> shuffled = new ArrayList<>(items);
    Collections.shuffle(shuffled, ThreadLocalRandom.current());
    // Keep image questions in random positions; do not force image question to be first.
    return shuffled;
  }

  private List<QuestionItem> ensurePracticeQuestionBankHasImage(List<QuestionItem> items, String courseNo) {
    List<QuestionItem> working = new ArrayList<>(items);
    if (working.stream().anyMatch(this::hasImagePath)) {
      return prioritizeImageQuestion(working);
    }

    String normalizedCourseNo = normalizeSourceId(courseNo);
    if (normalizedCourseNo.isBlank() || working.isEmpty()) {
      return prioritizeImageQuestion(working);
    }

    Set<String> excludedIds = working.stream()
      .map(QuestionItem::id)
      .collect(Collectors.toSet());
    QuestionItem imageQuestion = pickRandomCourseImageQuestion(normalizedCourseNo, excludedIds);
    if (imageQuestion == null) {
      return prioritizeImageQuestion(working);
    }

    int replaceIndex = ThreadLocalRandom.current().nextInt(working.size());
    working.set(replaceIndex, imageQuestion);
    return prioritizeImageQuestion(working);
  }

  private QuestionItem pickRandomCourseImageQuestion(String courseNo, Set<String> excludedIds) {
    List<QuestionItem> candidates = new ArrayList<>();

    candidates.addAll(questionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .filter(question -> hasText(question.getImagePath()))
      .map(this::toQuestionItem)
      .filter(item -> !excludedIds.contains(item.id()))
      .collect(Collectors.toList()));

    candidates.addAll(blankQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .filter(question -> hasText(question.getImagePath()))
      .map(this::toQuestionItem)
      .filter(item -> !excludedIds.contains(item.id()))
      .collect(Collectors.toList()));

    candidates.addAll(essayQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .filter(question -> hasText(question.getImagePath()))
      .map(this::toQuestionItem)
      .filter(item -> !excludedIds.contains(item.id()))
      .collect(Collectors.toList()));

    if (candidates.isEmpty()) {
      return null;
    }

    return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
  }

  private boolean hasImagePath(QuestionItem item) {
    return item != null && hasText(item.imagePath());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private ExamAnswerView toAnswerView(ExamAnswer answer) {
    return new ExamAnswerView(
      answer.getQuestionId(),
      answer.getAnswerText(),
      answer.getAnswerImagePath(),
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
      attempt.getTimeLimitSeconds(),
      resolveAttemptTotalScore(attempt.getSessionId())
    );
  }

  private Integer resolveAttemptTotalScore(String sessionId) {
    try {
      Long total = examAnswerRepository.sumScoreBySessionId(sessionId);
      return total == null ? 0 : total.intValue();
    } catch (DataAccessException ex) {
      log.warn("Failed to load total score for sessionId={}: {}", sessionId, ex.getMessage());
      return 0;
    }
  }

  private ExamAnswerDetailView toAnswerDetailView(ExamAnswer answer) {
    return new ExamAnswerDetailView(
      answer.getId(),
      answer.getQuestionId(),
      resolveQuestionStem(answer),
      answer.getQuestionType().name().toLowerCase(),
      answer.getAnswerText(),
      answer.getAnswerImagePath(),
      resolveQuestionImagePath(answer),
      resolveQuestionOptions(answer),
      resolveQuestionCorrectAnswer(answer),
      answer.getCorrect(),
      answer.getScore(),
      answer.isReviewed(),
      answer.getReviewNote(),
      answer.getAiReviewLog(),
      resolveQuestionMaxScore(answer.getQuestionType(), answer.getQuestionId()),
      resolveQuestionDifficulty(answer.getQuestionType(), answer.getQuestionId()),
      answer.getAnsweredAt() == null ? null : answer.getAnsweredAt().toString()
    );
  }

  private String normalizeAnswerText(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private String normalizeAnswerImagePath(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private void validateAnswerPayload(QuestionItem question, String answerText, String answerImagePath) {
    if (question.type() == QuestionType.ESSAY) {
      if (answerText.isBlank() && answerImagePath.isBlank()) {
        throw new IllegalArgumentException("请至少填写文字答案或上传一张图片");
      }
      return;
    }

    if (answerText.isBlank()) {
      throw new IllegalArgumentException("答案不能为空");
    }
  }

  private ScoreAppealView toScoreAppealView(ScoreAppeal appeal) {
    return new ScoreAppealView(
      appeal.getId(),
      appeal.getSessionId(),
      appeal.getUserId(),
      appeal.getCourseNo(),
      appeal.getReason(),
      appeal.getStatus() == null ? null : appeal.getStatus().name().toLowerCase(),
      appeal.getCreatedAt() == null ? null : appeal.getCreatedAt().toString(),
      appeal.getHandledAt() == null ? null : appeal.getHandledAt().toString(),
      appeal.getHandledBy(),
      appeal.getHandledNote()
    );
  }

  private Integer resolveQuestionMaxScore(QuestionType type, String questionId) {
    if (type == null || questionId == null || questionId.isBlank()) {
      return null;
    }

    return switch (type) {
      case CHOICE -> questionBankRepository.findById(questionId)
        .map(QuestionBank::getPoints)
        .orElse(1);
      case JUDGE -> judgeQuestionBankRepository.findById(questionId)
        .map(JudgeQuestionBank::getPoints)
        .orElse(null);
      case BLANK -> blankQuestionBankRepository.findById(questionId)
        .map(BlankQuestionBank::getPoints)
        .orElse(null);
      case ESSAY -> essayQuestionBankRepository.findById(questionId)
        .map(EssayQuestionBank::getPoints)
        .orElse(null);
    };
  }

  private Double resolveQuestionDifficulty(QuestionType type, String questionId) {
    if (type == null || questionId == null || questionId.isBlank()) {
      return null;
    }

    return switch (type) {
      case CHOICE -> questionBankRepository.findById(questionId)
        .map(QuestionBank::getDifficulty)
        .orElse(null);
      case JUDGE -> judgeQuestionBankRepository.findById(questionId)
        .map(JudgeQuestionBank::getDifficulty)
        .orElse(null);
      case BLANK -> blankQuestionBankRepository.findById(questionId)
        .map(BlankQuestionBank::getDifficulty)
        .orElse(null);
      case ESSAY -> essayQuestionBankRepository.findById(questionId)
        .map(EssayQuestionBank::getDifficulty)
        .orElse(null);
    };
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

  private String resolveQuestionImagePath(ExamAnswer answer) {
    if (answer.getQuestionType() == null) {
      return "";
    }

    return switch (answer.getQuestionType()) {
      case CHOICE -> questionBankRepository.findById(answer.getQuestionId())
        .map(QuestionBank::getImagePath)
        .orElse("");
      case JUDGE -> judgeQuestionBankRepository.findById(answer.getQuestionId())
        .map(q -> {
          JudgeQuestionMedia media = findJudgeQuestionMediaSafely(q.getId());
          return media != null && media.getImagePath() != null ? media.getImagePath() : "";
        })
        .orElse("");
      case BLANK -> blankQuestionBankRepository.findById(answer.getQuestionId())
        .map(BlankQuestionBank::getImagePath)
        .orElse("");
      case ESSAY -> essayQuestionBankRepository.findById(answer.getQuestionId())
        .map(EssayQuestionBank::getImagePath)
        .orElse("");
    };
  }

  private String resolveQuestionCorrectAnswer(ExamAnswer answer) {
    if (answer.getQuestionType() == null) {
      return "";
    }

    return switch (answer.getQuestionType()) {
      case CHOICE -> questionBankRepository.findById(answer.getQuestionId())
        .map(QuestionBank::getAnswerKey)
        .orElse("");
      case JUDGE -> judgeQuestionBankRepository.findById(answer.getQuestionId())
        .map(q -> q.isAnswerKey() ? "正确" : "错误")
        .orElse("");
      case BLANK -> blankQuestionBankRepository.findById(answer.getQuestionId())
        .map(BlankQuestionBank::getAnswerKey)
        .orElse("");
      case ESSAY -> essayQuestionBankRepository.findById(answer.getQuestionId())
        .map(EssayQuestionBank::getStandardAnswer)
        .orElse("");
    };
  }

  private List<String> resolveQuestionOptions(ExamAnswer answer) {
    if (answer.getQuestionType() == null) {
      return List.of();
    }

    return switch (answer.getQuestionType()) {
      case CHOICE -> questionBankRepository.findById(answer.getQuestionId())
        .map(question -> question.getOptions().stream()
          .map(QuestionOption::getOptionText)
          .toList())
        .orElse(List.of());
      default -> List.of();
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
