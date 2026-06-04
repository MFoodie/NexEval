<template>
  <section class="admin-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="sidebar card">
      <div class="sidebar-collapse-row">
        <button
          type="button"
          class="sidebar-collapse-btn"
          :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="toggleSidebar"
        >
          <span class="sidebar-collapse-icon">{{ sidebarCollapsed ? "»" : "«" }}</span>
          <span class="sidebar-collapse-text">{{ sidebarCollapsed ? "展开" : "收起" }}</span>
        </button>
      </div>

      <div class="sidebar-profile">
        <img :src="avatarUrl" alt="管理员头像" class="sidebar-avatar" @click="triggerAvatarPicker" />
        <div class="sidebar-profile-meta">
          <div class="sidebar-name-row">
            <div class="sidebar-name">{{ loginInfo?.name || "管理员" }}</div>
          </div>
          <div class="sidebar-id">卡号 {{ loginInfo?.cardNo || "-" }}</div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          :title="sidebarCollapsed ? item.label : ''"
          :class="['nav-item', { active: activeMenu === item.key }]"
          type="button"
          @click="activeMenu = item.key"
        >
          <img v-if="item.icon" :src="item.icon" :alt="item.label" class="nav-item-icon" />
          <span v-if="!sidebarCollapsed" class="nav-item-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-actions">
        <el-button text type="danger" @click="handleLogout">
          <img :src="iconExit" alt="退出" class="exit-icon" />
          <span class="exit-text">{{ sidebarCollapsed ? '退' : '退出登录' }}</span>
        </el-button>
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
      <section class="card panel-card profile-panel-card" v-if="activeMenu === 'profile'">
        <div class="profile-head">
          <h2 class="panel-title">简介</h2>
          <div class="profile-head-actions">
            <el-button class="profile-op" type="primary" size="small" @click="openEditDialog">修改个人信息</el-button>
            <el-button class="profile-op" size="small" :loading="avatarSaving" @click="handleResetAvatar">
              恢复默认头像
            </el-button>
          </div>
        </div>
        <div class="admin-info-wrap">
          <table class="profile-info-table">
            <tbody>
              <tr>
                <th class="avatar-label" rowspan="3">头像</th>
                <td class="avatar-cell" rowspan="3">
                  <div class="table-avatar-wrap">
                    <div class="avatar-click" @click="triggerAvatarPicker">
                      <img :src="avatarUrl" alt="管理员头像" class="admin-avatar" />
                    </div>
                    <div class="avatar-tip">点击修改头像</div>
                  </div>
                </td>
                <th>卡号</th>
                <td>{{ loginInfo?.cardNo || '-' }}</td>
                <th>姓名</th>
                <td>{{ loginInfo?.name || '-' }}</td>
              </tr>
              <tr>
                <th>性别</th>
                <td>{{ sexText }}</td>
                <th>手机号</th>
                <td>{{ displayPhone }}</td>
              </tr>
              <tr>
                <th>邮箱</th>
                <td>{{ loginInfo?.email || '-' }}</td>
                <th>角色</th>
                <td>{{ loginInfo?.type === 'admin' ? '管理员' : (loginInfo?.type || '-') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="card panel-card register-panel-card" v-if="activeMenu === 'register'">
        <h2 class="panel-title">用户信息注册</h2>
        <el-form label-position="top" @submit.prevent>
          <div class="register-grid">
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
            <el-radio-group v-model="form.type" class="same-radio-group">
              <el-radio class="same-radio" value="student">学生</el-radio>
              <el-radio class="same-radio" value="teacher">教师</el-radio>
            </el-radio-group>
          </el-form-item>

          </div>

          <template v-if="form.type === 'student'">
            <div class="register-grid section-offset">
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
            </div>
          </template>

          <template v-else>
            <div class="register-grid section-offset">
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
            </div>
          </template>

          <div class="register-footer-actions">
            <el-button type="primary" :loading="saving" @click="handleRegister">注册用户</el-button>
            <el-button @click="resetForm">重置</el-button>
            <el-button :loading="importingType === 'STUDENTS'" @click="triggerImport('STUDENTS')">批量导入学生</el-button>
            <el-button :loading="importingType === 'TEACHERS'" @click="triggerImport('TEACHERS')">批量导入教师</el-button>
          </div>
        </el-form>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'curriculum'">
        <h2 class="panel-title">课程及教学班管理</h2>
        <div class="curriculum-wrapper">
          <div class="curriculum-section">
            <h3 class="curriculum-subtitle">课程信息管理</h3>
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
            </el-form>
          </div>
          <div class="curriculum-section">
            <h3 class="curriculum-subtitle">教学班管理</h3>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="课程编号">
                <el-input v-model="classForm.cno" maxlength="8" placeholder="例如 BJSL0001" />
              </el-form-item>
              <el-form-item label="教师工号">
                <el-input v-model="classForm.eid" maxlength="8" placeholder="例如 09T09011" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <div class="action-row">
          <el-button type="primary" :loading="courseSaving" @click="handleCreateCourse">新增课程</el-button>
          <el-button :loading="importingType === 'COURSES'" @click="triggerImport('COURSES')">批量导入课程</el-button>
          <el-button type="primary" :loading="classSaving" @click="handleCreateClass">新增教学班</el-button>
          <el-button :loading="importingType === 'CLASSES'" @click="triggerImport('CLASSES')">批量导入教学班</el-button>
          <el-button :loading="importingType === 'SC'" @click="triggerImport('SC')">批量导入选课记录</el-button>
        </div>
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
        <div v-if="appealLoading" class="placeholder">正在加载复核申请...</div>
        <div v-else-if="scoreAppeals.length === 0" class="placeholder">暂无成绩复核申请</div>
        <el-table v-else :data="scoreAppeals" size="small">
          <el-table-column prop="userId" label="卡号" width="90" />
          <el-table-column prop="courseNo" label="课程号" width="90" />
          <el-table-column prop="reason" label="申请说明" width="240" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="scope">
              {{ formatAppealStatus(scope.row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="150">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="处理结果" min-width="90">
            <template #default="scope">
              <div>
                <div>{{ scope.row.handledBy || '-' }}</div>
                <div class="muted-text">{{ scope.row.handledNote || '-' }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <div class="action-buttons">
                <el-button
                  v-if="scope.row.status === 'pending'"
                  size="small"
                  class="approve-button"
                  plain
                  :loading="appealActionLoading === scope.row.id"
                  @click="handleReviewAppeal(scope.row, true)"
                >
                  同意
                </el-button>
                <el-button
                  v-if="scope.row.status === 'pending'"
                  size="small"
                  type="danger"
                  class="reject-button"
                  plain
                  :loading="appealActionLoading === scope.row.id"
                  @click="handleReviewAppeal(scope.row, false)"
                >
                  拒绝
                </el-button>
                <span v-else>已处理</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'teacher-vip'">
        <div class="panel-head">
          <h2 class="panel-title">教师权限</h2>
          <div class="vip-toolbar admin-search-bar">
            <el-input
              v-model="vipKeyword"
              clearable
              placeholder="搜索工号/卡号/姓名"
              class="vip-search search-input"
              size="small"
              @keyup.enter="loadTeacherVips"
            >
              <template #prefix>
                <img :src="iconQuery" alt="" aria-hidden="true" class="search-prefix-icon" />
              </template>
            </el-input>
            <el-select v-model="vipStatus" class="vip-filter search-select" placeholder="VIP 状态" size="small">
              <el-option label="全部" value="all" />
              <el-option label="仅 VIP" value="vip" />
              <el-option label="非 VIP" value="nonvip" />
            </el-select>
            <el-button class="search-button action-primary" size="small" type="primary" :loading="vipLoading" @click="loadTeacherVips">查询</el-button>
          </div>
        </div>

        <div v-if="vipLoading" class="placeholder">正在加载教师列表...</div>
        <div v-else-if="vipTeachers.length === 0" class="placeholder">暂无教师数据</div>
        <el-table v-else :data="vipTeachers" size="small" class="vip-table">
          <el-table-column prop="eid" label="工号" width="120" />
          <el-table-column prop="userId" label="卡号" width="120" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="department" label="学院" min-width="160" />
          <el-table-column label="职称" width="120">
            <template #default="scope">
              {{ formatTeacherTitle(scope.row.title) }}
            </template>
          </el-table-column>
          <el-table-column label="VIP" width="110">
            <template #default="scope">
              <el-switch
                class="vip-switch"
                :model-value="scope.row.vip"
                :loading="vipUpdatingId === scope.row.eid"
                @change="(value) => handleVipToggle(scope.row, value)"
              />
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="card panel-card" v-if="activeMenu === 'teacher-exam-perm'">
        <div class="panel-head">
          <h2 class="panel-title">出题权限</h2>
          <div class="vip-toolbar admin-search-bar">
            <el-input
              v-model="examPermKeyword"
              clearable
              placeholder="搜索工号/卡号/姓名"
              class="vip-search search-input"
              size="small"
              @keyup.enter="loadTeacherExamPerms"
            >
              <template #prefix>
                <img :src="iconQuery" alt="" aria-hidden="true" class="search-prefix-icon" />
              </template>
            </el-input>
            <el-select v-model="examPermStatus" class="vip-filter search-select" placeholder="出题权限状态" size="small">
              <el-option label="全部" value="all" />
              <el-option label="仅可出题" value="permitted" />
              <el-option label="不可出题" value="not_permitted" />
            </el-select>
            <el-button class="search-button action-primary" size="small" type="primary" :loading="examPermLoading" @click="loadTeacherExamPerms">查询</el-button>
          </div>
        </div>

        <div v-if="examPermLoading" class="placeholder">正在加载教师列表...</div>
        <div v-else-if="examPermTeachers.length === 0" class="placeholder">暂无教师数据</div>
        <el-table v-else :data="examPermTeachers" size="small" class="vip-table">
          <el-table-column prop="eid" label="工号" width="120" />
          <el-table-column prop="userId" label="卡号" width="120" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="department" label="学院" min-width="160" />
          <el-table-column label="职称" width="120">
            <template #default="scope">
              {{ formatTeacherTitle(scope.row.title) }}
            </template>
          </el-table-column>
          <el-table-column label="可出题" width="110">
            <template #default="scope">
              <el-switch
                :model-value="scope.row.canCreateExam"
                :loading="examPermUpdatingId === scope.row.eid"
                @change="(value) => handleExamPermToggle(scope.row, value)"
              />
            </template>
          </el-table-column>
        </el-table>
      </section>

      <input
        ref="importInputRef"
        class="avatar-input"
        type="file"
        accept=".xlsx"
        @change="handleImportFileChange"
      />
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
        <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <div class="water-wave-container">
      <div class="water-wave">
        <div class="wave-layer wave-layer-1"></div>
        <div class="wave-layer wave-layer-2"></div>
        <div class="wave-layer wave-layer-3"></div>
      </div>
    </div>

    <div v-if="activeMenu === 'profile'" class="profile-ship-container" aria-hidden="true">
      <img :src="shipGifUrl" alt="" class="profile-ship" />
      <img :src="doveGifUrl" alt="" class="profile-dove" />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { clearLogin, getLogin, saveLogin } from "../auth";
import { createExamSocket } from "../ws";
import doveGifUrl from "../assets/dove.gif";
import shipGifUrl from "../assets/ship.gif";
import iconPersonalInfo from "../assets/personal_info.svg";
import iconRegister from "../assets/register.svg";
import iconClass from "../assets/class.svg";
import iconRecheck from "../assets/recheck.svg";
import iconVIP from "../assets/VIP.svg";
import iconExamPerm from "../assets/examperm.svg";
import iconQuery from "../assets/query.svg";
import iconExit from "../assets/exit.svg";

const router = useRouter();
const loginInfo = reactive(getLogin() || {});
const wsClient = createExamSocket(null, {
  onOpen() {
    if (activeMenu.value === "review") {
      loadScoreAppeals();
    }
    if (activeMenu.value === "teacher-vip") {
      loadTeacherVips();
    }
    if (activeMenu.value === "teacher-exam-perm") {
      loadTeacherExamPerms();
    }
  }
});
const avatarSaving = ref(false);
const avatarInputRef = ref(null);
const importInputRef = ref(null);
const avatarUrl = ref(withAvatarVersion(loginInfo?.avatarUrl || "/avatar/admin_male.png"));
const ADMIN_SIDEBAR_COLLAPSED_KEY = "nexeval.admin.sidebar.collapsed";
const sidebarCollapsed = ref(readSidebarCollapsed());
const activeMenu = ref("profile");
const importingType = ref("");
const pendingImportType = ref("");
const importResult = ref(null);
const scoreAppeals = ref([]);
const appealLoading = ref(false);
const appealActionLoading = ref(null);
const vipTeachers = ref([]);
const vipLoading = ref(false);
const vipUpdatingId = ref("");
const vipKeyword = ref("");
const vipStatus = ref("all");
const editVisible = ref(false);
const profileSaving = ref(false);
const editForm = ref({
  name: "",
  phone: "",
  email: "",
  newPassword: ""
});

const examPermTeachers = ref([]);
const examPermLoading = ref(false);
const examPermUpdatingId = ref("");
const examPermKeyword = ref("");
const examPermStatus = ref("all");

const menuItems = [
  { key: "profile", label: "个人信息", icon: iconPersonalInfo },
  { key: "register", label: "用户信息注册", icon: iconRegister },
  { key: "curriculum", label: "课程及教学班管理", icon: iconClass },
  { key: "review", label: "成绩复核审理", icon: iconRecheck },
  { key: "teacher-vip", label: "教师 VIP 权限", icon: iconVIP },
  { key: "teacher-exam-perm", label: "出题权限", icon: iconExamPerm }
];

watch(activeMenu, (value) => {
  if (value === "review") {
    loadScoreAppeals();
  }
  if (value === "teacher-vip") {
    loadTeacherVips();
  }
  if (value === "teacher-exam-perm") {
    loadTeacherExamPerms();
  }
});

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
  return formatPhoneForDisplay(loginInfo?.phone);
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
    return "鏈缃?";
  }

  if (passwordStrengthScore.value <= 1) {
    return "寮?";
  }

  if (passwordStrengthScore.value === 2) {
    return "涓?";
  }

  return "寮?";
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

function readSidebarCollapsed() {
  try {
    return localStorage.getItem(ADMIN_SIDEBAR_COLLAPSED_KEY) === "1";
  } catch {
    return false;
  }
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  try {
    localStorage.setItem(ADMIN_SIDEBAR_COLLAPSED_KEY, sidebarCollapsed.value ? "1" : "0");
  } catch {
    // ignore storage write errors
  }
}

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

function formatTeacherTitle(title) {
  const normalized = String(title || "").trim().toLowerCase();
  if (normalized === "professor") {
    return "教授";
  }
  if (normalized === "associate_professor") {
    return "副教授";
  }
  if (normalized === "lecture" || normalized === "lecturer") {
    return "讲师";
  }
  return title || "-";
}

function formatAppealStatus(status) {
  const normalized = String(status || "").trim().toLowerCase();
  if (normalized === "pending") {
    return "待处理";
  }
  if (normalized === "approved") {
    return "已批准";
  }
  if (normalized === "rejected") {
    return "已拒绝";
  }
  return status || "-";
}

function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) {
    return "-";
  }
  const str = String(dateTimeStr).trim();
  return str.replace("T", "  ").replace("Z", "");
}

function withAvatarVersion(url) {
  if (!url) {
    return "/avatar/admin_male.png";
  }

  const divider = url.includes("?") ? "&" : "?";
  return `${url}${divider}t=${Date.now()}`;
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

function applyProfile(profile) {
  const next = {
    ...loginInfo,
    ...profile,
    cardNo: profile?.id || profile?.cardNo || loginInfo.cardNo || "",
    name: profile?.name || "",
    phone: profile?.phone || "",
    email: profile?.email || "",
    avatarUrl: profile?.avatarUrl || loginInfo.avatarUrl || "",
    type: profile?.type || loginInfo.type || "",
    sex: typeof profile?.sex === "boolean" ? profile.sex : loginInfo.sex
  };

  Object.assign(loginInfo, next);
  saveLogin(next);
  avatarUrl.value = withAvatarVersion(next.avatarUrl || "/avatar/admin_male.png");
}

function openEditDialog() {
  editForm.value = {
    name: loginInfo?.name || "",
    phone: displayPhone.value === "-" ? "" : displayPhone.value,
    email: loginInfo?.email || "",
    newPassword: ""
  };
  editVisible.value = true;
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

    applyProfile(profile);
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

async function handleSaveProfile() {
  if (!editForm.value.name.trim()) {
    ElMessage.warning("姓名不能为空");
    return;
  }

  if (editForm.value.newPassword && passwordStrengthScore.value < 3) {
    ElMessage.warning("新密码复杂度不足，需至少满足三种字符类型");
    return;
  }

  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  profileSaving.value = true;
  try {
    const profile = await wsClient.request("UPDATE_PROFILE", {
      userId: loginInfo?.cardNo,
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
    profileSaving.value = false;
  }
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

async function loadScoreAppeals() {
  if (!wsClient.isOpen()) {
    return;
  }

  appealLoading.value = true;
  try {
    const data = await wsClient.request("GET_SCORE_APPEALS", {});
    scoreAppeals.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "复核申请获取失败");
  } finally {
    appealLoading.value = false;
  }
}

async function handleReviewAppeal(appeal, approved) {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  appealActionLoading.value = appeal?.id ?? null;
  try {
    await wsClient.request("REVIEW_SCORE_APPEAL", {
      appealId: String(appeal.id),
      approved,
      reviewerId: loginInfo?.cardNo || "",
      handledNote: approved ? "同意复核，已将该考试大题清零" : "已拒绝复核申请"
    });
    ElMessage.success(approved ? "已同意复核申请" : "已拒绝复核申请");
    await loadScoreAppeals();
  } catch (error) {
    ElMessage.error(error.message || "处理失败");
  } finally {
    appealActionLoading.value = null;
  }
}

async function loadTeacherVips() {
  if (!wsClient.isOpen()) {
    return;
  }

  vipLoading.value = true;
  try {
    const data = await wsClient.request("GET_TEACHER_VIPS", {
      keyword: vipKeyword.value.trim(),
      vipStatus: vipStatus.value
    }, 20000);
    vipTeachers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "教师列表获取失败");
  } finally {
    vipLoading.value = false;
  }
}

async function handleVipToggle(row, value) {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }
  if (!row?.eid) {
    return;
  }

  const previous = row.vip;
  row.vip = value;
  vipUpdatingId.value = row.eid;
  try {
    const payload = await wsClient.request("UPDATE_TEACHER_VIP", {
      eid: row.eid,
      vip: value
    });
    const index = vipTeachers.value.findIndex((item) => item.eid === payload.eid);
    if (index !== -1) {
      vipTeachers.value.splice(index, 1, payload);
    }
    ElMessage.success("VIP 权限已更新");
  } catch (error) {
    row.vip = previous;
    ElMessage.error(error.message || "VIP 权限更新失败");
  } finally {
    vipUpdatingId.value = "";
  }
}

async function loadTeacherExamPerms() {
  if (!wsClient.isOpen()) return;
  examPermLoading.value = true;
  try {
    const data = await wsClient.request("GET_TEACHER_EXAM_PERMS", {
      keyword: examPermKeyword.value.trim(),
      permStatus: examPermStatus.value
    }, 20000);
    examPermTeachers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "教师列表获取失败");
  } finally {
    examPermLoading.value = false;
  }
}

async function handleExamPermToggle(row, value) {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }
  if (!row?.eid) return;

  const previous = row.canCreateExam;
  row.canCreateExam = value;
  examPermUpdatingId.value = row.eid;
  try {
    const payload = await wsClient.request("UPDATE_TEACHER_EXAM_PERM", {
      eid: row.eid,
      canCreateExam: value
    });
    const index = examPermTeachers.value.findIndex((item) => item.eid === payload.eid);
    if (index !== -1) {
      examPermTeachers.value.splice(index, 1, payload);
    }
    ElMessage.success("出题权限已更新");
  } catch (error) {
    row.canCreateExam = previous;
    ElMessage.error(error.message || "出题权限更新失败");
  } finally {
    examPermUpdatingId.value = "";
  }
}

onMounted(() => {
  if (activeMenu.value === "review") {
    loadScoreAppeals();
  }
  if (activeMenu.value === "teacher-vip") {
    loadTeacherVips();
  }
  if (activeMenu.value === "teacher-exam-perm") {
    loadTeacherExamPerms();
  }
});

onBeforeUnmount(() => {
  wsClient.close();
});
</script>

<style scoped>
.admin-shell {
  --admin-sidebar-width: 240px;
  display: grid;
  grid-template-columns: var(--admin-sidebar-width) 1fr;
  min-height: 100%;
  align-items: stretch;
  gap: 16px;
  transition: grid-template-columns 0.28s ease;
}

.admin-shell.sidebar-collapsed {
  --admin-sidebar-width: 112px;
}

.sidebar {
  position: relative;
  z-index: 10;
  min-height: 100%;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: gap 0.28s ease;
  background: var(--ne-surface);
  border: 1px solid var(--ne-border);
  border-radius: var(--ne-radius-lg);
  box-shadow: var(--ne-shadow-soft);
  width: var(--admin-sidebar-width);
}

.sidebar-collapse-row {
  display: flex;
  justify-content: flex-end;
}

.sidebar-collapse-btn {
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

.sidebar-collapse-btn:hover {
  color: var(--ne-primary);
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
}

.sidebar-collapse-icon {
  font-size: 14px;
  line-height: 1;
}

.sidebar-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.sidebar-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--ne-border);
  box-shadow: none;
  cursor: pointer;
}

.sidebar-profile-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.sidebar-name-row {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  flex-wrap: nowrap;
  max-width: 100%;
}

.sidebar-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ne-text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-id {
  white-space: nowrap;
  color: var(--ne-text-muted);
  font-size: 12px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  border: 1px solid transparent;
  background: transparent;
  border-radius: 10px;
  padding: 8px 12px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
  color: var(--ne-text);
  transition: all 0.2s ease;
  border-left-width: 4px;
  border-left-style: solid;
  border-left-color: transparent;
}

.nav-item:hover {
  background: var(--ne-hover-bg);
}

.nav-item.active {
  color: var(--ne-primary);
  border-left-color: var(--ne-primary);
  background: var(--ne-hover-bg);
}

.nav-item-label {
  display: inline-block;
  white-space: nowrap;
}

.nav-item-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  display: inline-block;
  vertical-align: middle;
  margin-right: 8px;
}

.admin-shell.sidebar-collapsed .sidebar-collapse-row {
  justify-content: center;
}

.admin-shell.sidebar-collapsed .sidebar-collapse-btn {
  padding: 6px;
}

.admin-shell.sidebar-collapsed .sidebar-collapse-text {
  display: none;
}

.admin-shell.sidebar-collapsed .sidebar-name,
.admin-shell.sidebar-collapsed .sidebar-id {
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.admin-shell.sidebar-collapsed .sidebar-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.admin-shell.sidebar-collapsed .sidebar {
  padding-left: 0;
  padding-right: 0;
  align-items: center;
}

.admin-shell.sidebar-collapsed .sidebar-avatar {
  margin: 0 auto;
}

.admin-shell.sidebar-collapsed .sidebar-nav {
  align-items: center;
}

.admin-shell.sidebar-collapsed .nav-item {
  width: 64px;
  padding: 10px 0;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.admin-shell.sidebar-collapsed .nav-item-icon {
  margin: 0;
}

.admin-shell.sidebar-collapsed .sidebar-actions {
  align-items: center;
}

.admin-shell.sidebar-collapsed .sidebar-actions .el-button {
  min-width: 64px;
}

.sidebar-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.exit-icon {
  width: 18px;
  height: 18px;
  vertical-align: middle;
  margin-right: 8px;
}

.admin-shell.sidebar-collapsed .sidebar-actions .exit-text {
  display: none;
}

.main-panel {
  position: relative;
  z-index: 10;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
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

.register-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.section-offset {
  margin-top: 16px;
}

.same-radio-group {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.same-radio .el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--ne-primary);
  background-color: var(--ne-primary);
}

:deep(.same-radio .el-radio__input.is-checked + .el-radio__label) {
  color: var(--ne-primary);
}

.register-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.register-grid :deep(.el-divider) {
  grid-column: 1 / -1;
  margin: 0;
}

.register-footer-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-start;
  margin-top: 24px;
  flex-wrap: nowrap;
}

.register-footer-actions > .el-button {
  white-space: nowrap;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.vip-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-search-bar {
  gap: 8px;
  justify-content: flex-end;
}

.admin-search-bar .search-prefix-icon {
  width: 16px;
  height: 16px;
  display: block;
  flex: 0 0 auto;
  margin-left: 6px;
}

.admin-search-bar :deep(.el-input__inner) {
  border-radius: 20px !important;
  min-height: 25px;
  padding: 0 12px;
}

.admin-search-bar :deep(.el-select .el-input__wrapper) {
  border-radius: 20px !important;
  min-height: 25px;
  padding: 0 8px;
}

.vip-search {
  width: 220px;
  flex: 0 0 auto;
}

.vip-filter {
  width: 110px;
}

.admin-search-bar .search-button {
  min-width: 52px;
  border-radius: 8px !important;
  padding: 4px 6px !important;
  font-size: 12px !important;
  min-height: auto !important;
}

.avatar-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.table-avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-click {
  position: relative;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--ne-border);
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

.admin-info-wrap {
  display: grid;
  gap: 16px;
  align-items: start;
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
  background: var(--ne-primary-soft);
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
  background: var(--ne-surface);
  vertical-align: top;
}

.admin-avatar {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-info-table .avatar-click {
  width: 86px;
  height: 86px;
}

:deep(.male-radio .el-radio__input.is-checked .el-radio__inner) {
  border-color: #06A7FF;
  background-color: #06A7FF;
}

:deep(.male-radio .el-radio__input.is-checked + .el-radio__label) {
  color: #06A7FF;
}

:deep(.female-radio .el-radio__input.is-checked .el-radio__inner) {
  border-color: #FF00FF;
  background-color: #FF00FF;
}

:deep(.female-radio .el-radio__input.is-checked + .el-radio__label) {
  color: #FF00FF;
}

:deep(.vip-switch.is-checked .el-switch__core) {
  border-color: #E0BF09;
  background-color: #E0BF09;
}

:deep(.vip-switch.is-checked .el-switch__action) {
  color: #E0BF09;
}

.panel-card {
  min-height: 400px;
}

.profile-panel-card {
  min-height: 260px;
}

.register-panel-card {
  min-height: 0;
}

@media (max-width: 1200px) {
  .register-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .register-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

[data-theme="dark"] .sidebar-name,
[data-theme="dark"] .sidebar-id,
[data-theme="dark"] .nav-item,
[data-theme="dark"] .nav-item-label,
[data-theme="dark"] .sidebar-collapse-btn,
[data-theme="dark"] .sidebar-collapse-text {
  color: #ffffff;
}

[data-theme="dark"] .nav-item.active {
  color: #ffffff;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 20px;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 14px;
  align-items: start;
  margin-top: 12px;
}

.register-grid .el-divider {
  grid-column: 1 / -1;
  margin: 12px 0 4px;
}

.register-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.action-row {
  margin-top: 20px;
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

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.approve-button.el-button.is-plain {
  background-color: rgba(16, 185, 22, 0.12) !important;
  border-color: #1bb91073 !important;
  color: #51AD13 !important;
  box-shadow: none !important;
}

.approve-button.el-button.is-plain:hover,
.approve-button.el-button.is-plain:focus {
  background-color: #11D635 !important;
  border-color: #11D635 !important;
  color: #ffffff !important;
}

.approve-button.el-button.is-plain:active {
  background-color: #51AD13 !important;
  border-color: #51AD13 !important;
  color: #ffffff !important;
}

[data-theme="dark"] .reject-button.el-button.is-plain {
  background-color: rgba(80, 24, 24, 0.92) !important;
}

[data-theme="dark"] .reject-button.el-button.is-plain:hover,
[data-theme="dark"] .reject-button.el-button.is-plain:focus {
  background-color: #F56C6C !important;
  color: #ffffff !important;
}

[data-theme="dark"] .reject-button.el-button.is-plain:active {
  background-color: #F56C6C !important;
  color: #ffffff !important;
}

.muted-text {
  color: #909399;
  font-size: 12px;
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

.curriculum-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 20px;
}

.curriculum-section {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background-color: #f9fafb;
}

.curriculum-subtitle {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 500;
  color: #374151;
}

[data-theme="dark"] .curriculum-section {
  background-color: #1f2430;
  border-color: #2f3744;
}

[data-theme="dark"] .curriculum-subtitle {
  color: #e5e7eb;
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .register-grid {
    grid-template-columns: 1fr;
  }

  .register-grid .el-divider {
    grid-column: auto;
  }

  .curriculum-wrapper {
    grid-template-columns: 1fr;
  }

  .profile-info-table th,
  .profile-info-table td {
    font-size: 13px;
    padding: 8px 10px;
  }
}
</style>
