<template>
  <div class="class-tools">
    <el-button
      size="small"
      class="class-tools__button"
      :disabled="disabled || students.length === 0"
      :loading="exporting"
      @click="handleExportCsv"
    >
      导出CSV
    </el-button>
    <el-button
      size="small"
      class="class-tools__button"
      :disabled="disabled || students.length === 0"
      @click="handleSortDesc"
    >
      降序排列
    </el-button>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";

const props = defineProps({
  students: {
    type: Array,
    default: () => []
  },
  course: {
    type: Object,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["sort"]);
const exporting = ref(false);

function normalizeSex(value) {
  if (value === true || value === 1 || value === "1" || value === "男") {
    return "男";
  }
  if (value === false || value === 0 || value === "0" || value === "女") {
    return "女";
  }
  return value == null || value === "" ? "-" : String(value);
}

function escapeCsvCell(value) {
  return `"${String(value ?? "").replace(/"/g, "\"\"")}"`;
}

function buildCsvContent(students) {
  const header = ["卡号", "学号", "姓名", "性别", "成绩"];
  const rows = students.map((student) => [
    student.userId ?? "",
    student.sno ?? "",
    student.name ?? "",
    normalizeSex(student.sex),
    student.grade ?? ""
  ]);

  return [header, ...rows].map((row) => row.map(escapeCsvCell).join(",")).join("\r\n");
}

function downloadBlob(blob, fileName) {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(objectUrl);
}

async function handleExportCsv() {
  if (props.disabled || props.students.length === 0) {
    return;
  }

  exporting.value = true;
  try {
    const csvContent = buildCsvContent(props.students);
    const blob = new Blob(["\uFEFF", csvContent], { type: "text/csv;charset=utf-8;" });
    const prefix = props.course?.cno || props.course?.cname || "class";
    downloadBlob(blob, `${prefix}_scores.csv`);
    ElMessage.success("CSV 导出成功");
  } catch (error) {
    ElMessage.error(error?.message || "CSV 导出失败");
  } finally {
    exporting.value = false;
  }
}

function sortByGradeDesc(students) {
  return [...students].sort((left, right) => {
    const leftGrade = left?.grade == null || left.grade === "" ? Number.NEGATIVE_INFINITY : Number(left.grade);
    const rightGrade = right?.grade == null || right.grade === "" ? Number.NEGATIVE_INFINITY : Number(right.grade);
    return rightGrade - leftGrade;
  });
}

function handleSortDesc() {
  if (props.disabled || props.students.length === 0) {
    return;
  }

  emit("sort", sortByGradeDesc(props.students));
}
</script>

<style scoped>
.class-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.class-tools__button {
  min-width: 96px;
  border-radius: 10px;
}
</style>
