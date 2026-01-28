<script setup lang="ts">
import {
  EllipsisOutlined,
  ExperimentOutlined,
  FileTextOutlined,
} from '@ant-design/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { KnowledgeBase } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  knowledge: KnowledgeBase | null
}

defineProps<Props>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 当前菜单
const currentMenu = computed(() => {
  const path = route.path
  if (path.includes('/setting')) {
    return 'setting'
  }
  if (path.includes('/recall')) {
    return 'test'
  }
  if (path.includes('/document/')) {
    return 'documents'
  }
  return 'documents'
})

// 菜单项
const menuItems = [
  {
    key: 'documents',
    icon: FileTextOutlined,
    labelKey: '文档',
  },
  {
    key: 'test',
    icon: ExperimentOutlined,
    labelKey: '召回测试',
  },
  // TODO: 设置后续支持
  // {
  //   key: 'setting',
  //   icon: SettingOutlined,
  //   labelKey: '设置',
  // },
]

// 菜单点击
function handleMenuClick(key: string) {
  const knowledgeId = route.params.id
  if (key === 'documents') {
    router.push({ path: `/knowledge/${knowledgeId}`, query: route.query })
  }
  else if (key === 'setting') {
    router.push({ path: `/knowledge/${knowledgeId}/setting`, query: route.query })
  }
  else if (key === 'test') {
    router.push({ path: `/knowledge/${knowledgeId}/recall`, query: route.query })
  }
}

// 更多操作
function handleMore() {
  // TODO: 更多操作弹窗
}
</script>

<template>
  <div class="knowledge-sidebar">
    <!-- 资产库信息 -->
    <div class="kb-header">
      <div class="kb-icon">
        <file-text-outlined />
      </div>
      <div class="kb-actions">
        <a-button type="text" size="small" @click="handleMore">
          <template #icon>
            <ellipsis-outlined />
          </template>
        </a-button>
      </div>
    </div>

    <div class="kb-info">
      <h3 class="kb-name">
        {{ knowledge?.name || t('加载中...') }}
      </h3>
      <p class="kb-desc">
        {{ knowledge?.description || '' }}
      </p>
    </div>

    <!-- 导航菜单 -->
    <div class="nav-menu">
      <div
        v-for="item in menuItems"
        :key="item.key"
        class="menu-item"
        :class="{ active: currentMenu === item.key }"
        @click="handleMenuClick(item.key)"
      >
        <component :is="item.icon" class="menu-icon" />
        <span class="menu-label">{{ t(item.labelKey) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.knowledge-sidebar {
  width: 100%;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.kb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.kb-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.kb-info {
  padding: 0 16px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.kb-name {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-desc {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.nav-menu {
  flex: 1;
  padding: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  color: rgba(0, 0, 0, 0.65);
}

.menu-item:hover {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.85);
}

.menu-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.menu-icon {
  font-size: 16px;
}

.menu-label {
  font-size: 14px;
}

.kb-stats {
  display: flex;
  padding: 16px;
  border-top: 1px solid #f0f0f0;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.stat-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.api-entry {
  padding: 8px 16px 16px;
}

.api-entry :deep(.ant-btn) {
  justify-content: flex-start;
  text-align: left;
}

.api-status {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #52c41a;
  border-radius: 50%;
  margin-left: auto;
}
</style>
