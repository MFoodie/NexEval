<template>
  <section class="exam-shell">
    <header class="exam-hero card">
      <div class="hero-main">
        <div class="hero-copy">
          <div class="hero-eyebrow">CAT 自适应练习</div>
          <h1 class="hero-title">{{ courseTitle }}</h1>
          <p class="hero-subtitle">
            课程编号：{{ courseNoText }}
            <span v-if="courseNameText">｜课程名称：{{ courseNameText }}</span>
          </p>
        </div>

        <div class="hero-status">
          <div class="status-item">
            <div class="status-topline">
              <span class="status-label">进度</span>
              <span class="progress-text">{{ answeredCount }}/{{ maxQuestions }}</span>
            </div>
            <div class="progress-track progress-track--hero">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <section class="exam-body">
      <el-row :gutter="16" class="cat-layout">
        <el-col :span="17" :xs="24" :md="17">
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
                    <el-tag class="question-type-tag" type="primary" effect="light">{{ currentTypeLabel }}</el-tag>
                  </div>
                </div>

                <div class="question-kp">
                  <el-breadcrumb separator="/">
                    <el-breadcrumb-item>知识点</el-breadcrumb-item>
                    <el-breadcrumb-item v-for="point in currentKnowledgePoints" :key="point">
                      {{ point }}
                    </el-breadcrumb-item>
                  </el-breadcrumb>
                </div>

                <h2 class="question-title question-title--large">{{ currentQuestion.stem }}</h2>

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
                  <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
                    提交并进入下一题
                  </el-button>
                </div>
              </template>
            </template>
          </div>
        </el-col>

        <el-col :span="7" :xs="24" :md="7">
          <aside class="card ai-panel">
            <div class="ai-panel-title">CAT 动态调度舱</div>
            <div class="ai-panel-subtitle">AI 预估掌握度</div>
            <div class="dashboard-row">
              <el-progress
                class="ai-dashboard"
                type="dashboard"
                :percentage="estimatedScore"
                :stroke-width="12"
                :show-text="false"
              />
              <div class="dashboard-caption">
                <span class="dashboard-caption-value">{{ estimatedScore }}%</span>
                <span class="dashboard-caption-text">当前预估</span>
              </div>
            </div>
            <div class="ai-metrics">
              <div class="ai-metric-card ai-metric-card--primary">
                <div class="ai-metric-label">AI 预估掌握度</div>
                <div class="ai-metric-value">{{ estimatedScore }}%</div>
              </div>
              <div class="ai-metric-card">
                <div class="ai-metric-label">系统精准度</div>
                <div class="ai-metric-value ai-metric-value--accent">{{ systemPrecision }}%</div>
              </div>
            </div>
            <div ref="growthChartEl" class="cat-growth-chart"></div>
          </aside>
        </el-col>
      </el-row>
    </section>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import * as echarts from "echarts";
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
const growthChartEl = ref(null);
const growthChart = shallowRef(null);
const growthPoints = ref([]);
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

const estimatedScore = computed(() => {
  const rawTheta = Number(theta.value);
  const score = Math.round((((Number.isFinite(rawTheta) ? rawTheta : 0) + 3) / 6) * 100);
  return Math.min(100, Math.max(0, score));
});

const systemPrecision = computed(() => {
  const se = Number(standardError.value);
  if (!Number.isFinite(se)) {
    return 0;
  }
  return Math.min(100, Math.max(0, Math.round((1 - se / 3.0) * 100)));
});

const currentKnowledgePoints = computed(() => {
  const question = currentQuestion.value || {};
  const candidates = [
    question.knowledgePoint,
    question.knowledgePoints,
    question.knowledge,
    question.tags,
    question.tag
  ];
  const points = [];

  for (const entry of candidates) {
    if (Array.isArray(entry)) {
      for (const item of entry) {
        const text = String(item || "").trim();
        if (text) points.push(text);
      }
      continue;
    }

    if (typeof entry === "string") {
      const chunks = entry.split(/[，,;/、]/).map(item => item.trim()).filter(Boolean);
      if (chunks.length > 0) {
        points.push(...chunks);
      }
    }
  }

  if (points.length === 0) {
    return ["未标注知识点"];
  }

  return [...new Set(points)];
});

const growthXAxis = computed(() => growthPoints.value.map(item => `第${item.questionNo}题`));
const growthSeries = computed(() => growthPoints.value.map(item => item.score));

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

function buildGrowthChartOption() {
  return {
    animationDuration: 700,
    animationDurationUpdate: 700,
    animationEasing: "cubicOut",
    animationEasingUpdate: "cubicOut",
    grid: {
      left: 34,
      right: 20,
      top: 24,
      bottom: 28
    },
    tooltip: {
      trigger: "axis"
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: growthXAxis.value,
      axisLabel: {
        color: "#6f6659",
        fontSize: 11
      },
      axisLine: {
        lineStyle: {
          color: "rgba(111, 102, 89, 0.4)"
        }
      },
      axisTick: {
        show: false
      }
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 100,
      splitNumber: 5,
      axisLabel: {
        color: "#6f6659",
        fontSize: 11
      },
      axisLine: {
        show: false
      },
      splitLine: {
        lineStyle: {
          color: "rgba(111, 102, 89, 0.16)"
        }
      }
    },
    series: [
      {
        name: "预估分",
        type: "line",
        smooth: true,
        symbol: "circle",
        symbolSize: 7,
        data: growthSeries.value,
        lineStyle: {
          width: 3,
          color: "#111111"
        },
        itemStyle: {
          color: "#111111",
          borderColor: "#ffffff",
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(17, 17, 17, 0.26)" },
            { offset: 1, color: "rgba(17, 17, 17, 0.04)" }
          ])
        }
      }
    ]
  };
}

function initGrowthChart() {
  if (!growthChartEl.value || growthChart.value) {
    return;
  }

  growthChart.value = echarts.init(growthChartEl.value);
  growthChart.value.setOption(buildGrowthChartOption());
}

function updateGrowthChart() {
  if (!growthChart.value) {
    return;
  }

  growthChart.value.setOption(buildGrowthChartOption(), { notMerge: true });
}

function syncGrowthPoint() {
  const questionNo = Number(answeredCount.value);
  if (!Number.isFinite(questionNo) || questionNo <= 0) {
    return;
  }

  const score = estimatedScore.value;
  const lastPoint = growthPoints.value[growthPoints.value.length - 1];
  if (lastPoint && lastPoint.questionNo === questionNo) {
    growthPoints.value = [...growthPoints.value.slice(0, -1), { ...lastPoint, score }];
    return;
  }

  growthPoints.value = [...growthPoints.value, { questionNo, score }];
}

function handleChartResize() {
  growthChart.value?.resize();
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
onMounted(() => {
  nextTick(() => {
    initGrowthChart();
    updateGrowthChart();
  });
  window.addEventListener("resize", handleChartResize);
});

watch([answeredCount, theta], () => {
  syncGrowthPoint();
});

watch(growthPoints, () => {
  updateGrowthChart();
}, { deep: true });

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleChartResize);
  growthChart.value?.dispose();
  growthChart.value = null;
  wsClient?.close();
});
</script>

<style scoped>
.cat-layout {
  margin-top: 16px;
  align-items: flex-start;
}

.hero-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-status {
  flex: 0 0 300px;
  max-width: 100%;
  align-self: flex-end;
  margin-bottom: 2px;
}

.status-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.01));
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.status-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.status-label {
  font-size: 13px;
  color: var(--ne-text-muted);
}

.progress-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.progress-track--hero {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(var(--ne-primary-rgb), 0.14);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--ne-primary), var(--ne-accent));
}

.question-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-kp {
  margin-top: 12px;
}

.question-title--large {
  margin: 14px 0 18px;
  font-size: 22px;
  line-height: 1.6;
}

.option-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.option-item {
  width: 100%;
  margin-right: 0;
  padding: 2px 0;
}

.option-tag {
  display: inline-block;
  margin-right: 0.5em;
}

.option-item :deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.6;
}

.question-actions {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  min-height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.ai-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.ai-panel > * {
  width: 100%;
}

.ai-panel-title {
  width: 100%;
  font-size: 16px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.ai-panel-subtitle {
  width: 100%;
  font-size: 13px;
  color: var(--ne-text-muted);
}

.dashboard-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
}

.ai-dashboard {
  margin-top: 0;
  flex: 0 0 auto;
}

.dashboard-caption {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 8px;
  min-width: 96px;
}

.dashboard-caption-value {
  font-size: 38px;
  line-height: 1;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.dashboard-caption-text {
  font-size: 22px;
  line-height: 1;
  color: var(--ne-text-muted);
  font-weight: 500;
}

.dashboard-caption {
  color: var(--ne-text-muted);
}

.ai-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.ai-metric-card {
  padding: 12px 12px 10px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(var(--ne-primary-rgb), 0.04), rgba(var(--ne-primary-rgb), 0.02));
  border: 1px solid rgba(var(--ne-primary-rgb), 0.12);
}

.ai-metric-card--primary {
  background: linear-gradient(135deg, rgba(var(--ne-accent-rgb), 0.14), rgba(var(--ne-accent-rgb), 0.06));
  border-color: rgba(var(--ne-accent-rgb), 0.2);
}

.ai-metric-label {
  font-size: 12px;
  line-height: 1.4;
  color: var(--ne-text-muted);
}

.ai-metric-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  color: var(--ne-text-strong);
}

.ai-metric-value--accent {
  color: var(--ne-primary);
}

.chart-placeholder {
  width: 100%;
  height: 300px;
  border-radius: 12px;
  border: 1px dashed rgba(var(--ne-primary-rgb), 0.2);
  background: linear-gradient(135deg, rgba(var(--ne-primary-rgb), 0.05), rgba(var(--ne-accent-rgb), 0.06));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--ne-text-muted);
}

.chart-placeholder-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ne-text-strong);
}

.chart-placeholder-sub {
  margin-top: 6px;
  font-size: 12px;
}

@media (max-width: 980px) {
  .hero-main {
    flex-direction: column;
  }

  .hero-status {
    width: 100%;
    flex: 1 1 auto;
    align-self: stretch;
    margin-bottom: 0;
  }

  .dashboard-row {
    justify-content: flex-start;
  }

  .dashboard-caption-value {
    font-size: 30px;
  }

  .dashboard-caption-text {
    font-size: 18px;
  }
}
</style>
