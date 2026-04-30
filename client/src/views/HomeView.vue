<template>
  <section class="home-shell">
    <aside class="home-sidebar card">
      <div class="side-profile">
        <img :src="avatarUrl" alt="默认头像" class="side-avatar" @click="triggerAvatarPicker" />
        <div class="side-name">{{ userName }}</div>
        <div class="side-id">卡号 {{ cardNo }}</div>
      </div>

      <nav class="side-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="side-item"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="side-status">
        <div class="status-label">WebSocket</div>
        <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
      </div>

      <div class="side-actions">
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>

      <input
        ref="avatarInputRef"
        class="avatar-input"
        type="file"
        accept="image/png,image/jpeg"
        @change="handleAvatarFileChange"
      />
    </aside>

    <main class="home-main">
      <div class="dashboard-head card">
        <h1 class="card-title">在线考试系统</h1>
      </div>

      <section class="card panel-card" v-if="activeMenu === 'profile'">
        <h2 class="panel-title">个人信息</h2>
        <div class="avatar-wrap">
          <div class="avatar-click" @click="triggerAvatarPicker">
            <img :src="avatarUrl" alt="默认头像" class="avatar-image" />
          </div>
          <div class="avatar-tip">点击修改头像</div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="卡号">{{ cardNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userName }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ sexText }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ displayPhone }}</el-descriptions-item>
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
        </div>
      </section>

      <section class="card panel-card exam-panel-card" v-else>
        <h2 class="panel-title">{{ actionPanelTitle }}</h2>

        <template v-if="isTeacher">
          <div class="teacher-layout">
            <div class="teacher-sidebar">
              <div class="section-title">教学班</div>
              <div v-if="teacherLoading" class="placeholder">正在加载教学班...</div>
              <div v-else-if="teacherClasses.length === 0" class="placeholder">暂无教学班</div>
              <div v-else class="class-list">
                <button
                  v-for="clazz in teacherClasses"
                  :key="`${clazz.cno}-${clazz.eid}`"
                  type="button"
                  class="class-card"
                  :class="{ active: selectedClass?.cno === clazz.cno && selectedClass?.eid === clazz.eid }"
                  @click="selectClass(clazz)"
                >
                  <div class="class-title">
                    <span class="class-code">{{ clazz.cno }}</span>
                    <span class="class-name">{{ clazz.cname }}</span>
                  </div>
                </button>
              </div>
            </div>

            <div class="teacher-main">
              <div class="student-header">
                <div>
                  <div class="section-title">学生列表</div>
                  <div class="student-subtitle">
                    {{ selectedClass ? `${selectedClass.cno} ${selectedClass.cname}` : "请先选择教学班" }}
                  </div>
                </div>
                <div v-if="selectedClass" class="student-count">共 {{ selectedClass.students?.length || 0 }} 人</div>
              </div>
              <div v-if="!selectedClass" class="placeholder">请选择教学班查看学生</div>
              <el-table v-else :data="selectedClass.students || []" size="small">
                <el-table-column prop="userId" label="卡号" width="120" />
                <el-table-column prop="sno" label="学号" width="120" />
                <el-table-column prop="name" label="姓名" width="120" />
                <el-table-column label="性别" width="80">
                  <template #default="scope">
                    {{ scope.row.sex ? '男' : '女' }}
                  </template>
                </el-table-column>
                <el-table-column prop="grade" label="成绩" width="80">
                  <template #default="scope">
                    {{ scope.row.grade ?? '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button size="small" @click="handleGradeStudent(scope.row)">成绩批改</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </template>

        <template v-else-if="isStudent">
          <div v-if="studentLoading" class="placeholder">正在加载教学班...</div>
          <el-table v-else-if="studentClasses.length" :data="studentClasses" size="small">
            <el-table-column prop="cno" label="课程号" width="140" />
            <el-table-column prop="cname" label="课程名" />
            <el-table-column prop="teacherName" label="教师姓名" width="120" />
            <el-table-column label="操作" width="140">
              <template #default="scope">
                <el-button type="primary" size="small" :loading="starting" @click="handleStartExamForClass(scope.row)">
                  进入考试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="placeholder">暂无教学班</div>
        </template>

        <el-form v-else @submit.prevent>
          <el-form-item label="考试账号">
            <el-input v-model="userId" disabled />
          </el-form-item>

          <el-button type="primary" :loading="starting" @click="handleStartExam">{{ actionButtonText }}</el-button>
        </el-form>
      </section>
    </main>

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
const displayPhone = computed(() => formatPhoneForDisplay(phone.value));
const actionPanelTitle = computed(() => (isTeacher.value ? "考试批改" : "进入考试"));
const actionButtonText = computed(() => (isTeacher.value ? "考试批改" : "进入考试"));
const activeMenu = ref("action");
const menuItems = computed(() => [
  { key: "profile", label: "个人信息" },
  { key: "action", label: actionPanelTitle.value }
]);
const teacherClasses = ref([]);
const studentClasses = ref([]);
const teacherLoading = ref(false);
const studentLoading = ref(false);
const selectedClass = ref(null);

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
      refreshClassLists();
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

function refreshClassLists() {
  if (!wsClient || !wsClient.isOpen()) {
    return;
  }

  if (isTeacher.value) {
    fetchTeacherClasses();
  }

  if (isStudent.value) {
    fetchStudentClasses();
  }
}

async function fetchTeacherClasses() {
  const eid = teacherInfo.value?.eid;
  if (!eid) {
    teacherClasses.value = [];
    selectedClass.value = null;
    return;
  }

  teacherLoading.value = true;
  try {
    const data = await wsClient.request("GET_TEACHER_CLASSES", { eid }, 20000);
    teacherClasses.value = Array.isArray(data) ? data : [];
    selectedClass.value = teacherClasses.value[0] || null;
  } catch (error) {
    ElMessage.error(error.message || "教学班获取失败");
  } finally {
    teacherLoading.value = false;
  }
}

async function fetchStudentClasses() {
  const sno = studentInfo.value?.sno;
  if (!sno) {
    studentClasses.value = [];
    return;
  }

  studentLoading.value = true;
  try {
    const data = await wsClient.request("GET_STUDENT_CLASSES", { sno }, 20000);
    studentClasses.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "教学班获取失败");
  } finally {
    studentLoading.value = false;
  }
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
  refreshClassLists();
}

function isAllZeroPhone(value) {
  const text = String(value || "").trim();
  return text !== "" && /^0+$/.test(text);
}

function formatPhoneForDisplay(value) {
  const text = String(value || "").trim();
  if (!text || text === "-" || isAllZeroPhone(text)) {
    return "-";
  }
  return text;
}

function openEditDialog() {
  editForm.value = {
    name: userName.value === "-" ? "" : userName.value,
    phone: phone.value === "-" || isAllZeroPhone(phone.value) ? "" : phone.value,
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

function handleStartExamForClass() {
  handleStartExam();
}

function selectClass(clazz) {
  selectedClass.value = clazz;
}

function handleGradeStudent() {
  ElMessage.info("成绩批改功能开发中");
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.home-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 20px;
}

.home-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 520px;
}

.side-profile {
  text-align: center;
}

.side-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--ne-border);
  box-shadow: var(--ne-shadow-soft);
  cursor: pointer;
}

.side-name {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.side-id {
  margin-top: 2px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-item {
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  border-radius: 10px;
  padding: 10px 12px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
  color: var(--ne-text);
  transition: all 0.2s ease;
}

.side-item:hover {
  border-color: rgba(42, 92, 255, 0.35);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.side-item.active {
  border-color: rgba(42, 92, 255, 0.6);
  color: var(--ne-primary);
  background: var(--ne-primary-soft);
  box-shadow: var(--ne-shadow-soft);
}

.side-status {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--ne-border);
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(42, 92, 255, 0.06), rgba(0, 194, 255, 0.08));
}

.status-label {
  color: var(--ne-text-muted);
  font-size: 12px;
}

.side-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.home-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-content: start;
  align-self: stretch;
}

.dashboard-head {
  margin-bottom: 0;
  height: 56px;
  padding: 0 28px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.dashboard-head .card-title {
  margin-bottom: 0;
}

.panel-card {
  min-height: 0;
  position: relative;
  overflow: hidden;
}

.exam-panel-card {
  flex: 1;
}

.panel-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: var(--ne-gradient-accent);
}

.panel-title {
  margin: 0 0 12px;
  font-size: 20px;
  color: var(--ne-text-strong);
}

.avatar-wrap {
  width: 92px;
}

.avatar-click {
  position: relative;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--ne-border);
  box-shadow: var(--ne-shadow-soft);
}

.avatar-image {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--ne-surface);
}

.avatar-tip {
  margin-top: 8px;
  text-align: center;
  font-size: 12px;
  color: var(--ne-primary);
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

.teacher-layout {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 16px;
  align-items: start;
}

.section-title {
  margin-bottom: 8px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.teacher-sidebar {
  display: grid;
  gap: 12px;
}

.teacher-main {
  display: grid;
  gap: 12px;
}

.student-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.student-subtitle {
  color: var(--ne-text-strong);
  font-weight: 600;
}

.student-count {
  color: var(--ne-text-muted);
  font-size: 12px;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-card {
  text-align: left;
  border: 1px solid var(--ne-border);
  border-radius: 12px;
  padding: 6px 10px;
  background: var(--ne-surface);
  cursor: pointer;
  transition: all 0.2s ease;
  max-width: 180px;
}

.class-card:hover {
  border-color: rgba(42, 92, 255, 0.35);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.class-card.active {
  border-color: rgba(42, 92, 255, 0.6);
  background: var(--ne-primary-soft);
}

.class-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.class-code {
  font-weight: 700;
  color: var(--ne-text-strong);
}

.class-name {
  margin-top: 0;
  color: var(--ne-text-muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pwd-strength {
  margin-top: 10px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.pwd-hint {
  margin: 8px 0 0;
  color: var(--ne-text-subtle);
  font-size: 12px;
}

@media (max-width: 900px) {
  .home-shell {
    grid-template-columns: 1fr;
  }

  .teacher-layout {
    grid-template-columns: 1fr;
  }
}
</style>
