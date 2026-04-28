<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">
        <img class="brand-logo" :src="logoUrl" alt="NexEval Logo" />
        <div class="brand-text">
          <div class="brand-title">NexEval</div>
          <div class="brand-subtitle">智能评估系统</div>
        </div>
      </div>
      <nav class="nav">
        <RouterLink
          v-if="route.name !== 'login'"
          to="/"
          class="nav-link"
          active-class="nav-link--active"
        >
          首页
        </RouterLink>
        <RouterLink
          v-else
          to="/login"
          class="nav-link"
          active-class="nav-link--active"
        >
          登录
        </RouterLink>
      </nav>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from "vue-router";
import { RouterLink } from "vue-router";
import logoUrl from "./assets/logo.png";

const route = useRoute();
</script>
