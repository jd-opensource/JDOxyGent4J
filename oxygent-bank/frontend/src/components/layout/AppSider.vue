<script setup lang="ts">
import { BookOutlined } from '@ant-design/icons-vue'
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/locales'
import { useAppStore } from '@/stores/modules/app'

interface Props {
  collapsed?: boolean
}

withDefaults(defineProps<Props>(), {
  collapsed: false,
})

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const { t } = useI18n()

const menuItems = computed(() => [
  {
    key: '/knowledge',
    icon: () => h(BookOutlined),
    label: t('资产库'),
  },
  // {
  //   key: '/annotation',
  //   icon: () => h(TagOutlined),
  //   label: '标注',
  // },
])

function handleMenuClick(info: any) {
  if (info && info.key) {
    router.push(info.key as string)
  }
}

function onCollapse(collapsed: boolean, _type: string) {
  // Only update store if it's a responsive collapse (type === 'responsive')
  // or if we want to sync the state in general.
  // The 'clickTrigger' type is handled by the trigger slot (which is null here),
  // but if we used the default trigger, we'd need this.
  // Since we use custom trigger in header, this is mainly for breakpoint.
  appStore.setCollapsed(collapsed)
}

// 判断当前路由是否在资产库模块下
const selectedKeys = computed(() => {
  const path = route.path
  // 如果路径以 /knowledge 开头，则选中资产库菜单
  if (path.startsWith('/knowledge')) {
    return ['/knowledge']
  }
  // 如果路径以 /annotation 开头，则选中标注菜单
  if (path.startsWith('/annotation')) {
    return ['/annotation']
  }
  return [path]
})
</script>

<template>
  <a-layout-sider
    :collapsed="collapsed"
    :width="220"
    class="h-full shadow-lg overflow-y-auto"
    :trigger="null"
    collapsible
    breakpoint="lg"
    @collapse="onCollapse"
  >
    <div
      class="flex h-16 items-center justify-center overflow-hidden border-b border-gray-700 bg-gray-900 px-4 transition-all duration-300"
    >
      <div v-if="!collapsed" class="flex items-center gap-2">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white shadow-md">
          O
        </div>
        <span class="truncate text-lg font-bold tracking-wide text-white">OxyBank</span>
      </div>
      <div v-else class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white shadow-md">
        O
      </div>
    </div>
    <div class="py-2">
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :items="menuItems"
        class="border-none"
        @click="handleMenuClick"
      />
    </div>
  </a-layout-sider>
</template>

<style scoped></style>
