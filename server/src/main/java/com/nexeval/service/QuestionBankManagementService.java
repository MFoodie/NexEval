package com.nexeval.service;

import com.nexeval.dto.QuestionAIDraftRequest;
import com.nexeval.dto.QuestionAIDraftResponse;
import com.nexeval.dto.QuestionCreateRequest;
import com.nexeval.model.BlankQuestionBank;
import com.nexeval.model.EssayQuestionBank;
import com.nexeval.model.JudgeQuestionBank;
import com.nexeval.model.JudgeQuestionMedia;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionOption;
import com.nexeval.model.QuestionType;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.JudgeQuestionMediaRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.repository.TeachingClassRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionBankManagementService {

  private final TeacherProfileRepository teacherProfileRepository;
  private final TeachingClassRepository teachingClassRepository;
  private final QuestionBankRepository questionBankRepository;
  private final JudgeQuestionBankRepository judgeQuestionBankRepository;
  private final BlankQuestionBankRepository blankQuestionBankRepository;
  private final EssayQuestionBankRepository essayQuestionBankRepository;
  private final JudgeQuestionMediaRepository judgeQuestionMediaRepository;
  private final AiWeaknessService aiWeaknessService;

  public QuestionBankManagementService(
    TeacherProfileRepository teacherProfileRepository,
    TeachingClassRepository teachingClassRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository,
    JudgeQuestionMediaRepository judgeQuestionMediaRepository,
    AiWeaknessService aiWeaknessService
  ) {
    this.teacherProfileRepository = teacherProfileRepository;
    this.teachingClassRepository = teachingClassRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
    this.judgeQuestionMediaRepository = judgeQuestionMediaRepository;
    this.aiWeaknessService = aiWeaknessService;
  }

  @Transactional
  public Map<String, Object> createQuestion(QuestionCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request cannot be null");
    }

    String teacherEid = required(request.teacherEid(), "teacherEid");
    String courseNo = required(request.cno(), "cno");
    validateTeacherCoursePermission(teacherEid, courseNo, false);

    QuestionType questionType = parseQuestionType(request.questionType());
    String questionId = UUID.randomUUID().toString().replace("-", "");
    String stem = required(request.stem(), "stem");
    int points = normalizePoints(request.points());
    double difficulty = normalizeDifficulty(request.difficulty());
    double difficultyB = deriveDifficultyB(difficulty);
    String imagePath = normalizeImagePath(request.imagePath());
    BigDecimal imageMode = normalizeImageMode(request.imageMode());

    switch (questionType) {
      case CHOICE -> createChoiceQuestion(
        questionId,
        courseNo,
        stem,
        points,
        difficulty,
        difficultyB,
        imagePath,
        imageMode,
        request.options(),
        request.answerKey()
      );
      case JUDGE -> createJudgeQuestion(
        questionId,
        courseNo,
        stem,
        points,
        difficulty,
        difficultyB,
        imagePath,
        imageMode,
        request.answerKey()
      );
      case BLANK -> createBlankQuestion(
        questionId,
        courseNo,
        stem,
        points,
        difficulty,
        difficultyB,
        imagePath,
        imageMode,
        request.answerKey()
      );
      case ESSAY -> createEssayQuestion(
        questionId,
        courseNo,
        stem,
        points,
        difficulty,
        difficultyB,
        imagePath,
        imageMode,
        request.standardAnswer(),
        request.scoringRubric()
      );
      default -> throw new IllegalArgumentException("unsupported question type");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("questionId", questionId);
    result.put("questionType", questionType.name());
    result.put("cno", courseNo);
    result.put("message", "Question created successfully");
    return result;
  }

  public QuestionAIDraftResponse generateAiDraft(QuestionAIDraftRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request cannot be null");
    }

    String teacherEid = required(request.teacherEid(), "teacherEid");
    String courseNo = required(request.cno(), "cno");
    validateTeacherCoursePermission(teacherEid, courseNo, true);

    return aiWeaknessService.generateTeacherQuestionDraft(
      courseNo,
      normalize(request.courseName()),
      required(request.questionType(), "questionType"),
      request.points(),
      required(request.difficulty(), "difficulty")
    );
  }

  private void createChoiceQuestion(
    String questionId,
    String courseNo,
    String stem,
    int points,
    double difficulty,
    double difficultyB,
    String imagePath,
    BigDecimal imageMode,
    List<String> rawOptions,
    String rawAnswerKey
  ) {
    List<String> options = normalizeOptions(rawOptions);
    if (options.size() < 2) {
      throw new IllegalArgumentException("choice questions require at least 2 options");
    }

    String answerKey = required(rawAnswerKey, "answerKey");
    if (!options.contains(answerKey)) {
      throw new IllegalArgumentException("choice answer must match one existing option");
    }

    QuestionBank question = new QuestionBank();
    question.setId(questionId);
    question.setCno(courseNo);
    question.setStem(stem);
    question.setImagePath(imagePath);
    question.setImageMode(imageMode);
    question.setAnswerKey(answerKey);
    question.setPoints(points);
    question.setDifficulty(difficulty);
    question.setDifficultyB(difficultyB);
    question.setDiscriminationA(1.0);
    question.setActive(true);

    List<QuestionOption> questionOptions = new ArrayList<>();
    for (int index = 0; index < options.size(); index++) {
      QuestionOption option = new QuestionOption();
      option.setQuestion(question);
      option.setOptionOrder(index + 1);
      option.setOptionText(options.get(index));
      questionOptions.add(option);
    }
    question.setOptions(questionOptions);
    questionBankRepository.save(question);
  }

  private void createJudgeQuestion(
    String questionId,
    String courseNo,
    String stem,
    int points,
    double difficulty,
    double difficultyB,
    String imagePath,
    BigDecimal imageMode,
    String rawAnswerKey
  ) {
    JudgeQuestionBank question = new JudgeQuestionBank();
    question.setId(questionId);
    question.setCno(courseNo);
    question.setStem(stem);
    question.setAnswerKey(parseJudgeAnswer(rawAnswerKey));
    question.setPoints(points);
    question.setDifficulty(difficulty);
    question.setDifficultyB(difficultyB);
    question.setDiscriminationA(1.0);
    question.setActive(true);
    judgeQuestionBankRepository.save(question);

    if (imagePath != null && !imagePath.isBlank()) {
      try {
        JudgeQuestionMedia media = new JudgeQuestionMedia();
        media.setQuestionId(questionId);
        media.setImagePath(imagePath);
        media.setImageMode(imageMode);
        judgeQuestionMediaRepository.save(media);
      } catch (DataAccessException ex) {
        throw new IllegalArgumentException("failed to save judge question image");
      }
    }
  }

  private void createBlankQuestion(
    String questionId,
    String courseNo,
    String stem,
    int points,
    double difficulty,
    double difficultyB,
    String imagePath,
    BigDecimal imageMode,
    String rawAnswerKey
  ) {
    BlankQuestionBank question = new BlankQuestionBank();
    question.setId(questionId);
    question.setCno(courseNo);
    question.setStem(stem);
    question.setImagePath(imagePath);
    question.setImageMode(imageMode);
    question.setAnswerKey(required(rawAnswerKey, "answerKey"));
    question.setPoints(points);
    question.setDifficulty(difficulty);
    question.setDifficultyB(difficultyB);
    question.setDiscriminationA(1.0);
    question.setActive(true);
    blankQuestionBankRepository.save(question);
  }

  private void createEssayQuestion(
    String questionId,
    String courseNo,
    String stem,
    int points,
    double difficulty,
    double difficultyB,
    String imagePath,
    BigDecimal imageMode,
    String rawStandardAnswer,
    String rawScoringRubric
  ) {
    String standardAnswer = normalize(rawStandardAnswer);
    String scoringRubric = normalize(rawScoringRubric);

    EssayQuestionBank question = new EssayQuestionBank();
    question.setId(questionId);
    question.setCno(courseNo);
    question.setStem(stem);
    question.setImagePath(imagePath);
    question.setImageMode(imageMode);
    question.setPoints(points);
    question.setStandardAnswer(standardAnswer.isBlank() ? null : standardAnswer);
    question.setScoringRubric(scoringRubric.isBlank() ? null : scoringRubric);
    question.setDifficulty(difficulty);
    question.setDifficultyB(difficultyB);
    question.setDiscriminationA(1.0);
    question.setActive(true);
    essayQuestionBankRepository.save(question);
  }

  private QuestionType parseQuestionType(String rawType) {
    String normalized = required(rawType, "questionType").toUpperCase();
    try {
      return QuestionType.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("invalid question type");
    }
  }

  private List<String> normalizeOptions(List<String> rawOptions) {
    if (rawOptions == null) {
      return List.of();
    }

    return rawOptions.stream()
      .map(this::normalize)
      .filter(text -> !text.isBlank())
      .distinct()
      .toList();
  }

  private boolean parseJudgeAnswer(String rawAnswerKey) {
    String normalized = required(rawAnswerKey, "answerKey").toLowerCase();
    return switch (normalized) {
      case "true", "1", "yes", "y", "正确" -> true;
      case "false", "0", "no", "n", "错误" -> false;
      default -> throw new IllegalArgumentException("judge answer must be true or false");
    };
  }

  private int normalizePoints(Integer rawPoints) {
    int points = rawPoints == null ? 5 : rawPoints;
    if (points <= 0 || points > 100) {
      throw new IllegalArgumentException("points must be between 1 and 100");
    }
    return points;
  }

  private double normalizeDifficulty(String rawDifficulty) {
    return switch (required(rawDifficulty, "difficulty").toLowerCase()) {
      case "easy" -> 1.5;
      case "medium" -> 3.0;
      case "hard" -> 4.5;
      default -> throw new IllegalArgumentException("difficulty must be easy, medium, or hard");
    };
  }

  private double deriveDifficultyB(double difficulty) {
    if (difficulty <= 2.0) {
      return -1.0;
    }
    if (difficulty <= 3.5) {
      return 0.0;
    }
    return 1.0;
  }

  private String normalizeImagePath(String rawImagePath) {
    String imagePath = normalize(rawImagePath);
    if (imagePath.isBlank()) {
      return null;
    }
    if (!imagePath.startsWith("/question-images/")) {
      throw new IllegalArgumentException("invalid question image path");
    }
    return imagePath;
  }

  private BigDecimal normalizeImageMode(BigDecimal rawImageMode) {
    if (rawImageMode == null) {
      return null;
    }
    BigDecimal normalized = rawImageMode.setScale(2, RoundingMode.HALF_UP);
    if (normalized.compareTo(new BigDecimal("0.10")) < 0 || normalized.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("image scale must be between 0.10 and 1.00");
    }
    return normalized;
  }

  private void validateTeacherCoursePermission(String teacherEid, String courseNo, boolean requireVip) {
    var teacherProfile = teacherProfileRepository.findFirstByEid(teacherEid)
      .orElseThrow(() -> new IllegalArgumentException("teacher not found"));
    if (!teacherProfile.isCanCreateExam()) {
      throw new IllegalArgumentException("current teacher has no question-creation permission");
    }
    if (requireVip && !teacherProfile.isVip()) {
      throw new IllegalArgumentException("AI question drafting requires VIP permission");
    }

    boolean teachesCourse = teachingClassRepository.findTeacherClasses(teacherEid)
      .stream()
      .anyMatch(item -> courseNo.equals(item.getCno()));
    if (!teachesCourse) {
      throw new IllegalArgumentException("question operations are limited to your own courses");
    }
  }

  private String required(String value, String fieldName) {
    String text = normalize(value);
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " cannot be blank");
    }
    return text;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
