package com.nexeval.service;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.AnswerResponse;
import com.nexeval.dto.ExamAnswerDetailView;
import com.nexeval.dto.ExamAttemptView;
import com.nexeval.dto.ExamAnswerView;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.dto.ScoreAppealRequest;
import com.nexeval.dto.ScoreAppealView;
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
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.PracticePaperQuestionRepository;
import com.nexeval.repository.PracticePaperRepository;
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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class CatExamService {

  private static final Logger log = LoggerFactory.getLogger(CatExamService.class);

  private static final int DEFAULT_EXAM_DURATION_MINUTES = 60;
  private static final int CAT_MAX_QUESTIONS = 20;
  private static final double CAT_SE_THRESHOLD = 0.35;
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
  private final ScoreAppealRepository scoreAppealRepository;
  private final TeacherProfileRepository teacherProfileRepository;
  private final AiGradingService aiGradingService;
  private final AiPracticeService aiPracticeService;
  private final IrtCatService irtCatService;

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
    ExamAttemptRepository examAttemptRepository,
    ScoreAppealRepository scoreAppealRepository,
    TeacherProfileRepository teacherProfileRepository,
    AiGradingService aiGradingService,
    AiPracticeService aiPracticeService,
    IrtCatService irtCatService
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
    this.scoreAppealRepository = scoreAppealRepository;
    this.teacherProfileRepository = teacherProfileRepository;
    this.aiGradingService = aiGradingService;
    this.aiPracticeService = aiPracticeService;
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
      throw new IllegalArgumentException("No objective questions configured for course: " + normalizedCourseNo);
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
    state.put("standardError", session.getStandardError());
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
    validateAnswerPayload(question, answerText, answerImagePath);
    boolean scoreEnabled = question.scorable();
    boolean correct = scoreEnabled && matchesAnswer(question.answerKey(), answerText);

    boolean alreadyAnswered = session.getAnsweredQuestionIds().contains(question.id());
    session.markAnswered(question.id(), correct, question.difficulty(), scoreEnabled && session.getMode() != SessionMode.CAT);
    if (session.getMode() == SessionMode.CAT && scoreEnabled && !alreadyAnswered) {
      session.recordIrtAnswer(question.id(), question.discriminationA(), question.difficultyB(), correct);
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

    QuestionItem best = null;
    double bestInfo = -1.0;
    double theta = session.getTheta();

    for (QuestionItem item : questionBank) {
      if (!item.scorable() || item.type() == QuestionType.ESSAY) {
        continue;
      }
      if (answered.contains(item.id()) || used.contains(item.id())) {
        continue;
      }
      double info = irtCatService.information(theta, item.discriminationA(), item.difficultyB());
      if (info > bestInfo) {
        bestInfo = info;
        best = item;
      }
    }

    if (best != null) {
      session.markQuestionUsed(best.id());
    }

    return best;
  }

  private void updateCatEstimate(ExamSession session) {
    IrtCatService.IrtEstimate estimate = irtCatService.estimateThetaEap(session.getIrtHistory());
    session.updateIrtEstimate(estimate.theta(), estimate.standardError());
  }

  private boolean shouldFinishCat(ExamSession session) {
    if (session.getAnsweredCount() >= session.getMaxQuestions()) {
      return true;
    }
    return session.getStandardError() > 0.0 && session.getStandardError() < CAT_SE_THRESHOLD;
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
      String courseNo = normalizeSourceId(session.getCourseNo());
      if (!courseNo.isBlank() && isCourseSource(courseNo)) {
        cacheKey = session.getMode().name() + ":COURSE:" + courseNo;
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
    List<QuestionItem> items = new ArrayList<>();

    items.addAll(questionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .map(this::toQuestionItem)
      .toList());

    items.addAll(judgeQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .map(this::toQuestionItem)
      .toList());

    items.addAll(blankQuestionBankRepository.findAllByActiveTrueAndCno(courseNo).stream()
      .map(this::toQuestionItem)
      .toList());

    return items.stream()
      .filter(item -> item.scorable() && item.type() != QuestionType.ESSAY)
      .collect(Collectors.toCollection(ArrayList::new));
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
      1,
      true
    );
  }

  private QuestionItem toQuestionItem(JudgeQuestionBank question) {
    return new QuestionItem(
      question.getId(),
      question.getStem(),
      null,
      null,
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
      answer.getCorrect(),
      answer.getScore(),
      answer.isReviewed(),
      answer.getReviewNote(),
      answer.getAiReviewLog(),
      resolveQuestionMaxScore(answer.getQuestionType(), answer.getQuestionId())
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
      case CHOICE -> 1;
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
