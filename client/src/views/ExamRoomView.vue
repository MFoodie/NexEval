<template>
  <section class="exam-shell">
    <div class="exam-container">
      <header class="exam-hero card">
      <div class="hero-main">
        <div>
          <div class="hero-eyebrow">当前课程</div>
          <h1 class="hero-title">{{ courseTitle }}</h1>
          <p class="hero-subtitle">
            课程编号：{{ courseNoText }}
            <span v-if="courseNameText">｜课程名称：{{ courseNameText }}</span>
          </p>
        </div>
      </div>

      <div class="hero-status">
        <div class="status-item">
          <span class="status-label">进度</span>
          <div class="progress-inline">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-text">{{ answeredCount }}/{{ maxQuestions }}</span>
          </div>
        </div>
        <div v-if="isExamMode" class="status-item">
          <span class="status-label">剩余时间</span>
          <span class="status-value">{{ remainingLabel }}</span>
        </div>
      </div>
    </header>

      <section class="exam-body">
      <div class="exam-question card">
        <el-skeleton :rows="6" animated v-if="loading" />

        <template v-else>
          <template v-if="finished">
            <div class="finish-summary card">
              <div class="finish-summary-main">
                <div class="finish-eyebrow">{{ finishedTitle }}</div>
                <h2 class="finish-title">总得分 {{ totalScore }} / {{ totalMaxScore || maxQuestions }}</h2>
                <p class="finish-subtitle">客观题已自动判分，大题可点击 AI 评估查看评分与评价。</p>
              </div>
              <el-button type="primary" @click="router.push('/')">返回首页</el-button>
            </div>

            <div v-if="attemptAnswers.length === 0" class="placeholder">正在加载成绩...</div>

            <div v-else class="finish-review-list">
              <div v-for="(item, index) in finishedReviewItems" :key="item.question.id" class="finish-review-item">
                <div class="finish-review-head">
                  <div class="question-index">第 {{ index + 1 }} 题</div>
                  <div class="question-meta">
                    <span class="question-type">{{ typeLabels[item.question.type] || '题目' }}</span>
                  </div>
                </div>

                <h3 class="finish-review-title">{{ item.question.stem }}</h3>

                <div v-if="item.question.imagePath" class="finish-review-image-wrap">
                  <img :src="item.question.imagePath.startsWith('/') ? item.question.imagePath : '/' + item.question.imagePath" class="finish-review-image" />
                </div>

                <div v-if="item.answer" class="finish-review-answer">
                  <div class="finish-review-row">
                    <span>作答</span>
                    <strong>{{ item.answer.answerText || (item.answer.answerImagePath ? '仅图片作答' : '未作答') }}</strong>
                  </div>
                  <div class="finish-review-row">
                    <span>得分</span>
                    <strong>{{ item.answer.score ?? '-' }} / {{ item.answer.maxScore ?? '-' }}</strong>
                  </div>
                  <div v-if="item.answer.correct != null" class="finish-review-row">
                    <span>判定</span>
                    <strong>{{ item.answer.correct ? '正确' : '错误' }}</strong>
                  </div>
                </div>

                <div v-if="item.answer?.answerImagePath" class="finish-review-answer-image-wrap">
                  <img :src="item.answer.answerImagePath.startsWith('/') ? item.answer.answerImagePath : '/' + item.answer.answerImagePath" class="finish-review-answer-image" />
                </div>

                <div v-if="!isExamMode && item.question.type === 'essay' && item.answer" class="finish-review-ai">
                  <el-button
                    type="primary"
                    plain
                    size="small"
                    :loading="aiEvaluatingAnswerId === item.answer.answerId"
                    @click="handleStudentAiReviewAnswer(item.answer)"
                  >
                    AI 评估
                  </el-button>
                  <div v-if="item.answer.aiReviewLog" class="finish-review-ai-comment">
                    {{ formatAiComment(item.answer.aiReviewLog) }}
                  </div>
                </div>
              </div>
            </div>
          </template>

            <template v-else-if="currentQuestion">
            <div v-if="!reviewMode">
              <div class="question-head">
                <div class="question-index">第 {{ currentIndex + 1 }} 题</div>
                <div class="question-meta">
                  <span class="question-type">{{ currentTypeLabel }}</span>
                </div>
              </div>
            <h2 class="question-title">{{ currentQuestion.stem }}</h2>

            <div v-if="questionImageSrc" class="question-image-wrap" :style="questionImageWrapStyle">
              <img :src="questionImageSrc" alt="题目图片" class="question-image" :style="questionImageStyle" />
            </div>

            <template v-if="isOptionQuestion">
              <el-radio-group v-model="answerValue" class="option-group">
                <el-radio
                  v-for="(option, index) in currentQuestion.options"
                  :key="option"
                  :label="option"
                  class="option-item"
                >
                  <span class="option-tag">{{ currentQuestion.type === 'judge' ? (index === 0 ? 'T' : 'F') : String.fromCharCode(65 + index) }}</span>
                  <span class="option-text">{{ formatOptionText(option) }}</span>
                </el-radio>
              </el-radio-group>
            </template>

            <template v-else-if="currentQuestion.type === 'blank'">
              <el-input v-model="answerValue" class="answer-input" placeholder="请输入答案" clearable />
            </template>

            <template v-else-if="currentQuestion.type === 'essay'">
              <el-input
                v-model="answerValue"
                class="answer-input"
                type="textarea"
                :rows="6"
                placeholder="请输入作答内容"
              />

              <div class="essay-image-actions">
                <input
                  ref="essayImageInputRef"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  class="essay-image-input"
                  @change="handleEssayImageSelected"
                />
                <el-button size="small" :loading="uploadingEssayImage" @click="triggerEssayImagePicker">
                  上传作答图片
                </el-button>
                <el-button v-if="essayImagePath" size="small" type="danger" plain @click="clearEssayImage">
                  移除图片
                </el-button>
              </div>

              <div v-if="essayImagePreviewSrc" class="essay-image-preview-wrap">
                <img :src="essayImagePreviewSrc" alt="作答图片" class="essay-image-preview" />
              </div>
            </template>

            <div class="status-legend">
              <span class="legend-chip legend-answered">已答</span>
              <span class="legend-chip legend-current">当前题目</span>
              <span class="legend-chip legend-unanswered">未答</span>
            </div>

            <div class="question-grid">
              <button
                v-for="(item, index) in questions"
                :key="item.id"
                type="button"
                class="question-node"
                :class="questionNodeClass(index, item.id)"
                :title="item.stem"
                @click="goToQuestion(index)"
              >
                {{ index + 1 }}
              </button>
            </div>

              <div class="question-actions">
                <el-button :loading="savingAnswer" @click="goToPrevQuestion">上一题</el-button>
                <el-button v-if="!isLastQuestion" :loading="savingAnswer" @click="goToNextQuestion">下一题</el-button>
                <el-button v-else type="primary" :loading="savingAnswer" @click="enterReviewMode">
                  提交
                </el-button>
              </div>
            </div>

            <div v-else class="review-list card">
              <div class="review-title">整卷复核（可编辑）</div>
              <el-collapse v-model="activeReviewPanels">
                <el-collapse-item
                  v-for="(q, idx) in questions"
                  :key="q.id"
                  :title="`第 ${idx + 1} 题 · ${q.stem || '无题干'}`"
                  :name="String(q.id)"
                >
                  <div class="review-question-stem">{{ q.stem }}</div>
                  <div v-if="q.imagePath" class="review-question-image-wrap">
                    <img :src="q.imagePath.startsWith('/') ? q.imagePath : '/' + q.imagePath" class="review-question-image" />
                  </div>

                  <div v-if="q.type === 'choice' || q.type === 'judge'" class="review-answer-section">
                    <el-radio-group v-model="answerMap[q.id]" class="option-group">
                      <el-radio v-for="(opt, oi) in q.options" :key="oi" :label="opt">{{ formatOptionText(opt) }}</el-radio>
                    </el-radio-group>
                  </div>

                  <div v-if="q.type === 'blank'" class="review-answer-section">
                    <el-input v-model="answerMap[q.id]" placeholder="请输入答案" clearable />
                  </div>

                  <div v-if="q.type === 'essay'" class="review-answer-section">
                    <el-input v-model="answerMap[q.id]" type="textarea" :rows="6" placeholder="请输入作答内容" />
                    <div class="essay-image-actions">
                      <input type="file" :ref="setEssayInputRef(q.id)" accept="image/png,image/jpeg,image/webp" class="essay-image-input" @change="event => handleReviewEssayImageSelected(event, q.id)" />
                      <el-button size="small" @click="() => triggerReviewEssayPicker(q.id)">上传作答图片</el-button>
                      <el-button v-if="answerImageMap[q.id]" size="small" type="danger" plain @click="() => removeReviewEssayImage(q.id)">移除图片</el-button>
                    </div>
                    <div v-if="answerImageMap[q.id]" class="essay-image-preview-wrap">
                      <img :src="answerImageMap[q.id].startsWith('/') ? answerImageMap[q.id] : '/' + answerImageMap[q.id]" class="essay-image-preview" />
                    </div>
                  </div>

                  <div class="review-item-actions">
                    <el-button type="primary" size="small" @click="() => saveReviewAnswer(q)">保存</el-button>
                    <el-button size="small" @click="() => goToQuestion(idx)">跳转到该题</el-button>
                  </div>
                </el-collapse-item>
              </el-collapse>

              <div class="review-actions">
                <el-button type="danger" :loading="finishing" @click="confirmAndFinishExam">交卷</el-button>
                <el-button @click="exitReviewMode">返回答题</el-button>
              </div>
            </div>
          </template>
        </template>
      </div>

      <aside class="exam-aside">
        <div class="card aside-card">
          <div class="aside-title">提示</div>
          <ul class="aside-tips">
            <li v-if="isExamMode">考试限时，支持提前交卷。</li>
            <li v-else>每题可多次提交，以最后一次为准。</li>
            <li v-if="isExamMode">系统自动判分客观题，主观题可人工批改。</li>
            <li v-else>当前为固定题库练习模式。</li>
            <li>如遇网络异常，请刷新页面重试。</li>
          </ul>
        </div>
      </aside>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { getLogin } from "../auth";
import { createExamSocket } from "../ws";

const route = useRoute();
const router = useRouter();
const loginInfo = getLogin();

const sessionId = computed(() => route.params.sessionId);
const currentUserId = computed(() => String(loginInfo?.cardNo || loginInfo?.id || "").trim());
const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());
const courseTitle = computed(() => courseNameText.value || `课程 ${courseNoText.value}`);
const loading = ref(false);
const finishing = ref(false);
const uploadingEssayImage = ref(false);
const sessionMode = ref("practice");
const timeLimitSeconds = ref(-1);
const remainingSeconds = ref(-1);
let timerId = null;

const questions = ref([]);
const attemptAnswers = ref([]);
const currentIndex = ref(0);
const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const answerValue = ref("");
const answerMap = ref({});
const answerImageMap = ref({});
const essayImagePath = ref("");
const essayImageInputRef = ref(null);
const theta = ref(0);
const answeredCount = ref(0);
const maxQuestions = ref(10);
const finished = ref(false);
const answeredSet = ref(new Set());
const reviewMode = ref(false);
const savingAnswer = ref(false);
const isLastQuestion = computed(() => questions.value.length > 0 && currentIndex.value === questions.value.length - 1);
let wsClient = null;

const activeReviewPanels = ref([]);
const essayInputRefs = new Map();
const aiEvaluatingAnswerId = ref("");

const typeLabels = {
  choice: "选择题",
  judge: "判断题",
  blank: "填空题",
  essay: "大题"
};

const currentTypeLabel = computed(() => {
  const type = currentQuestion.value?.type || "choice";
  return typeLabels[type] || "题目";
});

const questionImageSrc = computed(() => {
  const path = String(currentQuestion.value?.imagePath || "").trim();
  if (!path) {
    return "";
  }

  if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
    return path;
  }

  return `/${path}`;
});

const questionImageScale = computed(() => {
  const raw = Number(currentQuestion.value?.imageMode);

  if (!Number.isFinite(raw)) {
    return 1;
  }

  return Math.min(1, Math.max(0.01, Number(raw.toFixed(2))));
});

const questionImageWrapStyle = computed(() => {
  const widthPercent = `${(questionImageScale.value * 100).toFixed(2)}%`;
  return {
    width: widthPercent,
    margin: "0 auto",
  };
});

const questionImageStyle = computed(() => {
  return {
    width: "100%",
    maxWidth: "100%",
    height: "auto",
    display: "block",
  };
});

const essayImagePreviewSrc = computed(() => {
  const path = String(essayImagePath.value || "").trim();
  if (!path) {
    return "";
  }
  if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
    return path;
  }
  return `/${path}`;
});

const isOptionQuestion = computed(() => {
  const type = currentQuestion.value?.type || "choice";
  return type === "choice" || type === "judge";
});

const isExamMode = computed(() => sessionMode.value === "exam");

const finishedTitle = computed(() => (isExamMode.value ? "考试完成" : "练习完成"));

const totalScore = computed(() => attemptAnswers.value.reduce((sum, item) => sum + (Number(item.score ?? 0) || 0), 0));

const totalMaxScore = computed(() => attemptAnswers.value.reduce((sum, item) => sum + (Number(item.maxScore ?? 0) || 0), 0));

const finishedReviewItems = computed(() => {
  return questions.value.map((question) => ({
    question,
    answer: getAttemptAnswer(question.id)
  }));
});

const remainingLabel = computed(() => formatSeconds(remainingSeconds.value));

const progressPercent = computed(() => {
  if (!maxQuestions.value) {
    return 0;
  }

  const ratio = answeredCount.value / maxQuestions.value;
  return Math.min(100, Math.max(0, Math.round(ratio * 100)));
});

function formatSeconds(value) {
  if (value == null || value < 0) {
    return "-";
  }
  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function stopTimer() {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
}

function startTimer() {
  stopTimer();
  if (!isExamMode.value || remainingSeconds.value < 0) {
    return;
  }
  timerId = setInterval(() => {
    if (finished.value || !isExamMode.value) {
      stopTimer();
      return;
    }
    remainingSeconds.value = Math.max(0, remainingSeconds.value - 1);
    if (remainingSeconds.value <= 0) {
      finishExam("timeout");
      stopTimer();
    }
  }, 1000);
}

async function loadAttemptAnswers() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  try {
    const payload = await wsClient.request("GET_ATTEMPT_ANSWERS", {
      sessionId: sessionId.value
    });
    attemptAnswers.value = Array.isArray(payload) ? payload : [];
  } catch (error) {
    ElMessage.error(error.message || "成绩加载失败。");
  }
}

async function initializeSessionData() {
  await loadSessionState();
  await loadQuestions();
  if (finished.value) {
    await loadAttemptAnswers();
  } else {
    await loadSessionAnswers();
  }
}

async function loadQuestions() {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  loading.value = true;
  try {
    const payload = await wsClient.request("GET_EXAM_QUESTIONS", {
      sessionId: sessionId.value
    });
    questions.value = Array.isArray(payload) ? payload : [];
    maxQuestions.value = questions.value.length || maxQuestions.value;
    currentIndex.value = 0;
    syncAnswerForCurrentQuestion();
  } catch (error) {
    ElMessage.error(error.message || "题目加载失败。");
  } finally {
    loading.value = false;
  }
}

async function loadSessionState() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  try {
    const payload = await wsClient.request("GET_SESSION_STATE", {
      sessionId: sessionId.value
    });
    theta.value = payload.theta ?? theta.value;
    answeredCount.value = payload.answeredCount ?? answeredCount.value;
    maxQuestions.value = payload.maxQuestions ?? maxQuestions.value;
    finished.value = payload.finished ?? finished.value;
    sessionMode.value = payload.mode ?? sessionMode.value;
    timeLimitSeconds.value = payload.timeLimitSeconds ?? timeLimitSeconds.value;
    remainingSeconds.value = payload.remainingSeconds ?? remainingSeconds.value;
    answeredSet.value = new Set(payload.answeredQuestionIds || []);
    if (finished.value) {
      stopTimer();
    } else {
      startTimer();
    }
  } catch (error) {
    ElMessage.error(error.message || "会话状态获取失败。");
  }
}

async function loadSessionAnswers() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  try {
    const payload = await wsClient.request("GET_SESSION_ANSWERS", {
      sessionId: sessionId.value
    });
    const nextMap = {};
    const nextImageMap = {};
    const nextAnswered = new Set();

    (Array.isArray(payload) ? payload : []).forEach((item) => {
      if (!item?.questionId) {
        return;
      }
      nextMap[item.questionId] = item.answerText ?? "";
      if (item.answerImagePath) {
        nextImageMap[item.questionId] = item.answerImagePath;
      }
      nextAnswered.add(item.questionId);
    });

    answerMap.value = nextMap;
    answerImageMap.value = nextImageMap;
    answeredSet.value = nextAnswered;
    syncAnswerForCurrentQuestion();
  } catch (error) {
    ElMessage.error(error.message || "答题记录获取失败。");
  }
}

async function persistCurrentAnswer() {
  if (!currentQuestion.value) {
    return true;
  }

  const question = currentQuestion.value;
  const isEssay = question.type === "essay";
  const textValue = String(answerValue.value || "").trim();
  const imageValue = String(essayImagePath.value || "").trim();

  if (!isEssay && !textValue) {
    return true;
  }

  if (isEssay && !textValue && !imageValue) {
    return true;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return false;
  }

  savingAnswer.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId: question.id,
      selectedOption: textValue,
      answerImagePath: isEssay ? imageValue : ""
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    finished.value = payload.finished;
    answeredSet.value = new Set([...answeredSet.value, question.id]);
    saveAnswerForQuestion(question.id, textValue);
    saveAnswerImageForQuestion(question.id, isEssay ? imageValue : "");
    return true;
  } catch (error) {
    ElMessage.error(error.message || "保存失败。");
    return false;
  } finally {
    savingAnswer.value = false;
  }
}

async function enterReviewMode() {
  const saved = await persistCurrentAnswer();
  if (!saved) {
    return;
  }
  reviewMode.value = true;
}

async function confirmAndFinishExam() {
  if (finishing.value || finished.value) {
    return;
  }
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  try {
    await ElMessageBox.confirm(
      "确认交卷后将无法继续修改答案，是否继续？",
      "确认交卷",
      {
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
  } catch {
    return;
  }

  await finishExam("manual");
}

async function finishExam(reason = "manual") {
  if (finishing.value || finished.value) {
    return;
  }
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  finishing.value = true;
  try {
    const payload = await wsClient.request("FINISH_SESSION", {
      sessionId: sessionId.value,
      reason
    });
    finished.value = payload.finished ?? true;
    stopTimer();
    await loadAttemptAnswers();
    ElMessage.success("已交卷。");
  } catch (error) {
    ElMessage.error(error.message || "交卷失败。");
  } finally {
    finishing.value = false;
  }
}

async function goToPrevQuestion() {
  if (!questions.value.length) {
    return;
  }
  const saved = await persistCurrentAnswer();
  if (!saved) {
    return;
  }
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : questions.value.length - 1;
  syncAnswerForCurrentQuestion();
}

async function goToNextQuestion() {
  if (!questions.value.length) {
    return;
  }
  const saved = await persistCurrentAnswer();
  if (!saved) {
    return;
  }
  currentIndex.value = currentIndex.value < questions.value.length - 1 ? currentIndex.value + 1 : 0;
  syncAnswerForCurrentQuestion();
}

async function goToQuestion(index) {
  if (!questions.value.length) {
    return;
  }
  const saved = await persistCurrentAnswer();
  if (!saved) {
    return;
  }
  currentIndex.value = index;
  syncAnswerForCurrentQuestion();
}

async function saveReviewAnswer(question) {
  const questionId = question.id;
  const textValue = String(answerMap.value[questionId] || "").trim();
  const isEssay = question.type === "essay";
  const imageValue = String(answerImageMap.value[questionId] || "").trim();

  if (!isEssay && !textValue) {
    ElMessage.warning("答案为空，未保存。请填写后保存。");
    return;
  }

  savingAnswer.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId,
      selectedOption: textValue,
      answerImagePath: isEssay ? imageValue : ""
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    finished.value = payload.finished;
    answeredSet.value = new Set([...answeredSet.value, questionId]);
    ElMessage.success("已保存");
  } catch (err) {
    ElMessage.error(err.message || "保存失败");
  } finally {
    savingAnswer.value = false;
  }
}

function exitReviewMode() {
  reviewMode.value = false;
}

function formatOptionText(option) {
  if (currentQuestion.value?.type === "judge") {
    return option === "true" ? "正确" : "错误";
  }
  return option;
}

function saveAnswerForQuestion(questionId, value) {
  if (!questionId) {
    return;
  }
  answerMap.value = {
    ...answerMap.value,
    [questionId]: value
  };
}

function saveAnswerImageForQuestion(questionId, imagePath) {
  if (!questionId) {
    return;
  }

  const next = {
    ...answerImageMap.value,
  };

  if (imagePath) {
    next[questionId] = imagePath;
  } else {
    delete next[questionId];
  }

  answerImageMap.value = next;
}

function questionNodeClass(index, questionId) {
  if (index === currentIndex.value) {
    return "is-current";
  }
  if (answeredSet.value.has(questionId)) {
    return "is-answered";
  }
  return "is-unanswered";
}

function getAttemptAnswer(questionId) {
  return attemptAnswers.value.find((item) => item.questionId === questionId) || null;
}

function parseAiReviewLog(raw) {
  const text = String(raw || "").trim();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

function formatAiComment(raw) {
  const parsed = parseAiReviewLog(raw);
  if (!parsed) {
    return "";
  }

  if (parsed.raw) {
    return parsed.raw;
  }

  const parts = [];
  if (parsed.comment) {
    parts.push(parsed.comment);
  }
  if (parsed.confidence != null) {
    parts.push(`置信度：${Math.round(Number(parsed.confidence) * 100)}%`);
  }
  return parts.join(" | ");
}

async function handleStudentAiReviewAnswer(answer) {
  if (!answer?.answerId) {
    return;
  }

  if (!currentUserId.value) {
    ElMessage.error("未找到当前用户信息，无法进行 AI 评估");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  aiEvaluatingAnswerId.value = answer.answerId;
  try {
    const payload = await wsClient.request("STUDENT_AI_REVIEW_ANSWER", {
      answerId: String(answer.answerId),
      userId: currentUserId.value
    }, 30000);

    const index = attemptAnswers.value.findIndex((item) => item.answerId === payload.answerId);
    if (index !== -1) {
      attemptAnswers.value.splice(index, 1, payload);
    }
    ElMessage.success("AI 评估已完成");
  } catch (error) {
    ElMessage.error(error.message || "AI 评估失败");
  } finally {
    aiEvaluatingAnswerId.value = "";
  }
}

function syncAnswerForCurrentQuestion() {
  const questionId = currentQuestion.value?.id;
  if (!questionId) {
    answerValue.value = "";
    essayImagePath.value = "";
    return;
  }
  answerValue.value = answerMap.value[questionId] ?? "";
  essayImagePath.value = answerImageMap.value[questionId] ?? "";
}

function triggerEssayImagePicker() {
  essayImageInputRef.value?.click();
}

async function handleEssayImageSelected(event) {
  const file = event?.target?.files?.[0];
  event.target.value = "";

  if (!file) {
    return;
  }

  if (!sessionId.value) {
    ElMessage.error("会话不存在，无法上传图片");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  if (!currentQuestion.value || currentQuestion.value.type !== "essay") {
    ElMessage.warning("仅大题支持上传作答图片");
    return;
  }

  if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
    ElMessage.warning("仅支持 JPG/PNG/WEBP 图片");
    return;
  }

  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning("图片不能超过 8MB");
    return;
  }

  uploadingEssayImage.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sessionId", String(sessionId.value));

    const response = await fetch("/api/answer-image/upload", {
      method: "POST",
      body: formData,
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload?.message || "上传失败");
    }

    essayImagePath.value = String(payload?.imagePath || "").trim();
    saveAnswerImageForQuestion(currentQuestion.value.id, essayImagePath.value);
    ElMessage.success("图片上传成功");
  } catch (error) {
    ElMessage.error(error.message || "上传失败");
  } finally {
    uploadingEssayImage.value = false;
  }
}

function clearEssayImage() {
  essayImagePath.value = "";
  const questionId = currentQuestion.value?.id;
  if (questionId) {
    saveAnswerImageForQuestion(questionId, "");
  }
}

function setEssayInputRef(questionId) {
  return (el) => {
    if (!el) {
      essayInputRefs.delete(questionId);
      return;
    }
    essayInputRefs.set(questionId, el);
  };
}

function triggerReviewEssayPicker(questionId) {
  const el = essayInputRefs.get(questionId);
  el?.click();
}

async function handleReviewEssayImageSelected(event, questionId) {
  const file = event?.target?.files?.[0];
  event.target.value = "";
  if (!file) return;
  if (!sessionId.value) {
    ElMessage.error("会话不存在，无法上传图片");
    return;
  }
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }
  if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
    ElMessage.warning("仅支持 JPG/PNG/WEBP 图片");
    return;
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning("图片不能超过 8MB");
    return;
  }

  try {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sessionId", String(sessionId.value));

    const response = await fetch("/api/answer-image/upload", {
      method: "POST",
      body: formData,
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload?.message || "上传失败");
    answerImageMap.value = { ...answerImageMap.value, [questionId]: String(payload?.imagePath || "") };
    ElMessage.success("图片上传成功");
  } catch (error) {
    ElMessage.error(error.message || "上传失败");
  }
}

function removeReviewEssayImage(questionId) {
  const next = { ...answerImageMap.value };
  delete next[questionId];
  answerImageMap.value = next;
}

watch(currentQuestion, () => {
  syncAnswerForCurrentQuestion();
});

function connectWebSocket() {
  wsClient = createExamSocket(sessionId.value, {
    onOpen() {
      initializeSessionData();
    },
    onClose() {
    },
    onError() {
    },
    onMessage(data) {
      if (data.type === "EVENT" && data.event === "ANSWER_UPDATED") {
        theta.value = data.payload?.theta ?? theta.value;
        answeredCount.value = data.payload?.answeredCount ?? answeredCount.value;
        finished.value = data.payload?.finished ?? finished.value;
      }
    }
  });
}

onMounted(() => {
  connectWebSocket();
});

onBeforeUnmount(() => {
  stopTimer();
  wsClient?.close();
});
</script>

<style scoped>
.exam-shell {
  display: grid;
  gap: 20px;
  width: 100%;
}

.exam-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 20px;
}

.exam-hero {
  position: relative;
  overflow: hidden;
}

.exam-hero::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: var(--ne-gradient-accent);
}

.hero-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.hero-eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: var(--ne-text-muted);
}

.hero-title {
  margin: 6px 0 8px;
  font-size: 28px;
  font-family: "Space Grotesk", "Manrope", sans-serif;
  color: var(--ne-text-strong);
}

.hero-subtitle {
  margin: 0;
  color: var(--ne-text-muted);
}

.hero-status {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  align-items: center;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ne-text-muted);
  font-size: 14px;
}

.status-label {
  color: var(--ne-text-subtle);
}

.status-value {
  color: var(--ne-text-strong);
  font-weight: 600;
}


.progress-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-track {
  width: 160px;
  height: 6px;
  background: rgba(var(--ne-primary-rgb), 0.1);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--ne-gradient-primary);
  border-radius: inherit;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--ne-text-muted);
}

.exam-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  align-items: start;
}

.exam-question {
  min-height: 420px;
}

.question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.question-meta {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}

.question-type {
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid var(--ne-border);
  background: var(--ne-primary-soft);
  color: var(--ne-primary);
  font-size: 12px;
}

.question-title {
  margin: 0 0 18px;
  font-size: 20px;
  color: var(--ne-text-strong);
}

.question-image-wrap {
  margin: 0 0 18px;
  padding: 12px;
  border: 1px solid rgba(var(--ne-primary-rgb), 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  overflow: auto;
}

.question-image {
  border-radius: 10px;
}

.option-group {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
}

.answer-input {
  margin-bottom: 24px;
}

.essay-image-input {
  display: none;
}

.essay-image-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.essay-image-preview-wrap {
  margin-bottom: 20px;
  border: 1px solid rgba(var(--ne-primary-rgb), 0.12);
  border-radius: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.82);
  max-width: 72%;
}

.essay-image-preview {
  width: 100%;
  display: block;
  border-radius: 8px;
}

.status-legend {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
  color: var(--ne-text-muted);
}

.legend-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
}

.legend-chip::before {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.legend-answered::before {
  background: rgba(var(--ne-primary-rgb), 0.35);
}

.legend-current::before {
  background: var(--ne-primary);
}

.legend-unanswered::before {
  background: var(--ne-border);
}

.question-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.question-node {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--ne-border);
  background: var(--ne-question-node-unanswered-bg, var(--ne-primary-soft));
  color: var(--ne-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.question-node.is-answered {
  background: rgba(var(--ne-primary-rgb), 0.18);
  border-color: rgba(var(--ne-primary-rgb), 0.4);
  color: var(--ne-primary);
}

.question-node.is-current {
  background: var(--ne-primary);
  border-color: var(--ne-primary);
  color: #fff;
  box-shadow: var(--ne-shadow-soft);
}

.question-node:hover {
  transform: translateY(-1px);
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
}

.option-item {
  margin: 0;
  padding: 12px 14px;
  border-radius: var(--ne-radius-md);
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.option-item.is-checked {
  border-color: rgba(var(--ne-primary-rgb), 0.6);
  background: var(--ne-primary-soft);
}

.option-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(var(--ne-primary-rgb), 0.12);
  color: var(--ne-primary);
  font-weight: 600;
  font-size: 12px;
  margin-right: 10px;
}

.option-text {
  font-size: 14px;
  color: var(--ne-text);
}

.question-actions {
  display: flex;
  gap: 12px;
}

.exam-aside {
  display: grid;
  gap: 16px;
}

.aside-card {
  display: grid;
  gap: 12px;
}

.aside-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ne-text-strong);
}

.aside-row {
  display: flex;
  justify-content: space-between;
  color: var(--ne-text-muted);
  font-size: 13px;
  gap: 12px;
}

.aside-row strong {
  color: var(--ne-text-strong);
  font-weight: 600;
}

.aside-tips {
  margin: 0;
  padding-left: 16px;
  color: var(--ne-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .hero-main {
    flex-direction: column;
  }

  .exam-body {
    grid-template-columns: 1fr;
  }
}

.review-list {
  padding: 16px;
  display: block;
}
.review-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.review-list :deep(.el-collapse-item__header) {
  white-space: normal;
  line-height: 1.5;
  align-items: flex-start;
  padding: 12px 16px;
}

.review-list :deep(.el-collapse-item__title) {
  white-space: normal;
  word-break: break-word;
}

.review-question-stem {
  margin-bottom: 8px;
  color: var(--ne-text-strong);
}
.review-item-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
.review-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}
.review-question-image-wrap img {
  max-width: 100%;
  border-radius: 8px;
}

.finish-summary {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.finish-summary-main {
  display: grid;
  gap: 8px;
}

.finish-eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--ne-text-muted);
}

.finish-title {
  margin: 0;
  font-size: 26px;
  color: var(--ne-text-strong);
}

.finish-subtitle {
  margin: 0;
  color: var(--ne-text-muted);
}

.finish-review-list {
  display: grid;
  gap: 14px;
}

.finish-review-item {
  padding: 16px;
  border: 1px solid var(--ne-border);
  border-radius: 16px;
  background: var(--ne-surface);
  display: grid;
  gap: 12px;
}

.finish-review-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.finish-review-title {
  margin: 0;
  font-size: 18px;
  color: var(--ne-text-strong);
}

.finish-review-image-wrap,
.finish-review-answer-image-wrap {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.finish-review-image,
.finish-review-answer-image {
  width: 100%;
  display: block;
  border-radius: 8px;
}

.finish-review-answer {
  display: grid;
  gap: 8px;
}

.finish-review-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--ne-text-muted);
}

.finish-review-row strong {
  color: var(--ne-text-strong);
}

.finish-review-ai {
  display: grid;
  gap: 8px;
}

.finish-review-ai-comment {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(var(--ne-primary-rgb), 0.06);
  color: var(--ne-text-strong);
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
