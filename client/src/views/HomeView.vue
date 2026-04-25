<template>
  <section>
    <div class="dashboard-head card">
      <h1 class="card-title">在线考试系统</h1>
      <p class="card-subtitle">请先确认个人信息，再进入考试。</p>
    </div>

    <div class="dashboard-grid">
      <section class="card panel-card">
        <h2 class="panel-title">个人信息</h2>
        <div class="avatar-wrap">
          <div class="avatar-click" @click="triggerAvatarPicker">
            <img :src="avatarUrl" alt="默认头像" class="avatar-image" />
            <div class="avatar-tip">点击修改头像</div>
          </div>
          <input
            ref="avatarInputRef"
            class="avatar-input"
            type="file"
            accept="image/png,image/jpeg"
            @change="handleAvatarFileChange"
          />
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="卡号">{{ cardNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userName }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ sexText }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ email }}</el-descriptions-item>
          <template v-if="isStudent && studentInfo">
            <el-descriptions-item label="学号">{{ studentInfo.sno || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ studentInfo.enterYear || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ studentInfo.major || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学院">{{ studentInfo.department || '-' }}</el-descriptions-item>
          </template>
          <template v-if="isTeacher && teacherInfo">
            <el-descriptions-item label="工号">{{ teacherInfo.eid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入职年份">{{ teacherInfo.enterYear || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职称">{{ teacherInfo.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学院">{{ teacherInfo.department || '-' }}</el-descriptions-item>
          </template>
        </el-descriptions>

        <div class="action-row">
          <el-button :loading="avatarSaving" @click="handleResetAvatar">恢复默认头像</el-button>
          <el-button type="primary" plain @click="openEditDialog">修改信息</el-button>
          <el-button text type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </section>

      <section class="card panel-card">
        <h2 class="panel-title">进入考试</h2>
        <p class="ws-line">
          WebSocket:
          <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
        </p>

        <el-form @submit.prevent>
          <el-form-item label="考试账号">
            <el-input v-model="userId" disabled />
          </el-form-item>

          <el-button type="primary" :loading="starting" @click="handleStartExam">进入考试</el-button>
        </el-form>
      </section>
    </div>

    <el-dialog v-model="editVisible" title="修改个人信息" width="520px">
      <el-form label-position="top">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="新密码（不修改可留空）">
          <el-input v-model="editForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          <div class="pwd-strength">
            密码强度：
            <el-tag size="small" :type="passwordStrengthTagType">{{ passwordStrengthText }}</el-tag>
          </div>
          <p class="pwd-hint">规则：大写、小写、数字、特殊符号中至少满足三种。</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { clearLogin, getLogin, saveLogin } from "../auth";
import { createExamSocket } from "../ws";

const router = useRouter();
const loginInfo = getLogin();

const cardNo = ref(loginInfo?.cardNo || "-");
const userName = ref(loginInfo?.name || "-");
const sex = ref(typeof loginInfo?.sex === "boolean" ? loginInfo.sex : null);
const userType = ref(loginInfo?.type || "");
const phone = ref(loginInfo?.phone || "-");
const email = ref(loginInfo?.email || "-");
const studentInfo = ref(loginInfo?.studentInfo || null);
const teacherInfo = ref(loginInfo?.teacherInfo || null);
const avatarUrl = ref(withAvatarVersion(loginInfo?.avatarUrl || "/avatar/student_male.png"));
const userId = ref(loginInfo?.cardNo || "");
const starting = ref(false);
const avatarSaving = ref(false);
const wsStatus = ref("connecting");
const avatarInputRef = ref(null);
const sexText = computed(() => {
  if (sex.value === true) {
    return "男";
  }

  if (sex.value === false) {
    return "女";
  }

  return "-";
});
const wsTagType = computed(() => {
  if (wsStatus.value === "connected") {
    return "success";
  }

  if (wsStatus.value === "error") {
    return "danger";
  }

  return "info";
});
const isStudent = computed(() => userType.value === "student");
const isTeacher = computed(() => userType.value === "teacher");

const editVisible = ref(false);
const saving = ref(false);
const editForm = ref({
  name: "",
  phone: "",
  email: "",
  newPassword: ""
});

const passwordStrengthScore = computed(() => {
  const value = editForm.value.newPassword || "";
  let score = 0;

  if (/[A-Z]/.test(value)) score++;
  if (/[a-z]/.test(value)) score++;
  if (/\d/.test(value)) score++;
  if (/[^A-Za-z0-9]/.test(value)) score++;

  return score;
});

const passwordStrengthText = computed(() => {
  if (!editForm.value.newPassword) {
    return "未设置";
  }

  if (passwordStrengthScore.value <= 1) {
    return "弱";
  }

  if (passwordStrengthScore.value === 2) {
    return "中";
  }

  return "强";
});

const passwordStrengthTagType = computed(() => {
  if (!editForm.value.newPassword) {
    return "info";
  }

  if (passwordStrengthScore.value <= 1) {
    return "danger";
  }

  if (passwordStrengthScore.value === 2) {
    return "warning";
  }

  return "success";
});

let wsClient = null;

function withAvatarVersion(url) {
  if (!url) {
    return "/avatar/student_male.png";
  }

  const divider = url.includes("?") ? "&" : "?";
  return `${url}${divider}t=${Date.now()}`;
}

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

function handleLogout() {
  clearLogin();
  router.push("/login");
}

function triggerAvatarPicker() {
  avatarInputRef.value?.click();
}

async function handleAvatarFileChange(event) {
  const file = event?.target?.files?.[0];
  event.target.value = "";

  if (!file) {
    return;
  }

  if (!["image/png", "image/jpeg"].includes(file.type)) {
    ElMessage.warning("仅支持 JPG 或 PNG 图片");
    return;
  }

  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning("图片不能超过 8MB");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  avatarSaving.value = true;
  try {
    const circlePngDataUrl = await cropImageToCirclePng(file, 256);
    const profile = await wsClient.request("UPDATE_AVATAR", {
      userId: cardNo.value,
      imageBase64: circlePngDataUrl
    }, 30000);

    applyProfile(profile);
    ElMessage.success("头像已更新");
  } catch (error) {
    ElMessage.error(error.message || "头像更新失败");
  } finally {
    avatarSaving.value = false;
  }
}

async function handleResetAvatar() {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  avatarSaving.value = true;
  try {
    const profile = await wsClient.request("RESET_AVATAR", {
      userId: cardNo.value
    });

    applyProfile(profile);
    ElMessage.success("已恢复默认头像");
  } catch (error) {
    ElMessage.error(error.message || "恢复默认头像失败");
  } finally {
    avatarSaving.value = false;
  }
}

function cropImageToCirclePng(file, size = 256) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onerror = () => reject(new Error("读取图片失败"));
    reader.onload = () => {
      const image = new Image();

      image.onerror = () => reject(new Error("解析图片失败"));
      image.onload = () => {
        const canvas = document.createElement("canvas");
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext("2d");

        if (!ctx) {
          reject(new Error("浏览器不支持 Canvas"));
          return;
        }

        const srcSize = Math.min(image.width, image.height);
        const sx = (image.width - srcSize) / 2;
        const sy = (image.height - srcSize) / 2;

        ctx.clearRect(0, 0, size, size);
        ctx.beginPath();
        ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
        ctx.closePath();
        ctx.clip();
        ctx.drawImage(image, sx, sy, srcSize, srcSize, 0, 0, size, size);

        resolve(canvas.toDataURL("image/png"));
      };

      image.src = String(reader.result || "");
    };

    reader.readAsDataURL(file);
  });
}

function applyProfile(profile) {
  userName.value = profile.name || "-";
  phone.value = profile.phone || "-";
  email.value = profile.email || "-";
  userType.value = profile.type || userType.value;
  sex.value = typeof profile.sex === "boolean" ? profile.sex : sex.value;
  studentInfo.value = profile.studentInfo || null;
  teacherInfo.value = profile.teacherInfo || null;
  avatarUrl.value = withAvatarVersion(profile.avatarUrl || avatarUrl.value);
  saveLogin(profile);
}

function openEditDialog() {
  editForm.value = {
    name: userName.value === "-" ? "" : userName.value,
    phone: phone.value === "-" ? "" : phone.value,
    email: email.value === "-" ? "" : email.value,
    newPassword: ""
  };
  editVisible.value = true;
}

async function handleSaveProfile() {
  if (!editForm.value.name.trim() || !editForm.value.phone.trim()) {
    ElMessage.warning("姓名和手机号不能为空");
    return;
  }

  if (editForm.value.newPassword && passwordStrengthScore.value < 3) {
    ElMessage.warning("新密码复杂度不足，需至少满足三种字符类型");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  saving.value = true;
  try {
    const profile = await wsClient.request("UPDATE_PROFILE", {
      userId: cardNo.value,
      name: editForm.value.name.trim(),
      phone: editForm.value.phone.trim(),
      email: editForm.value.email.trim(),
      newPassword: editForm.value.newPassword
    });

    applyProfile(profile);
    editVisible.value = false;
    ElMessage.success("个人信息已更新");
  } catch (error) {
    ElMessage.error(error.message || "修改失败");
  } finally {
    saving.value = false;
  }
}

async function handleStartExam() {
  if (!userId.value.trim()) {
    ElMessage.warning("Please input user id.");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  starting.value = true;
  try {
    const payload = await wsClient.request("START_SESSION", {
      userId: userId.value.trim()
    });
    router.push(`/exam/${payload.sessionId}`);
  } catch (error) {
    ElMessage.error(error.message || "Failed to start exam session.");
  } finally {
    starting.value = false;
  }
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.dashboard-head {
  margin-bottom: 16px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel-card {
  min-height: 280px;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 20px;
}

.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.avatar-click {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.avatar-image {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  object-fit: cover;
  background: #ffffff;
}

.avatar-tip {
  color: #909399;
  font-size: 12px;
}

.avatar-input {
  display: none;
}

.action-row {
  margin-top: 14px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.ws-line {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 14px;
}

.pwd-strength {
  margin-top: 10px;
  color: #6b7280;
  font-size: 13px;
}

.pwd-hint {
  margin: 8px 0 0;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
