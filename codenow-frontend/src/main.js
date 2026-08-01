/**
 * 应用入口文件
 * 初始化 Vue 应用，注册全局插件（Pinia 状态管理、Vue Router 路由），
 * 并挂载到 DOM 节点
 * 模板组件样式由插件按需导入；ElMessage 等编程式服务需要显式导入样式
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'highlight.js/styles/github.css'

// 模板组件样式由插件按需导入；ElMessage 等编程式服务需要显式导入样式。

/** 创建 Vue 应用实例 */
const app = createApp(App)

/** 注册 Pinia 状态管理插件 */
app.use(createPinia())

/** 注册 Vue Router 路由插件 */
app.use(router)

/** 挂载应用到 #app 节点 */
app.mount('#app')
