import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import "./styles.css";

const themeKey = "nexeval.theme";
try {
	const saved = localStorage.getItem(themeKey);
	const theme = saved || "beige";
	document.documentElement.setAttribute("data-theme", theme);
} catch {
	document.documentElement.setAttribute("data-theme", "beige");
}

createApp(App).use(router).use(ElementPlus).mount("#app");
