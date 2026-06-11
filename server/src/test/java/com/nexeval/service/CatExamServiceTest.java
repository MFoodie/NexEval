package com.nexeval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import com.nexeval.dto.AnswerRequest;
import com.nexeval.dto.NextQuestionResponse;
import com.nexeval.model.ExamAnswer;
import com.nexeval.model.ExamAttempt;
import com.nexeval.model.ExamAttemptStatus;
import com.nexeval.model.QuestionBank;
import com.nexeval.model.QuestionType;
import com.nexeval.model.SessionMode;
import com.nexeval.repository.BlankQuestionBankRepository;
import com.nexeval.repository.ExamAnswerRepository;
import com.nexeval.repository.ExamAttemptRepository;
import com.nexeval.repository.ExamDefinitionRepository;
import com.nexeval.repository.ExamPaperQuestionRepository;
import com.nexeval.repository.ExamPaperRepository;
import com.nexeval.repository.EssayQuestionBankRepository;
import com.nexeval.repository.JudgeQuestionBankRepository;
import com.nexeval.repository.JudgeQuestionMediaRepository;
import com.nexeval.repository.PaperPublishRepository;
import com.nexeval.repository.PaperQuestionItemRepository;
import com.nexeval.repository.PracticePaperQuestionRepository;
import com.nexeval.repository.PracticePaperRepository;
import com.nexeval.repository.QuestionBankRepository;
import com.nexeval.repository.ScRecordRepository;
import com.nexeval.repository.ScoreAppealRepository;
import com.nexeval.repository.TeacherProfileRepository;
import com.nexeval.ws.ExamWebSocketHub;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatExamServiceTest {

  private QuestionBankRepository questionBankRepository;
  private JudgeQuestionBankRepository judgeQuestionBankRepository;
  private BlankQuestionBankRepository blankQuestionBankRepository;
  private ExamAnswerRepository examAnswerRepository;
  private ExamAttemptRepository examAttemptRepository;
  private AiPracticeService aiPracticeService;
  private CatExamService service;

  @BeforeEach
  void setUp() {
    questionBankRepository = mock(QuestionBankRepository.class);
    judgeQuestionBankRepository = mock(JudgeQuestionBankRepository.class);
    blankQuestionBankRepository = mock(BlankQuestionBankRepository.class);
    examAnswerRepository = mock(ExamAnswerRepository.class);
    examAttemptRepository = mock(ExamAttemptRepository.class);
    aiPracticeService = mock(AiPracticeService.class);

    service = new CatExamService(
      mock(ExamWebSocketHub.class),
      mock(ExamDefinitionRepository.class),
      mock(ExamPaperQuestionRepository.class),
      mock(PracticePaperRepository.class),
      mock(PracticePaperQuestionRepository.class),
      questionBankRepository,
      judgeQuestionBankRepository,
      mock(JudgeQuestionMediaRepository.class),
      blankQuestionBankRepository,
      mock(EssayQuestionBankRepository.class),
      examAnswerRepository,
      examAttemptRepository,
      mock(ScoreAppealRepository.class),
      mock(TeacherProfileRepository.class),
      mock(ExamPaperRepository.class),
      mock(PaperQuestionItemRepository.class),
      mock(PaperPublishRepository.class),
      mock(ScRecordRepository.class),
      mock(AiGradingService.class),
      aiPracticeService,
      mock(AiWeaknessService.class),
      new IrtCatService()
    );
  }

  @Test
  void catSessionLoadsChoiceQuestionsOnly() {
    QuestionBank choice = choiceQuestion("choice-1", 3.0);
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01")).thenReturn(List.of(choice));

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse next = service.getNextQuestion(started.sessionId());

    assertEquals(QuestionType.CHOICE.name().toLowerCase(), next.question().type());
    verifyNoInteractions(judgeQuestionBankRepository, blankQuestionBankRepository);
  }

  @Test
  void catRaisesDifficultyAfterCorrectAnswer() {
    QuestionBank easy = choiceQuestion("easy", 2.5);
    QuestionBank medium = choiceQuestion("medium", 3.0);
    QuestionBank nearHard = choiceQuestion("near-hard", 3.5);
    QuestionBank hard = choiceQuestion("hard", 4.5);
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(easy, medium, nearHard, hard));
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse first = service.getNextQuestion(started.sessionId());
    assertEquals("medium", first.question().id());

    service.submitAnswer(started.sessionId(), new AnswerRequest("medium", "A", ""));
    NextQuestionResponse second = service.getNextQuestion(started.sessionId());

    assertEquals("near-hard", second.question().id());
  }

  @Test
  void catDoesNotRaiseDifficultyAfterWrongAnswer() {
    QuestionBank veryEasy = choiceQuestion("very-easy", 1.5);
    QuestionBank easy = choiceQuestion("easy", 2.5);
    QuestionBank medium = choiceQuestion("medium", 3.0);
    QuestionBank hard = choiceQuestion("hard", 4.5);
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(veryEasy, easy, medium, hard));
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse first = service.getNextQuestion(started.sessionId());
    assertEquals("medium", first.question().id());

    service.submitAnswer(started.sessionId(), new AnswerRequest("medium", "wrong", ""));
    NextQuestionResponse second = service.getNextQuestion(started.sessionId());

    assertEquals("easy", second.question().id());
  }

  @Test
  void catUsesSameDifficultyWhenPreferredDirectionIsUnavailable() {
    QuestionBank mediumOne = choiceQuestion("medium-1", 3.0);
    QuestionBank mediumTwo = choiceQuestion("medium-2", 3.0);
    QuestionBank hard = choiceQuestion("hard", 4.5);
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(mediumOne, mediumTwo, hard));
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse first = service.getNextQuestion(started.sessionId());
    assertEquals(3.0, first.question().difficulty());

    service.submitAnswer(started.sessionId(), new AnswerRequest(first.question().id(), "wrong", ""));
    NextQuestionResponse second = service.getNextQuestion(started.sessionId());

    assertEquals(3.0, second.question().difficulty());
  }

  @Test
  void catContinuesWhenPreferredDirectionIsUnavailable() {
    QuestionBank medium = choiceQuestion("medium", 3.0);
    QuestionBank hard = choiceQuestion("hard", 4.5);
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(medium, hard));
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse first = service.getNextQuestion(started.sessionId());
    assertEquals("medium", first.question().id());

    service.submitAnswer(started.sessionId(), new AnswerRequest("medium", "wrong", ""));
    NextQuestionResponse second = service.getNextQuestion(started.sessionId());

    assertEquals(false, second.finished());
    assertEquals("hard", second.question().id());
  }

  @Test
  void catSessionRejectsCourseWithoutChoiceQuestions() {
    when(judgeQuestionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01")).thenReturn(List.of());

    IllegalArgumentException error = assertThrows(
      IllegalArgumentException.class,
      () -> service.startCatSession("student-1", "COURSE01")
    );

    assertEquals("No choice questions configured for course: COURSE01", error.getMessage());
  }

  @Test
  void newCatSessionAvoidsLatestCompletedSessionQuestions() {
    QuestionBank previousChoice = choiceQuestion("choice-old", 3.0);
    QuestionBank freshChoice = choiceQuestion("choice-new", 3.0);
    ExamAttempt previousAttempt = new ExamAttempt();
    previousAttempt.setSessionId("previous-session");
    previousAttempt.setStatus(ExamAttemptStatus.SUBMITTED);
    ExamAnswer previousAnswer = new ExamAnswer();
    previousAnswer.setQuestionId("choice-old");

    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(previousChoice, freshChoice));
    when(examAttemptRepository.findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(
      "student-1", "COURSE01", SessionMode.CAT
    )).thenReturn(List.of(previousAttempt));
    when(examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc("previous-session"))
      .thenReturn(List.of(previousAnswer));

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse next = service.getNextQuestion(started.sessionId());

    assertEquals("choice-new", next.question().id());
  }

  @Test
  void allWrongAnswersInitiallyTrendTowardLowerDifficulty() {
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(realisticDifficultyBank());
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    double[] expected = {3.0, 2.5, 2.0, 1.5};
    for (double expectedDifficulty : expected) {
      NextQuestionResponse next = service.getNextQuestion(started.sessionId());
      assertEquals(expectedDifficulty, next.question().difficulty());
      service.submitAnswer(
        started.sessionId(),
        new AnswerRequest(next.question().id(), "wrong", "")
      );
    }
  }

  @Test
  void allCorrectAnswersInitiallyTrendTowardHigherDifficulty() {
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(realisticDifficultyBank());
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    double[] expected = {3.0, 3.5, 4.0, 4.5};
    for (double expectedDifficulty : expected) {
      NextQuestionResponse next = service.getNextQuestion(started.sessionId());
      assertEquals(expectedDifficulty, next.question().difficulty());
      service.submitAnswer(
        started.sessionId(),
        new AnswerRequest(next.question().id(), "A", "")
      );
    }
  }

  @Test
  void catUsesConfiguredQuestionCountInsteadOfStoppingOnStandardError() {
    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(realisticDifficultyBank());
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    int answered = 0;
    while (answered < 20) {
      NextQuestionResponse next = service.getNextQuestion(started.sessionId());
      assertEquals(false, next.finished());
      service.submitAnswer(
        started.sessionId(),
        new AnswerRequest(next.question().id(), answered % 2 == 0 ? "A" : "wrong", "")
      );
      answered += 1;
    }

    NextQuestionResponse finished = service.getNextQuestion(started.sessionId());
    assertEquals(true, finished.finished());
    assertEquals(20, finished.answeredCount());
  }

  @Test
  void recentQuestionFallbackDoesNotBreakWrongAnswerDirection() {
    QuestionBank recentEasy = choiceQuestion("recent-easy", 2.5);
    QuestionBank medium = choiceQuestion("medium", 3.0);
    QuestionBank freshHard = choiceQuestion("fresh-hard", 3.5);
    ExamAttempt previousAttempt = new ExamAttempt();
    previousAttempt.setSessionId("previous-session");
    previousAttempt.setStatus(ExamAttemptStatus.SUBMITTED);
    ExamAnswer previousAnswer = new ExamAnswer();
    previousAnswer.setQuestionId("recent-easy");

    when(questionBankRepository.existsByActiveTrueAndCno("COURSE01")).thenReturn(true);
    when(questionBankRepository.findAllByActiveTrueAndCno("COURSE01"))
      .thenReturn(List.of(recentEasy, medium, freshHard));
    when(examAttemptRepository.findAllByUserIdAndCourseNoAndModeOrderByStartedAtDesc(
      "student-1", "COURSE01", SessionMode.CAT
    )).thenReturn(List.of(previousAttempt));
    when(examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc("previous-session"))
      .thenReturn(List.of(previousAnswer));
    when(examAnswerRepository.findFirstBySessionIdAndQuestionId(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString()
    )).thenReturn(Optional.empty());

    var started = service.startCatSession("student-1", "COURSE01");
    NextQuestionResponse first = service.getNextQuestion(started.sessionId());
    service.submitAnswer(started.sessionId(), new AnswerRequest(first.question().id(), "wrong", ""));
    NextQuestionResponse second = service.getNextQuestion(started.sessionId());

    assertEquals("recent-easy", second.question().id());
  }

  @Test
  void catReportUsesPersistedAnswerDetailsInsteadOfClientAnswerCount() {
    QuestionBank question = choiceQuestion("choice-1", 3.0);
    ExamAnswer answer = new ExamAnswer();
    answer.setQuestionId("choice-1");
    answer.setQuestionType(QuestionType.CHOICE);
    answer.setAnswerText("A");
    answer.setCorrect(true);
    when(examAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc("session-1"))
      .thenReturn(List.of(answer));
    when(questionBankRepository.findById("choice-1")).thenReturn(Optional.of(question));
    when(aiPracticeService.generateCatSummary(
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyString(),
      org.mockito.ArgumentMatchers.anyInt(),
      org.mockito.ArgumentMatchers.anyInt(),
      org.mockito.ArgumentMatchers.anyInt(),
      org.mockito.ArgumentMatchers.anyInt(),
      org.mockito.ArgumentMatchers.anyList()
    )).thenReturn("summary");

    Map<String, Object> report = service.generateCatReport(
      "session-1", "COURSE01", "Course", 60, 80, 99, 20
    );

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.nexeval.dto.ExamAnswerDetailView>> answersCaptor =
      ArgumentCaptor.forClass(List.class);
    verify(aiPracticeService).generateCatSummary(
      eq("COURSE01"), eq("Course"), eq(60), eq(80), eq(1), eq(20), answersCaptor.capture()
    );
    assertEquals(1, answersCaptor.getValue().size());
    assertEquals("choice-1", answersCaptor.getValue().get(0).questionId());
    assertEquals(1, report.get("answeredCount"));
  }

  private QuestionBank choiceQuestion(String id, double difficulty) {
    QuestionBank question = new QuestionBank();
    question.setId(id);
    question.setStem("Choice question");
    question.setAnswerKey("A");
    question.setPoints(5);
    question.setDifficulty(difficulty);
    question.setDifficultyB(0.0);
    question.setDiscriminationA(1.0);
    question.setCno("COURSE01");
    question.setActive(true);
    question.setOptions(List.of());
    return question;
  }

  private List<QuestionBank> realisticDifficultyBank() {
    List<QuestionBank> questions = new java.util.ArrayList<>();
    double[] difficulties = {1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5};
    for (double difficulty : difficulties) {
      for (int index = 0; index < 4; index++) {
        questions.add(choiceQuestion("q-" + difficulty + "-" + index, difficulty));
      }
    }
    return questions;
  }

}
