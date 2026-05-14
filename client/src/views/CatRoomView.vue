<template>
  <section class="exam-shell">
    <header class="exam-hero card">
      <div class="hero-main">
        <div>
          <div class="hero-eyebrow">CAT 自适应练习</div>
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
        <div class="status-item">
          <span class="status-label">Theta</span>
          <span class="status-value">{{ theta.toFixed(2) }}</span>
        </div>
        <div class="status-item">
          <span class="status-label">SE</span>
          <span class="status-value">{{ standardError.toFixed(2) }}</span>
        </div>
      </div>
    </header>

    <section class="exam-body">
      <div class="exam-question card">
        <el-skeleton :rows="6" animated v-if="loading" />

        <template v-else>
          <el-result v-if="finished" icon="success" title="CAT 练习完成">
            <template #extra>
              <el-button type="primary" @click="router.push('/')">返回首页</el-button>
            </template>
          </el-result>

          <template v-else-if="currentQuestion">
            <div class="question-head">
              <div class="question-index">第 {{ answeredCount + 1 }} 题</div>
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
                  <span class="option-tag">{{ String.fromCharCode(65 + index) }}</span>
                  <span class="option-text">{{ formatOptionText(option) }}</span>
                </el-radio>
              </el-radio-group>
            </template>

            <template v-else-if="currentQuestion.type === 'blank'">
              <el-input v-model="answerValue" class="answer-input" placeholder="请输入答案" clearable />
            </template>

            <div class="question-actions">
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交并下一题
              </el-button>
            </div>
          </template>
        </template>
      </div>
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
const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());
const courseTitle = computed(() => courseNameText.value || `课程 ${courseNoText.value}`);

const loading = ref(false);
const submitting = ref(false);
const answeredCount = ref(0);
const maxQuestions = ref(20);
const theta = ref(0);
const standardError = ref(9.99);
const finished = ref(false);

const currentQuestion = ref(null);
const answerValue = ref("");
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

const progressPercent = computed(() => {
  if (!maxQuestions.value) return 0;
  return Math.min(100, (answeredCount.value / maxQuestions.value) * 100);
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

function formatOptionText(option) {
  if (currentQuestion.value?.type === "judge") {
    return option === "true" ? "正确" : "错误";
  }
  return option;
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
    standardError.value = payload.standardError ?? standardError.value;
    answeredCount.value = payload.answeredCount ?? answeredCount.value;
    maxQuestions.value = payload.maxQuestions ?? maxQuestions.value;
    finished.value = payload.finished ?? finished.value;
  } catch (error) {
    ElMessage.error(error.message || "会话状态获取失败。")
  }
}

async function loadNextQuestion() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  loading.value = true;
  try {
    const payload = await wsClient.request("NEXT_QUESTION", {
      sessionId: sessionId.value
    });
    theta.value = payload.theta ?? theta.value;
    standardError.value = payload.standardError ?? standardError.value;
    answeredCount.value = payload.answeredCount ?? answeredCount.value;
    maxQuestions.value = payload.maxQuestions ?? maxQuestions.value;
    finished.value = payload.finished ?? false;
    currentQuestion.value = payload.question || null;
    answerValue.value = "";
  } catch (error) {
    ElMessage.error(error.message || "题目加载失败。");
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!currentQuestion.value) {
    return;
  }

  const answerText = String(answerValue.value || "").trim();
  if (!answerText) {
    ElMessage.warning("请先作答");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试。");
    return;
  }

  submitting.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId: currentQuestion.value.id,
      selectedOption: answerText
    });

    theta.value = payload.theta ?? theta.value;
    standardError.value = payload.standardError ?? standardError.value;
    answeredCount.value = payload.answeredCount ?? answeredCount.value;
    finished.value = payload.finished ?? finished.value;

    if (finished.value) {
      currentQuestion.value = null;
      return;
    }

    await loadNextQuestion();
  } catch (error) {
    ElMessage.error(error.message || "提交失败。")
  } finally {
    submitting.value = false;
  }
}

function connectWebSocket() {
  wsClient = createExamSocket(null, {
    onOpen() {
      loadSessionState().then(loadNextQuestion);
    },
    onError() {
      ElMessage.error("WebSocket 连接失败");
    }
  });
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>
