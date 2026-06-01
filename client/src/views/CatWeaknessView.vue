<template>
  <section class="weak-shell">
    <div class="weak-container">
      <header class="weak-hero card">
        <div>
          <div class="weak-eyebrow">CAT 错题针对性训练</div>
          <h1 class="weak-title">AI 10 题强化训练</h1>
          <p class="weak-subtitle">
            课程编号：{{ courseNoText }}
            <span v-if="courseNameText">｜课程名称：{{ courseNameText }}</span>
          </p>
        </div>
        <div class="weak-actions">
          <el-button @click="router.push({ name: 'home', query: { menu: 'cat' } })">返回首页</el-button>
          <el-button type="primary" plain :loading="generatingTraining" @click="generateTraining">
            重新生成训练
          </el-button>
        </div>
      </header>

      <section class="weak-body">
        <div class="weak-main card">
          <div v-if="generatingTraining" class="weak-loading">
            <el-skeleton :rows="8" animated />
          </div>

          <template v-else-if="trainingFinished">
            <div class="weak-finish">
              <h2>训练完成</h2>
              <p>本轮共完成 {{ historyList.length }} / {{ totalQuestionCount }} 题，AI 已完成逐题评估。</p>
              <div class="weak-summary">
                <div class="weak-summary-card">
                  <span>答对</span>
                  <strong>{{ correctCount }}</strong>
                </div>
                <div class="weak-summary-card">
                  <span>答错</span>
                  <strong>{{ wrongCount }}</strong>
                </div>
                <div class="weak-summary-card">
                  <span>正确率</span>
                  <strong>{{ accuracyPercent }}%</strong>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="currentQuestion">
            <div class="weak-question-head">
              <div>
                <div class="weak-question-index">第 {{ currentIndex + 1 }} / {{ totalQuestionCount }} 题</div>
                <div class="weak-question-type">{{ currentTypeLabel }}</div>
              </div>
              <div v-if="currentQuestion.knowledgePoints.length" class="weak-kp-list">
                <span v-for="point in currentQuestion.knowledgePoints" :key="point" class="weak-kp-tag">{{ point }}</span>
              </div>
            </div>

            <h2 class="weak-question-title">{{ currentQuestion.stem }}</h2>

            <div v-if="currentQuestion.imagePath" class="weak-image-wrap">
              <img :src="normalizeImageSrc(currentQuestion.imagePath)" alt="题目图片" />
            </div>

            <template v-if="isOptionQuestion">
              <el-radio-group v-model="answerValue" class="weak-option-group">
                <el-radio
                  v-for="(option, index) in currentQuestion.options"
                  :key="`${currentQuestion.id}-${index}`"
                  :label="option"
                  class="weak-option-item"
                  :disabled="questionAnswered"
                >
                  <span class="weak-option-tag">{{ optionLabel(index, currentQuestion.type) }}</span>
                  <span class="weak-option-text">{{ formatOptionText(option, currentQuestion.type) }}</span>
                </el-radio>
              </el-radio-group>
            </template>

            <template v-else-if="currentQuestion.type === 'essay'">
              <el-input
                v-model="answerValue"
                type="textarea"
                :rows="7"
                resize="vertical"
                :disabled="questionAnswered"
                placeholder="请输入你的简答题答案"
              />
            </template>

            <template v-else>
              <el-input
                v-model="answerValue"
                :disabled="questionAnswered"
                placeholder="请输入你的答案"
                clearable
              />
            </template>

            <div class="weak-submit">
              <el-button
                type="primary"
                size="large"
                :loading="evaluatingAnswer"
                :disabled="questionAnswered"
                @click="handleSubmit"
              >
                提交并 AI 评估
              </el-button>
              <el-button v-if="questionAnswered" size="large" @click="goNextQuestion">
                {{ currentIndex + 1 >= totalQuestionCount ? "查看结果" : "下一题" }}
              </el-button>
            </div>

            <div v-if="currentResult" class="weak-result" :class="currentResult.correct ? 'is-correct' : 'is-wrong'">
              <div class="weak-result-head">
                <div class="weak-result-status">
                  <strong>{{ currentResult.correct ? "回答正确" : "回答错误" }}</strong>
                  <span class="weak-result-score">得分：{{ currentResult.score }}%</span>
                </div>
                <span class="weak-result-badge" :class="currentResult.correct ? 'is-correct' : 'is-wrong'">
                  {{ currentResult.correct ? "正确" : "错误" }}
                </span>
              </div>
              <div class="weak-result-grid">
                <div class="weak-result-card">
                  <div class="weak-result-label">你的答案</div>
                  <div class="weak-result-value">{{ formatAnswer(currentResult.userAnswer) }}</div>
                </div>
                <div class="weak-result-card">
                  <div class="weak-result-label">参考答案</div>
                  <div class="weak-result-value">{{ formatAnswer(currentResult.correctAnswer) }}</div>
                </div>
              </div>
              <div class="weak-result-analysis">
                <div class="weak-result-label">AI 解析</div>
                <p class="weak-result-explain">{{ currentResult.explanation }}</p>
              </div>
            </div>
          </template>

          <div v-else class="weak-empty">
            <p>暂无训练题目，点击下方按钮开始生成。</p>
            <el-button type="primary" size="large" :loading="generatingTraining" @click="generateTraining">
              开始训练
            </el-button>
          </div>

          <p v-if="errorMessage" class="weak-error">{{ errorMessage }}</p>
        </div>

        <aside class="weak-aside">
          <div class="card weak-side-card">
            <div class="weak-side-title">训练进度</div>
            <el-progress :percentage="progressPercent" :stroke-width="10" />
            <div class="weak-side-stats">
              <div class="weak-stat">
                <span>总题数</span>
                <strong>{{ totalQuestionCount }}</strong>
              </div>
              <div class="weak-stat">
                <span>已完成</span>
                <strong>{{ historyList.length }}</strong>
              </div>
            </div>
          </div>

          <div class="card weak-side-card">
            <div class="weak-side-title">本轮记录</div>
            <div v-if="!historyList.length" class="weak-history-empty">还没有作答记录</div>
            <div v-else class="weak-history-list">
              <div v-for="item in historyList" :key="item.id" class="weak-history-item">
                <div class="weak-history-stem">{{ item.stem }}</div>
                <div class="weak-history-meta">
                  <span :class="item.correct ? 'badge-correct' : 'badge-wrong'">
                    {{ item.correct ? "正确" : "错误" }}
                  </span>
                  <span>你的答案：{{ formatAnswer(item.userAnswer) }}</span>
                  <span>参考答案：{{ formatAnswer(item.correctAnswer) }}</span>
                </div>
                <p class="weak-history-explain">{{ item.explanation }}</p>
              </div>
            </div>
          </div>
        </aside>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { fetchWeaknessEvaluation, fetchWeaknessTraining } from "../services/aiWeaknessService";

const route = useRoute();
const router = useRouter();

const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());
const trainingKey = computed(() => String(route.query.trainingKey || "").trim());

const generatingTraining = ref(false);
const evaluatingAnswer = ref(false);
const answerValue = ref("");
const errorMessage = ref("");
const generatedQuestions = ref([]);
const historyList = ref([]);
const currentIndex = ref(0);
const currentResult = ref(null);

const typeLabels = {
  choice: "选择题",
  judge: "判断题",
  blank: "填空题",
  essay: "简答题"
};

const sessionContext = computed(() => {
  const key = trainingKey.value;
  if (!key) {
    return { weakPoints: [], wrongAnswers: [] };
  }
  try {
    const raw = window.sessionStorage.getItem(key);
    const parsed = raw ? JSON.parse(raw) : {};
    return {
      weakPoints: Array.isArray(parsed?.weakPoints) ? parsed.weakPoints : [],
      wrongAnswers: Array.isArray(parsed?.wrongAnswers) ? parsed.wrongAnswers : []
    };
  } catch {
    return { weakPoints: [], wrongAnswers: [] };
  }
});

const totalQuestionCount = computed(() => generatedQuestions.value.length);
const currentQuestion = computed(() => generatedQuestions.value[currentIndex.value] || null);
const currentTypeLabel = computed(() => typeLabels[currentQuestion.value?.type] || "题目");
const isOptionQuestion = computed(() => ["choice", "judge"].includes(currentQuestion.value?.type || ""));
const questionAnswered = computed(() => Boolean(currentResult.value));
const progressPercent = computed(() => {
  if (!totalQuestionCount.value) return 0;
  return Math.round((historyList.value.length / totalQuestionCount.value) * 100);
});
const trainingFinished = computed(() => {
  return totalQuestionCount.value > 0 && historyList.value.length >= totalQuestionCount.value && currentIndex.value >= totalQuestionCount.value;
});
const correctCount = computed(() => historyList.value.filter(item => item.correct).length);
const wrongCount = computed(() => historyList.value.filter(item => !item.correct).length);
const accuracyPercent = computed(() => {
  if (!historyList.value.length) return 0;
  return Math.round((correctCount.value / historyList.value.length) * 100);
});

function normalize(value) {
  return String(value || "").trim();
}

function normalizeImageSrc(path) {
  const text = normalize(path);
  if (!text) return "";
  if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
    return text;
  }
  return `/${text}`;
}

function formatAnswer(answer) {
  const text = normalize(answer);
  return text || "未作答";
}

function optionLabel(index, type) {
  if (type === "judge") {
    return index === 0 ? "T" : "F";
  }
  return String.fromCharCode(65 + index);
}

function formatOptionText(option, type) {
  if (type === "judge") {
    return option === "true" ? "正确" : "错误";
  }
  return option;
}

function normalizeQuestion(raw) {
  if (!raw) return null;
  return {
    id: normalize(raw.id) || `q-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    type: normalize(raw.type).toLowerCase(),
    stem: normalize(raw.stem),
    options: Array.isArray(raw.options) ? raw.options.map(option => normalize(option)).filter(Boolean) : [],
    correctAnswer: normalize(raw.correctAnswer),
    knowledgePoints: Array.isArray(raw.knowledgePoints) ? raw.knowledgePoints.map(point => normalize(point)).filter(Boolean) : [],
    imagePath: normalize(raw.imagePath),
    explanation: normalize(raw.explanation)
  };
}

async function generateTraining() {
  generatingTraining.value = true;
  errorMessage.value = "";
  currentIndex.value = 0;
  currentResult.value = null;
  historyList.value = [];
  generatedQuestions.value = [];
  answerValue.value = "";

  try {
    const payload = {
      courseNo: courseNoText.value,
      courseName: courseNameText.value,
      weakPoints: sessionContext.value.weakPoints,
      history: sessionContext.value.wrongAnswers,
      choiceCount: 3,
      judgeCount: 3,
      blankCount: 3,
      essayCount: 1
    };
    const data = await fetchWeaknessTraining(payload);
    const questions = Array.isArray(data?.questions) ? data.questions.map(normalizeQuestion).filter(Boolean) : [];
    if (!questions.length) {
      throw new Error("AI 未生成训练题目");
    }
    generatedQuestions.value = questions;
  } catch (error) {
    errorMessage.value = error?.message || "训练题生成失败";
  } finally {
    generatingTraining.value = false;
  }
}

async function handleSubmit() {
  if (!currentQuestion.value) {
    return;
  }
  if (!normalize(answerValue.value)) {
    ElMessage.warning("请先作答");
    return;
  }

  evaluatingAnswer.value = true;
  try {
    const payload = {
      question: {
        type: currentQuestion.value.type,
        stem: currentQuestion.value.stem,
        options: currentQuestion.value.options,
        correctAnswer: currentQuestion.value.correctAnswer
      },
      userAnswer: normalize(answerValue.value)
    };
    const data = await fetchWeaknessEvaluation(payload);
    currentResult.value = {
      id: currentQuestion.value.id,
      stem: currentQuestion.value.stem,
      correct: Boolean(data?.correct),
      score: Number(data?.score || 0),
      userAnswer: normalize(answerValue.value),
      correctAnswer: normalize(data?.correctAnswer || currentQuestion.value.correctAnswer),
      explanation: normalize(data?.explanation || currentQuestion.value.explanation || "暂无解析")
    };
    historyList.value.push(currentResult.value);
    ElMessage[currentResult.value.correct ? "success" : "warning"](currentResult.value.correct ? "回答正确" : "回答错误");
  } catch (error) {
    ElMessage.error(error?.message || "AI 评估失败");
  } finally {
    evaluatingAnswer.value = false;
  }
}

function goNextQuestion() {
  if (currentIndex.value + 1 >= totalQuestionCount.value) {
    currentIndex.value = totalQuestionCount.value;
    currentResult.value = null;
    answerValue.value = "";
    return;
  }
  currentIndex.value += 1;
  currentResult.value = null;
  answerValue.value = "";
}

onMounted(() => {
  generateTraining();
});
</script>

<style scoped>
.weak-shell {
  display: grid;
  gap: 20px;
  width: 100%;
}

.weak-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 20px;
}

.weak-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.weak-eyebrow {
  font-size: 13px;
  color: var(--ne-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.weak-title {
  margin: 6px 0 4px;
  font-size: 28px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.weak-subtitle {
  margin: 0;
  color: var(--ne-text-muted);
}

.weak-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.weak-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;
  align-items: start;
}

.weak-main {
  display: grid;
  gap: 18px;
}

.weak-question-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.weak-question-index {
  font-size: 13px;
  color: var(--ne-text-muted);
}

.weak-question-type {
  margin-top: 6px;
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--ne-primary-soft);
  color: var(--ne-primary);
  font-size: 12px;
  font-weight: 600;
}

.weak-kp-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.weak-kp-tag {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(var(--ne-primary-rgb), 0.12);
  color: var(--ne-primary);
  font-size: 12px;
  border: 1px solid rgba(var(--ne-primary-rgb), 0.18);
}

.weak-question-title {
  margin: 0;
  font-size: 21px;
  line-height: 1.7;
  color: var(--ne-text-strong);
}

.weak-image-wrap {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
}

.weak-image-wrap img {
  width: 100%;
  display: block;
}

.weak-option-group {
  display: grid;
  gap: 12px;
  margin-bottom: 24px;
}

.weak-option-item {
  margin: 0;
  padding: 12px 14px;
  border-radius: var(--ne-radius-md);
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  transition: all 0.2s ease;
}

.weak-option-item:hover {
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.weak-option-item.is-checked {
  border-color: rgba(var(--ne-primary-rgb), 0.6);
  background: var(--ne-primary-soft);
}

.weak-option-item :deep(.el-radio__label) {
  white-space: normal;
  line-height: 1.6;
  color: var(--ne-text);
}

.weak-option-tag {
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

.weak-option-text {
  color: var(--ne-text);
}

.weak-submit {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.weak-result {
  border-radius: 16px;
  padding: 16px 18px;
  border: 1px solid;
}

.weak-result.is-correct {
  background: #f0fdf4;
  border-color: rgba(34, 197, 94, 0.18);
}

.weak-result.is-wrong {
  background: #fff5f5;
  border-color: rgba(225, 29, 72, 0.18);
}

.weak-result-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  color: var(--ne-text-strong);
}

.weak-result-status {
  display: grid;
  gap: 4px;
}

.weak-result-score {
  color: var(--ne-text-muted);
  font-size: 13px;
}

.weak-result-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.weak-result-badge.is-correct {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}

.weak-result-badge.is-wrong {
  background: rgba(225, 29, 72, 0.12);
  color: #e11d48;
}

.weak-result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.weak-result-card,
.weak-result-analysis {
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  padding: 14px 16px;
}

.weak-result-label {
  color: var(--ne-text-muted);
  font-size: 12px;
  margin-bottom: 8px;
}

.weak-result-value {
  color: var(--ne-text-strong);
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
}

.weak-result-analysis {
  margin-top: 12px;
}

.weak-result-explain {
  margin: 0;
  color: var(--ne-text);
  line-height: 1.7;
}

.weak-empty,
.weak-finish,
.weak-loading {
  min-height: 320px;
  display: grid;
  place-items: center;
  text-align: center;
}

.weak-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.weak-summary-card {
  border-radius: 16px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  padding: 16px;
  display: grid;
  gap: 8px;
}

.weak-summary-card span {
  color: var(--ne-text-muted);
  font-size: 13px;
}

.weak-summary-card strong {
  font-size: 26px;
  color: var(--ne-text-strong);
}

.weak-aside {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 20px;
}

.weak-side-card {
  display: grid;
  gap: 14px;
}

.weak-side-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.weak-side-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.weak-stat {
  border-radius: 14px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  padding: 12px;
  display: grid;
  gap: 6px;
}

.weak-stat span {
  color: var(--ne-text-muted);
  font-size: 12px;
}

.weak-stat strong {
  color: var(--ne-text-strong);
  font-size: 22px;
}

.weak-history-empty {
  color: var(--ne-text-muted);
  font-size: 13px;
}

.weak-history-list {
  display: grid;
  gap: 12px;
  max-height: 560px;
  overflow: auto;
}

.weak-history-item {
  border-radius: 14px;
  border: 1px solid var(--ne-border);
  padding: 12px;
  background: var(--ne-surface);
}

.weak-history-stem {
  font-size: 14px;
  color: var(--ne-text-strong);
  line-height: 1.6;
}

.weak-history-meta {
  display: grid;
  gap: 4px;
  margin-top: 8px;
  color: var(--ne-text-muted);
  font-size: 12px;
}

.weak-history-explain {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--ne-text);
  line-height: 1.6;
}

.badge-correct {
  color: #0f5132;
  background: #d1e7dd;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  width: fit-content;
}

.badge-wrong {
  color: #842029;
  background: #f8d7da;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
  width: fit-content;
}

.weak-error {
  margin: 0;
  color: #ef4444;
}

[data-theme="dark"] .weak-result.is-correct {
  background: rgba(22, 163, 74, 0.12);
  border-color: rgba(34, 197, 94, 0.28);
}

[data-theme="dark"] .weak-result.is-wrong {
  background: rgba(190, 24, 93, 0.14);
  border-color: rgba(244, 114, 182, 0.24);
}

[data-theme="dark"] .weak-result-card,
[data-theme="dark"] .weak-result-analysis {
  background: rgba(12, 16, 22, 0.24);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .weak-result-score,
[data-theme="dark"] .weak-result-label {
  color: #d5dbe5;
}

[data-theme="dark"] .weak-result-value,
[data-theme="dark"] .weak-result-explain,
[data-theme="dark"] .weak-result-head {
  color: #f8fafc;
}

@media (max-width: 960px) {
  .weak-body {
    grid-template-columns: 1fr;
  }

  .weak-aside {
    position: static;
  }
}

@media (max-width: 640px) {
  .weak-summary {
    grid-template-columns: 1fr;
  }

  .weak-result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
