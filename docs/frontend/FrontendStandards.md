# NexEval 前端开发规范

## 1. 代码风格
- 语言：ES2022 + Vue 3 Composition API
- 格式化：ESLint + Prettier
- 单行最大长度：100
- 强制命名导出（工具函数）

## 2. 命名约定
- 组件：PascalCase
- 组合式函数：useXxx
- 服务层：XxxService
- Store：useXxxStore
- CSS 类名：ne-xxx__item--active

## 3. 组件拆分原则
- 单一职责：组件只负责一个清晰功能
- 页面组件负责数据与状态，基础组件只做展示
- 页面组件不得直接访问 API，必须通过 services

## 4. 状态管理
- 全局：Pinia
- 局部：组件内部 state
- 表单：封装 useForm 组合函数

## 5. API 规范
- 所有请求统一在 /src/services
- 组件中禁止直接使用 axios
- API 返回统一 Result<T>

## 6. 样式规范
- 使用 CSS 变量配置颜色、间距、圆角
- 禁止大量内联样式
- 组件样式尽量 scoped

## 7. 资源规范
- 图标统一来自一套库
- 避免混用 SVG 样式
- 字体、图标由 DesignSystem 指定

## 8. 组件文件结构
- Component.vue
  - <template>
  - <script setup>
  - <style scoped>
- 同级保持一个 index.js 统一导出
