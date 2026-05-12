<template>
  <section class="exam-shell">
    <header class="exam-hero card">
      <div class="hero-main">
        <div>
          <div class="hero-eyebrow">教师批改</div>
          <h1 class="hero-title">{{ courseTitle }}</h1>
          <p class="hero-subtitle">
            学生：{{ studentLabel }}
            <span v-if="studentSno">｜学号：{{ studentSno }}</span>
            <span v-if="courseNoText">｜课程编号：{{ courseNoText }}</span>
          </p>
        </div>
      </div>

      <div class="hero-status">
        <div class="status-item">
          <span class="status-label">考试记录</span>
          <el-select
            v-model="selectedAttemptId"
            placeholder="选择考试记录"
            size="small"
            class="attempt-select"
            @change="handleAttemptChange"
          >
            <el-option
              v-for="attempt in gradingAttempts"
              :key="attempt.sessionId"
              :label="formatAttemptLabel(attempt)"
              :value="attempt.sessionId"
            />
          </el-select>
        </div>
        <el-button size="small" @click="router.push('/')">返回主页</el-button>
      </div>
    </header>

    <section class="exam-body">
      <div class="exam-question card">
        <el-skeleton :rows="6" animated v-if="loading" />

        <template v-else>
          <div v-if="gradingAttempts.length === 0" class="placeholder">暂无考试记录</div>

          <template v-else-if="currentAnswer">
            <div class="question-head">
              <div class="question-index">第 {{ currentIndex + 1 }} 题</div>
              <div class="question-meta">
                <span class="question-type">{{ currentTypeLabel }}</span>
                <span class="question-type question-type--muted">{{ reviewStatusText(currentAnswer) }}</span>
              </div>
            </div>

            <h2 class="question-title">{{ currentAnswer.stem || '无题干' }}</h2>

            <div v-if="questionImageSrc" class="question-image-wrap">
              <img :src="questionImageSrc" alt="题目图片" class="question-image" />
            </div>

            <div class="grading-answer-panel">
              <div class="aside-title">学生作答</div>
              <div class="grading-answer-text">
                <template v-if="currentAnswer.answerText && String(currentAnswer.answerText).trim() !== ''">
                  {{ currentAnswer.answerText }}
                </template>
                <template v-else-if="currentAnswer.answerImagePath">
                  <!-- image only: show nothing here, image preview appears below -->
                </template>
                <template v-else>未作答</template>
              </div>
              <img
                v-if="currentAnswer.answerImagePath"
                :src="normalizeAnswerImageSrc(currentAnswer.answerImagePath)"
                alt="作答图片"
                class="grading-answer-image"
              />
            </div>

            <div class="grading-score-panel">
              <div class="aside-row">
                <span>判定</span>
                <strong>{{ correctLabel }}</strong>
              </div>
              <div class="aside-row">
                <span>原始得分</span>
                <strong>{{ currentAnswer.score ?? '-' }}</strong>
              </div>
              <div v-if="canReviewAnswer(currentAnswer)" class="grading-score-input">
                <span class="score-label">人工评分</span>
                <el-input-number
                  v-model="currentAnswer.score"
                  :min="0"
                  :max="currentAnswer.maxScore ?? 100"
                  size="small"
                />
              </div>
            </div>

            <div class="question-actions grading-actions">
              <el-button :loading="gradingSaving" @click="goToPrevQuestion">上一题</el-button>
              <el-button v-if="!isLastQuestion" :loading="gradingSaving" @click="goToNextQuestion">下一题</el-button>
              <el-button v-else type="primary" :loading="gradingSaving" @click="goToFirstQuestion">
                回到第一题
              </el-button>
              <el-button
                v-if="canReviewAnswer(currentAnswer)"
                type="primary"
                :loading="gradingSaving"
                @click="handleReviewAnswer(currentAnswer)"
              >
                保存
              </el-button>
              <el-button
                v-if="canAiReviewAnswer(currentAnswer)"
                type="primary"
                plain
                :loading="aiReviewingId === currentAnswer.answerId"
                @click="handleAiReviewAnswer(currentAnswer)"
              >
                AI 批改
              </el-button>
              <el-button v-if="hasAiLog(currentAnswer)" plain @click="openAiLog(currentAnswer)">
                查看 AI 日志
              </el-button>
            </div>
          </template>

          <el-result v-else icon="warning" title="暂无可批改内容">
            <template #extra>
              <el-button type="primary" @click="router.push('/')">返回主页</el-button>
            </template>
          </el-result>
        </template>
      </div>

      <aside class="exam-aside">
        <div class="card aside-card">
          <div class="aside-title">题目导航</div>
          <div class="status-legend">
            <span class="legend-chip legend-answered">已批改</span>
            <span class="legend-chip legend-current">当前题目</span>
            <span class="legend-chip legend-unanswered">待处理</span>
          </div>

          <div class="question-grid">
            <button
              v-for="(answer, index) in gradingAnswers"
              :key="answer.answerId || `${index}`"
              type="button"
              class="question-node"
              :class="questionNodeClass(index, answer)"
              :title="answer.stem || `第 ${index + 1} 题`"
              @click="goToQuestion(index)"
            >
              {{ index + 1 }}
            </button>
          </div>

          <div class="aside-row">
            <span>已批改</span>
            <strong>{{ reviewedCount }}</strong>
          </div>
          <div class="aside-row">
            <span>待处理</span>
            <strong>{{ pendingCount }}</strong>
          </div>
          <div class="aside-row">
            <span>总题数</span>
            <strong>{{ totalQuestions }}</strong>
          </div>
        </div>

        <div class="card aside-card">
          <div class="aside-title">批改提示</div>
          <ul class="aside-tips">
            <li>左右切换题目时会自动保存当前题目的分数。</li>
            <li>主观题支持人工评分，也可触发 AI 批改。</li>
            <li>如需查看学生提交的图片答案，可以在当前题目中直接查看。</li>
          </ul>
        </div>
      </aside>
    </section>

    <el-dialog v-model="aiLogVisible" title="AI 批改日志" width="560px">
      <el-input v-model="aiLogContent" type="textarea" :rows="8" readonly />
      <template #footer>
        <el-button @click="copyAiLog">复制</el-button>
        <el-button @click="aiLogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getLogin } from "../auth";
import { createExamSocket } from "../ws";

const route = useRoute();
const router = useRouter();
const loginInfo = getLogin();

const cardNo = ref(loginInfo?.cardNo || loginInfo?.id || "");
const teacherInfo = ref(loginInfo?.teacherInfo || null);
const teacherVipView = ref(null);
const isVipTeacher = computed(() => Boolean(teacherVipView.value?.vip ?? teacherInfo.value?.vip));
const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());
const courseTitle = computed(() => courseNameText.value || `课程 ${courseNoText.value}`);
const studentLabel = computed(() => String(route.query.studentName || "").trim() || route.query.userId || "-");
const studentSno = computed(() => String(route.query.sno || "").trim());
const userId = computed(() => String(route.query.userId || "").trim());
const teacherEid = computed(() => String(route.query.teacherEid || teacherInfo.value?.eid || "").trim());
const reviewerId = computed(() => String(teacherVipView.value?.userId || cardNo.value || loginInfo?.id || "").trim());

const loading = ref(true);
const gradingSaving = ref(false);
const gradingAttempts = ref([]);
const gradingAnswers = ref([]);
const selectedAttemptId = ref("");
const currentIndex = ref(0);
const aiReviewingId = ref("");
const aiLogVisible = ref(false);
const aiLogContent = ref("");
let wsClient = null;

const currentAnswer = computed(() => gradingAnswers.value[currentIndex.value] || null);
const totalQuestions = computed(() => gradingAnswers.value.length || 0);
const currentPosition = computed(() => (totalQuestions.value ? currentIndex.value + 1 : 0));
const progressPercent = computed(() => {
  if (!totalQuestions.value) {
    return 0;
  }
  return Math.min(100, Math.max(0, Math.round((currentPosition.value / totalQuestions.value) * 100)));
});
const isLastQuestion = computed(() => totalQuestions.value > 0 && currentIndex.value === totalQuestions.value - 1);
const reviewedCount = computed(() => gradingAnswers.value.filter((answer) => reviewStatus(answer) === "reviewed").length);
const pendingCount = computed(() => Math.max(0, totalQuestions.value - reviewedCount.value));
const currentTypeLabel = computed(() => {
  const type = currentAnswer.value?.type || "essay";
  const labels = {
    choice: "选择题",
    judge: "判断题",
    blank: "填空题",
    essay: "大题"
  };
  return labels[type] || "题目";
});
const questionImageSrc = computed(() => {
  const path = String(currentAnswer.value?.questionImagePath || currentAnswer.value?.imagePath || currentAnswer.value?.stemImagePath || "").trim();
  if (!path) {
    return "";
  }
  return normalizeAnswerImageSrc(path);
});
const correctLabel = computed(() => {
  if (!currentAnswer.value) {
    return "-";
  }
  if (currentAnswer.value.correct === true) {
    return "正确";
  }
  if (currentAnswer.value.correct === false) {
    return "错误";
  }
  return "-";
});

function connectWebSocket() {
  wsClient = createExamSocket(null, {
    onOpen() {
      loadTeacherVipView()
        .catch(() => null)
        .finally(() => {
          loadExamAttempts();
        });
    },
    onClose() {
      loading.value = false;
    },
    onError() {
      loading.value = false;
    }
  });
}

async function loadTeacherVipView() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  const keyword = teacherEid.value || cardNo.value || String(loginInfo?.id || "").trim();
  if (!keyword) {
    return;
  }

  try {
    const data = await wsClient.request("GET_TEACHER_VIPS", {
      keyword,
      vipStatus: "all"
    }, 20000);
    const rows = Array.isArray(data) ? data : [];
    const matched = rows.find((item) => item?.eid === teacherEid.value || item?.userId === cardNo.value) || rows[0] || null;
    teacherVipView.value = matched;
  } catch {
    teacherVipView.value = null;
  }
}

async function loadExamAttempts() {
  if (!wsClient || !wsClient.isOpen() || !userId.value || !courseNoText.value || courseNoText.value === "-") {
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    const data = await wsClient.request("GET_EXAM_ATTEMPTS", {
      userId: userId.value,
      courseNo: courseNoText.value
    });
    gradingAttempts.value = Array.isArray(data) ? data : [];
    selectedAttemptId.value = gradingAttempts.value[0]?.sessionId || "";
    if (selectedAttemptId.value) {
      await loadAttemptAnswers(selectedAttemptId.value);
    } else {
      gradingAnswers.value = [];
      currentIndex.value = 0;
    }
  } catch (error) {
    ElMessage.error(error.message || "考试记录获取失败");
  } finally {
    loading.value = false;
  }
}

async function loadAttemptAnswers(sessionId) {
  if (!wsClient || !wsClient.isOpen() || !sessionId) {
    return;
  }

  loading.value = true;
  try {
    const data = await wsClient.request("GET_ATTEMPT_ANSWERS", {
      sessionId
    });
    gradingAnswers.value = Array.isArray(data) ? data : [];
    currentIndex.value = 0;
  } catch (error) {
    ElMessage.error(error.message || "答卷获取失败");
  } finally {
    loading.value = false;
  }
}

function handleAttemptChange(value) {
  selectedAttemptId.value = value || "";
  if (selectedAttemptId.value) {
    loadAttemptAnswers(selectedAttemptId.value);
  } else {
    gradingAnswers.value = [];
    currentIndex.value = 0;
  }
}

function formatAttemptLabel(attempt) {
  if (!attempt) {
    return "";
  }
  const started = attempt.startedAt ? new Date(attempt.startedAt).toLocaleString() : "-";
  const status = attempt.status ? attempt.status.toUpperCase() : "";
  return `${started} ${status}`.trim();
}

function canReviewAnswer(answer) {
  return answer?.type === "essay";
}

function canAiReviewAnswer(answer) {
  return isVipTeacher.value && answer?.type === "essay";
}

function hasAiLog(answer) {
  return Boolean(answer?.aiReviewLog);
}

function openAiLog(answer) {
  aiLogContent.value = formatAiLog(String(answer?.aiReviewLog || ""));
  aiLogVisible.value = true;
}

function formatAiLog(raw) {
  const text = String(raw || "").trim();
  if (!text) {
    return "";
  }
  try {
    const value = JSON.parse(text);
    return JSON.stringify(value, null, 2);
  } catch {
    return text;
  }
}

async function copyAiLog() {
  const text = aiLogContent.value || "";
  if (!text) {
    ElMessage.warning("没有可复制的内容");
    return;
  }

  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("已复制 AI 日志");
  } catch {
    ElMessage.error("复制失败，请手动复制");
  }
}

function reviewStatus(answer) {
  if (!answer || answer.type !== "essay") {
    return "reviewed";
  }
  if (answer.reviewed === true) {
    return "reviewed";
  }
  if (answer.reviewed === false && answer.score != null) {
    return "needs_review";
  }
  return "pending";
}

function reviewStatusText(answer) {
  const status = reviewStatus(answer);
  if (status === "reviewed") {
    return "已批改";
  }
  if (status === "needs_review") {
    return "需人工复核";
  }
  return "待批改";
}

function normalizeAnswerImageSrc(path) {
  const text = String(path || "").trim();
  if (!text) {
    return "";
  }
  if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
    return text;
  }
  return `/${text}`;
}

async function persistCurrentReview() {
  if (!currentAnswer.value || !canReviewAnswer(currentAnswer.value)) {
    return true;
  }

  const score = currentAnswer.value.score;
  if (score == null || score === "") {
    return true;
  }

  return handleReviewAnswer(currentAnswer.value, true);
}

async function handleReviewAnswer(answer, silent = false) {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return false;
  }
  if (!answer?.answerId) {
    return false;
  }

  gradingSaving.value = true;
  try {
    const payload = await wsClient.request("REVIEW_ANSWER", {
      answerId: String(answer.answerId),
      score: answer.score,
      reviewerId: reviewerId.value
    });
    const index = gradingAnswers.value.findIndex((item) => item.answerId === payload.answerId);
    if (index !== -1) {
      gradingAnswers.value.splice(index, 1, payload);
    }
    if (!silent) {
      ElMessage.success("批改已保存");
    }
    return true;
  } catch (error) {
    if (!silent) {
      ElMessage.error(error.message || "批改保存失败");
    }
    return false;
  } finally {
    gradingSaving.value = false;
  }
}

async function handleAiReviewAnswer(answer) {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }
  if (!answer?.answerId) {
    return;
  }

  aiReviewingId.value = answer.answerId;
  try {
    const payload = await wsClient.request(
      "AI_REVIEW_ANSWER",
      {
        answerId: String(answer.answerId),
        reviewerId: reviewerId.value
      },
      30000
    );
    const index = gradingAnswers.value.findIndex((item) => item.answerId === payload.answerId);
    if (index !== -1) {
      gradingAnswers.value.splice(index, 1, payload);
    }
    ElMessage.success("AI 批改已完成");
  } catch (error) {
    ElMessage.error(error.message || "AI 批改失败");
  } finally {
    aiReviewingId.value = "";
  }
}

async function goToPrevQuestion() {
  if (!gradingAnswers.value.length) {
    return;
  }
  const saved = await persistCurrentReview();
  if (!saved) {
    return;
  }
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : gradingAnswers.value.length - 1;
}

async function goToNextQuestion() {
  if (!gradingAnswers.value.length) {
    return;
  }
  const saved = await persistCurrentReview();
  if (!saved) {
    return;
  }
  currentIndex.value = currentIndex.value < gradingAnswers.value.length - 1 ? currentIndex.value + 1 : 0;
}

async function goToFirstQuestion() {
  if (!gradingAnswers.value.length) {
    return;
  }
  const saved = await persistCurrentReview();
  if (!saved) {
    return;
  }
  currentIndex.value = 0;
}

async function goToQuestion(index) {
  if (!gradingAnswers.value.length) {
    return;
  }
  const saved = await persistCurrentReview();
  if (!saved) {
    return;
  }
  currentIndex.value = index;
}

function questionNodeClass(index, answer) {
  if (index === currentIndex.value) {
    return "is-current";
  }
  if (reviewStatus(answer) === "reviewed") {
    return "is-answered";
  }
  return "is-unanswered";
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
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

.attempt-select {
  width: 280px;
}

.exam-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
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

.question-type--muted {
  background: rgba(148, 163, 184, 0.12);
  color: var(--ne-text-muted);
}

.question-title {
  margin: 0 0 18px;
  font-size: 20px;
  color: var(--ne-text-strong);
}

.question-image-wrap {
  margin: 0 0 18px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  overflow: auto;
}

.question-image {
  max-width: 100%;
  border-radius: 10px;
  display: block;
}

.grading-answer-panel,
.grading-score-panel {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.grading-answer-text {
  margin-top: 8px;
  color: var(--ne-text-strong);
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.grading-answer-image {
  margin-top: 12px;
  max-width: min(560px, 100%);
  width: 100%;
  border-radius: 10px;
  border: 1px solid var(--ne-border);
  display: block;
}

.grading-score-panel {
  display: grid;
  gap: 10px;
}

.grading-score-input {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.score-label {
  color: var(--ne-text-muted);
  font-size: 13px;
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
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px 8px;
  margin-bottom: 18px;
}

.question-node {
  width: 100%;
  aspect-ratio: 1;
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

.question-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.grading-actions {
  margin-top: 12px;
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

.placeholder {
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ne-text-muted);
}

@media (max-width: 980px) {
  .hero-main {
    flex-direction: column;
  }

  .exam-body {
    grid-template-columns: 1fr;
  }

  .question-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}
</style>
