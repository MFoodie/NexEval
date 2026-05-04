<template>
  <section class="exam-shell">
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
          <el-result v-if="finished" icon="success" :title="finishedTitle">
            <template #extra>
              <el-button type="primary" @click="router.push('/')">返回首页</el-button>
            </template>
          </el-result>

          <template v-else-if="currentQuestion">
            <div class="question-head">
              <div class="question-index">第 {{ currentIndex + 1 }} 题</div>
              <div class="question-meta">
                <span class="question-type">{{ currentTypeLabel }}</span>
              </div>
            </div>
            <h2 class="question-title">{{ currentQuestion.stem }}</h2>

            <template v-if="isOptionQuestion">
              <el-radio-group v-model="answerValue" class="option-group">
                <el-radio
                  v-for="(option, index) in currentQuestion.options"
                  :key="option"
                  :label="option"
                  class="option-item"
                >
                  <span class="option-tag">{{ String.fromCharCode(65 + index) }}</span>
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
                @click="goToQuestion(index)"
              >
                {{ index + 1 }}
              </button>
            </div>

            <div class="question-actions">
              <el-button @click="goToPrevQuestion">上一题</el-button>
              <el-button @click="goToNextQuestion">下一题</el-button>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交答案
              </el-button>
              <el-button v-if="isExamMode" type="danger" :loading="finishing" @click="handleFinishExam">
                交卷
              </el-button>
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
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";

const route = useRoute();
const router = useRouter();

const sessionId = computed(() => route.params.sessionId);
const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());
const courseTitle = computed(() => courseNameText.value || `课程 ${courseNoText.value}`);
const loading = ref(false);
const submitting = ref(false);
const finishing = ref(false);
const sessionMode = ref("practice");
const timeLimitSeconds = ref(-1);
const remainingSeconds = ref(-1);
let timerId = null;

const questions = ref([]);
const currentIndex = ref(0);
const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const answerValue = ref("");
const answerMap = ref({});
const theta = ref(0);
const answeredCount = ref(0);
const maxQuestions = ref(10);
const finished = ref(false);
const answeredSet = ref(new Set());
let wsClient = null;

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

const isOptionQuestion = computed(() => {
  const type = currentQuestion.value?.type || "choice";
  return type === "choice" || type === "judge";
});

const isExamMode = computed(() => sessionMode.value === "exam");

const finishedTitle = computed(() => (isExamMode.value ? "考试完成" : "练习完成"));

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
      handleFinishExam("timeout");
      stopTimer();
    }
  }, 1000);
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
    const nextAnswered = new Set();

    (Array.isArray(payload) ? payload : []).forEach((item) => {
      if (!item?.questionId) {
        return;
      }
      nextMap[item.questionId] = item.answerText ?? "";
      nextAnswered.add(item.questionId);
    });

    answerMap.value = nextMap;
    answeredSet.value = nextAnswered;
    syncAnswerForCurrentQuestion();
  } catch (error) {
    ElMessage.error(error.message || "答题记录获取失败。");
  }
}

async function handleSubmit() {
  if (!currentQuestion.value) {
    return;
  }

  if (!String(answerValue.value || "").trim()) {
    ElMessage.warning(isOptionQuestion.value ? "请选择一个选项。" : "请输入答案。");
    return;
  }

  submitting.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId: currentQuestion.value.id,
      selectedOption: answerValue.value
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    finished.value = payload.finished;
    answeredSet.value = new Set([...answeredSet.value, currentQuestion.value.id]);
    saveAnswerForQuestion(currentQuestion.value.id, answerValue.value);

    if (payload.correct) {
      ElMessage.success("提交成功。");
    } else {
      ElMessage.info("提交成功。");
    }
    if (!payload.finished) {
      goToNextQuestion();
    } else {
      stopTimer();
    }
  } catch (error) {
    ElMessage.error(error.message || "提交失败。");
  } finally {
    submitting.value = false;
  }
}

async function handleFinishExam(reason = "manual") {
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
    ElMessage.success("已交卷。");
  } catch (error) {
    ElMessage.error(error.message || "交卷失败。");
  } finally {
    finishing.value = false;
  }
}

function goToPrevQuestion() {
  if (!questions.value.length) {
    return;
  }
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : questions.value.length - 1;
  syncAnswerForCurrentQuestion();
}

function goToNextQuestion() {
  if (!questions.value.length) {
    return;
  }
  currentIndex.value = currentIndex.value < questions.value.length - 1 ? currentIndex.value + 1 : 0;
  syncAnswerForCurrentQuestion();
}

function goToQuestion(index) {
  if (!questions.value.length) {
    return;
  }
  currentIndex.value = index;
  syncAnswerForCurrentQuestion();
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

function questionNodeClass(index, questionId) {
  if (index === currentIndex.value) {
    return "is-current";
  }
  if (answeredSet.value.has(questionId)) {
    return "is-answered";
  }
  return "is-unanswered";
}

function syncAnswerForCurrentQuestion() {
  const questionId = currentQuestion.value?.id;
  if (!questionId) {
    answerValue.value = "";
    return;
  }
  answerValue.value = answerMap.value[questionId] ?? "";
}

watch(currentQuestion, () => {
  syncAnswerForCurrentQuestion();
});

function connectWebSocket() {
  wsClient = createExamSocket(sessionId.value, {
    onOpen() {
      loadSessionState();
      loadSessionAnswers();
      loadQuestions();
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
  background: rgba(42, 92, 255, 0.1);
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

.option-group {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
}

.answer-input {
  margin-bottom: 24px;
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
  background: rgba(42, 92, 255, 0.35);
}

.legend-current::before {
  background: var(--ne-primary);
}

.legend-unanswered::before {
  background: #d5dbe5;
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
  background: #f2f4f8;
  color: var(--ne-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.question-node.is-answered {
  background: rgba(42, 92, 255, 0.18);
  border-color: rgba(42, 92, 255, 0.4);
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
  border-color: rgba(42, 92, 255, 0.35);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.option-item.is-checked {
  border-color: rgba(42, 92, 255, 0.6);
  background: var(--ne-primary-soft);
}

.option-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(42, 92, 255, 0.12);
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
</style>
