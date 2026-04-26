<template>
  <section class="admin-shell">
    <aside class="sidebar card">
      <div class="sidebar-profile">
        <img :src="avatarUrl" alt="管理员头像" class="sidebar-avatar" @click="triggerAvatarPicker" />
        <div class="sidebar-name">{{ loginInfo?.name || "管理员" }}</div>
        <div class="sidebar-id">卡号 {{ loginInfo?.cardNo || "-" }}</div>
      </div>

      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          :class="['nav-item', { active: activeMenu === item.key }]"
          type="button"
          @click="activeMenu = item.key"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="sidebar-actions">
        <el-button :loading="avatarSaving" @click="handleResetAvatar">恢复默认头像</el-button>
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

    <main class="main-panel">
      <section class="card panel-card" v-if="activeMenu === 'profile'">
        <h2 class="panel-title">个人信息</h2>
        <div class="admin-info-wrap">
          <div class="avatar-wrap">
            <div class="avatar-click" @click="triggerAvatarPicker">
              <img :src="avatarUrl" alt="管理员头像" class="admin-avatar" />
            </div>
            <div class="avatar-tip">点击修改头像</div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="卡号">{{ loginInfo?.cardNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ loginInfo?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ sexText }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ displayPhone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ loginInfo?.email || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'register'">
        <h2 class="panel-title">用户信息注册</h2>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="卡号">
            <el-input v-model="form.id" maxlength="9" placeholder="请输入 9 位卡号" />
          </el-form-item>

          <el-form-item label="姓名">
            <el-input v-model="form.name" maxlength="20" placeholder="请输入姓名" />
          </el-form-item>

          <el-form-item label="性别">
            <el-radio-group v-model="form.sex">
              <el-radio class="male-radio" value="男">男</el-radio>
              <el-radio class="female-radio" value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="用户类型">
            <el-select v-model="form.type" style="width: 100%" placeholder="请选择类型">
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
            </el-select>
          </el-form-item>

          <template v-if="form.type === 'student'">
            <el-divider content-position="left">学生信息</el-divider>
            <el-form-item label="学号">
              <el-input v-model="form.sno" maxlength="8" placeholder="请输入学号" />
            </el-form-item>
            <el-form-item label="入学年份">
              <el-input v-model="form.studentEnterYear" placeholder="例如 2023" />
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="form.major" maxlength="20" placeholder="请输入专业" />
            </el-form-item>
            <el-form-item label="学院">
              <el-input v-model="form.studentDepartment" maxlength="30" placeholder="请输入学院" />
            </el-form-item>
          </template>

          <template v-else>
            <el-divider content-position="left">教师信息</el-divider>
            <el-form-item label="工号">
              <el-input v-model="form.eid" maxlength="8" placeholder="请输入工号" />
            </el-form-item>
            <el-form-item label="入职年份">
              <el-input v-model="form.teacherEnterYear" placeholder="例如 2015" />
            </el-form-item>
            <el-form-item label="职称">
              <el-select v-model="form.title" style="width: 100%" placeholder="请选择职称">
                <el-option label="教授" value="professor" />
                <el-option label="副教授" value="associate_professor" />
                <el-option label="讲师" value="lecture" />
              </el-select>
            </el-form-item>
            <el-form-item label="学院">
              <el-input v-model="form.teacherDepartment" maxlength="30" placeholder="请输入学院" />
            </el-form-item>
          </template>

          <div class="action-row">
            <el-button type="primary" :loading="saving" @click="handleRegister">注册用户</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'course'">
        <h2 class="panel-title">课程信息管理</h2>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="课程编号">
            <el-input v-model="courseForm.cno" maxlength="8" placeholder="例如 BJSL0001" />
          </el-form-item>
          <el-form-item label="课程名称">
            <el-input v-model="courseForm.cname" maxlength="20" placeholder="请输入课程名称" />
          </el-form-item>
          <el-form-item label="学分">
            <el-input v-model="courseForm.credit" placeholder="例如 3 或 4" />
          </el-form-item>
          <div class="action-row">
            <el-button type="primary" :loading="courseSaving" @click="handleCreateCourse">新增课程</el-button>
            <el-button :loading="importingType === 'COURSES'" @click="triggerImport('COURSES')">批量导入课程</el-button>
          </div>
        </el-form>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'class'">
        <h2 class="panel-title">教学班管理</h2>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="课程编号">
            <el-input v-model="classForm.cno" maxlength="8" placeholder="例如 BJSL0001" />
          </el-form-item>
          <el-form-item label="教师工号">
            <el-input v-model="classForm.eid" maxlength="8" placeholder="例如 09T09011" />
          </el-form-item>
          <div class="action-row">
            <el-button type="primary" :loading="classSaving" @click="handleCreateClass">新增教学班</el-button>
            <el-button :loading="importingType === 'CLASSES'" @click="triggerImport('CLASSES')">批量导入教学班</el-button>
            <el-button :loading="importingType === 'SC'" @click="triggerImport('SC')">批量导入选课记录</el-button>
          </div>
        </el-form>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'batch'">
        <h2 class="panel-title">批量导入</h2>
        <div class="batch-grid">
          <el-button :loading="importingType === 'STUDENTS'" @click="triggerImport('STUDENTS')">导入学生</el-button>
          <el-button :loading="importingType === 'TEACHERS'" @click="triggerImport('TEACHERS')">导入教师</el-button>
          <el-button :loading="importingType === 'COURSES'" @click="triggerImport('COURSES')">导入课程</el-button>
          <el-button :loading="importingType === 'CLASSES'" @click="triggerImport('CLASSES')">导入教学班</el-button>
          <el-button :loading="importingType === 'SC'" @click="triggerImport('SC')">导入选课记录</el-button>
        </div>

        <div class="import-summary" v-if="importResult">
          <div>导入类型：{{ importResult.importType }}</div>
          <div>导入成功：{{ importResult.imported }}</div>
          <div>跳过重复：{{ importResult.skipped }}</div>
          <div>失败行数：{{ importResult.failed }}</div>
          <div v-if="importResult.errors?.length" class="error-list">
            {{ importResult.errors.join('；') }}
          </div>
        </div>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'review'">
        <h2 class="panel-title">成绩复核审理</h2>
        <div class="placeholder">该功能暂未开放。</div>
      </section>

      <input
        ref="importInputRef"
        class="avatar-input"
        type="file"
        accept=".xlsx"
        @change="handleImportFileChange"
      />
    </main>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { clearLogin, getLogin, saveLogin } from "../auth";
import { createExamSocket } from "../ws";

const router = useRouter();
const loginInfo = getLogin();
const wsClient = createExamSocket(null);
const avatarSaving = ref(false);
const avatarInputRef = ref(null);
const importInputRef = ref(null);
const avatarUrl = ref(withAvatarVersion(loginInfo?.avatarUrl || "/avatar/admin_male.png"));
const activeMenu = ref("profile");
const importingType = ref("");
const pendingImportType = ref("");
const importResult = ref(null);

const menuItems = [
  { key: "profile", label: "个人信息" },
  { key: "register", label: "用户信息注册" },
  { key: "course", label: "课程信息管理" },
  { key: "class", label: "教学班管理" },
  { key: "batch", label: "批量导入" },
  { key: "review", label: "成绩复核审理" }
];

const sexText = computed(() => {
  if (loginInfo?.sex === true) {
    return "男";
  }

  if (loginInfo?.sex === false) {
    return "女";
  }

  return "-";
});

const displayPhone = computed(() => {
  const text = String(loginInfo?.phone || "").trim();
  if (!text || /^0+$/.test(text)) {
    return "-";
  }
  return text;
});

const saving = ref(false);
const courseSaving = ref(false);
const classSaving = ref(false);
const form = reactive({
  id: "",
  name: "",
  sex: "男",
  type: "student",
  sno: "",
  studentEnterYear: "",
  major: "",
  studentDepartment: "",
  eid: "",
  teacherEnterYear: "",
  title: "lecture",
  teacherDepartment: ""
});

const courseForm = reactive({
  cno: "",
  cname: "",
  credit: ""
});

const classForm = reactive({
  cno: "",
  eid: ""
});

function resetForm() {
  form.id = "";
  form.name = "";
  form.sex = "男";
  form.type = "student";
  form.sno = "";
  form.studentEnterYear = "";
  form.major = "";
  form.studentDepartment = "";
  form.eid = "";
  form.teacherEnterYear = "";
  form.title = "lecture";
  form.teacherDepartment = "";
}

function withAvatarVersion(url) {
  if (!url) {
    return "/avatar/admin_male.png";
  }

  const divider = url.includes("?") ? "&" : "?";
  return `${url}${divider}t=${Date.now()}`;
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

  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  avatarSaving.value = true;
  try {
    const circlePngDataUrl = await cropImageToCirclePng(file, 256);
    const profile = await wsClient.request("UPDATE_AVATAR", {
      userId: loginInfo?.cardNo,
      imageBase64: circlePngDataUrl
    }, 30000);

    saveLogin(profile);
    avatarUrl.value = withAvatarVersion(profile.avatarUrl || "/avatar/admin_male.png");
    ElMessage.success("头像已更新");
  } catch (error) {
    ElMessage.error(error.message || "头像更新失败");
  } finally {
    avatarSaving.value = false;
  }
}

async function handleResetAvatar() {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  avatarSaving.value = true;
  try {
    const profile = await wsClient.request("RESET_AVATAR", {
      userId: loginInfo?.cardNo
    });

    saveLogin(profile);
    avatarUrl.value = withAvatarVersion(profile.avatarUrl || "/avatar/admin_male.png");
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

async function handleRegister() {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  if (!form.id.trim() || !form.name.trim()) {
    ElMessage.warning("请先填写卡号和姓名");
    return;
  }

  saving.value = true;
  try {
    await wsClient.request("REGISTER_USER", {
      id: form.id.trim(),
      name: form.name.trim(),
      sex: form.sex,
      type: form.type,
      sno: form.sno.trim(),
      studentEnterYear: form.studentEnterYear.trim(),
      major: form.major.trim(),
      studentDepartment: form.studentDepartment.trim(),
      eid: form.eid.trim(),
      teacherEnterYear: form.teacherEnterYear.trim(),
      title: form.title,
      teacherDepartment: form.teacherDepartment.trim()
    });

    ElMessage.success("注册成功，默认密码为 123456");
    resetForm();
  } catch (error) {
    ElMessage.error(error.message || "注册失败");
  } finally {
    saving.value = false;
  }
}

async function handleCreateCourse() {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  if (!courseForm.cno.trim() || !courseForm.cname.trim() || !courseForm.credit.trim()) {
    ElMessage.warning("请完整填写课程编号、课程名称、学分");
    return;
  }

  courseSaving.value = true;
  try {
    await wsClient.request("CREATE_COURSE", {
      cno: courseForm.cno.trim(),
      cname: courseForm.cname.trim(),
      credit: courseForm.credit.trim()
    });

    courseForm.cno = "";
    courseForm.cname = "";
    courseForm.credit = "";
    ElMessage.success("课程创建成功");
  } catch (error) {
    ElMessage.error(error.message || "课程创建失败");
  } finally {
    courseSaving.value = false;
  }
}

async function handleCreateClass() {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  if (!classForm.cno.trim() || !classForm.eid.trim()) {
    ElMessage.warning("请先填写课程编号和教师工号");
    return;
  }

  classSaving.value = true;
  try {
    await wsClient.request("CREATE_CLASS", {
      cno: classForm.cno.trim(),
      eid: classForm.eid.trim()
    });

    classForm.cno = "";
    classForm.eid = "";
    ElMessage.success("教学班创建成功");
  } catch (error) {
    ElMessage.error(error.message || "教学班创建失败");
  } finally {
    classSaving.value = false;
  }
}

function triggerImport(importType) {
  pendingImportType.value = importType;
  importInputRef.value?.click();
}

async function handleImportFileChange(event) {
  const file = event?.target?.files?.[0];
  event.target.value = "";

  if (!file || !pendingImportType.value) {
    return;
  }

  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  const lowerName = file.name.toLowerCase();
  if (!lowerName.endsWith(".xlsx")) {
    ElMessage.warning("仅支持 xlsx 文件");
    return;
  }

  importingType.value = pendingImportType.value;
  importResult.value = null;

  try {
    const fileBase64 = await readFileAsDataUrl(file);
    const result = await wsClient.request("IMPORT_BATCH", {
      importType: pendingImportType.value,
      fileBase64
    }, 60000);

    importResult.value = result;
    ElMessage.success("批量导入已完成");
  } catch (error) {
    ElMessage.error(error.message || "批量导入失败");
  } finally {
    importingType.value = "";
  }
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onerror = () => reject(new Error("读取文件失败"));
    reader.onload = () => resolve(String(reader.result || ""));

    reader.readAsDataURL(file);
  });
}

function handleLogout() {
  wsClient.close();
  clearLogin();
  router.push("/login");
}

onBeforeUnmount(() => {
  wsClient.close();
});
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
}

.sidebar {
  min-height: 520px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-profile {
  text-align: center;
}

.sidebar-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #dcdfe6;
  cursor: pointer;
}

.sidebar-name {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
}

.sidebar-id {
  margin-top: 2px;
  color: #6b7280;
  font-size: 13px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 10px;
  padding: 10px 12px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
}

.nav-item.active {
  border-color: #0059f5;
  color: #0059f5;
  background: #eff5ff;
}

.sidebar-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.main-panel {
  min-width: 0;
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
  border: 1px solid #dcdfe6;
}

.avatar-tip {
  margin-top: 8px;
  text-align: center;
  font-size: 12px;
  color: #409eff;
}

.avatar-input {
  display: none;
}

.admin-info-wrap {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 16px;
  align-items: start;
}

.admin-avatar {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  object-fit: cover;
}

:deep(.male-radio .el-radio__input.is-checked .el-radio__inner) {
  border-color: #0059F5;
  background-color: #0059F5;
}

:deep(.male-radio .el-radio__input.is-checked + .el-radio__label) {
  color: #0059F5;
}

:deep(.female-radio .el-radio__input.is-checked .el-radio__inner) {
  border-color: #ff00ff;
  background-color: #ff00ff;
}

:deep(.female-radio .el-radio__input.is-checked + .el-radio__label) {
  color: #ff00ff;
}

.panel-card {
  min-height: 400px;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 20px;
}

.action-row {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.placeholder {
  color: #909399;
  font-size: 14px;
}

.batch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.import-summary {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  display: grid;
  gap: 6px;
  color: #374151;
}

.error-list {
  color: #c0392b;
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-info-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
