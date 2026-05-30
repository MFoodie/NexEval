const API_BASE = import.meta.env.VITE_API_BASE || "";

async function postJson(path, payload) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const data = await response.json().catch(() => ({}));

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
  return postJson("/api/ai/weakness/question", payload);
}

export function fetchWeaknessExplanation(payload) {
  return postJson("/api/ai/weakness/explain", payload);
}

export function fetchAiQuestionDraft(payload) {
  return postJson("/api/question-bank/ai-draft", payload);
}
