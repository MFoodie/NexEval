<template>
  <section class="login-wrap">
    <div class="card login-panel">
      <div class="login-bg-container">
        <img class="login-bg-image" :src="bgUrl" alt="background" />
      </div>

      <div class="card login-card">
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

        <div class="login-hero-mini">
          <h1 class="card-title">重置密码</h1>
          <p class="card-subtitle">请输入您的新密码</p>
        </div>

        <el-form class="login-form" @submit.prevent="handleSubmit">
          <el-form-item label="新密码">
            <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>

          <el-form-item label="确认密码">
            <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>

          <el-button
            class="login-submit"
            type="primary"
            native-type="submit"
            :loading="submitting"
          >
            重置密码
          </el-button>
        </el-form>

        <p class="switch-line">
          <RouterLink to="/login">返回登录</RouterLink>
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import logoUrl from "../assets/nexeval.svg";
import bgUrl from "../assets/bg.png";

const router = useRouter();
const route = useRoute();
const newPassword = ref("");
const confirmPassword = ref("");
const submitting = ref(false);
const themeKey = "nexeval.theme";
const themeEvent = "nexeval-theme-change";
const themeOptions = [
  { value: "beige", label: "米白" },
  { value: "classic", label: "经典" },
  { value: "dark", label: "深色" }
];
const theme = ref("beige");
const suppressEmit = ref(false);

function applyTheme(value) {
  document.documentElement.setAttribute("data-theme", value);
}

function emitTheme(value) {
  window.dispatchEvent(new CustomEvent(themeEvent, { detail: value }));
}

function handleThemeEvent(event) {
  const value = event?.detail;
  if (!value || value === theme.value) return;
  suppressEmit.value = true;
  theme.value = value;
  window.setTimeout(() => { suppressEmit.value = false; }, 0);
}

async function handleSubmit() {
  if (!newPassword.value.trim()) {
    ElMessage.warning("请输入新密码");
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning("两次输入的密码不一致");
    return;
  }

  const token = route.query.token;
  if (!token) {
    ElMessage.error("缺少重置令牌，请从邮件链接进入");
    return;
  }

  submitting.value = true;
  try {
    const res = await fetch("/api/password-reset/reset", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, newPassword: newPassword.value.trim() })
    });
    const data = await res.json();
    if (data.success) {
      ElMessage.success("密码重置成功，请登录");
      router.push("/login");
    } else {
      ElMessage.error(data.message);
    }
  } catch {
    ElMessage.error("网络错误，请稍后重试");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(themeKey);
    if (saved && themeOptions.some(item => item.value === saved)) {
      theme.value = saved;
    }
  } catch { /* ignore */ }
  applyTheme(theme.value);
  window.addEventListener(themeEvent, handleThemeEvent);
});

watch(theme, (value) => {
  applyTheme(value);
  if (!suppressEmit.value) emitTheme(value);
  try { localStorage.setItem(themeKey, value); } catch { /* ignore */ }
});

onBeforeUnmount(() => {
  window.removeEventListener(themeEvent, handleThemeEvent);
});
</script>

<style scoped>
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

.login-wrap > * {
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
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes login-bg-breathe {
  from { transform: scale(1); }
  to { transform: scale(1.2); }
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
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-hero-mini {
  margin-top: 10px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 2px;
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

.login-logo {
  width: 84px;
  height: 44px;
  border-radius: 8px;
  object-fit: contain;
  object-position: left center;
  background: transparent;
  box-shadow: none;
  margin-right: 0px;
}

@media (max-width: 599px) {
  .login-logo {
    width: 44px;
    height: 44px;
    border-radius: 12px;
  }
}

.login-brand-text {
  display: flex;
  align-items: baseline;
  gap: 10px;
  white-space: nowrap;
  margin-left: -30px;
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

.login-form {
  display: grid;
  gap: 4px;
}

.login-submit {
  width: 96px;
  box-shadow: 0 10px 24px rgba(var(--ne-primary-rgb), 0.2);
  margin: 16px auto 0;
}

.switch-line {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--ne-text-muted);
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

@media (max-width: 640px) {
  .login-panel {
    width: 100%;
  }

  .login-card {
    padding: 20px;
  }
}
</style>
