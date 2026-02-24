<script setup lang="ts">
import { ArrowLeftOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePagination } from 'alova/client'
import type { Chunk } from '../types'
import ChunkList from './components/ChunkList.vue'
import ChunkEditDrawer from './components/ChunkEditDrawer.vue'
import { useI18n } from '@/locales'
import Apis from '@/api'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 路由参数
const kbId = computed(() => route.params.id as string)
const fileId = computed(() => route.params.docId as string)

// TODO: 后续支持（筛选/搜索/展开折叠等工具栏能力）
const showToolbar = false

// 获取 chunk 列表 - 使用 usePagination
const {
  data: chunkList,
  loading,
  page,
  pageSize,
  total,
  reload,
} = usePagination(
  (page, pageSize) =>

    Apis.knowledgeBaseDocumentChunkManagement.get_kb_file_chunks_api_v1_kb_base__kb_id__file__file_id__chunks_get(
      {
        pathParams: {
          kb_id: kbId.value,
          file_id: fileId.value,
        },
        params: {
          page,
          size: pageSize,
        },
      },
    ),
  {
    data: response => response.data?.items || [],
    total: response => response.data?.total || 0,
    initialData: {
      data: [],
      total: 0,
    },
    initialPage: 1,
    initialPageSize: 10,
  },
)

const inferredKbType = computed<'structured' | 'unstructured'>(() => {
  const first = chunkList.value?.[0] as any | undefined
  if (
    typeof first?.chunk_to_return === 'string'
    || typeof first?.chunk_to_emb === 'string'
  ) {
    return 'unstructured'
  }
  if (route.query.kb_type === 'structured')
    return 'structured'
  return 'structured'
})

const renderAsMarkdown = computed(
  () => inferredKbType.value === 'unstructured',
)

// API 返回的 Chunk 数据结构映射到内部 Chunk 类型
const chunks = computed<Chunk[]>(() => {
  return chunkList.value.map((item: any, idx: number): Chunk => {
    const isUnstructuredItem = typeof item.chunk_to_return === 'string'

    const content = isUnstructuredItem
      ? item.chunk_to_return
      : Object.entries(item)
          .filter(
            ([key]) =>
              !['kb_id', 'ori_file_id', 'chunk_id', 'file_id'].includes(key),
          )
          .map(
            ([key, value]) =>
              `${key}: ${typeof value === 'string' ? value : JSON.stringify(value)}`,
          )
          .join('; ')

    const rawIndex = item.chunk_index ?? item.index
    const index = typeof rawIndex === 'number' ? rawIndex + 1 : idx + 1

    return {
      id: item.chunk_id || item.id || '',
      documentId: item.ori_file_id || item.file_id || fileId.value,
      index,
      content,
      characterCount:
          item.character_count ?? item.characterCount ?? (item.name?.length || 0),
      hitCount: item.hit_count ?? item.hitCount ?? 0,
      status: item.status === 'enabled' ? 'enabled' : 'disabled',
    }
  })
})

// 选中的 chunk
const selectedChunk = ref<Chunk | null>(null)

// 编辑抽屉
const editDrawerOpen = ref(false)
const editingChunk = ref<Chunk | null>(null)

// 文档名称（从路由获取或使用默认值）
const documentName = computed(() => {
  return fileId.value || t('文档')
})

// 监听数据变化，自动选中第一个 chunk
watch(
  chunks,
  (newChunks) => {
    if (newChunks.length > 0 && !selectedChunk.value) {
      selectedChunk.value = newChunks[0] ?? null
    }
  },
  { immediate: true },
)

// 返回文档列表
function goBack() {
  router.push({ path: `/knowledge/${kbId.value}`, query: route.query })
}

// 选中 chunk
function handleSelectChunk(chunk: Chunk) {
  selectedChunk.value = chunk
}

// 编辑 chunk - 打开 Drawer
function handleEditChunk(chunk: Chunk) {
  editingChunk.value = chunk
  editDrawerOpen.value = true
}

// 保存编辑后的 chunk
function handleSaveChunk(updatedChunk: Chunk) {
  const index = chunks.value.findIndex(c => c.id === updatedChunk.id)
  if (index > -1) {
    // 同步更新选中的 chunk
    if (selectedChunk.value?.id === updatedChunk.id) {
      selectedChunk.value = updatedChunk
    }
    // TODO: 调用后端接口保存
    message.success(t('保存成功'))
  }
}

// 删除 chunk
function handleDeleteChunk(_chunk: Chunk) {
  // TODO: 调用后端接口删除
  reload()
}

// 切换页码
function handlePageChange(newPage: number) {
  page.value = newPage
}

// 切换页码大小
function handlePageSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
}
</script>

<template>
  <div class="-m-4 flex h-[calc(100vh-112px)] bg-white">
    <!-- 左侧边栏（与详情页共享） -->
    <!-- <div class="flex w-56 flex-shrink-0 flex-col border-r border-gray-100 bg-white">
          <div class="flex items-center justify-between p-4">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 text-xl text-white">
              <file-text-outlined />
            </div>
            <a-button type="text" size="small">
              <template #icon>
                <ellipsis-outlined />
              </template>
            </a-button>
          </div>
          <div class="border-b border-gray-100 px-4 pb-4">
            <h3 class="m-0 mb-1 truncate text-sm font-semibold text-gray-800">
              资产库
            </h3>
            <p class="m-0 line-clamp-3 text-xs leading-relaxed text-gray-400">
              文档分段管理
            </p>
          </div>
          <div class="py-2">
            <div
              class="flex cursor-pointer items-center gap-2 bg-blue-50 px-4 py-2.5 text-blue-500"
              @click="goBack"
            >
              <file-text-outlined class="text-base" />
              <span class="text-sm">文档</span>
            </div>
          </div>
        </div> -->

    <!-- 中间主内容 -->
    <div class="flex flex-1 min-h-0 flex-col overflow-hidden">
      <!-- 头部 -->
      <div class="flex items-center gap-3 border-b border-gray-100 px-4 py-3">
        <a-button type="text" class="p-1" @click="goBack">
          <template #icon>
            <arrow-left-outlined />
          </template>
        </a-button>
        <div class="flex flex-1 items-center gap-2">
          <file-text-outlined class="text-lg text-green-500" />
          <span class="text-sm font-medium text-gray-800">{{
            documentName
          }}</span>
          <a-tag>{{ renderAsMarkdown ? t('非结构化') : t('结构化') }}</a-tag>
        </div>
        <!-- TODO: 右上角筛选/搜索/展开折叠（后续支持） -->
        <div v-if="showToolbar" class="flex items-center gap-2">
          <!-- placeholder -->
        </div>

        <!-- TODO: 右上角筛选/搜索/展开折叠按钮（后续支持） -->
        <!--
            <div class="flex items-center gap-2">
              <a-dropdown>
                <a-button>
                  <template #icon>
                    <plus-outlined />
                  </template>
                  添加分段
                </a-button>
                <template #overlay>
                  <a-menu @click="handleAddChunk">
                    <a-menu-item key="manual">
                      手动添加
                    </a-menu-item>
                    <a-menu-item key="import">
                      批量导入
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <span class="mx-1 h-5 w-px bg-gray-200" />
              <span class="text-xs text-gray-500">可用</span>
              <a-switch :checked="true" size="small" />
              <a-button type="text">
                <template #icon>
                  <setting-outlined />
                </template>
              </a-button>
              <a-button type="text">
                <template #icon>
                  <ellipsis-outlined />
                </template>
              </a-button>
            </div>
            -->
      </div>

      <!-- Chunk 列表 -->
      <div class="flex-1 min-h-0 overflow-hidden">
        <component
          :is="ChunkList"
          class="h-full"
          :chunks="chunks"
          :selected-chunk="selectedChunk"
          :loading="loading"
          :render-as-markdown="renderAsMarkdown"
          :current-page="page"
          :page-size="pageSize"
          @select="handleSelectChunk"
          @edit="handleEditChunk"
          @delete="handleDeleteChunk"
        />
      </div>

      <!-- 底部分页 - 固定在底部 -->
      <div
        class="flex flex-shrink-0 items-center justify-between border-t border-gray-100 bg-white px-4 py-3"
      >
        <a-pagination
          :current="page"
          :total="total"
          :page-size="pageSize"
          size="small"
          show-quick-jumper
          @change="handlePageChange"
        />
        <div class="flex gap-2">
          <span
            v-for="size in [10, 25, 50]"
            :key="size"
            class="cursor-pointer rounded px-3 py-1 text-[13px] transition-all"
            :class="
              pageSize === size
                ? 'bg-blue-50 text-blue-500'
                : 'text-gray-400 hover:bg-gray-100'
            "
            @click="handlePageSizeChange(size)"
          >
            {{ size }}
          </span>
        </div>
      </div>
    </div>

    <!-- 右侧信息面板 TODO: 后续支持 -->
    <!-- <chunk-info-panel /> -->
  </div>

  <!-- 编辑分段抽屉 -->
  <component
    :is="ChunkEditDrawer"
    v-model:open="editDrawerOpen"
    :chunk="editingChunk"
    @save="handleSaveChunk"
  />
</template>

  <style scoped>
  .line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
