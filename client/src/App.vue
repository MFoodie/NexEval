<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">NexEval CAT</div>
      <nav class="nav">
        <RouterLink v-if="route.name !== 'login'" to="/">首页</RouterLink>
        <RouterLink v-else to="/login">登录</RouterLink>
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

const route = useRoute();
</script>
