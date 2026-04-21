import { createRouter, createWebHistory } from "vue-router";
import HomeView from "./views/HomeView.vue";
import ExamRoomView from "./views/ExamRoomView.vue";

const routes = [
  {
    path: "/",
    name: "home",
    component: HomeView
  },
  {
    path: "/exam/:sessionId",
    name: "exam",
    component: ExamRoomView
  }
];

export default createRouter({
  history: createWebHistory(),
  routes
});
