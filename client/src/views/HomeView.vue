<template>
  <div class="min-h-screen w-full flex home-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <aside class="w-64 flex-shrink-0 p-4 home-sidebar card">
      <div class="side-collapse-row">
        <button
          type="button"
          class="side-collapse-btn"
          :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          @click="toggleSidebar"
        >
          <span class="side-collapse-icon">{{ sidebarCollapsed ? '»' : '«' }}</span>
          <span class="side-collapse-text">{{ sidebarCollapsed ? '展开' : '收起' }}</span>
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

      <nav class="side-nav flex flex-col gap-1">
        <button
          v-for="item in menuItems"
          :key="item.key"
          type="button"
          class="side-item"
          :title="sidebarCollapsed ? item.label : ''"
          :class="{ active: activeMenu === item.key, 'side-item--cat': item.key === 'cat' }"
          @click="setActiveMenu(item.key)"
        >
          <img v-if="item.icon" :src="item.icon" :alt="item.label" class="side-item-icon" />
          <span v-else-if="item.symbol" class="side-item-symbol" aria-hidden="true">{{ item.symbol }}</span>
          <span v-if="!sidebarCollapsed" class="side-item-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="side-actions">
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

    <main class="flex-1 overflow-auto p-8 home-main bg-slate-50">
      <div class="max-w-7xl mx-auto w-full flex flex-col gap-6">
        <section class="card panel-card bg-white rounded-xl border border-gray-200 shadow-sm p-6" v-if="activeMenu === 'profile'">
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

        <section class="card panel-card exam-panel-card bg-white rounded-xl border border-gray-200 shadow-sm p-6" v-else-if="activeMenu === 'action'">
        <div class="mb-6">
          <h2 class="text-2xl font-semibold">{{ actionPanelTitle }}</h2>
          <p class="text-sm">考试、练习与批改在此统一管理。</p>
        </div>
        <div class="exam-intro grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="exam-intro-item bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div class="exam-intro-title text-sm text-gray-500 mb-1">当前视图</div>
            <div class="exam-intro-value text-2xl font-semibold text-gray-900">{{ actionPanelTitle }}</div>
          </div>
          <div class="exam-intro-item bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div class="exam-intro-title text-sm text-gray-500 mb-1">教学班/课程</div>
            <div class="exam-intro-value text-2xl font-semibold text-gray-900">{{ isTeacher ? teacherClasses.length : studentClasses.length }}</div>
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
                <el-table-column prop="grade" label="成绩" width="20">
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
          <div v-else class="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
            <div class="mb-4 flex items-center student-search-bar">
              <el-input
                class="search-input"
                style="width:260px;"
                v-model="searchKeyword"
                placeholder="按课程号或课程名检索"
                size="small"
                clearable
                @clear="fetchStudentClasses"
                @keyup.enter="handleSearchCourses"
              />
              <el-button class="search-button action-primary" size="small" type="primary" @click="handleSearchCourses">检索</el-button>
            </div>
            <div class="section-title">课程列表</div>
            <el-table :data="studentClasses" size="small" class="student-classes-table">
              <el-table-column
                prop="cno"
                label="课程号"
                width="120"
                align="left"
                header-align="left"
                class-name="col-large"
                header-class-name="col-large-header"
              />

              <el-table-column
                prop="cname"
                label="课程名"
                width="130"
                align="left"
                header-align="left"
                class-name="col-cname col-large"
                header-class-name="col-cname-header col-large-header"
              />

              <el-table-column
                prop="teacherName"
                label="教师姓名"
                width="160"
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

              <el-table-column prop="classMax" label="班级最高" width="80" align="center" header-align="center"
                class-name="col-stat" header-class-name="col-stat-header">
                <template #default="{ row }">{{ row.classMax ?? '-' }}</template>
              </el-table-column>
              
              <el-table-column prop="classMin" label="班级最低" width="80" align="center" header-align="center"
                class-name="col-stat" header-class-name="col-stat-header">
                <template #default="{ row }">{{ row.classMin ?? '-' }}</template>
              </el-table-column>
              
              <el-table-column prop="classAvg" label="班级均分" width="80" align="center" header-align="center"
                class-name="col-stat" header-class-name="col-stat-header">
                <template #default="{ row }">{{ row.classAvg != null ? formatAvg(row.classAvg) : '-' }}</template>
              </el-table-column>

              <el-table-column label="操作" width="240" align="center" header-align="center">
                <template #default="scope">
                  <div class="student-action-grid">
                    <el-button
                      type="primary"
                      size="small"
                      class="action-primary"
                      :loading="startingExam"
                      @click="handleStartExamForClass(scope.row)"
                    >
                      进入考试
                    </el-button>
                    <el-button size="mini" class="action-rect" @click="handleStartPracticeForClass(scope.row)">题目练习</el-button>
                    <el-button size="mini" class="action-rect" @click="handleViewWrongQuestions(scope.row, 'PRACTICE')">查看错题</el-button>
                    <el-button size="mini" class="action-rect" @click="openAppealHistory(scope.row)">成绩复核</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
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

        <section
          class="card panel-card bg-white rounded-xl border border-gray-200 shadow-sm p-6"
          v-else-if="activeMenu === 'exam-create' && isExamCreator"
        >
          <div class="mb-6">
            <h2 class="text-2xl font-semibold">组卷出题</h2>
            <p class="text-sm">从题库中选择题目组成试卷。</p>
          </div>

          <div class="exam-create-layout">
            <div class="exam-create-left">
              <div class="section-title">筛选题目</div>
              <div class="exam-create-filters">
                <el-select v-model="examCreateCno" clearable placeholder="课程" class="filter-item">
                  <el-option
                    v-for="c in teacherClasses"
                    :key="c.cno"
                    :label="`${c.cno} ${c.cname}`"
                    :value="c.cno"
                  />
                </el-select>
                <el-select v-model="examCreateType" clearable placeholder="题型" class="filter-item">
                  <el-option
                    v-for="opt in questionTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-select v-model="examCreateDifficulty" clearable placeholder="难度" class="filter-item">
                  <el-option
                    v-for="opt in questionDifficultyOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-input
                  v-model="examCreateKeyword"
                  clearable
                  placeholder="搜索题干关键词"
                  class="filter-item"
                  @keyup.enter="handleSearchQuestions"
                />
                <el-button type="primary" :loading="examCreateSearching" @click="handleSearchQuestions">搜索</el-button>
              </div>

              <div v-if="examCreateSearching" class="placeholder">正在搜索题目...</div>
              <div v-else-if="examCreateSearchResults.length === 0" class="placeholder">
                请选择筛选条件后点击搜索
              </div>
              <el-table
                v-else
                :data="examCreateSearchResults"
                size="small"
                class="question-search-table"
                max-height="400"
              >
                <el-table-column label="题干" min-width="300">
                  <template #default="scope">
                    <span class="question-stem">{{ scope.row.stem }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="题型" width="80">
                  <template #default="scope">
                    {{ formatQuestionType(scope.row.questionType) }}
                  </template>
                </el-table-column>
                <el-table-column label="难度" width="60">
                  <template #default="scope">
                    {{ scope.row.difficulty }}
                  </template>
                </el-table-column>
                <el-table-column label="分值" width="60">
                  <template #default="scope">
                    {{ scope.row.points }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="scope">
                    <el-button
                      size="small"
                      type="primary"
                      :disabled="isQuestionSelected(scope.row.id)"
                      @click="addQuestionToPaper(scope.row)"
                    >
                      {{ isQuestionSelected(scope.row.id) ? '已选' : '加入' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="exam-create-right">
              <div class="section-title">试卷预览</div>
              <el-form label-width="80px" size="small" class="paper-form">
                <el-form-item label="试卷名称">
                  <el-input v-model="examCreatePaperName" placeholder="请输入试卷名称" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input v-model="examCreateDescription" placeholder="试卷描述（可选）" />
                </el-form-item>
                <el-form-item label="考试时长">
                  <el-input-number v-model="examCreateDuration" :min="1" :max="300" /> 分钟
                </el-form-item>
              </el-form>

              <div class="section-title" style="margin-top:4px">发布到教学班</div>
              <el-select
                v-model="examCreateSelectedClasses"
                multiple
                placeholder="选择教学班（可多选）"
                style="width:100%"
              >
                <el-option
                  v-for="c in teacherClasses"
                  :key="`${c.cno}-${c.eid}`"
                  :label="`${c.cno} ${c.cname}`"
                  :value="`${c.cno}|${c.eid}`"
                />
              </el-select>

              <div class="paper-stats">
                <span>共 {{ selectedQuestionCount }} 题</span>
                <span>总分 {{ selectedTotalPoints }} 分</span>
              </div>

              <div v-if="examCreateSelected.length === 0" class="placeholder">尚未选择题目</div>
              <div v-else class="paper-question-list">
                <div
                  v-for="(q, index) in examCreateSelected"
                  :key="q.id"
                  class="paper-question-item"
                >
                  <span class="pq-index">{{ index + 1 }}</span>
                  <span class="pq-type">{{ formatQuestionType(q.questionType) }}</span>
                  <span class="pq-stem">{{ q.stem }}</span>
                  <span class="pq-points">{{ q.points }}分</span>
                  <el-button
                    size="small"
                    type="danger"
                    :icon="'Delete'"
                    circle
                    @click="removeQuestionFromPaper(index)"
                  />
                </div>
              </div>

              <el-button
                type="primary"
                class="create-paper-btn"
                :loading="examCreateSubmitting"
                :disabled="examCreateSelected.length === 0 || !examCreatePaperName.trim()"
                @click="handleCreateExamPaper"
              >
                创建试卷
              </el-button>
            </div>
          </div>
        </section>

        <section
          class="card panel-card exam-panel-card bg-white rounded-xl border border-gray-200 shadow-sm p-6"
          v-else-if="activeMenu === 'cat' && isStudent"
        >
          <div class="mb-6">
            <h2 class="text-2xl font-semibold">CAT 智能自适应练习</h2>
            <p class="text-sm">基于实时作答表现自动调整难度，提供个性化练习路径。</p>
          </div>

          <div v-if="studentLoading" class="placeholder">正在加载教学班...</div>
          <div v-else-if="studentClasses.length" class="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
            <div class="section-title">课程列表</div>
              <el-table :data="studentClasses" size="small" class="student-classes-table">
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
                label="操作"
                width="260"
                align="center"
                header-align="center"
                class-name="col-large"
                header-class-name="col-large-header"
              >
                <template #default="scope">
                  <div class="student-action-buttons flex flex-row items-center justify-end gap-2 whitespace-nowrap">
                    <el-button
                      type="primary"
                      size="small"
                      class="action-primary action-primary--cat"
                      :loading="startingCat"
                      @click="handleStartCatForClass(scope.row)"
                    >
                      开始 CAT 练习
                    </el-button>
                    <div class="student-action-grid">
                      <el-button
                        type="primary"
                        size="small"
                        class="action-primary action-primary--cat"
                        :loading="startingCat"
                        @click="handleStartCatForClass(scope.row)"
                      >
                        开始 CAT 练习
                      </el-button>
                      <el-button size="mini" class="action-rect" @click="handleStartPracticeForClass(scope.row)">题目练习</el-button>
                      <el-button size="mini" class="action-rect" @click="handleViewWrongQuestions(scope.row, 'CAT')">查看错题</el-button>
                      <el-button size="mini" class="action-rect" @click="openAppealHistory(scope.row)">成绩复核</el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else class="placeholder">暂无教学班</div>
        </section>
      </div>
    </main>

    <!-- 个人信息页面下方的水波纹流动效果 -->
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
        <div class="practice-dialog-tip">当前入口仅支持普通练习。</div>
      </div>

      <div class="practice-section">
        <div class="practice-section-label">难度级别</div>
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

    <el-dialog v-model="paperDialogVisible" title="选择试卷" width="520px">
      <div v-if="paperDialogLoading" class="placeholder">正在加载可用试卷...</div>
      <div v-else-if="paperDialogPapers.length === 0" class="placeholder">
        该课程暂无已发布的试卷，将使用默认考试。
      </div>
      <div v-else class="paper-dialog-list">
        <div
          v-for="paper in paperDialogPapers"
          :key="paper.definitionId"
          class="paper-dialog-item"
          @click="handleSelectPaper(paper)"
        >
          <div class="paper-dialog-name">{{ paper.paperName }}</div>
          <div class="paper-dialog-meta">
            {{ paper.description || '无描述' }} · {{ paper.questionCount }} 题 · {{ paper.durationMinutes }} 分钟
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="handleStartDefaultExam">使用默认考试</el-button>
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

    <el-dialog v-model="wrongDialogVisible" title="查看错题" width="760px" :close-on-click-modal="false">
      <div v-if="wrongAnswers.length === 0" class="placeholder">暂无错题</div>
      <div v-else class="wrong-answers-dialog">
        <div class="wrong-card-toolbar flex items-center justify-between mb-4">
          <div class="wrong-card-title text-lg font-semibold">第 {{ wrongQuestionIndex + 1 }} / {{ totalWrong }} 题</div>
          <div class="wrong-card-buttons flex items-center gap-2">
            <el-button size="small" @click="showPrevWrong" :disabled="wrongQuestionIndex === 0">上一题</el-button>
            <el-button size="small" @click="showNextWrong" :disabled="wrongQuestionIndex >= totalWrong - 1">下一题</el-button>
          </div>
        </div>

        <el-card class="wrong-question-card">
          <div class="wrong-question-section">
            <div class="wrong-question-label">题目</div>
            <div class="wrong-question-content">
              <div class="wrong-question-meta">
                <span class="wrong-question-type">{{ formatQuestionType(currentWrong?.type) }}</span>
              </div>
              <div class="wrong-question-text">{{ currentWrong?.stem || '-' }}</div>
              <img
                v-if="currentWrong?.questionImagePath"
                :src="normalizeAnswerImageSrc(currentWrong.questionImagePath)"
                alt="题目图片"
                class="wrong-question-image"
              />
            </div>
          </div>

          <div v-if="currentWrong?.options?.length" class="wrong-question-section">
            <div class="wrong-question-label">选项</div>
            <div class="wrong-question-content wrong-option-list">
              <div
                v-for="(option, index) in currentWrong.options"
                :key="index"
                class="wrong-option-item"
                :class="{
                  selected: isOptionSelected(option, index),
                  correct: showCorrectAnswer && isOptionCorrect(option, index)
                }"
              >
                <span class="wrong-option-label">{{ optionLabel(index) }}</span>
                <span>{{ option }}</span>
              </div>
            </div>
          </div>

          <div class="wrong-question-section">
            <div class="wrong-question-label">你的答案</div>
            <div class="wrong-question-content">
              <div class="wrong-question-text">{{ currentWrong?.answerText || '-' }}</div>
              <img
                v-if="currentWrong?.answerImagePath"
                :src="normalizeAnswerImageSrc(currentWrong.answerImagePath)"
                alt="你的答案图片"
                class="wrong-question-image"
              />
            </div>
          </div>

          <div class="wrong-question-section">
            <div class="wrong-question-label">参考答案</div>
            <div class="wrong-question-content">
              <el-button type="text" @click="toggleShowCorrectAnswer">
                {{ showCorrectAnswer ? '隐藏答案' : '显示答案' }}
              </el-button>
              <div v-if="currentWrong?.type === 'choice'">
                <div v-if="showCorrectAnswer" class="wrong-answer-box">
                  正确选项已标注
                </div>
              </div>
              <div v-else-if="showCorrectAnswer" class="wrong-answer-box">
                {{ currentWrong?.correctAnswer || '标准答案暂无' }}
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <template #footer>
        <el-button @click="wrongDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { clearLogin, getLogin, saveLogin } from "../auth";
import { createExamSocket } from "../ws";
import doveGifUrl from "../assets/dove.gif";
import shipGifUrl from "../assets/ship.gif";
import iconPersonalInfo from "../assets/personal_info.svg";
import iconExam from "../assets/exam.svg";
import iconCorrect from "../assets/correct.svg";
import iconCat from "../assets/CAT.svg";
import iconCreateExam from "../assets/exam.svg";
import iconExit from "../assets/exit.svg";

const route = useRoute();
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
const startingCat = ref(false);
const startingExam = ref(false);
const practiceDialogVisible = ref(false);
const practiceCourse = ref(null);
const practiceDifficulty = ref("中");
const practiceQuestionCount = ref(10);
const avatarSaving = ref(false);
const wsStatus = ref("connecting");
const HOME_ACTIVE_MENU_KEY = "nexeval.home.activeMenu";
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
const isExamCreator = computed(() => Boolean(isTeacher.value && teacherInfo.value?.canCreateExam));
const displayPhone = computed(() => formatPhoneForDisplay(phone.value));
const actionPanelTitle = computed(() => (isTeacher.value ? "考试批改" : "题目练习与考试"));
const practiceLevels = [
  { value: "易", color: "#B5E61D" },
  { value: "中", color: "#00A0E8" },
  { value: "难", color: "#8448CC" }
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
  if (isExamCreator.value) {
    items.push({
      key: "exam-create",
      label: "组卷出题",
      icon: iconCreateExam
    });
  }
  if (isStudent.value) {
    items.push({
      key: "cat",
      label: "CAT 智能自适应练习",
      icon: iconCat
    });
  }
  return items;
});
const teacherClasses = ref([]);
const studentClasses = ref([]);
const teacherLoading = ref(false);
const studentLoading = ref(false);
const selectedClass = ref(null);
const searchKeyword = ref("");
const wrongDialogVisible = ref(false);
const wrongAnswers = ref([]);
const wrongQuestionIndex = ref(0);
const showCorrectAnswer = ref(false);
const currentWrong = computed(() => wrongAnswers.value[wrongQuestionIndex.value] || null);
const totalWrong = computed(() => wrongAnswers.value.length);
const searching = ref(false);
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

const examCreateCno = ref("");
const examCreateType = ref("");
const examCreateDifficulty = ref("");
const examCreateKeyword = ref("");
const examCreateSearchResults = ref([]);
const examCreateSearching = ref(false);
const examCreateSelected = ref([]);
const examCreatePaperName = ref("");
const examCreateDescription = ref("");
const examCreateDuration = ref(60);
const examCreateSubmitting = ref(false);
const examCreateSelectedClasses = ref([]);

const paperDialogVisible = ref(false);
const paperDialogLoading = ref(false);
const paperDialogPapers = ref([]);
const paperDialogClass = ref(null);

const questionTypeOptions = [
  { value: "", label: "全部类型" },
  { value: "CHOICE", label: "选择题" },
  { value: "JUDGE", label: "判断题" },
  { value: "BLANK", label: "填空题" },
  { value: "ESSAY", label: "简答题" }
];
const questionDifficultyOptions = [
  { value: "", label: "全部难度" },
  { value: "easy", label: "易" },
  { value: "medium", label: "中" },
  { value: "hard", label: "难" }
];

const selectedQuestionCount = computed(() => examCreateSelected.value.length);
const selectedTotalPoints = computed(() =>
  examCreateSelected.value.reduce((sum, q) => sum + (q.points || 0), 0)
);

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

function normalizeActiveMenu(value) {
  const text = String(value || "").trim();
  if (text === "profile" || text === "action" || text === "cat" || text === "exam-create") {
    if (text === "cat" && !isStudent.value) {
      return "action";
    }
    if (text === "exam-create" && !isExamCreator.value) {
      return "action";
    }
    return text;
  }
  return "action";
}

function readActiveMenu() {
  try {
    return localStorage.getItem(HOME_ACTIVE_MENU_KEY) || "";
  } catch {
    return "";
  }
}

function setActiveMenu(value) {
  const next = normalizeActiveMenu(value);
  activeMenu.value = next;
  try {
    localStorage.setItem(HOME_ACTIVE_MENU_KEY, next);
  } catch {
    // ignore storage write errors
  }
}

function resolveInitialMenu() {
  const fromQuery = String(route.query.menu || "").trim();
  if (fromQuery) {
    return fromQuery;
  }
  return readActiveMenu();
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
  const sno = String(studentInfo.value?.sno || "").trim();
  const userIdParam = String(cardNo.value || "").trim();
  if (!sno && !userIdParam) {
    studentClasses.value = [];
    return;
  }

  studentLoading.value = true;
  try {
    // 获取常规课程信息（包含 teacherName, grade 等）
    const data = await wsClient.request("GET_STUDENT_CLASSES", { sno, userId: userIdParam }, 20000);
    const base = Array.isArray(data) ? data : [];

    // 同时请求带有班级统计的视图，keyword 为空表示全部
    let stats = [];
    try {
      const s = await wsClient.request("SEARCH_COURSE_SCORES", { userId: cardNo.value, keyword: "" }, 20000);
      stats = Array.isArray(s) ? s : [];
    } catch (e) {
      // 如果统计失败，继续使用基础数据
      stats = [];
    }

    // 按课程号合并统计字段到基础列表，保持 teacherName 和 grade 为主
    const statsMap = new Map();
    for (const item of stats) {
      if (item && item.cno) statsMap.set(String(item.cno), item);
    }

    studentClasses.value = base.map(row => {
      const key = String(row.cno);
      const stat = statsMap.get(key);
      return {
        ...row,
        classMax: stat?.classMax ?? null,
        classMin: stat?.classMin ?? null,
        classAvg: stat?.classAvg ?? null
      };
    });
  } catch (error) {
    ElMessage.error(error.message || "教学班获取失败");
  } finally {
    studentLoading.value = false;
  }
}

function triggerAvatarPicker() {
  avatarInputRef.value?.click();
}

function formatAvg(value) {
  if (value == null) return "-";
  return Number(value).toFixed(1);
}

async function handleSearchCourses() {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，无法检索");
    return;
  }

  // 空关键字时视为重置，直接重新获取学生课程列表
  if (!searchKeyword.value || !String(searchKeyword.value).trim()) {
    await fetchStudentClasses();
    return;
  }

  searching.value = true;
  try {
    const data = await wsClient.request("SEARCH_COURSE_SCORES", { userId: cardNo.value, keyword: searchKeyword.value }, 20000);
    studentClasses.value = Array.isArray(data) ? data : [];
  } catch (err) {
    ElMessage.error(err.message || "检索失败");
  } finally {
    searching.value = false;
  }
}

async function handleViewWrongQuestions(clazz, mode = "PRACTICE") {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接，请稍后再试");
    return;
  }

  if (!clazz?.cno) {
    ElMessage.error("课程信息不完整，无法查看错题");
    return;
  }

  try {
    const attempts = await wsClient.request("GET_EXAM_ATTEMPTS", { courseNo: clazz.cno, userId: cardNo.value, mode }, 20000);
    if (!Array.isArray(attempts) || attempts.length === 0) {
      ElMessage.info("暂无对应练习记录");
      return;
    }

    const latest = attempts[0];
    const sessionId = latest.sessionId || latest.sessionIdString || latest.sessionId;
    if (!sessionId) {
      ElMessage.error("无法解析练习会话");
      return;
    }

    const answers = await wsClient.request("GET_ATTEMPT_ANSWERS", { sessionId }, 20000);
    if (!Array.isArray(answers)) {
      ElMessage.error("获取答题记录失败");
      return;
    }

    const wrongs = answers.filter(a => {
      const type = String(a.type || "").toLowerCase();
      const isIncorrect = a.correct === false || a.correct === 0 || a.correct === 'false';
      const isEssayWithContent = type === "essay" && (String(a.answerText || "").trim() || String(a.answerImagePath || "").trim());
      return isIncorrect || (type === "essay" && a.correct !== true && isEssayWithContent);
    });
    if (wrongs.length === 0) {
      ElMessage.info("没有答错的题目");
      return;
    }

    wrongAnswers.value = wrongs;
    wrongQuestionIndex.value = 0;
    showCorrectAnswer.value = false;
    wrongDialogVisible.value = true;
  } catch (err) {
    ElMessage.error(err.message || "获取错题失败");
  }
}

function showPrevWrong() {
  if (wrongQuestionIndex.value > 0) {
    wrongQuestionIndex.value -= 1;
    showCorrectAnswer.value = false;
  }
}

function showNextWrong() {
  if (wrongQuestionIndex.value < totalWrong.value - 1) {
    wrongQuestionIndex.value += 1;
    showCorrectAnswer.value = false;
  }
}

function optionLabel(index) {
  return String.fromCharCode(65 + index);
}

function normalizeListValue(value) {
  return String(value || "").trim();
}

function isOptionSelected(option, index) {
  if (!currentWrong.value) {
    return false;
  }
  const answer = normalizeListValue(currentWrong.value.answerText);
  const label = optionLabel(index);
  return answer.toUpperCase() === label || normalizeListValue(option) === answer;
}

function isOptionCorrect(option, index) {
  if (!currentWrong.value) {
    return false;
  }
  const correct = normalizeListValue(currentWrong.value.correctAnswer);
  const label = optionLabel(index);
  return correct.toUpperCase() === label || normalizeListValue(option) === correct;
}

function formatQuestionType(type) {
  switch (String(type || "").toLowerCase()) {
    case "choice":
      return "单选题";
    case "judge":
      return "判断题";
    case "blank":
      return "填空题";
    case "essay":
      return "简答题";
    default:
      return "题目";
  }
}

function toggleShowCorrectAnswer() {
  showCorrectAnswer.value = !showCorrectAnswer.value;
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
    borderColor: practiceDifficulty.value === level.value ? level.color : "rgba(var(--ne-primary-rgb), 0.16)",
    background: practiceDifficulty.value === level.value ? `${level.color}18` : "var(--practice-level-card-bg, var(--ne-surface))"
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

async function confirmCatStart() {
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

  startingCat.value = true;
  try {
    const payload = await wsClient.request("START_CAT", {
      userId: userId.value.trim(),
      courseNo
    });
    practiceDialogVisible.value = false;
    setActiveMenu("cat");
    router.push({
      name: "cat",
      params: {
        sessionId: payload.sessionId
      },
      query: {
        courseNo,
        courseName,
        mode: "cat"
      }
    });
  } catch (error) {
    ElMessage.error(error.message || "Failed to start CAT session.");
  } finally {
    startingCat.value = false;
  }
}

async function handleStartExam(courseNo = "", courseName = "", definitionId = "") {
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
      courseNo: String(courseNo || "").trim(),
      definitionId: String(definitionId || "").trim()
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

function handleStartCatForClass(clazz) {
  practiceCourse.value = clazz || null;
  confirmCatStart();
}

async function handleStartExamForClass(clazz) {
  if (!clazz) return;
  const sno = String(studentInfo.value?.sno || "").trim();
  if (!sno) {
    handleStartExam(clazz.cno, clazz.cname);
    return;
  }

  paperDialogClass.value = clazz;
  paperDialogLoading.value = true;
  paperDialogPapers.value = [];
  paperDialogVisible.value = true;

  try {
    const data = await wsClient.request("GET_AVAILABLE_PAPERS", {
      sno,
      userId: String(cardNo.value || "").trim()
    }, 15000);
    const allPapers = Array.isArray(data) ? data : [];
    paperDialogPapers.value = allPapers.filter(
      (p) => p.cno === clazz.cno && p.eid === clazz.eid
    );
  } catch {
    paperDialogPapers.value = [];
  } finally {
    paperDialogLoading.value = false;
  }
}

function handleSelectPaper(paper) {
  paperDialogVisible.value = false;
  handleStartExam(
    paperDialogClass.value?.cno || "",
    paperDialogClass.value?.cname || "",
    paper?.definitionId || ""
  );
}

function handleStartDefaultExam() {
  paperDialogVisible.value = false;
  handleStartExam(
    paperDialogClass.value?.cno || "",
    paperDialogClass.value?.cname || ""
  );
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

async function handleSearchQuestions() {
  if (!wsClient.isOpen()) return;
  examCreateSearching.value = true;
  try {
    const data = await wsClient.request("SEARCH_QUESTIONS", {
      cno: examCreateCno.value,
      questionType: examCreateType.value,
      difficulty: examCreateDifficulty.value,
      keyword: examCreateKeyword.value.trim()
    }, 20000);
    examCreateSearchResults.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || "题目搜索失败");
  } finally {
    examCreateSearching.value = false;
  }
}

function isQuestionSelected(questionId) {
  return examCreateSelected.value.some((q) => q.id === questionId);
}

function addQuestionToPaper(question) {
  if (!isQuestionSelected(question.id)) {
    examCreateSelected.value.push({ ...question });
  }
}

function removeQuestionFromPaper(index) {
  examCreateSelected.value.splice(index, 1);
}

async function handleCreateExamPaper() {
  if (!wsClient.isOpen()) {
    ElMessage.error("WebSocket 未连接");
    return;
  }
  if (!examCreatePaperName.value.trim()) {
    ElMessage.error("请输入试卷名称");
    return;
  }
  if (examCreateSelected.value.length === 0) {
    ElMessage.error("请至少选择一道题目");
    return;
  }

  examCreateSubmitting.value = true;
  try {
    const questionIds = examCreateSelected.value.map((q, i) => ({
      questionId: q.id,
      questionType: q.questionType,
      displayOrder: i + 1
    }));
    const classList = examCreateSelectedClasses.value.map((s) => {
      const [cno, eid] = s.split("|");
      return { cno, eid };
    });
    await wsClient.request("CREATE_EXAM_PAPER", {
      teacherEid: teacherInfo.value?.eid || "",
      paperName: examCreatePaperName.value.trim(),
      description: examCreateDescription.value.trim(),
      durationMinutes: examCreateDuration.value,
      questionIdsJson: JSON.stringify(questionIds),
      classListJson: JSON.stringify(classList)
    }, 30000);
    ElMessage.success("试卷创建成功");
    examCreatePaperName.value = "";
    examCreateDescription.value = "";
    examCreateDuration.value = 60;
    examCreateSelected.value = [];
    examCreateSelectedClasses.value = [];
    examCreateSearchResults.value = [];
  } catch (error) {
    ElMessage.error(error.message || "试卷创建失败");
  } finally {
    examCreateSubmitting.value = false;
  }
}

onMounted(connectWebSocket);

setActiveMenu(resolveInitialMenu());

watch(
  () => route.query.menu,
  (value) => {
    if (value != null && String(value).trim() !== "") {
      setActiveMenu(value);
    }
  }
);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.home-shell {
  --home-sidebar-width: 240px;
  display: grid;
  grid-template-columns: var(--home-sidebar-width) 1fr;
  width: 100%;
  gap: 16px;
  transition: grid-template-columns 0.28s ease;
}

.min-h-screen {
  min-height: 100vh;
}

.w-full {
  width: 100%;
}

.w-64 {
  width: 256px;
}

.flex {
  display: flex;
}

.flex-col {
  flex-direction: column;
}

.flex-row {
  flex-direction: row;
}

.flex-1 {
  flex: 1 1 auto;
}

.flex-shrink-0 {
  flex-shrink: 0;
}

.overflow-auto {
  overflow: auto;
}

.p-8 {
  padding: 32px;
}

.p-4 {
  padding: 16px;
}

.gap-1 {
  gap: 4px;
}

.gap-2 {
  gap: 8px;
}

.gap-4 {
  gap: 16px;
}

.bg-slate-50 {
  background: var(--ne-bg);
}

.bg-white {
  background: var(--ne-surface);
}

.bg-transparent {
  background: transparent;
}

.border {
  border-width: 1px;
  border-style: solid;
}

.border-gray-200 {
  border-color: var(--ne-border);
}

.border-l-4 {
  border-left-width: 4px;
  border-left-style: solid;
}

.border-transparent {
  border-color: transparent;
}

.border-blue-600 {
  border-left-color: var(--ne-primary);
}

.text-gray-500 {
  color: var(--ne-text-muted);
}

.text-gray-900 {
  color: var(--ne-text-strong);
}

.text-sm {
  font-size: 14px;
}

.text-2xl {
  font-size: 22px;
}

.font-semibold {
  font-weight: 600;
}

.mb-1 {
  margin-bottom: 4px;
}

.text-blue-600 {
  color: var(--ne-primary);
}

.shadow-sm {
  box-shadow: var(--ne-shadow-soft);
}

.rounded-xl {
  border-radius: var(--ne-radius-lg);
}

.rounded-md {
  border-radius: var(--ne-radius-md);
}

.grid {
  display: grid;
}

.grid-cols-1 {
  grid-template-columns: minmax(0, 1fr);
}

.p-6 {
  padding: 24px;
}

.p-5 {
  padding: 20px;
}

.max-w-7xl {
  max-width: 1280px;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

.gap-6 {
  gap: 24px;
}

.items-center {
  align-items: center;
}

.justify-end {
  justify-content: flex-end;
}

.whitespace-nowrap {
  white-space: nowrap;
}

.student-search-bar {
  gap: 12px;
  justify-content: flex-end;
}

.student-search-bar .el-input__inner {
  border-radius: 24px !important;
  min-height: 50px;
  padding: 0 14px;
}

.student-search-bar .search-input {
  min-width: 220px;
  flex: 0 0 auto;
}

.student-search-bar .search-button {
  min-width: 82px;
  border-radius: 8px !important;
  padding: 4px 10px !important;
  font-size: 12px !important;
  min-height: auto !important;
}

.wrong-answers-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wrong-card-toolbar {
  gap: 12px;
}

.wrong-option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wrong-option-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f5f7fb;
  color: #333;
}

.wrong-option-item.selected {
  background: #d9ecff;
  border: 1px solid #69b1ff;
}

.wrong-option-item.correct {
  background: #e6f9ed;
  border: 1px solid #67c23a;
}

.wrong-option-label {
  font-weight: 600;
  width: 24px;
}

.wrong-question-meta {
  margin-bottom: 8px;
}

.wrong-question-type {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  background: #f0f6ff;
  color: #606f8b;
  font-size: 12px;
}

.wrong-question-card {
  border-radius: 20px;
  padding: 20px;
  background: var(--ne-surface);
  border: 1px solid var(--ne-border);
}

.wrong-question-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.wrong-question-section:last-child {
  margin-bottom: 0;
}

.wrong-question-label {
  font-weight: 600;
  color: var(--ne-text-strong);
}

.wrong-question-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wrong-question-text {
  min-height: 48px;
  padding: 14px 16px;
  border: 1px solid var(--ne-border);
  border-radius: 16px;
  background: var(--ne-surface-2, #f9fafb);
  color: var(--ne-text-strong);
  white-space: pre-wrap;
}

.wrong-question-image {
  max-width: 100%;
  max-height: 280px;
  border-radius: 14px;
  border: 1px solid var(--ne-border);
  object-fit: contain;
}

.wrong-answer-box {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface-2, #f7fafc);
  min-height: 72px;
  white-space: pre-wrap;
}

.home-shell.sidebar-collapsed {
  --home-sidebar-width: 112px;
}

.home-sidebar {
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 520px;
  transition: gap 0.28s ease;
  background: var(--ne-surface);
  border: 1px solid var(--ne-border);
  border-radius: var(--ne-radius-lg);
  box-shadow: var(--ne-shadow-soft);
  width: var(--home-sidebar-width);
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
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
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
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--ne-border);
  box-shadow: none;
  cursor: pointer;
}

.side-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ne-text-strong);
}

.side-vip-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.side-id {
  color: var(--ne-text-muted);
  font-size: 12px;
}

.side-item-label {
  display: inline-block;
  white-space: nowrap;
  font-size: 14px !important;
  line-height: 20px;
  font-weight: 600 !important;
}

.side-item--cat .side-item-label {
  font-size: 13px !important;
  font-weight: 600 !important;
  letter-spacing: 0;
}

.side-item--cat .side-item-icon {
  width: 24px;
  height: 24px;
  margin-left: -2px;
  margin-right: 4px;
}

.side-item-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
  display: inline-block;
  vertical-align: middle;
  margin-right: 8px;
}

.side-item-symbol {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  font-size: 13px;
  line-height: 1;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.home-shell.sidebar-collapsed .side-profile-meta {
  width: 0;
  height: 0;
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

.home-shell.sidebar-collapsed .side-item-symbol {
  margin: 0;
}

.home-shell.sidebar-collapsed .side-actions {
  align-items: center;
}

.home-shell.sidebar-collapsed .side-actions .el-button {
  min-width: 64px;
}

.home-shell.sidebar-collapsed .home-sidebar {
  padding-left: 0;
  padding-right: 0;
  align-items: center;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-item {
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

.side-item:hover {
  background: var(--ne-hover-bg);
}

.side-item.active {
  color: var(--ne-primary);
  border-left-color: var(--ne-primary);
  background: var(--ne-hover-bg);
}

.side-actions {
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

.home-shell.sidebar-collapsed .side-actions .exit-text {
  display: none;
}

.home-main {
  position: relative;
  z-index: 10;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-content: start;
  align-self: stretch;
  background: transparent;
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
  box-shadow: 0 8px 18px rgba(var(--ne-primary-rgb), 0.14);
}

.panel-card {
  min-height: 0;
  position: relative;
  overflow: hidden;
  width: 100%;
}

.exam-panel-card {
  flex: 1;
  width: 100%;
}

.panel-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto 0;
  height: 0;
  background: transparent;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 20px;
  color: var(--ne-text-strong);
}

.exam-intro {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.exam-intro-item {
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
}
.exam-intro-title {
  color: var(--ne-text-muted);
  font-size: 13px;
  margin-bottom: 6px;
}
.exam-intro-value {
  margin-top: 0;
  color: var(--ne-text-strong);
  font-weight: 700;
  font-size: 20px;
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
  flex-direction: row;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  width: auto;
  white-space: nowrap;
}

.student-action-buttons--wrap {
  flex-wrap: wrap;
  justify-content: flex-start;
  white-space: normal;
}

.student-action-buttons .el-button {
  width: auto;
  box-sizing: border-box;
}

.action-primary {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 8px;
  line-height: 1.2;
}

.action-primary--cat {
  padding: 7px 14px;
  font-size: 12px;
  min-width: 120px;
  line-height: 1.2;
}

.action-more {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 8px;
  border: 1px solid var(--ne-border);
  background: var(--ne-surface);
  color: var(--ne-text-muted);
  cursor: pointer;
}

.action-more:hover {
  background: var(--ne-hover-bg);
  color: var(--ne-text);
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
  color: var(--ne-text-strong);
}

.ai-icon {
  width: 50px;
  height: 50px;
  display: inline-block;
}

.practice-dialog-course {
  font-size: 20px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.practice-dialog-tip {
  margin-top: 6px;
  color: var(--ne-text-muted);
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
  color: var(--ne-text-muted);
}

.practice-mode-switch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid rgba(var(--ne-primary-rgb), 0.18);
  border-radius: 14px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(var(--ne-primary-rgb), 0.06), rgba(var(--ne-accent-rgb), 0.08));
}

.practice-mode-switch-copy {
  min-width: 0;
  flex: 1;
}

:deep(.practice-mode-toggle:not(.is-checked) .el-switch__core) {
  background-color: #A6A6A6;
  border-color: #A6A6A6;
}

:deep(.practice-mode-toggle:not(.is-checked) .el-switch__action) {
  color: #A6A6A6;
}

.practice-level-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.practice-level-grid.disabled {
  opacity: 0.55;
}

.practice-level-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 54px;
  border-radius: 14px;
  border: 1px solid rgba(var(--ne-primary-rgb), 0.16);
  background: var(--ne-surface);
  color: var(--ne-text-strong);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}

.practice-level-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(var(--ne-primary-rgb), 0.08);
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
}

.practice-level-card.selected {
  box-shadow: 0 12px 24px rgba(var(--ne-primary-rgb), 0.12);
}

.practice-level-card.disabled {
  cursor: not-allowed;
}

[data-theme="dark"] .practice-level-card {
  --practice-level-card-bg: #0f1115;
  border-color: #303844;
  color: #f5f7fa;
}

[data-theme="dark"] .practice-level-card:hover {
  background: #171b22;
  border-color: #3d4756;
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

.practice-mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.practice-mode-card {
  border: 1px solid rgba(var(--ne-primary-rgb), 0.18);
  background: var(--ne-surface);
  border-radius: 14px;
  min-height: 98px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.practice-mode-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(var(--ne-primary-rgb), 0.08);
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
}

.practice-mode-card.selected {
  border-color: rgba(var(--ne-primary-rgb), 0.6);
  background: linear-gradient(135deg, rgba(var(--ne-primary-rgb), 0.08), rgba(var(--ne-accent-rgb), 0.1));
}

.practice-mode-card--cat.selected {
  border-color: rgba(var(--ne-accent-rgb), 0.6);
  background: linear-gradient(135deg, rgba(var(--ne-accent-rgb), 0.14), rgba(var(--ne-primary-rgb), 0.08));
}

.practice-mode-title {
  color: var(--ne-text-strong);
  font-size: 14px;
  font-weight: 700;
}

.practice-mode-desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ne-text-muted);
  line-height: 1.5;
}

.practice-cat-tip {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ne-text-muted);
  line-height: 1.55;
  background: rgba(var(--ne-accent-rgb), 0.08);
  border: 1px solid rgba(var(--ne-accent-rgb), 0.2);
  border-radius: 10px;
  padding: 8px 10px;
}

.practice-tip-icon {
  width: 35px;
  height: 35px;
  margin-top: -5px;
  flex: 0 0 auto;
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
  color: var(--ne-text-muted);
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
  background: var(--ne-primary-soft);
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
  border-color: var(--ne-hover-border);
  background: var(--ne-hover-bg);
  box-shadow: var(--ne-shadow-soft);
  transform: translateY(-1px);
}

.class-card.active {
  border-color: rgba(var(--ne-primary-rgb), 0.6);
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

::v-deep(.student-classes-table) .el-table__body-wrapper td:nth-child(1) .cell,
::v-deep(.student-classes-table) .el-table__body-wrapper td:nth-child(2) .cell,
::v-deep(.student-classes-table) .el-table__body-wrapper td:nth-child(3) .cell {
  font-weight: 400 !important;
}

/* Exam creation panel */
.exam-create-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
  min-height: 500px;
}

.exam-create-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-create-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  width: 140px;
}

.question-stem {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.exam-create-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  padding-left: 20px;
}

.paper-form {
  margin-bottom: 0;
}

.paper-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #6b7280;
}

.paper-question-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 400px;
  overflow-y: auto;
}

.paper-question-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 13px;
}

.pq-index {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  border-radius: 50%;
  font-size: 12px;
  flex-shrink: 0;
}

.pq-type {
  color: #6b7280;
  font-size: 12px;
  flex-shrink: 0;
  min-width: 48px;
}

.pq-stem {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pq-points {
  color: #3b82f6;
  font-weight: 500;
  flex-shrink: 0;
}

.create-paper-btn {
  margin-top: 8px;
  width: 100%;
}

@media (max-width: 900px) {
  .exam-create-layout {
    grid-template-columns: 1fr;
  }
  .exam-create-right {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #e5e7eb;
    padding-top: 16px;
  }
}

.paper-dialog-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.paper-dialog-item {
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.paper-dialog-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.paper-dialog-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.paper-dialog-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
</style>