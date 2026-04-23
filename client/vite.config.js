import { defineConfig } from "vite";
import basicSsl from "@vitejs/plugin-basic-ssl";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue(), basicSsl()],
  server: {
    port: 5173,
    https: true,
    proxy: {
      "/ws": {
        target: "https://localhost:8443",
        changeOrigin: true,
        ws: true,
        secure: false
      },
      "/avatar": {
        target: "https://localhost:8443",
        changeOrigin: true,
        secure: false
      }
    }
  }
});
