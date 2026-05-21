<template>
  <section class="exam-shell">
    <div class="exam-container">
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
        </div>
      </header>

      <section class="exam-body">
        <div class="exam-question card">
          <el-skeleton :rows="6" animated v-if="loading" />

          <template v-else>
            <div
              v-if="finished"
              class="report-wrapper max-w-4xl mx-auto p-6 bg-white border border-gray-100 rounded-2xl shadow-sm space-y-10"
            >
                <section class="report-ai-section">
                  <div class="report-badge">
                    NexEval AI 深度诊断报告
                  </div>
                  <div class="report-ai-inner">
                    <div class="report-ai-header">
                      <div>
                        <h3 class="report-main-title">AI 智能评估简报</h3>
                        <p class="report-main-subtitle">AI Evaluation Summary</p>
                      </div>
                      <el-button
                        type="primary"
                        :loading="reportGenerating"
                        class="!bg-[#8A4F3C] !border-[#8A4F3C] hover:!bg-[#7A4635]"
                        @click="handleGenerateAiSummary"
                      >
                        {{ reportGenerated ? "重新生成" : "点击生成简报" }}
                      </el-button>
                    </div>
                    <div class="report-content-box">
                      <p v-if="reportContent" class="report-content-text">
                        {{ reportContent }}
                      </p>
                      <p v-else-if="reportGenerating" class="report-content-placeholder">
                        AI 正在分析作答轨迹并逐字生成诊断结论...
                      </p>
                      <p v-else class="report-content-placeholder">
                        点击“生成简报”后，系统将基于当前 CAT 作答数据生成个性化诊断报告。
                      </p>
                      <p v-if="reportError" class="report-content-error">{{ reportError }}</p>
                    </div>
                  </div>
                </section>

                <section class="report-grid">
                  <div class="report-card">
                    <h4 class="report-section-title">核心技能掌握度分级</h4>
                    <div class="skill-list">
                      <div class="skill-row">
                        <div class="skill-label-row"><span>指令系统</span><span>85%</span></div>
                        <div class="skill-track"><div class="skill-fill" style="background:#111111;width:85%"></div></div>
                      </div>
                      <div class="skill-row">
                        <div class="skill-label-row"><span>存储体系</span><span>60%</span></div>
                        <div class="skill-track"><div class="skill-fill" style="background:#9ca3af;width:60%"></div></div>
                      </div>
                      <div class="skill-row">
                        <div class="skill-label-row"><span>I/O 接口</span><span>21%</span></div>
                        <div class="skill-track"><div class="skill-fill" style="background:#CF7357;width:21%"></div></div>
                      </div>
                    </div>
                  </div>

                  <div class="report-card">
                    <h4 class="report-section-title">AI 题目难度调度轨迹</h4>
                    <p class="report-card-desc">展示系统根据您的即时对错，动态调整题目难度的过程</p>
                    <div class="report-chart-wrap">
                      <svg viewBox="0 0 300 140" preserveAspectRatio="xMidYMid meet" style="width:100%;height:100%">
                        <defs>
                          <linearGradient id="catTrajectoryFill" x1="0%" y1="0%" x2="0%" y2="100%">
                            <stop offset="0%" stop-color="#CF7357" stop-opacity="0.18" />
                            <stop offset="100%" stop-color="#CF7357" stop-opacity="0.02" />
                          </linearGradient>
                        </defs>
                        <path
                          d="M8 112 C 28 110, 42 92, 58 90 C 74 88, 88 102, 106 96 C 124 90, 138 64, 156 62 C 174 60, 186 86, 206 76 C 226 66, 240 40, 258 38 C 274 36, 286 48, 296 44 L 296 136 L 8 136 Z"
                          fill="url(#catTrajectoryFill)"
                        />
                        <path
                          d="M8 112 C 28 110, 42 92, 58 90 C 74 88, 88 102, 106 96 C 124 90, 138 64, 156 62 C 174 60, 186 86, 206 76 C 226 66, 240 40, 258 38 C 274 36, 286 48, 296 44"
                          fill="none"
                          stroke="#CF7357"
                          stroke-width="3"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </div>
                  </div>
                </section>

                <section class="report-card">
                  <h4 class="report-section-title">答题详情与薄弱知识点聚类</h4>
                  <div>
                    <span
                      v-for="tag in weakKnowledgeTags"
                      :key="tag"
                      class="report-tag"
                    >
                      {{ tag }}
                    </span>
                  </div>
                  <div class="report-answer-grid">
                    <button
                      v-for="item in answerGrid"
                      :key="item.no"
                      type="button"
                      :class="item.correct ? 'answer-btn answer-btn--correct' : 'answer-btn answer-btn--wrong'"
                      @click="handleReviewQuestion(item)"
                    >
                      {{ item.no }}
                    </button>
                  </div>
                </section>

                <div class="report-actions">
                  <button
                    type="button"
                    class="report-btn-secondary"
                    @click="router.push({ name: 'home', query: { menu: 'cat' } })"
                  >
                    返回首页
                  </button>
                  <button
                    type="button"
                    class="report-btn-primary"
                    @click="handleStartWeaknessTraining"
                  >
                    开启错题针对性训练
                  </button>
                </div>
              </div>

            <template v-else-if="currentQuestion">
              <div v-if="currentKnowledgePoints.length" class="question-kp">
                <el-breadcrumb separator="/">
                  <el-breadcrumb-item>知识点</el-breadcrumb-item>
                  <el-breadcrumb-item v-for="point in currentKnowledgePoints" :key="point">
                    {{ point }}
                  </el-breadcrumb-item>
                </el-breadcrumb>
              </div>

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
                <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
                  提交并进入下一题
                </el-button>
              </div>
            </template>
          </template>
        </div>

        <aside class="exam-aside">
          <div class="card ai-panel">
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
          </div>
        </aside>
      </section>
    </div>
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
const reportGenerating = ref(false);
const reportGenerated = ref(false);
const reportContent = ref("");
const reportError = ref("");

const currentQuestion = ref(null);
const answerValue = ref("");
const growthChartEl = ref(null);
const growthChart = shallowRef(null);
const growthPoints = ref([]);
let wsClient = null;
let reportTypingToken = 0;

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

  return [...new Set(points)];
});

const growthXAxis = computed(() => growthPoints.value.map(item => `第${item.questionNo}题`));
const growthSeries = computed(() => growthPoints.value.map(item => item.score));
const weakKnowledgeTags = [
  "DMA控制方式（高频错题）",
  "Cache命中率计算"
];
const answerGrid = computed(() => Array.from({ length: 20 }, (_, index) => ({
  no: index + 1,
  correct: index < 12
})));

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

async function handleGenerateAiSummary() {
  if (reportGenerating.value) {
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    reportError.value = "WebSocket 未连接，无法生成 AI 简报。";
    return;
  }

  reportGenerating.value = true;
  reportGenerated.value = false;
  reportError.value = "";
  reportContent.value = "";

  const typingToken = ++reportTypingToken;

  try {
    const payload = await wsClient.request("GENERATE_CAT_REPORT", {
      sessionId: String(sessionId.value || ""),
      courseNo: courseNoText.value,
      courseName: courseNameText.value || courseTitle.value,
      estimatedScore: estimatedScore.value,
      systemPrecision: systemPrecision.value,
      answeredCount: answeredCount.value,
      maxQuestions: maxQuestions.value
    });
    const fullText = String(payload?.reportText || "").trim();
    if (!fullText) {
      throw new Error("AI 未返回简报内容");
    }
    await typeReportText(fullText, typingToken);
    reportGenerated.value = true;
  } catch (error) {
    if (typingToken !== reportTypingToken) return;
    reportError.value = error?.message || "生成失败，请稍后重试。";
  } finally {
    if (typingToken === reportTypingToken) {
      reportGenerating.value = false;
    }
  }
}

async function typeReportText(fullText, token) {
  reportContent.value = "";
  for (let i = 0; i < fullText.length; i++) {
    if (token !== reportTypingToken) {
      return;
    }
    reportContent.value += fullText.charAt(i);
    await new Promise((resolve) => {
      window.setTimeout(resolve, 14);
    });
  }
}

function handleReviewQuestion(item) {
  if (item.correct) {
    ElMessage.success(`第 ${item.no} 题已作对，可查看完整解析。`);
    return;
  }
  ElMessage.warning(`第 ${item.no} 题为薄弱项，建议优先复盘。`);
}

function handleStartWeaknessTraining() {
  ElMessage.success("已进入错题针对性训练流程。");
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
  reportTypingToken += 1;
  window.removeEventListener("resize", handleChartResize);
  growthChart.value?.dispose();
  growthChart.value = null;
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

.option-item :deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.6;
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

.submit-btn {
  min-height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.question-kp {
  margin-top: 12px;
}

.report-wrapper {
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* ── 第一块：AI 智能评估简报 ── */
.report-ai-section {
  background: #FDFCF8;
  border: 1px solid rgba(207, 115, 87, 0.18);
  border-radius: 12px;
  padding: 20px;
}

.report-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 12px;
  border-radius: 999px;
  background: rgba(207, 115, 87, 0.12);
  color: #CF7357;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
}

.report-ai-inner {
  border-left: 3px solid #CF7357;
  padding-left: 14px;
}

.report-ai-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-main-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: #111111;
}

.report-main-subtitle {
  margin: 4px 0 0;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #9ca3af;
}

.report-content-box {
  margin-top: 14px;
  min-height: 132px;
  border-radius: 8px;
  border: 1px solid rgba(207, 115, 87, 0.12);
  background: #ffffff;
  padding: 12px 16px;
}

/* ── AI 内容文字 ── */
.report-content-text {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.95;
  letter-spacing: 0.01em;
  white-space: pre-wrap;
  margin: 0;
}

.report-content-placeholder {
  font-size: 13px;
  color: #b0b7c3;
  line-height: 1.7;
  margin: 0;
}

.report-content-error {
  margin-top: 8px;
  font-size: 12px;
  color: #ef4444;
}

/* ── 下方卡片网格 ── */
.report-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.report-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 24px;
}

.report-card-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 12px;
  line-height: 1.6;
}

.report-chart-wrap {
  width: 100%;
  height: 160px;
  margin: 12px auto 0;
}

/* ── 技能进度条 ── */
.skill-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.skill-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.skill-label-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.skill-track {
  width: 100%;
  height: 7px;
  border-radius: 999px;
  background: #f3f4f6;
  overflow: hidden;
}

.skill-fill {
  height: 100%;
  border-radius: 999px;
}

/* ── 薄弱知识点标签 ── */
.report-tag {
  display: inline-block;
  font-size: 11px;
  font-family: monospace;
  background: #fff1f2;
  color: #e11d48;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(225, 29, 72, 0.15);
  margin-right: 6px;
  margin-bottom: 6px;
}

/* ── 答题格子 ── */
.report-answer-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.answer-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: monospace;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid;
  background: none;
}

.answer-btn--correct {
  background: #f0fdf4;
  color: #16a34a;
  border-color: rgba(22, 163, 74, 0.3);
}

.answer-btn--wrong {
  background: #fff1f2;
  color: #e11d48;
  border-color: rgba(225, 29, 72, 0.3);
}

/* ── 底部操作栏 ── */
.report-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  flex-wrap: wrap;
}

.report-btn-secondary {
  padding: 8px 16px;
  font-size: 13px;
  color: #6b7280;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}

.report-btn-secondary:hover {
  background: #f9fafb;
}

.report-btn-primary {
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
  background: #8A4F3C;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.report-btn-primary:hover {
  background: #7A4635;
}

@media (max-width: 768px) {
  .report-grid {
    grid-template-columns: 1fr;
  }

  .report-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .report-btn-secondary,
  .report-btn-primary {
    width: 100%;
  }
}

/* ── 下方各子标题 ── */
.report-section-title {
  margin: 0 0 18px;
  padding: 0 0 12px 10px;
  border-left: 3px solid #CF7357;
  border-bottom: 1px solid #e5e7eb;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #374151;
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

.cat-growth-chart {
  width: 100%;
  height: 220px;
}

@media (max-width: 980px) {
  .hero-main {
    flex-direction: column;
  }

  .exam-body {
    grid-template-columns: 1fr;
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
