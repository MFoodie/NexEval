import { createRouter, createWebHistory } from "vue-router";
import { isLoggedIn } from "./auth";
import LoginView from "./views/LoginView.vue";
import HomeView from "./views/HomeView.vue";
import ExamRoomView from "./views/ExamRoomView.vue";

const routes = [
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: {
      guestOnly: true
    }
  },
  {
    path: "/",
    name: "home",
    component: HomeView,
    meta: {
      requiresAuth: true
    }
  },
  {
    path: "/exam/:sessionId",
    name: "exam",
    component: ExamRoomView,
    meta: {
      requiresAuth: true
    }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: "login" };
  }

  if (to.meta.guestOnly && isLoggedIn()) {
    return { name: "home" };
  }

  return true;
});

export default router;
