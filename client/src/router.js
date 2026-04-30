import { createRouter, createWebHistory } from "vue-router";
import { getLogin, isLoggedIn } from "./auth";
import LoginView from "./views/LoginView.vue";
import RegisterView from "./views/RegisterView.vue";
import HomeView from "./views/HomeView.vue";
import ExamRoomView from "./views/ExamRoomView.vue";
import AdminView from "./views/AdminView.vue";

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
    path: "/register",
    name: "register",
    component: RegisterView,
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
    path: "/admin",
    name: "admin",
    component: AdminView,
    meta: {
      requiresAuth: true,
      adminOnly: true
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
  const login = getLogin();
  const isAdmin = login?.type === "admin";

  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: "login" };
  }

  if (to.meta.adminOnly && !isAdmin) {
    return { name: "home" };
  }

  if (to.name === "home" && isAdmin) {
    return { name: "admin" };
  }

  if (to.name === "exam" && isAdmin) {
    return { name: "admin" };
  }

  if (to.meta.guestOnly && isLoggedIn()) {
    return isAdmin ? { name: "admin" } : { name: "home" };
  }

  return true;
});

export default router;
