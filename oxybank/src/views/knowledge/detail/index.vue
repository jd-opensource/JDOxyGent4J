<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePagination } from 'alova/client'
import type { KnowledgeBase } from '../types'

import DocumentTable from './components/DocumentTable.vue'
import KnowledgeSidebar from './components/KnowledgeSidebar.vue'
import Apis from '@/api'

const route = useRoute()
const router = useRouter()

// 资产库ID
const knowledgeId = computed(() => route.params.id as string)
const kbType = computed(() => route.query.kb_type as string)

// 获取所有资产库列表（用于侧边栏显示资产库信息）
const {
  data: allKnowledgeBases,
  loading: kbListLoading,
} = usePagination(
  (page, pageSize) =>
    Apis.knowledgeBaseManagement.get_all_knowledge_base_api_v1_kb_base_get({
      params: {
        page,
        size: pageSize,
      },
    }),
  {
    data: response => response.data?.items || [],
    total: response => response.data?.total || 0,
    initialData: {
      data: [],
      total: 0,
    },
    initialPage: 1,
    initialPageSize: 100,
  },
)

// 当前资产库数据
const knowledge = computed<KnowledgeBase | null>(() => {
  const kbList = allKnowledgeBases.value
  if (!kbList || kbList.length === 0)
    return null

  const currentKb = kbList.find(kb => kb.kb_id === knowledgeId.value)
  if (!currentKb)
    return null

  // 将API数据映射为组件需要的格式
  return {
    id: currentKb.kb_id || '',
    name: currentKb.kb_name || '',
    description: currentKb.kb_description || '',
    documentCount: 0, // 由DocumentTable组件获取
    totalCharacters: 0, // API未提供
    hitCount: 0, // API未提供
    status: currentKb.kb_status === 'active' ? 'active' : 'inactive',
    createdAt: currentKb.create_time || '',
    updatedAt: currentKb.update_time || '',
  }
})

watch(
  () => knowledge.value?.name,
  (name) => {
    if (!name)
      return
    const q = route.query.kb_name
    const hasKbName = Array.isArray(q) ? Boolean(q[0]) : typeof q === 'string' && q.length > 0
    if (!hasKbName) {
      router.replace({
        query: {
          ...route.query,
          kb_name: name,
        },
      })
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="-m-4 flex h-[calc(100vh-64px)] bg-gray-50">
    <!-- 左侧边栏 -->
    <div class="w-64 bg-white">
      <knowledge-sidebar :knowledge="knowledge" />
    </div>

    <!-- 右侧主内容 -->
    <div class="flex-1 overflow-y-auto bg-gray-50 p-6">
      <a-spin :spinning="kbListLoading">
        <document-table :knowledge-id="knowledgeId" :kb-type="kbType" />
      </a-spin>
    </div>
  </div>
</template>

<style scoped></style>
