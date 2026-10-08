import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { reveal } from './directives/reveal'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/theme.css'
import './assets/auth.css'
import './assets/reveal.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// v-reveal：滚动入场。注册为全局指令而非按需 import，
// 因为阶段 4 会在多处用到，逐个组件 import 会让模板变成负担。
app.directive('reveal', reveal)

app.mount('#app')
