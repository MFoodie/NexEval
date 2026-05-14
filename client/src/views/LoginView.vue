<template>
  <section class="login-wrap">
    <div class="login-bg-container">
      <img class="login-bg-image" :src="currentBgUrl" alt="background" />
      <div class="login-bg-dots" aria-label="背景切换指示器">
        <span
         v-for="(item, index) in bgImages"
        :key="item"
        class="login-bg-dot"
        :class="{ active: currentBgIndex === index }"
        role="button"
        :aria-label="`切换到第${index + 1}张背景图`"
        @click="switchBg(index)"
      />
      </div>
    </div>
    <div class="card login-card">
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
      <div class="login-brand">
        <img class="login-logo" :src="logoUrl" alt="NexEval Logo" />
        <div class="login-brand-text">
          <div class="login-brand-title">NexEval</div>
          <div class="login-brand-subtitle">智能评估系统</div>
        </div>
      </div>

      <div class="login-hero-mini">
        <h1 class="card-title">考生登录</h1>
        <p class="card-subtitle">支持卡号、手机号或邮箱 + 密码登录</p>
      </div>

      <div class="login-note">
        <span class="login-note-label">提示</span>
        <span class="login-note-text">登录后即可进入课程练习、正式考试、成绩复核与教师批改流程。</span>
      </div>

      <el-form class="login-form" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="account" placeholder="请输入卡号/手机号/邮箱" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button class="login-submit" type="primary" native-type="submit" :loading="submitting">登录</el-button>
      </el-form>
      <p class="switch-line">
        没有账号？<RouterLink to="/register">去注册</RouterLink>
      </p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { saveLogin } from "../auth";
import { createExamSocket } from "../ws";
import logoUrl from "../assets/logo.png";
import bgUrl from "../assets/bg.png";
import bg1Url from "../assets/bg1.png";
import bg2Url from "../assets/bg2.png";
import bg3Url from "../assets/bg3.png";

const router = useRouter();
const account = ref("");
const password = ref("");
const submitting = ref(false);
const bgImages = [bgUrl, bg1Url, bg2Url, bg3Url];
const currentBgIndex = ref(0);
const currentBgUrl = computed(() => bgImages[currentBgIndex.value]);
let bgTimer = null;
const wsStatus = ref("connecting");
const wsTagType = computed(() => {
  if (wsStatus.value === "connected") {
    return "success";
  }

  if (wsStatus.value === "error") {
    return "danger";
  }

  return "info";
});
let wsClient = null;

function connectWebSocket() {
  wsClient = createExamSocket(null, {
    onOpen() {
      wsStatus.value = "connected";
    },
    onClose() {
      wsStatus.value = "closed";
    },
    onError() {
      wsStatus.value = "error";
    }
  });
}

function startBackgroundRotation() {
  bgTimer = window.setInterval(() => {
    currentBgIndex.value = (currentBgIndex.value + 1) % bgImages.length;
  }, 3000);
}

function switchBg(index) {
  currentBgIndex.value = index;
  if (bgTimer) {
    window.clearInterval(bgTimer);
  }
  startBackgroundRotation();
}


async function handleLogin() {
  if (!account.value.trim() || !password.value.trim()) {
    ElMessage.warning("请先输入账号和密码");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后重试");
    return;
  }

  submitting.value = true;

  try {
    const profile = await wsClient.request("LOGIN", {
      account: account.value.trim(),
      password: password.value.trim()
    });

    saveLogin(profile);
    ElMessage.success("登录成功");
    if (profile.type === "admin") {
      router.push("/admin");
    } else {
      router.push("/");
    }
  } catch (error) {
    ElMessage.error(error.message || "登录失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(connectWebSocket);
onMounted(() => {
  bgTimer = window.setInterval(() => {
    currentBgIndex.value = (currentBgIndex.value + 1) % bgImages.length;
  }, 3000);
});

onBeforeUnmount(() => {
  if (bgTimer) {
    window.clearInterval(bgTimer);
    bgTimer = null;
  }
  wsClient?.close();
});
</script>

<style scoped>
.login-wrap {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0 28px;
  gap: 40px;
  position: relative;
  overflow: hidden;
}

.login-wrap::before {
  content: "";
  position: fixed;
  inset: 0;
  background: url("../assets/background.jpg") center/cover no-repeat fixed;
  filter: blur(3px);
  transform: scale(1.04);
  z-index: 0;
}

.login-wrap::after {
  content: "";
  position: fixed;
  inset: 0;
  background: rgba(246, 248, 252, 0.58);
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
  border: 1px solid rgba(42, 92, 255, 0.14);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.18), 0 1px 0 rgba(255, 255, 255, 0.7) inset;
  align-items: center;
}

.login-bg-container {
  display: none;
  flex: 0 0 auto;
  width: 400px;
  height: 480px;
  overflow: hidden;
  position: relative;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
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
  cursor: pointer;
}

.login-bg-dot:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: scale(1.15);
}


.login-bg-dot.active {
  background: #ffffff;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.32), 0 4px 10px rgba(0, 0, 0, 0.14);
  transform: scale(1.15);
}


@media (min-width: 1200px) {
  .login-wrap {
    justify-content: flex-start;
    padding-left: 60px;
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

.login-hero-mini {
  margin-top: 10px;
}

.login-note {
  margin: 10px 0 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(42, 92, 255, 0.05), rgba(0, 194, 255, 0.04));
  border: 1px solid rgba(42, 92, 255, 0.1);
  color: var(--ne-text-muted);
  font-size: 13px;
  line-height: 1.7;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.login-note-label {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(42, 92, 255, 0.1);
  color: var(--ne-primary);
  font-size: 12px;
  font-weight: 600;
}

.login-note-text {
  flex: 1;
  padding-top: 1px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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
  color: #0f172a;
  letter-spacing: 0.2px;
}

.login-brand-subtitle {
  font-size: 14px;
  color: #64748b;
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
  box-shadow: 0 10px 24px rgba(42, 92, 255, 0.2);
  margin: 16px auto 0;
}

.switch-line {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 640px) {
  .login-panel {
    width: 100%;
  }

  .login-card {
    padding: 20px;
  }

  .hero-title {
    font-size: 26px;
  }

  .login-card {
    width: 100%;
  }
}
</style>
