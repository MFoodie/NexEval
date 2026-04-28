# 评估指标卡片（Element Plus）样板代码

```vue
<template>
  <el-card class="ne-metric-card" shadow="hover">
    <div class="ne-metric-card__header">
      <div class="ne-metric-card__title">{{ title }}</div>
      <el-tag :type="deltaType" size="small">{{ deltaLabel }}</el-tag>
    </div>

    <div class="ne-metric-card__value">{{ value }}</div>
    <div class="ne-metric-card__desc">{{ description }}</div>
  </el-card>
</template>

<script setup>
/**
 * NeMetricCard 组件
 * Props 说明：
 * - title: string | 指标标题
 * - value: string | 指标数值
 * - description: string | 描述文字
 * - delta: number | 百分比变化值
 */
const props = defineProps({
  title: { type: String, required: true },
  value: { type: String, required: true },
  description: { type: String, default: "" },
  delta: { type: Number, default: 0 }
});

const deltaType = computed(() => (props.delta >= 0 ? "success" : "danger"));
const deltaLabel = computed(() => (props.delta >= 0 ? `+${props.delta}%` : `${props.delta}%`));
</script>

<style scoped>
.ne-metric-card {
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 24px rgba(16, 24, 39, 0.08);
  transition: transform 200ms cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 200ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.ne-metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(16, 24, 39, 0.12);
}

.ne-metric-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ne-metric-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #7b8aa6;
}

.ne-metric-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2a3d;
}

.ne-metric-card__desc {
  font-size: 12px;
  color: #b7c2d3;
  margin-top: 6px;
}
</style>
```
