<template>
  <div class="shell">
    <header class="topbar">
      <div class="brand">NexEval CAT</div>
      <nav class="nav">
        <RouterLink to="/">Start</RouterLink>
      </nav>
    </header>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>
