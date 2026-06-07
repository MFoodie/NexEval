<template>
  <div v-if="loading" class="placeholder">正在加载教学班...</div>
  <div v-else class="student-course-shell" :class="{ 'student-course-shell--expanded': courseMembersVisible }">
    <div class="student-course-main card">
      <div class="student-search-bar">
        <el-input
          v-model="searchKeyword"
          class="search-input"
          style="width: 260px"
          placeholder="按课程号或课程名搜索"
          size="small"
          clearable
          @clear="handleClearSearch"
          @keyup.enter="handleSearchCourses"
        >
          <template #prefix>
            <img :src="iconQuery" alt="" aria-hidden="true" class="search-prefix-icon" />
          </template>
        </el-input>
        <el-button class="search-button" size="small" type="primary" @click="handleSearchCourses">搜索</el-button>
      </div>

      <div class="section-title">课程列表</div>
      <el-table :data="studentClasses" size="small" class="student-classes-table">
        <el-table-column
          prop="cno"
          label="课程号"
          width="108"
          align="left"
          header-align="left"
          class-name="col-large"
          header-class-name="col-large-header"
        />

        <el-table-column
          prop="cname"
          label="课程名"
          width="120"
          align="left"
          header-align="left"
          class-name="col-cname col-large"
          header-class-name="col-cname-header col-large-header"
        />

        <el-table-column
          prop="teacherName"
          label="教师姓名"
          width="104"
          align="center"
          header-align="center"
          class-name="col-large col-teacher-cell"
          header-class-name="col-large col-teacher-header"
        />

        <el-table-column
          prop="grade"
          label="成绩"
          width="60"
          align="center"
          header-align="center"
          class-name="col-large col-score-cell"
          header-class-name="col-large col-score-header"
        />

        <el-table-column
          prop="classMax"
          label="班级最高"
          width="80"
          align="center"
          header-align="center"
          class-name="col-stat"
          header-class-name="col-stat-header"
        >
          <template #default="{ row }">{{ row.classMax ?? "-" }}</template>
        </el-table-column>

        <el-table-column
          prop="classMin"
          label="班级最低"
          width="80"
          align="center"
          header-align="center"
          class-name="col-stat"
          header-class-name="col-stat-header"
        >
          <template #default="{ row }">{{ row.classMin ?? "-" }}</template>
        </el-table-column>

        <el-table-column
          prop="classAvg"
          label="班级均分"
          width="80"
          align="center"
          header-align="center"
          class-name="col-stat"
          header-class-name="col-stat-header"
        >
          <template #default="{ row }">{{ row.classAvg != null ? formatAvg(row.classAvg) : "-" }}</template>
        </el-table-column>

        <el-table-column
          label="操作"
          width="202"
          align="center"
          header-align="center"
          class-name="col-action"
          header-class-name="col-action-header"
        >
          <template #default="scope">
            <div class="student-action-grid">
              <el-button size="small" class="action-secondary" @click="toggleCourseMembers(scope.row)">
                {{ isCourseMembersOpen(scope.row) ? "收起" : "成员查看" }}
              </el-button>
              <el-button
                type="primary"
                size="small"
                class="action-primary--exam"
                @click="emit('start-exam', scope.row)"
              >
                考试
              </el-button>
              <el-dropdown trigger="hover" placement="bottom-start">
                <el-button size="small" class="action-more action-ellipsis" aria-label="更多操作">...</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="emit('start-practice', scope.row)">题目练习</el-dropdown-item>
                    <el-dropdown-item @click="emit('view-wrong', scope.row, 'PRACTICE')">查看错题</el-dropdown-item>
                    <el-dropdown-item @click="emit('appeal', scope.row)">成绩复核</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <aside v-if="courseMembersVisible" class="student-course-members card">
      <div class="student-course-members__title">{{ activeCourseMembersLabel || "课程成员" }}</div>
      <div v-if="courseMembersLoading" class="placeholder">正在加载...</div>
      <div v-else class="student-course-members__list">
        <div
          v-for="member in courseMembers"
          :key="`${member.userId}-${member.teacher ? 'teacher' : 'student'}`"
          class="student-course-member"
        >
          <img :src="member.avatarUrl" :alt="member.name" class="student-course-member__avatar" />
          <div class="student-course-member__name">{{ member.name }}</div>
          <span v-if="member.teacher" class="student-course-member__tag">教师</span>
        </div>
      </div>
      <div class="student-course-members__footer">
        <el-button size="small" class="student-course-members__collapse" @click="closeCourseMembers">收起</el-button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  loading: { type: Boolean, default: false },
  studentClasses: { type: Array, default: () => [] },
  iconQuery: { type: String, default: "" },
  searchCourses: { type: Function, required: true },
  resetCourses: { type: Function, required: true },
  loadCourseMembers: { type: Function, required: true }
});

const emit = defineEmits(["start-exam", "start-practice", "view-wrong", "appeal"]);

const searchKeyword = ref("");
const courseMembersVisible = ref(false);
const courseMembersLoading = ref(false);
const courseMembers = ref([]);
const activeCourseMembersKey = ref("");
const activeCourseMembersLabel = ref("");

function formatAvg(value) {
  if (value == null) return "-";
  return Number(value).toFixed(1);
}

function courseMembersKeyOf(clazz) {
  return `${clazz?.cno || ""}|${clazz?.eid || ""}`;
}

function isCourseMembersOpen(clazz) {
  return courseMembersVisible.value && activeCourseMembersKey.value === courseMembersKeyOf(clazz);
}

function closeCourseMembers() {
  courseMembersVisible.value = false;
  courseMembersLoading.value = false;
  activeCourseMembersKey.value = "";
  activeCourseMembersLabel.value = "";
  courseMembers.value = [];
}

function syncCourseMembersState() {
  if (!courseMembersVisible.value) {
    return;
  }
  const exists = props.studentClasses.some((clazz) => courseMembersKeyOf(clazz) === activeCourseMembersKey.value);
  if (!exists) {
    closeCourseMembers();
  }
}

watch(() => props.studentClasses, syncCourseMembersState);

async function handleSearchCourses() {
  const keyword = String(searchKeyword.value || "").trim();
  if (!keyword) {
    await props.resetCourses();
    return;
  }
  await props.searchCourses(keyword);
}

async function handleClearSearch() {
  searchKeyword.value = "";
  await props.resetCourses();
}

async function toggleCourseMembers(clazz) {
  const nextKey = courseMembersKeyOf(clazz);
  if (!nextKey.trim()) {
    return;
  }

  if (courseMembersVisible.value && activeCourseMembersKey.value === nextKey) {
    closeCourseMembers();
    return;
  }

  courseMembersVisible.value = true;
  courseMembersLoading.value = true;
  activeCourseMembersKey.value = nextKey;
  activeCourseMembersLabel.value = `${clazz?.cno || ""} ${clazz?.cname || ""}`.trim();

  try {
    const data = await props.loadCourseMembers(clazz);
    courseMembers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    closeCourseMembers();
    ElMessage.error(error.message || "课程成员获取失败");
  } finally {
    courseMembersLoading.value = false;
  }
}
</script>

<style scoped>
.placeholder {
  color: #909399;
  font-size: 14px;
}

.section-title {
  margin-bottom: 8px;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.student-search-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}

.student-search-bar .search-prefix-icon {
  width: 16px;
  height: 16px;
  display: block;
  flex: 0 0 auto;
  margin-left: 6px;
}

.student-search-bar :deep(.el-input__inner) {
  border-radius: 24px !important;
  min-height: 25px;
  padding: 0 14px;
}

.student-search-bar .search-input {
  min-width: 220px;
  flex: 0 0 auto;
}

.student-search-bar .search-button {
  min-width: 52px;
  border-radius: 8px !important;
  padding: 4px 6px !important;
  font-size: 12px !important;
  min-height: auto !important;
}

.student-course-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.student-course-shell--expanded {
  grid-template-columns: minmax(0, 4fr) minmax(220px, 1fr);
}

.student-course-main {
  min-width: 0;
}

.student-action-grid {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  min-height: 60px;
  width: max-content;
  margin: 0 auto;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.student-action-grid > * {
  flex: 0 0 auto;
}

.student-action-grid :deep(.el-button) {
  margin-left: 0 !important;
}

.student-action-grid :deep(.el-dropdown) {
  margin-left: 0 !important;
}

.student-course-main :deep(.student-action-grid) {
  display: inline-flex !important;
  flex-direction: row !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  width: auto !important;
}

.action-secondary {
  padding: 3px 7px;
  font-size: 10px;
  font-weight: 600;
  border-radius: 7px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  color: var(--ne-text-strong);
}

.action-secondary:hover {
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
}

.action-primary--exam {
  padding: 3px 9px;
  font-size: 10px;
  font-weight: 600;
  border-radius: 7px;
  line-height: 1;
}

.action-more {
  padding: 0 5px;
  font-size: 10px;
  border-radius: 7px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  color: var(--ne-text-muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-ellipsis {
  min-width: 24px;
  line-height: 1;
}

.action-more:hover {
  background: var(--ne-hover-bg);
  color: var(--ne-text);
}

.student-course-members {
  min-width: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-course-members__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ne-text-strong);
  line-height: 1.4;
}

.student-course-members__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.student-course-members__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 4px;
}

.student-course-members__collapse {
  min-width: 68px;
  border-radius: 8px;
}

.student-course-member {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--ne-border);
  border-radius: 12px;
  background: rgba(var(--ne-primary-rgb), 0.03);
}

.student-course-member__avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  object-fit: cover;
  flex: 0 0 auto;
}

.student-course-member__name {
  min-width: 0;
  flex: 1 1 auto;
  color: var(--ne-text-strong);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-course-member__tag {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(var(--ne-primary-rgb), 0.12);
  color: var(--ne-primary);
  font-size: 12px;
  font-weight: 700;
}

:deep(.col-large .cell),
.col-large-header {
  font-size: 14px !important;
  font-weight: 600 !important;
}

:deep(.col-score-cell .cell) {
  font-size: 14px !important;
  font-weight: 700 !important;
}

.student-course-main :deep(.student-classes-table .el-table__header-wrapper th:nth-child(4) .cell),
.student-course-main :deep(.student-classes-table .el-table__body-wrapper td:nth-child(4) .cell) {
  position: relative !important;
  left: -1px !important;
}

:deep(.student-classes-table td.col-cname .cell) {
  font-size: 13px !important;
  font-weight: 600 !important;
  line-height: 1.3 !important;
}

:deep(.student-classes-table) .el-table__body-wrapper td:nth-child(3) .cell {
  font-weight: 400 !important;
}

:deep(.student-classes-table) .cell {
  padding-left: 8px !important;
  padding-right: 8px !important;
}

:deep(.student-classes-table td.col-action),
:deep(.student-classes-table th.col-action-header) {
  padding-left: 2px !important;
  padding-right: 2px !important;
}

:deep(.student-classes-table td.col-action .cell),
:deep(.student-classes-table th.col-action-header .cell) {
  width: 100% !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
  overflow: visible !important;
}

@media (max-width: 768px) {
  .student-course-shell--expanded {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
