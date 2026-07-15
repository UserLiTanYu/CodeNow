import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Element Plus CSS 由 unplugin-vue-components 自动按需导入，无需全量导入

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
