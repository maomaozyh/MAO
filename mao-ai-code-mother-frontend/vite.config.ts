import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import { compression } from 'vite-plugin-compression2'
import { visualizer } from 'rollup-plugin-visualizer'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    // Ant Design Vue 按需引入：模板中的 <a-xxx> 编译期自动 import，
    // 配合 main.ts 去掉全量 app.use(Antd)，只打包实际用到的组件
    Components({
      resolvers: [
        // antd v4 用 CSS-in-JS，无需按组件引入样式
        AntDesignVueResolver({ importStyle: false }),
      ],
    }),
    // 产物预压缩（gzip + brotli），配合 nginx 静态服务直接用 .gz/.br 文件
    compression({ threshold: 10240, algorithms: ['gzip', 'brotliCompress'] }),
    // 构建体积分析报告：构建后打开 dist/stats.html 查看
    visualizer({ filename: 'dist/stats.html', gzipSize: true, brotliSize: true }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  build: {
    // 目标浏览器（es2018 已覆盖主流浏览器，可获得更小产物）
    target: 'es2018',
    cssCodeSplit: true,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 函数式分包（按 node_modules 包路径匹配，能覆盖 highlight.js/lib/common 这类子路径导入）
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (/[\\/]node_modules[\\/](@vue|vue|vue-router|pinia)[\\/]/.test(id)) return 'vue'
          if (/[\\/]node_modules[\\/](ant-design-vue|@ant-design|async-validator|dayjs)[\\/]/.test(id)) {
            return 'antd'
          }
          if (/[\\/]node_modules[\\/](markdown-it|highlight\.js|entities|linkify-it|mdurl)[\\/]/.test(id)) {
            return 'markdown'
          }
          if (/[\\/]node_modules[\\/]pptxgenjs[\\/]/.test(id)) return 'pptx'
          return undefined
        },
      },
    },
  },
})
