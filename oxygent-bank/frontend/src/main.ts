import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import './assets/styles/variables.css'
import './assets/styles/index.css'
import 'ant-design-vue/dist/reset.css'
import '@/api/index'

const app = createApp(App)

// 注册路由
app.use(router)

// 注册状态管理
app.use(pinia)
app.use(Antd)

app.mount('#app')
