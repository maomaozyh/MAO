import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Ant Design Vue 按需引入：模板里的 <a-xxx> 由 unplugin-vue-components 自动解析（见 vite.config.ts），
// 不再全量 app.use(Antd)，可显著缩小 antd chunk。
import 'ant-design-vue/dist/reset.css'

import '@/access'
import permissionDirective from '@/directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 注册权限指令 v-permission
app.directive('permission', permissionDirective)

// 全局错误兜底：组件 render/setup 抛出的未捕获异常默认会被 Vue 吞掉并导致对应区域白屏。
// 这里统一打印到控制台，便于定位（配合 router.onError 处理路由 chunk 加载失败）。
app.config.errorHandler = (err, instance, info) => {
  console.error('[app] 未捕获错误：', err, '\ninfo:', info, '\ncomponent:', instance)
}

app.mount('#app')
