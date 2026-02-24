import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore(
  'app',
  () => {
    const collapsed = ref(false) // 侧边栏折叠状态
    const theme = ref<'light' | 'dark'>('light') // 主题模式

    /**
     * 切换侧边栏折叠状态
     */
    function toggleCollapsed() {
      collapsed.value = !collapsed.value
    }

    /**
     * 设置侧边栏折叠状态
     */
    function setCollapsed(value: boolean) {
      collapsed.value = value
    }

    /**
     * 切换主题
     */
    function toggleTheme() {
      theme.value = theme.value === 'light' ? 'dark' : 'light'
    }

    /**
     * 设置主题
     */
    function setTheme(value: 'light' | 'dark') {
      theme.value = value
    }

    return {
      collapsed,
      theme,
      toggleCollapsed,
      setCollapsed,
      toggleTheme,
      setTheme,
    }
  },
  {
    persist: {
      key: 'app-store',
      pick: ['collapsed', 'theme'],
    },
  },
)
