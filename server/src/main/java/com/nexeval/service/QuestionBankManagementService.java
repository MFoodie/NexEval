package com.nexeval.service;

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

  public QuestionBankManagementService(
    TeacherProfileRepository teacherProfileRepository,
    TeachingClassRepository teachingClassRepository,
    QuestionBankRepository questionBankRepository,
    JudgeQuestionBankRepository judgeQuestionBankRepository,
    BlankQuestionBankRepository blankQuestionBankRepository,
    EssayQuestionBankRepository essayQuestionBankRepository,
    JudgeQuestionMediaRepository judgeQuestionMediaRepository
  ) {
    this.teacherProfileRepository = teacherProfileRepository;
    this.teachingClassRepository = teachingClassRepository;
    this.questionBankRepository = questionBankRepository;
    this.judgeQuestionBankRepository = judgeQuestionBankRepository;
    this.blankQuestionBankRepository = blankQuestionBankRepository;
    this.essayQuestionBankRepository = essayQuestionBankRepository;
    this.judgeQuestionMediaRepository = judgeQuestionMediaRepository;
  }

  @Transactional
  public Map<String, Object> createQuestion(QuestionCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("请求体不能为空");
    }

    String teacherEid = required(request.teacherEid(), "teacherEid");
    var teacherProfile = teacherProfileRepository.findFirstByEid(teacherEid)
      .orElseThrow(() -> new IllegalArgumentException("教师工号不存在"));
    if (!teacherProfile.isCanCreateExam()) {
      throw new IllegalArgumentException("当前教师没有出题权限");
    }

    String courseNo = required(request.cno(), "cno");
    boolean teachesCourse = teachingClassRepository.findTeacherClasses(teacherEid)
      .stream()
      .anyMatch(item -> courseNo.equals(item.getCno()));
    if (!teachesCourse) {
      throw new IllegalArgumentException("只能为本人教学班对应课程新增题目");
    }

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
      default -> throw new IllegalArgumentException("不支持的题型");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("questionId", questionId);
    result.put("questionType", questionType.name());
    result.put("cno", courseNo);
    result.put("message", "题目新增成功");
    return result;
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
      throw new IllegalArgumentException("选择题至少需要两个选项");
    }

    String answerKey = required(rawAnswerKey, "answerKey");
    if (!options.contains(answerKey)) {
      throw new IllegalArgumentException("选择题答案必须命中现有选项");
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
        throw new IllegalArgumentException("判断题图片保存失败，请先执行 judge_question_media 建表脚本");
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
      throw new IllegalArgumentException("题型不合法");
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
      default -> throw new IllegalArgumentException("判断题答案只能是正确或错误");
    };
  }

  private int normalizePoints(Integer rawPoints) {
    int points = rawPoints == null ? 5 : rawPoints;
    if (points <= 0 || points > 100) {
      throw new IllegalArgumentException("分值必须在 1 到 100 之间");
    }
    return points;
  }

  private double normalizeDifficulty(String rawDifficulty) {
    return switch (required(rawDifficulty, "difficulty").toLowerCase()) {
      case "easy" -> 1.5;
      case "medium" -> 3.0;
      case "hard" -> 4.5;
      default -> throw new IllegalArgumentException("难度必须是 easy、medium 或 hard");
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
      throw new IllegalArgumentException("题目图片路径不合法");
    }
    return imagePath;
  }

  private BigDecimal normalizeImageMode(BigDecimal rawImageMode) {
    if (rawImageMode == null) {
      return null;
    }
    BigDecimal normalized = rawImageMode.setScale(2, RoundingMode.HALF_UP);
    if (normalized.compareTo(new BigDecimal("0.10")) < 0 || normalized.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("图片缩放比例必须在 0.10 到 1.00 之间");
    }
    return normalized;
  }

  private String required(String value, String fieldName) {
    String text = normalize(value);
    if (text.isBlank()) {
      throw new IllegalArgumentException(fieldName + " 不能为空");
    }
    return text;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
