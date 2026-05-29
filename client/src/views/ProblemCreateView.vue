<template>
  <div class="problem-create-shell">
    <div class="problem-create-head">
      <div>
        <h2 class="problem-create-title">题目增加</h2>
        <p class="problem-create-copy">为自己的课程题库补充新题，支持四种题型、题目配图和客观题答案预设。</p>
      </div>
      <el-alert
        v-if="createdQuestionId"
        type="success"
        :closable="false"
        class="problem-create-alert"
        title="题目已写入题库"
        :description="`题目编号：${createdQuestionId}`"
      />
    </div>

    <div v-if="courseOptions.length === 0" class="placeholder">暂无可出题课程，请先为当前教师分配教学班。</div>

    <el-form v-else class="problem-create-form" label-position="top" @submit.prevent>
      <section class="problem-create-section">
        <div class="problem-create-section__title">基础信息</div>
        <div class="problem-create-row problem-create-row--four">
          <el-form-item label="课程">
            <el-select v-model="form.cno" placeholder="选择课程">
              <el-option
                v-for="course in courseOptions"
                :key="course.cno"
                :label="`${course.cno} ${course.cname}`"
                :value="course.cno"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="题型">
            <el-select v-model="form.questionType" placeholder="选择题型">
              <el-option
                v-for="item in questionTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="难度">
            <el-radio-group v-model="form.difficulty" class="problem-create-radio-group">
              <el-radio-button label="easy">易</el-radio-button>
              <el-radio-button label="medium">中</el-radio-button>
              <el-radio-button label="hard">难</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="分值">
            <el-input-number v-model="form.points" :min="1" :max="100" :step="1" />
          </el-form-item>
        </div>

        <el-form-item label="题干">
          <el-input
            v-model="form.stem"
            type="textarea"
            :rows="5"
            maxlength="512"
            show-word-limit
            placeholder="输入题干内容"
          />
        </el-form-item>
      </section>

      <section class="problem-create-section">
        <div class="problem-create-section__title">题目配图</div>
        <div class="problem-create-upload-row">
          <div class="problem-create-upload-actions">
            <el-button :loading="imageUploading" @click="triggerImagePicker">上传图片</el-button>
            <el-button v-if="form.imagePath" text type="danger" @click="removeImage">移除图片</el-button>
            <input
              ref="fileInputRef"
              type="file"
              class="problem-create-file"
              accept="image/png,image/jpeg,image/webp"
              @change="handleImageSelected"
            />
          </div>

          <el-form-item label="图片缩放" class="problem-create-scale-item">
            <el-select v-model="form.imageMode" :disabled="!form.imagePath">
              <el-option
                v-for="item in imageScaleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <div class="problem-create-upload-hint">支持 JPG、PNG、WEBP，单张不超过 8MB。</div>
        </div>

        <div v-if="imagePreviewSrc" class="problem-create-image-preview">
          <img :src="imagePreviewSrc" alt="题目图片预览" :style="imagePreviewStyle" />
        </div>
      </section>

      <section class="problem-create-section">
        <div class="problem-create-section__title">{{ currentTypeLabel }}配置</div>

        <template v-if="form.questionType === 'CHOICE'">
          <div class="problem-create-options">
            <div v-for="(option, index) in form.options" :key="`choice-${index}`" class="problem-create-option">
              <el-radio v-model="form.choiceAnswerIndex" :label="index" class="problem-create-option__radio">
                {{ String.fromCharCode(65 + index) }}
              </el-radio>
              <el-input
                v-model="form.options[index]"
                :placeholder="`选项 ${String.fromCharCode(65 + index)}`"
                maxlength="255"
              />
            </div>
          </div>
        </template>

        <template v-else-if="form.questionType === 'JUDGE'">
          <el-form-item label="正确答案">
            <el-radio-group v-model="form.judgeAnswer">
              <el-radio-button label="true">正确</el-radio-button>
              <el-radio-button label="false">错误</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </template>

        <template v-else-if="form.questionType === 'BLANK'">
          <el-form-item label="参考答案">
            <el-input
              v-model="form.blankAnswer"
              maxlength="255"
              placeholder="输入填空题标准答案"
            />
          </el-form-item>
        </template>

        <template v-else>
          <div class="problem-create-essay-tip">
            简答题暂不设置标准答案和评分细则，后续如需补充可再扩展。
          </div>
        </template>
      </section>

      <div class="problem-create-actions">
        <el-button @click="resetForm(false)">重置表单</el-button>
        <el-button type="primary" :loading="submitting" @click="submitQuestion">写入题库</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  teacherInfo: {
    type: Object,
    default: null
  },
  teacherClasses: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(["created"]);

const questionTypeOptions = [
  { value: "CHOICE", label: "选择题" },
  { value: "JUDGE", label: "判断题" },
  { value: "BLANK", label: "填空题" },
  { value: "ESSAY", label: "简答题" }
];

const imageScaleOptions = [
  { label: "100%", value: 1 },
  { label: "80%", value: 0.8 },
  { label: "60%", value: 0.6 }
];

function createDefaultForm() {
  return {
    cno: "",
    questionType: "CHOICE",
    stem: "",
    points: 5,
    difficulty: "medium",
    options: ["", "", "", ""],
    choiceAnswerIndex: null,
    judgeAnswer: "true",
    blankAnswer: "",
    standardAnswer: "",
    scoringRubric: "",
    imagePath: "",
    imageMode: 1
  };
}

const form = reactive(createDefaultForm());
const createdQuestionId = ref("");
const submitting = ref(false);
const imageUploading = ref(false);
const fileInputRef = ref(null);

const courseOptions = computed(() => {
  const seen = new Set();
  return (props.teacherClasses || []).reduce((list, item) => {
    const cno = normalize(item?.cno);
    if (!cno || seen.has(cno)) {
      return list;
    }
    seen.add(cno);
    list.push({
      cno,
      cname: normalize(item?.cname)
    });
    return list;
  }, []);
});

const currentTypeLabel = computed(() => {
  return questionTypeOptions.find((item) => item.value === form.questionType)?.label || "题目";
});

const imagePreviewSrc = computed(() => {
  const path = normalize(form.imagePath);
  if (!path) {
    return "";
  }
  if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
    return path;
  }
  return `/${path}`;
});

const imagePreviewStyle = computed(() => {
  return {
    width: `${Number(form.imageMode || 1) * 100}%`
  };
});

watch(
  courseOptions,
  (courses) => {
    if (!form.cno && courses.length > 0) {
      form.cno = courses[0].cno;
    }
  },
  { immediate: true }
);

watch(
  () => form.questionType,
  () => {
    createdQuestionId.value = "";
  }
);

function normalize(value) {
  return String(value || "").trim();
}

function triggerImagePicker() {
  fileInputRef.value?.click();
}

async function handleImageSelected(event) {
  const file = event?.target?.files?.[0];
  event.target.value = "";
  if (!file) {
    return;
  }

  const teacherEid = normalize(props.teacherInfo?.eid);
  if (!teacherEid) {
    ElMessage.error("教师工号缺失，无法上传题目图片");
    return;
  }
  if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
    ElMessage.warning("仅支持 JPG/PNG/WEBP 图片");
    return;
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning("图片不能超过 8MB");
    return;
  }

  imageUploading.value = true;
  try {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("teacherEid", teacherEid);
    const response = await fetch("/api/question-image/upload", {
      method: "POST",
      body: formData
    });
    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload?.message || "图片上传失败");
    }
    form.imagePath = normalize(payload?.imagePath);
    ElMessage.success("题目图片上传成功");
  } catch (error) {
    ElMessage.error(error.message || "图片上传失败");
  } finally {
    imageUploading.value = false;
  }
}

function removeImage() {
  form.imagePath = "";
  form.imageMode = 1;
}

function buildPayload() {
  const normalizedOptions = form.options.map(normalize).filter(Boolean);
  const teacherEid = normalize(props.teacherInfo?.eid);
  let answerKey = "";

  if (form.questionType === "CHOICE") {
    if (form.choiceAnswerIndex == null) {
      throw new Error("请选择选择题正确答案");
    }
    answerKey = normalize(form.options[form.choiceAnswerIndex] || "");
    if (!answerKey) {
      throw new Error("正确答案对应的选项不能为空");
    }
    if (normalizedOptions.length < 2) {
      throw new Error("选择题至少需要两个有效选项");
    }
  } else if (form.questionType === "JUDGE") {
    answerKey = form.judgeAnswer;
  } else if (form.questionType === "BLANK") {
    answerKey = normalize(form.blankAnswer);
    if (!answerKey) {
      throw new Error("请填写填空题参考答案");
    }
  }

  if (!teacherEid) {
    throw new Error("教师工号缺失");
  }
  if (!normalize(form.cno)) {
    throw new Error("请选择课程");
  }
  if (!normalize(form.stem)) {
    throw new Error("请输入题干");
  }

  return {
    teacherEid,
    cno: form.cno,
    questionType: form.questionType,
    stem: normalize(form.stem),
    points: Number(form.points || 0),
    difficulty: form.difficulty,
    imagePath: normalize(form.imagePath) || null,
    imageMode: normalize(form.imagePath) ? Number(form.imageMode || 1) : null,
    options: form.questionType === "CHOICE" ? normalizedOptions : [],
    answerKey,
    standardAnswer: "",
    scoringRubric: ""
  };
}

async function submitQuestion() {
  submitting.value = true;
  try {
    const payload = buildPayload();
    const response = await fetch("/api/question-bank", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result?.message || "题目新增失败");
    }
    createdQuestionId.value = normalize(result?.questionId);
    ElMessage.success(result?.message || "题目新增成功");
    emit("created", result);
    resetForm(true);
  } catch (error) {
    ElMessage.error(error.message || "题目新增失败");
  } finally {
    submitting.value = false;
  }
}

function resetForm(keepCourse) {
  const next = createDefaultForm();
  if (keepCourse) {
    next.cno = form.cno;
    next.questionType = form.questionType;
    next.difficulty = form.difficulty;
    next.points = form.points;
  }
  Object.assign(form, next);
}
</script>

<style scoped>
.problem-create-shell {
  display: grid;
  gap: 22px;
}

.problem-create-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.problem-create-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.problem-create-copy {
  margin: 8px 0 0;
  color: var(--ne-text-muted);
  line-height: 1.7;
}

.problem-create-alert {
  width: min(460px, 100%);
}

.problem-create-form {
  display: grid;
  gap: 28px;
}

.problem-create-section {
  display: grid;
  gap: 14px;
}

.problem-create-section__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--ne-text-strong);
}

.problem-create-row {
  display: grid;
  gap: 18px;
}

.problem-create-row--four {
  grid-template-columns: 1.35fr 1.35fr 1.1fr 0.8fr;
  align-items: start;
}

.problem-create-radio-group {
  display: inline-flex;
  flex-wrap: nowrap;
}

.problem-create-upload-row {
  display: grid;
  grid-template-columns: auto 220px 1fr;
  gap: 18px;
  align-items: end;
}

.problem-create-upload-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.problem-create-scale-item {
  margin-bottom: 0;
}

.problem-create-upload-hint {
  min-height: 40px;
  display: flex;
  align-items: center;
  color: var(--ne-text-muted);
  font-size: 13px;
}

.problem-create-file {
  display: none;
}

.problem-create-image-preview {
  border: 1px dashed rgba(var(--ne-primary-rgb), 0.22);
  border-radius: 16px;
  padding: 16px;
  background: rgba(var(--ne-primary-rgb), 0.03);
}

.problem-create-image-preview img {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
}

.problem-create-options {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.problem-create-option {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.problem-create-option__radio {
  margin-right: 0;
}

.problem-create-essay-tip {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(var(--ne-primary-rgb), 0.05);
  color: var(--ne-text-muted);
  line-height: 1.7;
}

.problem-create-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

@media (max-width: 1200px) {
  .problem-create-row--four {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .problem-create-upload-row {
    grid-template-columns: 1fr;
    align-items: start;
  }
}

@media (max-width: 720px) {
  .problem-create-row--four,
  .problem-create-option {
    grid-template-columns: 1fr;
  }

  .problem-create-options {
    grid-template-columns: 1fr;
  }

  .problem-create-actions {
    justify-content: stretch;
  }

  .problem-create-actions .el-button {
    flex: 1 1 auto;
  }
}
</style>
