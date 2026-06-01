const API_BASE = import.meta.env.VITE_API_BASE || "";

async function postJson(path, payload, options = {}) {
  const controller = new AbortController();
  const timeoutMs = Number(options.timeoutMs) > 0 ? Number(options.timeoutMs) : 30000;
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);
  let response;
  let data = {};

  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    data = await response.json().catch(() => ({}));
  } catch (error) {
    if (error?.name === "AbortError") {
      throw new Error("请求超时，请稍后重试");
    }
    throw error;
  } finally {
    window.clearTimeout(timer);
  }

  if (!response.ok) {
    throw new Error(data?.message || `请求失败: ${response.status}`);
  }

  if (typeof data?.success === "boolean" && !data.success) {
    throw new Error(data.message || "请求失败");
  }

  if (typeof data?.code === "number" && data.code !== 0) {
    throw new Error(data.message || "请求失败");
  }

  return data?.data ?? data;
}

export function fetchWeaknessQuestion(payload) {
  return postJson("/api/ai/weakness/question", payload, { timeoutMs: 30000 });
}

export function fetchWeaknessExplanation(payload) {
  return postJson("/api/ai/weakness/explain", payload, { timeoutMs: 30000 });
}

export function fetchAiQuestionDraft(payload) {
  return postJson("/api/question-bank/ai-draft", payload, { timeoutMs: 45000 });
}

export function fetchWeaknessTraining(payload) {
  return postJson("/api/ai/weakness/training", payload, { timeoutMs: 60000 });
}

export function fetchWeaknessEvaluation(payload) {
  return postJson("/api/ai/weakness/evaluate", payload, { timeoutMs: 30000 });
}
