<template>
  <section class="exam-shell">
    <header class="exam-hero card">
      <div class="hero-main">
        <div>
          <div class="hero-eyebrow">固定题库</div>
          <h1 class="hero-title">题库练习</h1>
          <p class="hero-subtitle">当前阶段先完成固定题库练习，提交后进入下一题。</p>
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
      </div>
    </header>

    <section class="exam-body">
      <div class="exam-question card">
        <el-skeleton :rows="6" animated v-if="loading" />

        <template v-else>
          <el-result v-if="finished" icon="success" title="练习完成">
            <template #extra>
              <el-button type="primary" @click="router.push('/')">返回首页</el-button>
            </template>
          </el-result>

          <template v-else-if="currentQuestion">
            <div class="question-head">
              <div class="question-index">第 {{ currentIndex + 1 }} 题</div>
              <div class="question-difficulty">难度系数：{{ currentQuestion.difficulty.toFixed(1) }}</div>
            </div>
            <h2 class="question-title">{{ currentQuestion.stem }}</h2>

            <el-radio-group v-model="selectedOption" class="option-group">
              <el-radio
                v-for="(option, index) in currentQuestion.options"
                :key="option"
                :label="option"
                class="option-item"
              >
                <span class="option-tag">{{ String.fromCharCode(65 + index) }}</span>
                <span class="option-text">{{ option }}</span>
              </el-radio>
            </el-radio-group>

            <div class="question-actions">
              <el-button @click="goToPrevQuestion">上一题</el-button>
              <el-button @click="goToNextQuestion">下一题</el-button>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交答案
              </el-button>
            </div>
          </template>
        </template>
      </div>

      <aside class="exam-aside">
        <div class="card aside-card">
          <div class="aside-title">提示</div>
          <ul class="aside-tips">
            <li>每题仅可提交一次，请确认后再提交。</li>
            <li>当前为固定题库练习模式。</li>
            <li>如遇网络异常，请刷新页面重试。</li>
          </ul>
        </div>
      </aside>
    </section>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";

const route = useRoute();
const router = useRouter();

const sessionId = computed(() => route.params.sessionId);
const loading = ref(false);
const submitting = ref(false);

const questions = ref([]);
const currentIndex = ref(0);
const currentQuestion = computed(() => questions.value[currentIndex.value] || null);
const selectedOption = ref("");
const theta = ref(0);
const answeredCount = ref(0);
const maxQuestions = ref(10);
const finished = ref(false);
const answeredSet = ref(new Set());
let wsClient = null;

const progressPercent = computed(() => {
  if (!maxQuestions.value) {
    return 0;
  }

  const ratio = answeredCount.value / maxQuestions.value;
  return Math.min(100, Math.max(0, Math.round(ratio * 100)));
});

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
    selectedOption.value = "";
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
    answeredSet.value = new Set(payload.answeredQuestionIds || []);
  } catch (error) {
    ElMessage.error(error.message || "会话状态获取失败。");
  }
}

async function handleSubmit() {
  if (!currentQuestion.value) {
    return;
  }

  if (!selectedOption.value) {
    ElMessage.warning("请选择一个选项。");
    return;
  }

  submitting.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId: currentQuestion.value.id,
      selectedOption: selectedOption.value
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    finished.value = payload.finished;
    answeredSet.value.add(currentQuestion.value.id);

    if (payload.correct) {
      ElMessage.success("提交成功。");
    } else {
      ElMessage.info("提交成功。");
    }
    if (!payload.finished) {
      goToNextQuestion();
    }
  } catch (error) {
    ElMessage.error(error.message || "提交失败。");
  } finally {
    submitting.value = false;
  }
}

function goToPrevQuestion() {
  if (!questions.value.length) {
    return;
  }
  currentIndex.value = currentIndex.value > 0 ? currentIndex.value - 1 : questions.value.length - 1;
  selectedOption.value = "";
}

function goToNextQuestion() {
  if (!questions.value.length) {
    return;
  }
  currentIndex.value = currentIndex.value < questions.value.length - 1 ? currentIndex.value + 1 : 0;
  selectedOption.value = "";
}

function connectWebSocket() {
  wsClient = createExamSocket(sessionId.value, {
    onOpen() {
      loadSessionState();
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
