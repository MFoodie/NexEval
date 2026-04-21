<template>
  <section class="card">
    <h1 class="card-title">Exam Room</h1>
    <p class="card-subtitle">
      Session: {{ sessionId }} | Progress: {{ answeredCount }}/{{ maxQuestions }} | Theta:
      {{ theta.toFixed(2) }}
    </p>
    <p class="ws-line">
      WS:
      <el-tag size="small" :type="wsTagType">{{ wsStatus }}</el-tag>
      <span v-if="lastWsEvent" class="ws-note">Last Event: {{ lastWsEvent }}</span>
    </p>

    <el-skeleton :rows="5" animated v-if="loading" />

    <template v-else>
      <el-result
        v-if="finished"
        icon="success"
        title="Session Finished"
        :sub-title="`Final theta: ${theta.toFixed(2)}`"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/')">Back to Home</el-button>
        </template>
      </el-result>

      <template v-else-if="question">
        <h3>{{ question.stem }}</h3>

        <el-radio-group v-model="selectedOption" style="display: block; margin: 16px 0">
          <el-radio
            v-for="option in question.options"
            :key="option"
            :label="option"
            style="display: block; margin: 8px 0"
          >
            {{ option }}
          </el-radio>
        </el-radio-group>

        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          Submit and Next
        </el-button>
      </template>
    </template>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { createExamSocket } from "../ws";

const route = useRoute();
const router = useRouter();

const sessionId = computed(() => route.params.sessionId);
const loading = ref(false);
const submitting = ref(false);

const question = ref(null);
const selectedOption = ref("");
const theta = ref(0);
const answeredCount = ref(0);
const maxQuestions = ref(10);
const finished = ref(false);
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
const lastWsEvent = ref("");
let wsClient = null;

async function loadNextQuestion() {
  if (!wsClient || !wsClient.isOpen()) {
    ElMessage.error("WebSocket is not connected.");
    return;
  }

  loading.value = true;
  try {
    const payload = await wsClient.request("NEXT_QUESTION", {
      sessionId: sessionId.value
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    maxQuestions.value = payload.maxQuestions;
    finished.value = payload.finished;
    question.value = payload.question;
    selectedOption.value = "";
  } catch (error) {
    ElMessage.error(error.message || "Failed to load next question.");
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!question.value) {
    return;
  }

  if (!selectedOption.value) {
    ElMessage.warning("Please select one option.");
    return;
  }

  submitting.value = true;
  try {
    const payload = await wsClient.request("SUBMIT_ANSWER", {
      sessionId: sessionId.value,
      questionId: question.value.id,
      selectedOption: selectedOption.value
    });

    theta.value = payload.theta;
    answeredCount.value = payload.answeredCount;
    finished.value = payload.finished;

    if (payload.correct) {
      ElMessage.success("Correct. Loading next question...");
    } else {
      ElMessage.info("Submitted. Loading next question...");
    }

    if (!payload.finished) {
      await loadNextQuestion();
    }
  } catch (error) {
    ElMessage.error(error.message || "Submit failed.");
  } finally {
    submitting.value = false;
  }
}

function connectWebSocket() {
  wsClient = createExamSocket(sessionId.value, {
    onOpen() {
      wsStatus.value = "connected";
      loadNextQuestion();
    },
    onClose() {
      wsStatus.value = "closed";
    },
    onError() {
      wsStatus.value = "error";
    },
    onMessage(data) {
      if (data.type === "EVENT") {
        lastWsEvent.value = data.event || "EVENT";
      } else if (data.type === "RESPONSE") {
        lastWsEvent.value = `${data.action || "REQUEST"}_RESPONSE`;
      } else {
        lastWsEvent.value = data.type || "RAW";
      }

      if (data.type === "EVENT" && data.event === "ANSWER_UPDATED") {
        theta.value = data.payload?.theta ?? theta.value;
        answeredCount.value = data.payload?.answeredCount ?? answeredCount.value;
        finished.value = data.payload?.finished ?? finished.value;
      }
    }
  });
}

onMounted(() => {
  connectWebSocket();
});

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

.ws-note {
  margin-left: 8px;
}
</style>
