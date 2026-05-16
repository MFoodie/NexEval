<template>
  <div class="shell">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <img class="brand-logo" :src="logoUrl" alt="NexEval Logo" />
          <div class="brand-text">
            <div class="brand-title">NexEval</div>
            <div class="brand-subtitle">智能评估系统</div>
          </div>
        </div>
        <nav class="nav">
          <RouterLink
            v-if="!isAuthRoute"
            to="/"
            class="nav-link"
            active-class="nav-link--active"
          >
            首页
          </RouterLink>
          <template v-else>
            <RouterLink
              to="/login"
              class="nav-link"
              active-class="nav-link--active"
            >
              登录
            </RouterLink>
            <RouterLink
              to="/register"
              class="nav-link"
              active-class="nav-link--active"
            >
              注册
            </RouterLink>
          </template>
          <div class="theme-switch" aria-label="主题切换">
            <span class="theme-switch-label">主题</span>
            <div class="theme-pill-group" role="group">
              <button
                v-for="item in themeOptions"
                :key="item.value"
                type="button"
                class="theme-pill"
                :class="{ 'is-active': theme === item.value }"
                @click="theme = item.value"
              >
                <span class="theme-pill-icon" aria-hidden="true">
                  <svg v-if="item.value === 'beige'" viewBox="0 0 24 24">
                    <path d="M12 4.5a1 1 0 0 1 1 1V7a1 1 0 1 1-2 0V5.5a1 1 0 0 1 1-1Zm0 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Zm7-4a1 1 0 0 1 1 1 1 1 0 0 1-1 1h-1.5a1 1 0 1 1 0-2H19ZM6.5 12.5a1 1 0 0 1-1 1H4a1 1 0 1 1 0-2h1.5a1 1 0 0 1 1 1Zm9.19-6.69a1 1 0 0 1 1.42 0l1.06 1.06a1 1 0 0 1-1.41 1.41l-1.07-1.06a1 1 0 0 1 0-1.41ZM6.92 15.5a1 1 0 0 1 1.41 0l1.07 1.06a1 1 0 0 1-1.42 1.41l-1.06-1.06a1 1 0 0 1 0-1.41Zm9.19 1.06a1 1 0 0 1 0 1.41l-1.06 1.06a1 1 0 1 1-1.42-1.41l1.07-1.06a1 1 0 0 1 1.41 0ZM7.98 7.56a1 1 0 0 1 0 1.41L6.92 10.03a1 1 0 0 1-1.41-1.42L6.56 7.56a1 1 0 0 1 1.42 0Z" />
                  </svg>
                  <svg v-else-if="item.value === 'classic'" viewBox="0 0 24 24">
                    <path d="M6 12a6 6 0 0 1 9.79-4.65 1 1 0 1 1-1.32 1.5A4 4 0 1 0 16 12a4 4 0 0 0-1.53-3.15 1 1 0 0 1 1.23-1.58A6 6 0 0 1 18 12c0 3.31-2.69 6-6 6s-6-2.69-6-6Zm10 1a1 1 0 1 1 0-2h4a1 1 0 1 1 0 2h-4Z" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24">
                    <path d="M14.5 3a1 1 0 0 1 1 1 7.5 7.5 0 0 0 7.5 7.5 1 1 0 0 1 1 1 9.5 9.5 0 1 1-9.5-9.5Z" />
                  </svg>
                </span>
                <span>{{ item.label }}</span>
              </button>
            </div>
          </div>
        </nav>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { RouterLink } from "vue-router";
import logoUrl from "./assets/nexeval.svg";

const route = useRoute();
const isAuthRoute = computed(() => {
  const path = route.path;
  const name = route.name;
  return name === "login" || name === "register" || path === "/login" || path === "/register";
});
const themeKey = "nexeval.theme";
const themeEvent = "nexeval-theme-change";
const themeOptions = [
  { value: "beige", label: "米白" },
  { value: "classic", label: "经典" },
  { value: "dark", label: "深色" }
];
const theme = ref("beige");
const suppressEmit = ref(false);

function applyTheme(value) {
  document.documentElement.setAttribute("data-theme", value);
}

function emitTheme(value) {
  window.dispatchEvent(new CustomEvent(themeEvent, { detail: value }));
}

function handleThemeEvent(event) {
  const value = event?.detail;
  if (!value || value === theme.value) {
    return;
  }
  suppressEmit.value = true;
  theme.value = value;
  window.setTimeout(() => {
    suppressEmit.value = false;
  }, 0);
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(themeKey);
    if (saved && themeOptions.some((item) => item.value === saved)) {
      theme.value = saved;
    }
  } catch {
    // ignore storage read errors
  }

  applyTheme(theme.value);
  window.addEventListener(themeEvent, handleThemeEvent);
});

watch(theme, (value) => {
  applyTheme(value);
  if (!suppressEmit.value) {
    emitTheme(value);
  }
  try {
    localStorage.setItem(themeKey, value);
  } catch {
    // ignore storage write errors
  }
});

onBeforeUnmount(() => {
  window.removeEventListener(themeEvent, handleThemeEvent);
});
</script>
