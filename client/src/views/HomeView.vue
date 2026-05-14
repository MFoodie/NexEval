<template>
  <section class="home-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="home-sidebar card">
      <div class="side-collapse-row">
        <button
          type="button"
          class="side-collapse-btn"
          :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="toggleSidebar"
        >
          <span class="side-collapse-icon">{{ sidebarCollapsed ? "»" : "«" }}</span>
          <span class="side-collapse-text">{{ sidebarCollapsed ? "展开" : "收起" }}</span>
        </button>
      </div>

      <div class="side-profile">
        <img :src="avatarUrl" alt="默认头像" class="side-avatar" @click="triggerAvatarPicker" />
        <div class="side-profile-meta">
          <div class="side-name-row">
            <div class="side-name">{{ userName }}</div>
            <img v-if="isVipTeacher" :src="vipIconUrl" alt="VIP" class="side-vip-icon" />
          </div>
          <div class="side-id">卡号 {{ cardNo }}</div>
        </div>
      </div>

      <nav class="side-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="side-item"
          :title="sidebarCollapsed ? item.label : ''"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          <img v-if="item.icon" :src="item.icon" :alt="item.label" class="side-item-icon" />
          <span v-if="!sidebarCollapsed" class="side-item-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="side-actions">
        <el-button text type="danger" @click="handleLogout">{{ sidebarCollapsed ? '退' : '退出登录' }}</el-button>
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
        <div class="dashboard-head-main">
          <h1 class="card-title">在线考试系统</h1>
          <p class="dashboard-subtitle">考试、练习、批改与复核统一在同一工作台中完成。</p>
        </div>
        <div class="dashboard-head-badges">
          <span class="dashboard-badge">{{ isTeacher ? '教师端' : '学生端' }}</span>
        </div>
      </div>

      <section class="card panel-card" v-if="activeMenu === 'profile'">
        <div class="profile-head">
          <h2 class="panel-title">简介</h2>
          <div class="profile-head-actions">
            <el-button class="profile-op" type="primary" size="small" @click="openEditDialog">修改个人信息</el-button>
            <el-button class="profile-op" size="small" :loading="avatarSaving" @click="handleResetAvatar">
              恢复默认头像
            </el-button>
          </div>
        </div>
        <table class="profile-info-table">
          <tbody>
            <tr>
              <th class="avatar-label" rowspan="3">头像</th>
              <td class="avatar-cell" rowspan="3">
                <div class="table-avatar-wrap">
                  <div class="avatar-click" @click="triggerAvatarPicker">
                    <img :src="avatarUrl" alt="默认头像" class="avatar-image" />
                  </div>
                  <div class="avatar-tip">点击修改头像</div>
                </div>
              </td>
              <th>卡号</th>
              <td>{{ cardNo }}</td>
              <th>姓名</th>
              <td>{{ userName }}</td>
            </tr>
            <tr>
              <th>性别</th>
              <td>{{ sexText }}</td>
              <th>手机号</th>
              <td>{{ displayPhone }}</td>
            </tr>
            <tr>
              <th>邮箱</th>
              <td>{{ email }}</td>
              <th>{{ isStudent ? '学号' : '工号' }}</th>
              <td>
                {{ isStudent ? (studentInfo?.sno || '-') : (teacherInfo?.eid || '-') }}
              </td>
            </tr>
            <tr v-if="isStudent || isTeacher">
              <th>{{ isStudent ? '入学年份' : '入职年份' }}</th>
              <td>
                {{ isStudent ? (studentInfo?.enterYear || '-') : (teacherInfo?.enterYear || '-') }}
              </td>
              <th>{{ isStudent ? '专业' : '职称' }}</th>
              <td>
                {{ isStudent ? (studentInfo?.major || '-') : (teacherInfo?.title || '-') }}
              </td>
              <th>学院</th>
              <td>{{ isStudent ? (studentInfo?.department || '-') : (teacherInfo?.department || '-') }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="card panel-card exam-panel-card" v-else>
        <h2 class="panel-title">{{ actionPanelTitle }}</h2>
        <div class="exam-intro">
          <div class="exam-intro-item">
            <div class="exam-intro-title">当前视图</div>
            <div class="exam-intro-value">{{ actionPanelTitle }}</div>
          </div>
          <div class="exam-intro-item">
            <div class="exam-intro-title">教学班/课程</div>
            <div class="exam-intro-value">{{ isTeacher ? teacherClasses.length : studentClasses.length }}</div>
          </div>
        </div>

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
                    size="small"
                    @click="handleStartPracticeForClass(scope.row)"
                  >
                    题目练习
                  </el-button>
                  <el-button type="primary" size="small" :loading="startingExam" @click="handleStartExamForClass(scope.row)">
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
            <el-button @click="handleStartPractice">题目练习</el-button>
            <el-button type="primary" :loading="startingExam" @click="handleStartExam">进入考试</el-button>
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

    <el-dialog v-model="practiceDialogVisible" width="560px">
      <template #title>
        <div class="practice-dialog-title">
          <img src="../assets/AI.svg" alt="AI" class="ai-icon" />
          题目练习设置
        </div>
      </template>
      <div class="practice-dialog-meta">
        <div class="practice-dialog-course">{{ practiceCourseLabel }}</div>
        <div class="practice-dialog-tip">AI 会读取当前课程题库，按你选的难度和题量生成练习题。</div>
      </div>

      <div class="practice-section">
        <div class="practice-section-label">难度</div>
        <div class="practice-level-grid">
          <button
            v-for="level in practiceLevels"
            :key="level.value"
            type="button"
            class="practice-level-card"
            :class="{ selected: practiceDifficulty === level.value }"
            :style="practiceLevelCardStyle(level)"
            @click="practiceDifficulty = level.value"
          >
            <span class="practice-level-dot" :style="{ backgroundColor: level.color }"></span>
            <span class="practice-level-label">{{ level.value }}</span>
          </button>
        </div>
      </div>

      <div class="practice-section">
        <div class="practice-section-label">练习题数量</div>
        <el-input-number v-model="practiceQuestionCount" :min="1" :max="50" :step="1" />
      </div>

      <template #footer>
        <el-button @click="practiceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="startingPractice" @click="confirmPracticeStart">开始练习</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="gradingVisible" title="考试批改" width="860px">
      <div class="grading-head">
        <div class="grading-meta">
          <div>学生：{{ gradingStudent?.name || '-' }}（{{ gradingStudent?.sno || '-' }}）</div>
          <div>课程：{{ selectedClass?.cno || '-' }} {{ selectedClass?.cname || '' }}</div>
        </div>
        <el-select
          class="grading-attempt-select"
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
      <div v-else class="grading-table">
        <div class="grading-table-head">
          <div class="grading-table-cell grading-table-cell--stem">题目</div>
          <div class="grading-table-cell grading-table-cell--answer">作答</div>
          <div class="grading-table-cell grading-table-cell--center">判定</div>
          <div class="grading-table-cell grading-table-cell--center">得分</div>
          <div class="grading-table-cell grading-table-cell--center">批改状态</div>
          <div class="grading-table-cell grading-table-cell--center">AI 日志</div>
          <div class="grading-table-cell grading-table-cell--center">操作</div>
        </div>

        <div
          v-for="answer in gradingAnswers"
          :key="answer.answerId"
          class="grading-table-row-scroll"
        >
          <div class="grading-table-row-content">
            <div class="grading-table-cell grading-table-cell--stem grading-table-question">
              {{ answer.stem || '-' }}
            </div>
            <div class="grading-table-cell grading-table-cell--answer grading-table-answer">
              <div class="grading-answer-text">{{ answer.answerText || '-' }}</div>
              <img
                v-if="answer.answerImagePath"
                :src="normalizeAnswerImageSrc(answer.answerImagePath)"
                alt="作答图片"
                class="grading-answer-image"
              />
            </div>
            <div class="grading-table-cell grading-table-cell--center">
              <span v-if="answer.correct === true">正确</span>
              <span v-else-if="answer.correct === false">错误</span>
              <span v-else>-</span>
            </div>
            <div class="grading-table-cell grading-table-cell--center">
              <el-input-number
                v-if="canReviewAnswer(answer)"
                v-model="answer.score"
                :min="0"
                :max="answer.maxScore ?? 100"
                size="small"
              />
              <span v-else>{{ answer.score ?? '-' }}</span>
            </div>
            <div class="grading-table-cell grading-table-cell--center">
              <el-tag v-if="reviewStatus(answer) === 'reviewed'" type="success">已批改</el-tag>
              <el-tag v-else-if="reviewStatus(answer) === 'needs_review'" type="warning">需人工复核</el-tag>
              <el-tag v-else type="info">待批改</el-tag>
            </div>
            <div class="grading-table-cell grading-table-cell--center">
              <el-button
                v-if="hasAiLog(answer)"
                size="small"
                plain
                @click="openAiLog(answer)"
              >
                查看
              </el-button>
              <span v-else>-</span>
            </div>
            <div class="grading-table-cell grading-table-cell--center">
              <el-button
                v-if="canReviewAnswer(answer)"
                size="small"
                :loading="gradingSaving"
                @click="handleReviewAnswer(answer)"
              >
                保存
              </el-button>
              <el-button
                v-if="canAiReviewAnswer(answer)"
                size="small"
                type="primary"
                plain
                :loading="aiReviewingId === answer.answerId"
                @click="handleAiReviewAnswer(answer)"
              >
                AI 批改
              </el-button>
              <span v-else>自动批改</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="aiLogVisible" title="AI 批改日志" width="560px">
      <el-input
        v-model="aiLogContent"
        type="textarea"
        :rows="8"
        readonly
      />
      <template #footer>
        <el-button @click="copyAiLog">复制</el-button>
        <el-button @click="aiLogVisible = false">关闭</el-button>
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
import iconPersonalInfo from "../assets/personal_info.svg";
import iconExam from "../assets/exam.svg";
import iconCorrect from "../assets/correct.svg";

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
const vipIconUrl = "/avatar/vip.svg";
const userId = ref(loginInfo?.cardNo || "");
const startingPractice = ref(false);
const startingExam = ref(false);
const practiceDialogVisible = ref(false);
const practiceCourse = ref(null);
const practiceDifficulty = ref("中");
const practiceQuestionCount = ref(10);
const avatarSaving = ref(false);
const wsStatus = ref("connecting");
const HOME_SIDEBAR_COLLAPSED_KEY = "nexeval.home.sidebar.collapsed";
const sidebarCollapsed = ref(readSidebarCollapsed());
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
const isVipTeacher = computed(() => Boolean(isTeacher.value && teacherInfo.value?.vip));
const displayPhone = computed(() => formatPhoneForDisplay(phone.value));
const actionPanelTitle = computed(() => (isTeacher.value ? "考试批改" : "题目练习与考试"));
const practiceLevels = [
  { value: "易", color: "#A5E617" },
  { value: "中", color: "#02A1E8" },
  { value: "难", color: "#8213E6" }
];
const practiceCourseLabel = computed(() => {
  if (!practiceCourse.value) {
    return "请选择教学班后再开始练习";
  }

  return `${practiceCourse.value.cno || "-"} ｜ ${practiceCourse.value.cname || "未命名课程"}`;
});
const activeMenu = ref("action");
const menuItems = computed(() => {
  const items = [
    { key: "profile", label: "个人信息", icon: iconPersonalInfo },
    { 
      key: "action", 
      label: actionPanelTitle.value,
      icon: isTeacher.value ? iconCorrect : iconExam
    }
  ];
  return items;
});
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
const aiReviewingId = ref("");
const aiLogVisible = ref(false);
const aiLogContent = ref("");
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

function readSidebarCollapsed() {
  try {
    return localStorage.getItem(HOME_SIDEBAR_COLLAPSED_KEY) === "1";
  } catch {
    return false;
  }
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  try {
    localStorage.setItem(HOME_SIDEBAR_COLLAPSED_KEY, sidebarCollapsed.value ? "1" : "0");
  } catch {
    // ignore storage write errors
  }
}

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
  openPracticeDialog({ cno: courseNo, cname: courseName });
}

function openPracticeDialog(clazz) {
  practiceCourse.value = clazz || null;
  practiceDifficulty.value = "中";
  practiceQuestionCount.value = 10;
  practiceDialogVisible.value = true;
}

function practiceLevelCardStyle(level) {
  return {
    borderColor: practiceDifficulty.value === level.value ? level.color : "rgba(42, 92, 255, 0.16)",
    background: practiceDifficulty.value === level.value ? `${level.color}18` : "#ffffff"
  };
}

async function confirmPracticeStart() {
  const courseNo = String(practiceCourse.value?.cno || "").trim();
  const courseName = String(practiceCourse.value?.cname || "").trim();

  if (!courseNo) {
    ElMessage.warning("请先选择教学班");
    return;
  }

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
      courseNo,
      difficulty: practiceDifficulty.value,
      questionCount: practiceQuestionCount.value
    });
    practiceDialogVisible.value = false;
    router.push({
      name: "exam",
      params: {
        sessionId: payload.sessionId
      },
      query: {
        courseNo,
        courseName,
        mode: "practice",
        difficulty: practiceDifficulty.value,
        questionCount: String(practiceQuestionCount.value)
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
  openPracticeDialog(clazz || null);
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

  await router.push({
    name: "grading",
    query: {
      courseNo: selectedClass.value.cno || "",
      courseName: selectedClass.value.cname || "",
      teacherEid: selectedClass.value.eid || "",
      userId: row?.userId || "",
      studentName: row?.name || "",
      sno: row?.sno || ""
    }
  });
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

function canAiReviewAnswer(answer) {
  return isVipTeacher.value && answer?.type === "essay";
}

function hasAiLog(answer) {
  return Boolean(answer?.aiReviewLog);
}

function openAiLog(answer) {
  aiLogContent.value = formatAiLog(String(answer?.aiReviewLog || ""));
  aiLogVisible.value = true;
}

function formatAiLog(raw) {
  const text = String(raw || "").trim();
  if (!text) {
    return "";
  }
  try {
    const value = JSON.parse(text);
    return JSON.stringify(value, null, 2);
  } catch {
    return text;
  }
}

async function copyAiLog() {
  const text = aiLogContent.value || "";
  if (!text) {
    ElMessage.warning("没有可复制的内容");
    return;
  }

  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success("已复制 AI 日志");
  } catch {
    ElMessage.error("复制失败，请手动复制");
  }
}

function reviewStatus(answer) {
  if (!answer || answer.type !== "essay") {
    return "reviewed";
  }
  if (answer.reviewed === true) {
    return "reviewed";
  }
  if (answer.reviewed === false && answer.score != null) {
    return "needs_review";
  }
  return "pending";
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

async function handleAiReviewAnswer(answer) {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }
  if (!answer?.answerId) {
    return;
  }

  aiReviewingId.value = answer.answerId;
  try {
    const payload = await wsClient.request("AI_REVIEW_ANSWER", {
      answerId: String(answer.answerId),
      reviewerId: cardNo.value
    }, 30000);
    const index = gradingAnswers.value.findIndex((item) => item.answerId === payload.answerId);
    if (index !== -1) {
      gradingAnswers.value.splice(index, 1, payload);
    }
    ElMessage.success("AI 批改已完成");
  } catch (error) {
    ElMessage.error(error.message || "AI 批改失败");
  } finally {
    aiReviewingId.value = "";
  }
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.home-shell {
  --home-sidebar-width: 240px;
  display: grid;
  grid-template-columns: var(--home-sidebar-width) 1fr;
  gap: 20px;
  transition: grid-template-columns 0.28s ease;
}

.home-shell.sidebar-collapsed {
  --home-sidebar-width: 100px;
}

.home-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 520px;
  transition: gap 0.28s ease;
}

.side-collapse-row {
  display: flex;
  justify-content: flex-end;
}

.side-collapse-btn {
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  color: var(--ne-text-muted);
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.side-collapse-btn:hover {
  color: var(--ne-primary);
  border-color: rgba(42, 92, 255, 0.4);
}

.side-collapse-icon {
  font-size: 14px;
  line-height: 1;
}

.side-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.side-profile-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.side-name-row {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  flex-wrap: nowrap;
  max-width: 100%;
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
  font-size: 16px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.side-vip-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.side-id {
  color: var(--ne-text-muted);
  font-size: 13px;
}

.side-item-label {
  display: inline-block;
  white-space: nowrap;
}

.side-item-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  display: inline-block;
  vertical-align: middle;
  margin-right: 8px;
}

.home-shell.sidebar-collapsed .side-collapse-row {
  justify-content: center;
}

.home-shell.sidebar-collapsed .side-collapse-btn {
  padding: 6px;
}

.home-shell.sidebar-collapsed .side-collapse-text {
  display: none;
}

.home-shell.sidebar-collapsed .side-profile {
  justify-content: center;
  gap: 0;
}

.home-shell.sidebar-collapsed .side-profile-meta {
  width: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.home-shell.sidebar-collapsed .side-avatar {
  margin: 0 auto;
}

.home-shell.sidebar-collapsed .side-nav {
  align-items: center;
}

.home-shell.sidebar-collapsed .side-item {
  width: 64px;
  padding: 10px 0;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.home-shell.sidebar-collapsed .side-item-icon {
  margin: 0;
}

.home-shell.sidebar-collapsed .side-actions {
  align-items: center;
}

.home-shell.sidebar-collapsed .side-actions .el-button {
  min-width: 64px;
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
  min-height: 72px;
  padding: 16px 28px;
  display: flex;
  flex-direction: row;
  justify-content: center;
  justify-content: space-between;
  gap: 16px;
  overflow: hidden;
  align-items: center;
}

.dashboard-head .card-title {
  margin-bottom: 0;
}

.dashboard-head-main {
  display: grid;
  gap: 4px;
}

.dashboard-subtitle {
  margin: 0;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.dashboard-head-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.dashboard-badge {
  padding: 7px 12px;
  border-radius: 999px;
  background: var(--ne-gradient-primary);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(42, 92, 255, 0.14);
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

.exam-intro {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.exam-intro-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--ne-border);
  background: linear-gradient(135deg, rgba(42, 92, 255, 0.04), rgba(0, 194, 255, 0.06));
}
.exam-intro-title {
  color: var(--ne-text-muted);
  font-size: 12px;
}
.exam-intro-value {
  margin-top: 4px;
  color: var(--ne-text-strong);
  font-weight: 700;
}


.avatar-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
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
  margin-top: 6px;
  text-align: center;
  font-size: 12px;
  color: var(--ne-primary);
}

.profile-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.profile-head .panel-title {
  margin-bottom: 0;
}

.profile-op {
  min-width: 64px;
}

.profile-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.profile-info-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.profile-info-table th,
.profile-info-table td {
  border: 1px solid var(--ne-border);
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.45;
}

.profile-info-table th {
  width: 10%;
  background: #f5f7fa;
  color: var(--ne-text-muted);
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.profile-info-table td {
  width: 23.333%;
  color: var(--ne-text-strong);
  word-break: break-all;
}

.profile-info-table .avatar-label {
  vertical-align: top;
}

.profile-info-table .avatar-cell {
  text-align: center;
  background: #ffffff;
  vertical-align: top;
}

.profile-info-table .avatar-click {
  width: 86px;
  height: 86px;
}

.profile-info-table .avatar-image {
  width: 86px;
  height: 86px;
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

.practice-dialog-meta {
  margin: 12px 0 18px;
}

.practice-dialog-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.ai-icon {
  width: 50px;
  height: 50px;
  display: inline-block;
}

.practice-dialog-course {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.practice-dialog-tip {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.practice-section {
  margin-bottom: 18px;
}

.practice-section-label {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.practice-level-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.practice-level-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 54px;
  border-radius: 14px;
  border: 1px solid rgba(42, 92, 255, 0.16);
  background: #ffffff;
  color: #0f172a;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}

.practice-level-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
}

.practice-level-card.selected {
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}

.practice-level-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.62) inset;
}

.practice-level-label {
  letter-spacing: 0.2px;
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
  flex: 1 1 auto;
  min-width: 0;
  display: grid;
  gap: 6px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.grading-attempt-select {
  flex: 0 0 260px;
  width: 260px;
}

.grading-answer-image {
  margin-top: 8px;
  width: min(220px, 100%);
  border-radius: 8px;
  border: 1px solid var(--ne-border);
  display: block;
}

.grading-answer-text {
  min-width: 0;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.5;
}

.grading-table {
  border: 1px solid var(--ne-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--ne-surface);
}

.grading-table-head,
.grading-table-row-content {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(240px, 320px) 90px 120px 120px 120px 120px;
  align-items: stretch;
}

.grading-table-head {
  background: #f8fafc;
  color: var(--ne-text-muted);
  font-weight: 600;
}

.grading-table-row-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  border-top: 1px solid var(--ne-border);
}

.grading-table-row-content {
  width: max-content;
  min-width: 100%;
}

.grading-table-cell {
  padding: 14px 16px;
  border-right: 1px solid var(--ne-border);
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 100%;
  min-width: 0;
  overflow: hidden;
}

.grading-table-head .grading-table-cell {
  justify-content: center;
}

.grading-table-cell:last-child {
  border-right: none;
}

.grading-table-cell--stem,
.grading-table-cell--answer {
  align-items: flex-start;
  justify-content: flex-start;
}

.grading-table-question,
.grading-table-answer {
  min-width: 0;
}

.grading-table-question {
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.5;
}

.grading-table-answer {
  flex-direction: column;
}

.grading-table-cell--center {
  justify-content: center;
}

.student-subtitle {
  color: var(--ne-text-strong);
  font-weight: 600;
}

.student-count {
  color: var(--ne-text-muted);
  font-size: 12px;
}

@media (max-width: 768px) {
  .grading-head {
    flex-direction: column;
    align-items: stretch;
  }

  .grading-attempt-select {
    flex: 1 1 auto;
    width: 100%;
  }
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

  .dashboard-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard-head-badges {
    justify-content: flex-start;
  }

  .profile-summary,
  .exam-intro {
    grid-template-columns: 1fr;
  }

  .profile-info-table th,
  .profile-info-table td {
    font-size: 13px;
    padding: 8px 10px;
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