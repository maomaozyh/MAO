<script setup lang="ts">
</script>

<template>
  <router-view />
</template>

<style>
/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei',
    'Source Han Sans CN', 'Noto Sans CJK SC', 'WenQuanYi Micro Hei',
    'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
  overflow-x: hidden;
}

html {
  overflow-x: hidden;
}
</style>
