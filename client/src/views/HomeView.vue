<template>
  <section class="card">
    <h1 class="card-title">B/S CAT Exam Skeleton</h1>
    <p class="card-subtitle">
      Start a test session, then answer adaptive questions through websocket channel.
    </p>

    <p class="ws-line">
      WS:
      <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
    </p>

    <el-form @submit.prevent>
      <el-form-item label="User ID">
        <el-input v-model="userId" placeholder="student-1001" />
      </el-form-item>
      <el-space>
        <el-button type="primary" :loading="starting" @click="handleStartExam">
          Start Exam
        </el-button>
      </el-space>
    </el-form>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";

const router = useRouter();
const userId = ref("student-1001");
const starting = ref(false);
const wsStatus = ref("connecting");
const wsTagType = computed(() => {
  if (wsStatus.value === "connected") {
    return "success";
  }

  if (wsStatus.value === "error") {
    return "danger";
  }

  return "info";
});
let wsClient = null;

function connectWebSocket() {
  wsClient = createExamSocket(null, {
    onOpen() {
      wsStatus.value = "connected";
    },
    onClose() {
      wsStatus.value = "closed";
    },
    onError() {
      wsStatus.value = "error";
    }
  });
}

async function handleStartExam() {
  if (!userId.value.trim()) {
    ElMessage.warning("Please input user id.");
    return;
  }

  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected. Please wait for reconnect.");
    return;
  }

  starting.value = true;
  try {
    const payload = await wsClient.request("START_SESSION", {
      userId: userId.value.trim()
    });
    router.push(`/exam/${payload.sessionId}`);
  } catch (error) {
    ElMessage.error(error.message || "Failed to start exam session.");
  } finally {
    starting.value = false;
  }
}

onMounted(connectWebSocket);

onBeforeUnmount(() => {
  wsClient?.close();
});
</script>

<style scoped>
.ws-line {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 14px;
}
</style>
