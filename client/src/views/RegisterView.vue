<template>
  <section class="login-wrap">
    <div
      v-if="showBoot"
      class="boot-overlay fixed top-0 left-0 w-screen h-screen z-9999"
      aria-hidden="true"
      @animationend="handleBootAnimationEnd"
    >
      <div class="boot-meta boot-meta--tl">SYS.INIT // V2.0.4</div>
      <div class="boot-meta boot-meta--br">SECURE_NODE: ACTIVE</div>
      <div class="boot-center flex flex-col items-center justify-center">
        <div class="boot-text text-3xl md:text-5xl font-medium tracking-wide text-gray-900">
          <span class="boot-line">Assess. Smarter._</span>
          <span class="boot-cursor">_</span>
        </div>
        <div class="boot-subtext mt-5 text-sm md:text-base text-gray-500 font-mono tracking-wider opacity-0">
          Delivering fair, fast, and data-driven insights.
        </div>
        <div class="boot-progress mt-6 h-2px bg-terracotta origin-center"></div>
      </div>
    </div>
    <div class="card login-panel">
      <div class="login-bg-container">
        <img class="login-bg-image" :src="currentBgUrl" alt="background" />
        <div class="login-bg-dots" aria-label="背景切换指示器">
          <span
            v-for="(item, index) in bgImages"
            :key="item"
            class="login-bg-dot"
            :class="{ active: currentBgIndex === index }"
          />
        </div>
      </div>
      <div class="login-card">
        <div class="login-card-head">
          <div class="login-brand">
            <img class="login-logo" :src="logoUrl" alt="NexEval Logo" />
            <div class="login-brand-text">
              <div class="login-brand-title">NexEval</div>
              <div class="login-brand-subtitle">智能评估系统</div>
            </div>
          </div>
          <div class="login-theme">
            <div class="theme-switch" aria-label="主题切换">
              <span class="theme-switch-label">主题</span>
              <div class="theme-pill-group" role="group">
                <button
                  v-for="item in themeOptions"
                  :key="item.value"
                  type="button"
                  class="theme-pill"
                  :class="{ 'is-active': theme === item.value }"
                  @click="theme = item.value"
                >
                  <span class="theme-pill-icon" aria-hidden="true">
                    <svg v-if="item.value === 'beige'" viewBox="0 0 24 24">
                      <path d="M12 4.5a1 1 0 0 1 1 1V7a1 1 0 1 1-2 0V5.5a1 1 0 0 1 1-1Zm0 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm7-4a1 1 0 0 1 1 1 1 1 0 0 1-1 1h-1.5a1 1 0 1 1 0-2H19ZM6.5 12.5a1 1 0 0 1-1 1H4a1 1 0 1 1 0-2h1.5a1 1 0 0 1 1 1Zm9.19-6.69a1 1 0 0 1 1.42 0l1.06 1.06a1 1 0 0 1-1.41 1.41l-1.07-1.06a1 1 0 0 1 0-1.41ZM6.92 15.5a1 1 0 0 1 1.41 0l1.07 1.06a1 1 0 0 1-1.42 1.41l-1.06-1.06a1 1 0 0 1 0-1.41Zm9.19 1.06a1 1 0 0 1 0 1.41l-1.06 1.06a1 1 0 1 1-1.42-1.41l1.07-1.06a1 1 0 0 1 1.41 0ZM7.98 7.56a1 1 0 0 1 0 1.41L6.92 10.03a1 1 0 0 1-1.41-1.42L6.56 7.56a1 1 0 0 1 1.42 0Z" />
                    </svg>
                    <svg v-else-if="item.value === 'classic'" viewBox="0 0 24 24">
                      <path d="M6 12a6 6 0 0 1 9.79-4.65 1 1 0 1 1-1.32 1.5A4 4 0 1 0 16 12a4 4 0 0 0-1.53-3.15 1 1 0 0 1 1.23-1.58A6 6 0 0 1 18 12c0 3.31-2.69 6-6 6s-6-2.69-6-6Zm10 1a1 1 0 1 1 0-2h4a1 1 0 1 1 0 2h-4Z" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24">
                      <path d="M14.5 3a1 1 0 0 1 1 1 7.5 7.5 0 0 0 7.5 7.5 1 1 0 0 1 1 1 9.5 9.5 0 1 1-9.5-9.5Z" />
                    </svg>
                  </span>
                  <span>{{ item.label }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>

      <h1 class="card-title">用户注册</h1>
      <p class="card-subtitle">注册新用户账号</p>

      <el-form class="register-form" @submit.prevent>
        <div class="form-grid form-grid--two">
          <el-form-item label="卡号">
            <el-input v-model="form.id" placeholder="9位卡号" maxlength="9" />
          </el-form-item>

          <el-form-item label="姓名">
            <el-input v-model="form.name" placeholder="请输入姓名" />
          </el-form-item>
        </div>

        <div class="form-grid form-grid--two">
          <el-form-item label="性别">
            <el-radio-group v-model="form.sex" class="sex-radio-group">
              <el-radio value="男" class="sex-radio sex-radio--male">男</el-radio>
              <el-radio value="女" class="sex-radio sex-radio--female">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="用户类型">
            <el-select v-model="form.type" placeholder="请选择用户类型" style="width:100%">
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
            </el-select>
          </el-form-item>
        </div>

        <template v-if="form.type === 'student'">
          <div class="form-grid form-grid--two">
            <el-form-item label="学号">
              <el-input v-model="form.sno" placeholder="请输入学号" />
            </el-form-item>
            <el-form-item label="入学年份">
              <el-input v-model="form.studentEnterYear" placeholder="如 2024" />
            </el-form-item>
          </div>
          <div class="form-grid form-grid--two">
            <el-form-item label="专业">
              <el-input v-model="form.major" placeholder="请输入专业" />
            </el-form-item>
            <el-form-item label="院系">
              <el-input v-model="form.studentDepartment" placeholder="请输入院系" />
            </el-form-item>
          </div>
        </template>

        <template v-else-if="form.type === 'teacher'">
          <div class="form-grid form-grid--two">
            <el-form-item label="工号">
              <el-input v-model="form.eid" placeholder="请输入工号" />
            </el-form-item>
            <el-form-item label="入职年份">
              <el-input v-model="form.teacherEnterYear" placeholder="如 2020" />
            </el-form-item>
          </div>
          <div class="form-grid form-grid--two">
            <el-form-item label="职称">
              <el-input v-model="form.title" placeholder="如 教授、副教授、讲师" />
            </el-form-item>
            <el-form-item label="院系">
              <el-input v-model="form.teacherDepartment" placeholder="请输入院系" />
            </el-form-item>
          </div>
        </template>

        <el-button class="login-submit" type="primary" :loading="submitting" @click="handleRegister">注册</el-button>

        <p class="switch-line">
          已有账号？<RouterLink to="/login">去登录</RouterLink>
        </p>
      </el-form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";
import logoUrl from "../assets/logo.png";
import bgUrl from "../assets/bg.png";
import bg1Url from "../assets/bg1.png";
import bg2Url from "../assets/bg2.png";
import bg3Url from "../assets/bg3.png";

const router = useRouter();
const submitting = ref(false);
const bgImages = [bgUrl, bg1Url, bg2Url, bg3Url];
const currentBgIndex = ref(0);
const currentBgUrl = computed(() => bgImages[currentBgIndex.value]);
let bgTimer = null;
const wsStatus = ref("connecting");
const themeKey = "nexeval.theme";
const themeEvent = "nexeval-theme-change";
const bootSeenKey = "nexeval.boot.seen";
const themeOptions = [
  { value: "beige", label: "米白" },
  { value: "classic", label: "经典" },
  { value: "dark", label: "深色" }
];
const theme = ref("beige");
const suppressEmit = ref(false);
const showBoot = ref(false);
const wsTagType = computed(() => {
  if (wsStatus.value === "connected") return "success";
  if (wsStatus.value === "error") return "danger";
  return "info";
});

const form = reactive({
  id: "",
  name: "",
  sex: "男",
  type: "",
  sno: "",
  studentEnterYear: "",
  major: "",
  studentDepartment: "",
  eid: "",
  teacherEnterYear: "",
  title: "",
  teacherDepartment: ""
});

let wsClient = null;

function connectWebSocket() {
  wsClient = createExamSocket(null, {
    onOpen() { wsStatus.value = "connected"; },
    onClose() { wsStatus.value = "closed"; },
    onError() { wsStatus.value = "error"; }
  });
}

function applyTheme(value) {
  document.documentElement.setAttribute("data-theme", value);
}

function emitTheme(value) {
  window.dispatchEvent(new CustomEvent(themeEvent, { detail: value }));
}

function handleThemeEvent(event) {
  const value = event?.detail;
  if (!value || value === theme.value) {
    return;
  }
  suppressEmit.value = true;
  theme.value = value;
  window.setTimeout(() => {
    suppressEmit.value = false;
  }, 0);
}

function handleBootAnimationEnd(event) {
  if (event?.animationName === "boot-depth-fade") {
    showBoot.value = false;
  }
}

async function handleRegister() {
  if (!form.id.trim() || !form.name.trim() || !form.type) {
    ElMessage.warning("请填写卡号、姓名和用户类型");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试");
    return;
  }

  submitting.value = true;

  try {
    await wsClient.request("REGISTER_USER", {
      id: form.id.trim(),
      name: form.name.trim(),
      sex: form.sex,
      type: form.type,
      sno: form.type === "student" ? form.sno.trim() : "",
      studentEnterYear: form.type === "student" ? form.studentEnterYear.trim() : "",
      major: form.type === "student" ? form.major.trim() : "",
      studentDepartment: form.type === "student" ? form.studentDepartment.trim() : "",
      eid: form.type === "teacher" ? form.eid.trim() : "",
      teacherEnterYear: form.type === "teacher" ? form.teacherEnterYear.trim() : "",
      title: form.type === "teacher" ? form.title.trim() : "",
      teacherDepartment: form.type === "teacher" ? form.teacherDepartment.trim() : ""
    });

    ElMessage.success("注册成功，请登录");
    router.push("/login");
  } catch (error) {
    ElMessage.error(error.message || "注册失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(connectWebSocket);
onMounted(() => {
  try {
    const seen = sessionStorage.getItem(bootSeenKey);
    showBoot.value = !seen;
    if (!seen) {
      sessionStorage.setItem(bootSeenKey, "1");
    }
  } catch {
    showBoot.value = true;
  }

  try {
    const saved = localStorage.getItem(themeKey);
    if (saved && themeOptions.some((item) => item.value === saved)) {
      theme.value = saved;
    }
  } catch {
    // ignore storage read errors
  }

  applyTheme(theme.value);
  window.addEventListener(themeEvent, handleThemeEvent);
  bgTimer = window.setInterval(() => {
    currentBgIndex.value = (currentBgIndex.value + 1) % bgImages.length;
  }, 3000);
});

watch(theme, (value) => {
  applyTheme(value);
  if (!suppressEmit.value) {
    emitTheme(value);
  }
  try {
    localStorage.setItem(themeKey, value);
  } catch {
    // ignore storage write errors
  }
});

onBeforeUnmount(() => {
  if (bgTimer) {
    window.clearInterval(bgTimer);
    bgTimer = null;
  }
  window.removeEventListener(themeEvent, handleThemeEvent);
  wsClient?.close();
});
</script>

<style scoped>
.boot-overlay {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: radial-gradient(1200px 600px at 50% 40%, rgba(255, 255, 255, 0.7), rgba(253, 252, 248, 0.98)),
    #fdfcf8;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  animation: boot-depth-fade 1.2s ease forwards;
  animation-delay: 2.4s;
}

.boot-text {
  width: 100%;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  font-family: "JetBrains Mono", "Space Grotesk", "Manrope", monospace;
  font-size: clamp(30px, 4.6vw, 48px);
  font-weight: 500;
  letter-spacing: 0.08em;
  color: #111111;
}

.boot-center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.boot-meta {
  position: absolute;
  font-family: "JetBrains Mono", "Space Grotesk", "Manrope", monospace;
  font-size: 10px;
  letter-spacing: 0.2em;
  color: rgba(45, 45, 45, 0.5);
}

.boot-meta--tl {
  top: 32px;
  left: 32px;
}

.boot-meta--br {
  bottom: 32px;
  right: 32px;
}

.boot-subtext {
  margin-top: 20px;
  font-size: clamp(13px, 2vw, 16px);
  font-family: "JetBrains Mono", "Space Grotesk", "Manrope", monospace;
  color: #6b6b6b;
  opacity: 0;
  animation: boot-fade-in 0.7s ease forwards 0.3s, boot-pulse 1.6s ease-in-out infinite 1.1s;
}

.boot-progress {
  height: 2px;
  background: #cf7357;
  margin-top: 24px;
  width: 160px;
  transform: scaleX(0);
  transform-origin: center;
  animation: boot-expand 1.6s ease-out forwards;
}

.boot-line {
  display: inline-block;
  overflow: hidden;
  white-space: nowrap;
  border-right: 3px solid #cf7357;
  animation: boot-type 1.6s steps(18, end) forwards;
}

.boot-cursor {
  display: inline-block;
  color: #cf7357;
  animation: boot-blink 0.8s steps(1, end) infinite;
}

@keyframes boot-type {
  from {
    width: 0;
  }
  to {
    width: 18ch;
  }
}

@keyframes boot-blink {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0;
  }
}

@keyframes boot-depth-fade {
  0% {
    transform: scale(1);
    opacity: 1;
    filter: blur(0);
  }
  100% {
    transform: scale(1.04);
    opacity: 0;
    filter: blur(10px);
    visibility: hidden;
    pointer-events: none;
  }
}

@keyframes boot-expand {
  to {
    transform: scaleX(1);
  }
}

@keyframes boot-fade-in {
  to {
    opacity: 1;
  }
}

.text-3xl {
  font-size: clamp(30px, 4.6vw, 48px);
}

.md\:text-5xl {
  font-size: clamp(36px, 5.6vw, 56px);
}

.font-medium {
  font-weight: 500;
}

.tracking-wide {
  letter-spacing: 0.08em;
}

.text-gray-900 {
  color: #111111;
}

.mt-5 {
  margin-top: 20px;
}

.text-sm {
  font-size: 14px;
}

.md\:text-base {
  font-size: 16px;
}

.text-gray-500 {
  color: #6b6b6b;
}

.font-mono {
  font-family: "JetBrains Mono", "Space Grotesk", "Manrope", monospace;
}

.tracking-wider {
  letter-spacing: 0.12em;
}

.opacity-0 {
  opacity: 0;
}

.mt-6 {
  margin-top: 24px;
}

.h-2px {
  height: 2px;
}

.bg-terracotta {
  background: #cf7357;
}

.origin-center {
  transform-origin: center;
}

.items-center {
  align-items: center;
}

.justify-center {
  justify-content: center;
}

@keyframes boot-pulse {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}


.login-wrap {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 24px 28px;
  gap: 40px;
  position: relative;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

.login-wrap::before {
  content: "";
  position: fixed;
  inset: 0;
  background: url("../assets/background.jpg") center/cover no-repeat fixed;
  filter: blur(3px);
  transform: scale(1);
  transform-origin: center center;
  animation: login-bg-breathe 36s ease-in-out infinite alternate;
  z-index: 0;
}

.login-wrap::after {
  content: "";
  position: fixed;
  inset: 0;
  background: var(--ne-login-overlay);
  z-index: 0;
}

.login-wrap > *:not(.boot-overlay) {
  position: relative;
  z-index: 1;
}

.login-panel {
  width: min(980px, calc(100% - 32px));
  display: grid;
  grid-template-columns: minmax(0, 400px) minmax(0, 1fr);
  gap: 40px;
  padding: 24px;
  border-radius: 18px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  box-shadow: var(--ne-shadow);
  align-items: center;
  animation: login-panel-enter 0.5s ease-out both;
}

@keyframes login-panel-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes login-bg-breathe {
  from {
    transform: scale(1);
  }

  to {
    transform: scale(1.2);
  }
}

.login-bg-container {
  display: none;
  flex: 0 0 auto;
  width: 400px;
  height: 480px;
  overflow: hidden;
  position: relative;
  border-radius: 14px;
  border: 1px solid var(--ne-border);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14);
}

.login-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-bg-dots {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.login-bg-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.45);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.18);
  transition: transform 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.login-bg-dot.active {
  background: #ffffff;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.32), 0 4px 10px rgba(0, 0, 0, 0.14);
  transform: scale(1.15);
}

@media (min-width: 1200px) {
  .login-wrap {
    justify-content: center;
    padding-left: 24px;
    padding-right: 24px;
  }

  .login-bg-container {
    display: flex;
  }
}

@media (max-width: 1199px) {
  .login-panel {
    grid-template-columns: 1fr;
  }
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.login-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.login-theme {
  display: flex;
  align-items: center;
}

.login-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: contain;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 24, 39, 0.12);
}

.login-brand-text {
  display: flex;
  align-items: baseline;
  gap: 10px;
  white-space: nowrap;
}

.login-brand-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--ne-text-strong);
  letter-spacing: 0.2px;
}

.login-brand-subtitle {
  font-size: 14px;
  color: var(--ne-text-muted);
}

.login-card {
  padding: 0;
  border: none;
  box-shadow: none;
  background: transparent;
}

.switch-line {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--ne-text-muted);
}

.register-form {
  display: grid;
  gap: 6px;
}

.form-grid {
  display: grid;
  gap: 6px;
}

.form-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.sex-radio-group {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 40px;
}

.sex-radio {
  margin-right: 0;
}

.sex-radio--male :deep(.el-radio__inner) {
  border-color: var(--ne-primary);
}

.sex-radio--male :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--ne-primary);
  background: var(--ne-primary);
}

.sex-radio--male :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: var(--ne-primary);
}

.sex-radio--female :deep(.el-radio__inner) {
  border-color: var(--ne-accent);
}

.sex-radio--female :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--ne-accent);
  background: var(--ne-accent);
}

.sex-radio--female :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: var(--ne-accent);
}

.login-submit {
  width: 96px;
  box-shadow: 0 10px 24px rgba(var(--ne-primary-rgb), 0.2);
  margin: 12px auto 0;
}

/* Ensure form controls inside the two-column grid align vertically with their labels */
.form-grid .el-form-item__content {
  display: flex;
  align-items: center;
  min-height: 40px;
}

/* Make the first row (卡号 / 姓名) have a slightly narrower left column so 卡号 input is shorter */
.register-form > .form-grid.form-grid--two:first-child {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

/* Nudge the labels in the second row (性别 / 用户类型) slightly down */
.register-form > .form-grid.form-grid--two:nth-child(2) .el-form-item__label {
  padding-top: 6px;
}

/* Tighten label spacing so the two text boxes sit closer together */
.register-form .el-form-item__label {
  padding-right: 4px;
  min-width: 48px;
}

/* Only shorten the 卡号 input itself so 姓名 stays in place */
.register-form > .form-grid.form-grid--two:first-child .el-form-item:first-child :deep(.el-input) {
  max-width: 220px;
}

.register-form > .form-grid.form-grid--two:first-child .el-form-item:first-child :deep(.el-input__wrapper) {
  width: 100%;
}

@media (max-width: 640px) {
  .login-panel {
    width: 100%;
  }

  .login-card {
    width: 100%;
    min-height: auto;
    padding: 20px;
  }

  .form-grid--two {
    grid-template-columns: 1fr;
  }
}
</style>
