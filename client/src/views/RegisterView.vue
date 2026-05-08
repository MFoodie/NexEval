<template>
  <section class="login-wrap">
    <div class="login-bg-container">
      <img class="login-bg-image" :src="bgUrl" alt="background" />
    </div>
    <div class="card login-card">
      <div class="login-brand">
        <img class="login-logo" :src="logoUrl" alt="NexEval Logo" />
        <div class="login-brand-text">
          <div class="login-brand-title">NexEval</div>
          <div class="login-brand-subtitle">智能评估系统</div>
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
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";
import logoUrl from "../assets/logo.png";
import bgUrl from "../assets/bg.png";

const router = useRouter();
const submitting = ref(false);
const wsStatus = ref("connecting");
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
  padding: 16px 0 28px;
  gap: 40px;
}

.login-bg-container {
  display: none;
  flex: 0 0 auto;
  width: 400px;
  height: 480px;
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  width: min(640px, calc(100% - 32px));
  border: 1px solid rgba(42, 92, 255, 0.12);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.14), 0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.switch-line {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
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
  border-color: #0059f5;
}

.sex-radio--male :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #0059f5;
  background: #0059f5;
}

.sex-radio--male :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #0059f5;
}

.sex-radio--female :deep(.el-radio__inner) {
  border-color: #ff00ff;
}

.sex-radio--female :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #ff00ff;
  background: #ff00ff;
}

.sex-radio--female :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #ff00ff;
}

.login-submit {
  width: 96px;
  box-shadow: 0 10px 24px rgba(42, 92, 255, 0.2);
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
