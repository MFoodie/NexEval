<template>
  <section class="login-wrap">
    <div class="card login-card">
      <div class="login-brand">
        <img class="login-logo" :src="logoUrl" alt="NexEval Logo" />
        <div class="login-brand-text">
          <div class="login-brand-title">NexEval</div>
          <div class="login-brand-subtitle">智能评估系统</div>
        </div>
      </div>
      <h1 class="card-title">考生登录</h1>
      <p class="card-subtitle">支持卡号、手机号或邮箱 + 密码登录</p>

      <p class="ws-line">
        WebSocket:
        <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
      </p>

      <el-form @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="account" placeholder="请输入卡号/手机号/邮箱" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <el-button type="primary" :loading="submitting" @click="handleLogin">登录</el-button>
      </el-form>
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

const router = useRouter();
const account = ref("");
const password = ref("");
const submitting = ref(false);
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

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.login-wrap {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
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
  display: grid;
}

.login-brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.3px;
}

.login-brand-subtitle {
  font-size: 12px;
  color: #64748b;
}

.login-card {
  width: min(520px, 100%);
}

.ws-line {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 14px;
}
</style>
