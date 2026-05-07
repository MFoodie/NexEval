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
          <el-table v-else-if="studentClasses.length" :data="studentClasses" size="small" class="student-classes-table">
            <el-table-column
              prop="cno"
              label="课程号"
              width="140"
              align="left"
              header-align="left"
              class-name="col-large"
              header-class-name="col-large-header"
            />
            <el-table-column
              prop="cname"
              label="课程名"
              align="left"
              header-align="left"
              class-name="col-large"
              header-class-name="col-large-header"
            />
            <el-table-column
              prop="teacherName"
              label="教师姓名"
              width="100"
              align="center"
              header-align="center"
              class-name="col-large col-teacher-cell"
              header-class-name="col-large col-teacher-header"
            />
            <el-table-column
              prop="grade"
              label="成绩"
              width="100"
              align="center"
              header-align="center"
              class-name="col-large col-score-cell"
              header-class-name="col-large col-score-header"
            />
            <el-table-column label="操作" width="110" align="center" header-align="center">
              <template #default="scope">
                <div class="student-action-buttons">
                  <el-button
                    type="primary"
                    size="small"
                    :loading="startingPractice"
                    @click="handleStartPracticeForClass(scope.row)"
                  >
                    题目练习
                  </el-button>
                  <el-button size="small" :loading="startingExam" @click="handleStartExamForClass(scope.row)">
                    进入考试
                  </el-button>
                  <el-button type="warning" plain size="small" @click="openAppealHistory(scope.row)">
                    成绩复核
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="placeholder">暂无教学班</div>
        </template>

        <el-form v-else @submit.prevent>
          <el-form-item label="考试账号">
            <el-input v-model="userId" disabled />
          </el-form-item>

          <div class="action-row">
            <el-button type="primary" :loading="startingPractice" @click="handleStartPractice">题目练习</el-button>
            <el-button :loading="startingExam" @click="handleStartExam">进入考试</el-button>
          </div>
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

    <el-dialog v-model="appealHistoryVisible" title="成绩复核记录" width="640px">
      <div v-if="appealLoading" class="placeholder">正在加载复核记录...</div>
      <div v-else>
        <div v-if="appealHistoryList.length === 0" class="placeholder">暂无复核记录</div>
        <el-table v-else :data="appealHistoryList" size="small">
          <el-table-column prop="createdAt" label="申请时间" width="180">
            <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="说明" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'pending'" type="info">待处理</el-tag>
              <el-tag v-else-if="row.status === 'approved'" type="success">同意</el-tag>
              <el-tag v-else-if="row.status === 'rejected'" type="danger">不同意</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="appealHistoryVisible = false">关闭</el-button>
        <el-button type="primary" @click="appealHistoryVisible = false; appealVisible = true">申请复核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealVisible" title="成绩复核申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-input :model-value="appealCourseLabel" disabled />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input
            v-model="appealReason"
            type="textarea"
            :rows="5"
            maxlength="1024"
            show-word-limit
            placeholder="请输入复核原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button type="primary" :loading="appealSubmitting" @click="handleSubmitScoreAppeal">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="gradingVisible" title="考试批改" width="860px">
      <div class="grading-head">
        <div class="grading-meta">
          <div>学生：{{ gradingStudent?.name || '-' }}（{{ gradingStudent?.sno || '-' }}）</div>
          <div>课程：{{ selectedClass?.cno || '-' }} {{ selectedClass?.cname || '' }}</div>
        </div>
        <el-select
          v-model="selectedAttemptId"
          placeholder="选择考试记录"
          size="small"
          @change="handleAttemptChange"
        >
          <el-option
            v-for="attempt in gradingAttempts"
            :key="attempt.sessionId"
            :label="formatAttemptLabel(attempt)"
            :value="attempt.sessionId"
          />
        </el-select>
      </div>

      <div v-if="gradingLoading" class="placeholder">正在加载答卷...</div>
      <div v-else-if="gradingAttempts.length === 0" class="placeholder">暂无考试记录</div>
      <el-table v-else :data="gradingAnswers" size="small">
        <el-table-column prop="stem" label="题目" min-width="240" />
        <el-table-column label="作答" min-width="220">
          <template #default="scope">
            <div>{{ scope.row.answerText || '-' }}</div>
            <img
              v-if="scope.row.answerImagePath"
              :src="normalizeAnswerImageSrc(scope.row.answerImagePath)"
              alt="作答图片"
              class="grading-answer-image"
            />
          </template>
        </el-table-column>
        <el-table-column label="判定" width="90">
          <template #default="scope">
            <span v-if="scope.row.correct === true">正确</span>
            <span v-else-if="scope.row.correct === false">错误</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="120">
          <template #default="scope">
            <el-input-number
              v-if="canReviewAnswer(scope.row)"
              v-model="scope.row.score"
              :min="0"
              :max="scope.row.maxScore ?? 100"
              size="small"
            />
            <span v-else>{{ scope.row.score ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评语" min-width="180">
          <template #default="scope">
            <el-input
              v-if="canReviewAnswer(scope.row)"
              v-model="scope.row.reviewNote"
              size="small"
              placeholder="填写评语"
            />
            <span v-else>{{ scope.row.reviewNote || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              v-if="canReviewAnswer(scope.row)"
              size="small"
              :loading="gradingSaving"
              @click="handleReviewAnswer(scope.row)"
            >
              保存
            </el-button>
            <span v-else>自动批改</span>
          </template>
        </el-table-column>
      </el-table>
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
const startingPractice = ref(false);
const startingExam = ref(false);
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
const actionPanelTitle = computed(() => (isTeacher.value ? "考试批改" : "题目练习与考试"));
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
const gradingVisible = ref(false);
const gradingLoading = ref(false);
const gradingStudent = ref(null);
const gradingAttempts = ref([]);
const gradingAnswers = ref([]);
const selectedAttemptId = ref("");
const gradingSaving = ref(false);
const appealVisible = ref(false);
const appealSubmitting = ref(false);
const appealCourse = ref(null);
const appealReason = ref("");
const appealHistoryVisible = ref(false);
const appealHistoryList = ref([]);
const appealLoading = ref(false);

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

const appealCourseLabel = computed(() => {
  if (!appealCourse.value) {
    return "-";
  }

  return `${appealCourse.value.cno || "-"} ${appealCourse.value.cname || ""}`.trim();
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

async function handleStartPractice(courseNo = "", courseName = "") {
  if (!userId.value.trim()) {
    ElMessage.warning("Please input user id.");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  startingPractice.value = true;
  try {
    const payload = await wsClient.request("START_PRACTICE", {
      userId: userId.value.trim(),
      courseNo: String(courseNo || "").trim()
    });
    router.push({
      name: "exam",
      params: {
        sessionId: payload.sessionId
      },
      query: {
        courseNo: String(courseNo || "").trim(),
        courseName: String(courseName || "").trim(),
        mode: "practice"
      }
    });
  } catch (error) {
    ElMessage.error(error.message || "Failed to start practice session.");
  } finally {
    startingPractice.value = false;
  }
}

async function handleStartExam(courseNo = "", courseName = "") {
  if (!userId.value.trim()) {
    ElMessage.warning("Please input user id.");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  startingExam.value = true;
  try {
    const payload = await wsClient.request("START_EXAM", {
      userId: userId.value.trim(),
      courseNo: String(courseNo || "").trim()
    });
    router.push({
      name: "exam",
      params: {
        sessionId: payload.sessionId
      },
      query: {
        courseNo: String(courseNo || "").trim(),
        courseName: String(courseName || "").trim(),
        mode: "exam"
      }
    });
  } catch (error) {
    ElMessage.error(error.message || "Failed to start exam session.");
  } finally {
    startingExam.value = false;
  }
}

function handleStartPracticeForClass(clazz) {
  handleStartPractice(clazz?.cno || "", clazz?.cname || "");
}

function handleStartExamForClass(clazz) {
  handleStartExam(clazz?.cno || "", clazz?.cname || "");
}

function selectClass(clazz) {
  selectedClass.value = clazz;
}

function openScoreAppealDialog(clazz) {
  appealCourse.value = clazz || null;
  appealReason.value = "";
  appealVisible.value = true;
}

async function openAppealHistory(clazz) {
  appealCourse.value = clazz || null;
  appealHistoryList.value = [];
  appealHistoryVisible.value = true;
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，无法获取复核记录");
    return;
  }
  appealLoading.value = true;
  try {
    const data = await wsClient.request("GET_SCORE_APPEALS", {
      userId: cardNo.value,
      courseNo: clazz?.cno || ""
    }, 20000);
    const raw = Array.isArray(data) ? data : [];
    const cno = String(clazz?.cno || "").trim();
    // 额外在客户端按课程号过滤，兼容后端返回字段名不同（courseNo / course_no）
    appealHistoryList.value = raw.filter((r) => {
      if (!r) return false;
      const a = r.courseNo || r.course_no || r.course || r.course_no?.toString?.();
      return String(a || "").trim() === cno;
    });
  } catch (err) {
    ElMessage.error(err.message || "获取复核记录失败");
  } finally {
    appealLoading.value = false;
  }
}

async function handleSubmitScoreAppeal() {
  if (!appealCourse.value) {
    ElMessage.warning("请先选择课程");
    return;
  }

  if (!appealReason.value.trim()) {
    ElMessage.warning("请填写复核说明");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  appealSubmitting.value = true;
  try {
    await wsClient.request("CREATE_SCORE_APPEAL", {
      userId: cardNo.value,
      courseNo: appealCourse.value.cno,
      reason: appealReason.value.trim()
    });
    ElMessage.success("成绩复核申请已提交");
    appealVisible.value = false;
  } catch (error) {
    ElMessage.error(error.message || "申请提交失败");
  } finally {
    appealSubmitting.value = false;
  }
}

async function handleGradeStudent(row) {
  if (!selectedClass.value) {
    return;
  }
  gradingStudent.value = row || null;
  gradingVisible.value = true;
  await loadExamAttempts();
}

async function loadExamAttempts() {
  if (!wsClient || !wsClient.isOpen() || !gradingStudent.value) {
    return;
  }

  gradingLoading.value = true;
  try {
    const data = await wsClient.request("GET_EXAM_ATTEMPTS", {
      userId: gradingStudent.value.userId,
      courseNo: selectedClass.value?.cno || ""
    });
    gradingAttempts.value = Array.isArray(data) ? data : [];
    selectedAttemptId.value = gradingAttempts.value[0]?.sessionId || "";
    if (selectedAttemptId.value) {
      await loadAttemptAnswers(selectedAttemptId.value);
    } else {
      gradingAnswers.value = [];
    }
  } catch (error) {
    ElMessage.error(error.message || "考试记录获取失败");
  } finally {
    gradingLoading.value = false;
  }
}

async function loadAttemptAnswers(sessionId) {
  if (!wsClient || !wsClient.isOpen() || !sessionId) {
    return;
  }

  gradingLoading.value = true;
  try {
    const data = await wsClient.request("GET_ATTEMPT_ANSWERS", {
      sessionId
    });
    gradingAnswers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "答卷获取失败");
  } finally {
    gradingLoading.value = false;
  }
}

function handleAttemptChange(value) {
  selectedAttemptId.value = value || "";
  if (selectedAttemptId.value) {
    loadAttemptAnswers(selectedAttemptId.value);
  }
}

function formatAttemptLabel(attempt) {
  if (!attempt) {
    return "";
  }
  const started = attempt.startedAt ? new Date(attempt.startedAt).toLocaleString() : "-";
  const status = attempt.status ? attempt.status.toUpperCase() : "";
  return `${started} ${status}`.trim();
}

function canReviewAnswer(answer) {
  return answer?.type === "essay";
}

function normalizeAnswerImageSrc(path) {
  const text = String(path || "").trim();
  if (!text) {
    return "";
  }
  if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
    return text;
  }
  return `/${text}`;
}

async function handleReviewAnswer(answer) {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }
  if (!answer?.answerId) {
    return;
  }

  gradingSaving.value = true;
  try {
    const payload = await wsClient.request("REVIEW_ANSWER", {
      answerId: String(answer.answerId),
      score: answer.score,
      reviewNote: answer.reviewNote || "",
      reviewerId: cardNo.value
    });
    const index = gradingAnswers.value.findIndex((item) => item.answerId === payload.answerId);
    if (index !== -1) {
      gradingAnswers.value.splice(index, 1, payload);
    }
    ElMessage.success("批改已保存");
  } catch (error) {
    ElMessage.error(error.message || "批改保存失败");
  } finally {
    gradingSaving.value = false;
  }
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

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.student-action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end; 
  width: auto;
}

.student-action-buttons .el-button {
  width: 90px; 
  max-width: 100%;
  box-sizing: border-box;
}

/* 放大表格中某些列的字号 */
::v-deep(.col-large .cell),
.col-large-header {
  font-size: 14px !important;
  font-weight: 600 !important;
}
::v-deep(.col-score-cell .cell) {
  font-size: 14px !important;
  font-weight: 700 !important;
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

.grading-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.grading-meta {
  display: grid;
  gap: 6px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.grading-answer-image {
  margin-top: 8px;
  width: min(220px, 100%);
  border-radius: 8px;
  border: 1px solid var(--ne-border);
  display: block;
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

::v-deep(.student-classes-table) .el-table__header-wrapper th:nth-child(3) .cell,
::v-deep(.student-classes-table) .el-table__header-wrapper th:nth-child(3) .cell,
::v-deep(.student-classes-table) .el-table__body-wrapper td:nth-child(3) .cell {
  padding-left: 4px !important;
  transform: translateX(-150px) !important;
}

::v-deep(.student-classes-table) .el-table__header-wrapper th:nth-child(4) .cell,
::v-deep(.student-classes-table) .el-table__body-wrapper td:nth-child(4) .cell {
  padding-left: 4px !important;
  transform: translateX(-90px) !important;
}
</style>