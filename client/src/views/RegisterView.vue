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
      <h1 class="card-title">用户注册</h1>
      <p class="card-subtitle">注册新用户账号</p>

      <p class="ws-line">
        WebSocket:
        <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
      </p>

      <el-form @submit.prevent>
        <el-form-item label="卡号">
          <el-input v-model="form.id" placeholder="9位卡号" maxlength="9" />
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="用户类型">
          <el-select v-model="form.type" placeholder="请选择用户类型" style="width:100%">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
          </el-select>
        </el-form-item>

        <template v-if="form.type === 'student'">
          <el-form-item label="学号">
            <el-input v-model="form.sno" placeholder="请输入学号" />
          </el-form-item>
          <el-form-item label="入学年份">
            <el-input v-model="form.studentEnterYear" placeholder="如 2024" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="form.major" placeholder="请输入专业" />
          </el-form-item>
          <el-form-item label="院系">
            <el-input v-model="form.studentDepartment" placeholder="请输入院系" />
          </el-form-item>
        </template>

        <template v-if="form.type === 'teacher'">
          <el-form-item label="工号">
            <el-input v-model="form.eid" placeholder="请输入工号" />
          </el-form-item>
          <el-form-item label="入职年份">
            <el-input v-model="form.teacherEnterYear" placeholder="如 2020" />
          </el-form-item>
          <el-form-item label="职称">
            <el-input v-model="form.title" placeholder="如 教授、副教授、讲师" />
          </el-form-item>
          <el-form-item label="院系">
            <el-input v-model="form.teacherDepartment" placeholder="请输入院系" />
          </el-form-item>
        </template>

        <el-button type="primary" :loading="submitting" @click="handleRegister" style="width:100%">
          注册
        </el-button>

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

.switch-line {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}
</style>
