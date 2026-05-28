<template>
  <section class="weak-shell">
    <div class="weak-container">
      <header class="weak-hero card">
        <div>
          <div class="weak-eyebrow">CAT 错题针对性训练</div>
          <h1 class="weak-title">AI 追踪薄弱点出题</h1>
          <p class="weak-subtitle">
            课程编号：{{ courseNoText }}
            <span v-if="courseNameText">｜课程名称：{{ courseNameText }}</span>
          </p>
        </div>
        <div class="weak-actions">
          <el-button type="default" @click="router.push({ name: 'home', query: { menu: 'cat' } })">
            返回首页
          </el-button>
        </div>
      </header>

      <section class="weak-body">
        <div class="weak-question card">
          <div v-if="loadingQuestion" class="weak-loading">
            <el-skeleton :rows="6" animated />
          </div>

          <div v-else-if="currentQuestion" class="weak-question-body">
            <div class="weak-question-head">
              <div class="weak-question-index">当前题目</div>
              <div class="weak-question-type">{{ currentTypeLabel }}</div>
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
                >
                  <span class="weak-option-tag">{{ optionLabel(index, currentQuestion.type) }}</span>
                  <span class="weak-option-text">{{ formatOptionText(option, currentQuestion.type) }}</span>
                </el-radio>
              </el-radio-group>
            </template>

            <template v-else>
              <el-input v-model="answerValue" class="weak-input" placeholder="请输入答案" clearable />
            </template>

            <div class="weak-submit">
              <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
                提交答案
              </el-button>
              <el-button v-if="canLoadNext" type="default" size="large" @click="loadNextQuestion">
                下一题
              </el-button>
            </div>
          </div>

          <div v-else class="weak-empty">
            <p>暂无题目，点击下方开始生成。</p>
            <el-button type="primary" size="large" @click="loadNextQuestion">开始训练</el-button>
          </div>

          <p v-if="errorMessage" class="weak-error">{{ errorMessage }}</p>
        </div>

        <aside class="weak-aside">
          <div class="card weak-history">
            <div class="weak-history-title">本次训练记录</div>
            <div v-if="!historyList.length" class="weak-history-empty">还没有作答记录</div>
            <div v-else class="weak-history-list">
              <div v-for="item in historyList" :key="item.id" class="weak-history-item">
                <div class="weak-history-main">
                  <div>
                    <div class="weak-history-stem">{{ item.stem }}</div>
                    <div class="weak-history-meta">
                      <span :class="item.correct ? 'badge-correct' : 'badge-wrong'">
                        {{ item.correct ? '正确' : '错误' }}
                      </span>
                      <span>你的答案：{{ formatAnswer(item.userAnswer) }}</span>
                      <span>正确答案：{{ formatAnswer(item.correctAnswer) }}</span>
                    </div>
                  </div>
                  <button
                    v-if="!item.correct"
                    type="button"
                    class="weak-explain-btn"
                    @click="toggleExplain(item)"
                  >
                    {{ item.showExplain ? '收起解析' : '解析' }}
                  </button>
                </div>
                <div v-if="item.showExplain" class="weak-explain">
                  <p v-if="item.explaining">AI 正在生成解析...</p>
                  <p v-else>{{ item.explanation || '暂无解析' }}</p>
                </div>
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
import { fetchWeaknessExplanation, fetchWeaknessQuestion } from "../services/aiWeaknessService";

const route = useRoute();
const router = useRouter();

const courseNoText = computed(() => String(route.query.courseNo || "").trim() || "-");
const courseNameText = computed(() => String(route.query.courseName || "").trim());

const AI_QUESTION_PROMPT = `你是 CAT 错题针对性训练的出题助手。请根据提供的薄弱知识点，生成 1 道新题。
要求：
1. 题型仅限 choice 或 judge。
2. 返回 JSON 格式，字段包含: id, type, stem, options, correctAnswer, knowledgePoints, imagePath。
3. options 为字符串数组，judge 题 options 必须是 ["true", "false"]。
4. correctAnswer 使用 options 原文或选项字母。`;

const AI_EXPLAIN_PROMPT = `你是 CAT 错题解析助手。根据题干、选项、正确答案与学生答案，给出简洁解析，说明错因并给出正确思路。`;

const loadingQuestion = ref(false);
const submitting = ref(false);
const currentQuestion = ref(null);
const answerValue = ref("");
const historyList = ref([]);
const errorMessage = ref("");
const canLoadNext = ref(false);

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

function formatAnswer(answer) {
  const text = String(answer || "").trim();
  return text || "未作答";
}

function normalizeImageSrc(path) {
  const text = String(path || "").trim();
  if (!text) return "";
  if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
    return text;
  }
  return `/${text}`;
}

function normalizeQuestion(raw) {
  if (!raw) return null;
  const options = Array.isArray(raw.options) ? raw.options : [];
  return {
    id: raw.id || `${Date.now()}`,
    type: raw.type || "choice",
    stem: raw.stem || "",
    options,
    correctAnswer: raw.correctAnswer || "",
    knowledgePoints: raw.knowledgePoints || [],
    imagePath: raw.imagePath || ""
  };
}

function collectWeakPoints() {
  return historyList.value
    .filter(item => !item.correct)
    .flatMap(item => item.knowledgePoints || [])
    .filter(Boolean);
}

async function loadNextQuestion() {
  if (loadingQuestion.value) return;
  loadingQuestion.value = true;
  errorMessage.value = "";
  canLoadNext.value = false;

  try {
    const payload = {
      prompt: AI_QUESTION_PROMPT,
      courseNo: courseNoText.value,
      courseName: courseNameText.value,
      weakPoints: collectWeakPoints(),
      history: historyList.value.map(item => ({
        id: item.id,
        stem: item.stem,
        correct: item.correct,
        userAnswer: item.userAnswer,
        correctAnswer: item.correctAnswer
      }))
    };
    const data = await fetchWeaknessQuestion(payload);
    currentQuestion.value = normalizeQuestion(data);
    answerValue.value = "";
  } catch (error) {
    errorMessage.value = error?.message || "加载题目失败";
  } finally {
    loadingQuestion.value = false;
  }
}

function matchAnswer(correctAnswer, selected, type) {
  const correct = String(correctAnswer || "").trim();
  const answer = String(selected || "").trim();
  if (!correct || !answer) return false;
  if (type === "judge") {
    return correct === answer || correct === (answer === "true" ? "T" : "F");
  }
  return correct === answer;
}

async function handleSubmit() {
  if (!currentQuestion.value) return;
  if (!answerValue.value) {
    ElMessage.warning("请先作答");
    return;
  }

  submitting.value = true;
  try {
    const correct = matchAnswer(
      currentQuestion.value.correctAnswer,
      answerValue.value,
      currentQuestion.value.type
    );

    historyList.value.unshift({
      id: currentQuestion.value.id,
      stem: currentQuestion.value.stem,
      correct,
      userAnswer: answerValue.value,
      correctAnswer: currentQuestion.value.correctAnswer,
      knowledgePoints: currentQuestion.value.knowledgePoints || [],
      explanation: currentQuestion.value.explanation || "",
      explaining: false,
      showExplain: false
    });

    ElMessage[correct ? "success" : "warning"](correct ? "回答正确" : "回答错误");
    canLoadNext.value = true;
  } finally {
    submitting.value = false;
  }
}

async function toggleExplain(item) {
  item.showExplain = !item.showExplain;
  if (!item.showExplain || item.explanation || item.explaining) {
    return;
  }

  item.explaining = true;
  try {
    const payload = {
      prompt: AI_EXPLAIN_PROMPT,
      question: {
        stem: item.stem,
        correctAnswer: item.correctAnswer,
        userAnswer: item.userAnswer
      }
    };
    const data = await fetchWeaknessExplanation(payload);
    item.explanation = String(data?.explanation || data?.text || "").trim();
  } catch (error) {
    item.explanation = error?.message || "解析生成失败";
  } finally {
    item.explaining = false;
  }
}

onMounted(() => {
  loadNextQuestion();
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
}

.weak-eyebrow {
  font-size: 13px;
  color: #7c6f6a;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.weak-title {
  margin: 6px 0 4px;
  font-size: 26px;
  font-weight: 700;
  color: #2c1d17;
}

.weak-subtitle {
  margin: 0;
  color: #6a5f59;
}

.weak-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.weak-question-body {
  display: grid;
  gap: 16px;
}

.weak-question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.weak-question-title {
  font-size: 20px;
  line-height: 1.6;
  margin: 0;
  color: #2c1d17;
}

.weak-option-group {
  display: grid;
  gap: 12px;
}

.weak-option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #ede7e4;
  background: #fffaf7;
}

.weak-option-tag {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #2c1d17;
  color: #fff;
  font-size: 13px;
}

.weak-input {
  width: 100%;
}

.weak-submit {
  display: flex;
  gap: 12px;
}

.weak-aside {
  position: sticky;
  top: 20px;
  align-self: start;
}

.weak-history-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.weak-history-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0e9e6;
}

.weak-history-item:last-child {
  border-bottom: none;
}

.weak-history-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.weak-history-stem {
  font-size: 14px;
  color: #2c1d17;
  margin-bottom: 6px;
}

.weak-history-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #6a5f59;
  font-size: 12px;
}

.badge-correct {
  color: #0f5132;
  background: #d1e7dd;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.badge-wrong {
  color: #842029;
  background: #f8d7da;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.weak-explain-btn {
  border: none;
  background: #f6ede8;
  color: #7a4635;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
}

.weak-explain {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff3ec;
  color: #5c3b2f;
  font-size: 12px;
  line-height: 1.5;
}

.weak-error {
  margin-top: 12px;
  color: #b42318;
}

@media (max-width: 960px) {
  .weak-body {
    grid-template-columns: 1fr;
  }

  .weak-aside {
    position: static;
  }
}
</style>
