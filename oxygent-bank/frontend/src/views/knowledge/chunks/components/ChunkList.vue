<script setup lang="ts">
import {
  DeleteOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import { ref } from 'vue'
import type { Chunk } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  chunks: Chunk[]
  selectedChunk: Chunk | null
  loading?: boolean
  renderAsMarkdown?: boolean
  currentPage?: number
  pageSize?: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'select', chunk: Chunk): void
  (e: 'toggleStatus', chunk: Chunk): void
  (e: 'edit', chunk: Chunk): void
  (e: 'delete', chunk: Chunk): void
}>()

const { t } = useI18n()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

function renderMarkdown(content: string) {
  return md.render(content)
}

// TODO: 后续支持（是否已启用/字符数/召回次数/状态开关）
const showChunkMeta = false
const showChunkStatus = false

function formatChunkPreview(content: string) {
  const trimmed = content.trim()
  if (!trimmed)
    return ''

  // Structured chunks might store JSON strings
  if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
    try {
      const parsed = JSON.parse(trimmed)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return Object.entries(parsed)
          .map(([k, v]) => `${k}: ${typeof v === 'string' ? v : JSON.stringify(v)}`)
          .join('; ')
      }
      return JSON.stringify(parsed)
    }
    catch {}
  }

  return content
}

function getDisplayIndex(index: number) {
  const currentPage = props.currentPage ?? 1
  const pageSize = props.pageSize ?? props.chunks.length
  return (currentPage - 1) * pageSize + index
}

// 选中的 chunk id 列表（用于批量操作）
const selectedIds = ref<string[]>([])

// 全选
const isAllSelected = ref(false)

// 切换全选
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  }
  else {
    selectedIds.value = props.chunks.map(c => c.id)
  }
  isAllSelected.value = !isAllSelected.value
}

// 切换单个选中
function toggleSelect(chunk: Chunk) {
  const index = selectedIds.value.indexOf(chunk.id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  }
  else {
    selectedIds.value.push(chunk.id)
  }
  isAllSelected.value = selectedIds.value.length === props.chunks.length
}

// 选中 chunk 查看详情
function handleSelect(chunk: Chunk) {
  emit('select', chunk)
}

// 切换状态
function handleToggleStatus(chunk: Chunk, _checked: boolean | string | number, event: Event) {
  event.stopPropagation()
  emit('toggleStatus', chunk)
}

// 编辑
function handleEdit(chunk: Chunk, event: Event) {
  event.stopPropagation()
  emit('edit', chunk)
}

// TODO: 后续支持删除
// function handleDelete(chunk: Chunk, event: Event) {
//   event.stopPropagation()
//   Modal.confirm({
//     title: '确认删除',
//     content: `确定要删除分段「分段-${String(chunk.index).padStart(2, '0')}」吗？此操作不可恢复。`,
//     okText: '删除',
//     okType: 'danger',
//     cancelText: '取消',
//     onOk() {
//       emit('delete', chunk)
//       message.success('删除成功')
//     },
//   })
// }
</script>

<template>
  <div class="flex h-full min-h-0 flex-col overflow-hidden">
    <!-- 列表头部 -->
    <div class="flex items-center gap-3 border-b border-gray-100 bg-gray-50 px-4 py-3">
      <a-checkbox
        :checked="isAllSelected"
        :indeterminate="selectedIds.length > 0 && selectedIds.length < chunks.length"
        @change="toggleSelectAll"
      />
      <span class="text-sm text-gray-500">{{ t('{count} 分段', { count: chunks.length }) }}</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex flex-1 items-center justify-center p-12">
      <a-spin />
    </div>

    <!-- Chunk 列表 -->
    <div v-else class="flex-1 min-h-0 overflow-y-auto">
      <div
        v-for="chunk in chunks"
        :key="chunk.id"
        class="group cursor-pointer border-b border-gray-100 px-4 py-4 transition-all hover:bg-gray-50"
        :class="{ 'border-l-[3px] border-l-blue-500 bg-blue-50': selectedChunk?.id === chunk.id }"
        @click="handleSelect(chunk)"
      >
        <div class="mb-2 flex items-center gap-3 text-xs">
          <a-checkbox
            :checked="selectedIds.includes(chunk.id)"
            @click.stop
            @change="toggleSelect(chunk)"
          />
          <span class="font-medium text-gray-800">{{ t('分段-{index}', { index: String(getDisplayIndex(chunk.index)).padStart(2, '0') }) }}</span>
          <!-- TODO: 后续支持：字符数/召回次数 -->
          <template v-if="showChunkMeta">
            <span class="text-gray-400">{{ t('{count} 字符', { count: chunk.characterCount }) }}</span>
            <span class="text-gray-400">{{ t('{count} 召回次数', { count: chunk.hitCount }) }}</span>
          </template>

          <!-- 右侧操作区 -->
          <div class="ml-auto flex items-center gap-2">
            <!-- 编辑按钮 -->
            <a-tooltip :title="t('编辑')">
              <a-button
                type="text"
                size="small"
                class="opacity-0 transition-opacity group-hover:opacity-100"
                @click="(e: Event) => handleEdit(chunk, e)"
              >
                <template #icon>
                  <edit-outlined class="text-gray-400 hover:text-blue-500" />
                </template>
              </a-button>
            </a-tooltip>

            <!-- 删除按钮 -->
            <a-tooltip :title="t('删除')">
              <a-button
                type="text"
                size="small"
                disabled
                class="opacity-0 transition-opacity group-hover:opacity-100"
              >
                <template #icon>
                  <delete-outlined class="text-gray-400 hover:text-red-500" />
                </template>
              </a-button>
            </a-tooltip>

            <!-- TODO: 后续支持：是否已启用/状态开关 -->
            <a-switch
              v-if="showChunkStatus"
              :checked="chunk.status === 'enabled'"
              size="small"
              @click.stop
              @change="(checked: boolean | string | number, e: Event) => handleToggleStatus(chunk, checked, e)"
            />
          </div>
        </div>
        <div v-if="props.renderAsMarkdown" class="md-preview line-clamp-3 text-[13px] leading-relaxed text-gray-500" v-html="renderMarkdown(chunk.content)" />
        <div v-else class="line-clamp-3 text-[13px] leading-relaxed text-gray-500">
          {{ formatChunkPreview(chunk.content) }}
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && chunks.length === 0" class="flex flex-1 items-center justify-center p-12">
      <a-empty :description="t('暂无分段数据')" />
    </div>
  </div>
</template>

    <style scoped>
    .line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3),
.md-preview :deep(h4),
.md-preview :deep(h5),
.md-preview :deep(h6) {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: rgb(55 65 81);
}

.md-preview :deep(p) {
  margin: 0;
}

.md-preview :deep(pre) {
  margin: 0;
  padding: 8px;
  border-radius: 6px;
  background: rgb(243 244 246);
  overflow: auto;
}

.md-preview :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
}

.md-preview :deep(blockquote) {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid rgb(229 231 235);
  color: rgb(107 114 128);
}
</style>
