<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  DatabaseOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useI18n } from '@/locales'

const route = useRoute()
const router = useRouter()
const { availableLocales, locale, t } = useI18n()

// 定义导航菜单项
const menuItems = [
  {
    key: '/knowledge',
    labelKey: '资产库',
    icon: DatabaseOutlined,
  },
  {
    key: '/annotation',
    labelKey: '标注',
    icon: AppstoreOutlined,
  },
]

// 计算当前选中的菜单项
const selectedKeys = computed(() => {
  // 如果当前路径包含菜单项的key，则认为该项被选中 (处理子路由情况)
  const matched = menuItems.find(item => route.path.startsWith(item.key))
  return matched ? [matched.key] : []
})

// 菜单点击事件
function handleMenuClick(info: any) {
  router.push(info.key as string)
}
</script>

<template>
  <a-layout-header class="sticky top-0 z-10 flex h-16 w-full items-center justify-between border-b border-gray-800 !bg-[#001529] px-4 shadow-sm backdrop-blur-sm sm:px-6">
    <!-- 左侧 Logo -->
    <div
      class="flex cursor-pointer items-center gap-3 transition-transform hover:scale-105"
      @click="router.push('/')"
    >
      <img src="/oxygent.png" alt="OxyGent Logo" class="h-8 w-auto object-contain">
    </div>

    <!-- 中间 导航菜单 (Tab 样式) -->
    <div class="flex-1 px-8">
      <a-menu
        :selected-keys="selectedKeys"
        mode="horizontal"
        theme="dark"
        class="border-b-0 bg-transparent leading-[64px]"
        @click="handleMenuClick"
      >
        <a-menu-item v-for="item in menuItems" :key="item.key">
          <template #icon>
            <component :is="item.icon" />
          </template>
          <span class="font-medium">{{ t(item.labelKey) }}</span>
        </a-menu-item>
      </a-menu>
    </div>

    <!-- 右侧 用户信息 -->
    <div class="flex items-center gap-4">
      <a-segmented
        v-model:value="locale"
        :options="availableLocales"
        size="small"
        class="language-switcher"
      />
      <a-dropdown placement="bottomRight">
        <div class="flex cursor-pointer items-center gap-2 rounded-full py-1 pl-1 pr-3 transition-colors hover:bg-white/10">
          <a-avatar size="small" class="bg-indigo-600">
            <template #icon>
              <user-outlined />
            </template>
          </a-avatar>
          <span class="hidden text-sm font-medium text-white sm:block">Admin</span>
        </div>
        <template #overlay>
          <a-menu>
            <a-menu-item key="profile">
              {{ t('个人中心') }}
            </a-menu-item>
            <a-menu-item key="settings">
              {{ t('设置') }}
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" danger>
              {{ t('退出登录') }}
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </a-layout-header>
</template>

<style scoped>
/* 覆盖 Ant Design Menu 默认样式以匹配设计 */
:deep(.ant-menu-horizontal) {
  border-bottom: none;
  line-height: 64px;
}

:deep(.ant-menu-item) {
  top: 0;
  margin-top: 0;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
  color: rgba(255, 255, 255, 0.65);
}

:deep(.ant-menu-item-selected) {
  color: #fff !important;
  border-bottom-color: #fff !important;
}

:deep(.ant-menu-item:hover) {
  color: #fff !important;
}

:deep(.ant-menu-item::after) {
  display: none !important; /* 移除默认的底部条 */
}

/* 语言切换器样式优化 */
:deep(.language-switcher.ant-segmented) {
  background-color: rgba(255, 255, 255, 0.15);
  padding: 2px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.language-switcher .ant-segmented-item) {
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s;
}

:deep(.language-switcher .ant-segmented-item:hover) {
  color: #fff;
  background-color: rgba(255, 255, 255, 0.1);
}

:deep(.language-switcher .ant-segmented-item-selected) {
  background-color: #4f46e5 !important; /* Indigo-600 */
  color: #fff !important;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

:deep(.language-switcher .ant-segmented-thumb) {
  background-color: #4f46e5 !important;
}
</style>
