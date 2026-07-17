import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'

// 模板组件样式由插件按需导入；ElMessage 等编程式服务需要显式导入样式。

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
