<script setup lang="ts">
import {
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { useAnnotationPlatform } from '@/composables/useAnnotationPlatform'
import type { AnnotationItem } from '@/views/annotation/types'
import { useI18n } from '@/locales'

const {
  annotationEntries,
  annotationForm,
  approveAnnotation,
  applyFilters,
  closeDrawer,
  confirmReject,
  dataList,
  detailLoading,
  detailTags,
  drawerOpen,
  filters,
  formatDateShort,
  formatDateTimeFull,
  getAgentAvatarStyle,
  getDataTypeLabel,
  getStatusLabel,
  hasAnnotationResult,
  hasRejectReason,
  ingestToKb,
  loadData,
  loadStats,
  loading,
  openDetail,
  openRejectModal,
  pagination,
  paginationInfo,
  progressSegments,
  qaContent,
  rejectModalOpen,
  rejectReason,
  resetFilters,
  selectedData,
  selectedId,
  setDefaultTimeRange,
  showAnnotationForm,
  showKbSection,
  showReviewSection,
  stats,
  statsLoading,
  submitAnnotation,
} = useAnnotationPlatform()

const { t } = useI18n()

const sidebarWidth = ref(320)
const sidebarMinWidth = 200
const sidebarMaxWidth = 400
const sidebarResizing = ref(false)
let sidebarStartX = 0
let sidebarStartWidth = 0

const columnWidths = reactive({
  id: 160,
  priority: 70,
  status: 90,
  type: 80,
  relation: 200,
  question: 320,
  groupTrace: 180,
  time: 150,
  action: 110,
})

const resizingColumnKey = ref<string | null>(null)
let columnStartX = 0
let columnStartWidth = 0

const { width: windowWidth } = useWindowSize()
const drawerWidth = computed(() => {
  if (windowWidth.value < 768)
    return '100%'
  if (windowWidth.value < 1200)
    return '60%'
  return '50%'
})

const statusOptions = computed(() => ([
  { label: t('All Status'), value: '' },
  { label: t('Pending'), value: 'pending' },
  { label: t('Annotated'), value: 'annotated' },
  { label: t('Approved'), value: 'approved' },
  { label: t('Rejected'), value: 'rejected' },
  { label: t('KB Ingested'), value: 'kb_ingested' },
  { label: t('KB Failed'), value: 'kb_failed' },
]))

const dataTypeOptions = computed(() => ([
  { label: t('All Types'), value: '' },
  { label: t('End-to-End'), value: 'e2e' },
  { label: t('Agent'), value: 'agent' },
  { label: t('LLM'), value: 'llm' },
  { label: t('Tool'), value: 'tool' },
  { label: t('Custom'), value: 'custom' },
]))

const priorityOptions = computed(() => ([
  { label: t('All Priorities'), value: '' },
  { label: t('P0-End-to-End'), value: '0' },
  { label: t('P1-Agent'), value: '1' },
  { label: t('P2-LLM'), value: '2' },
  { label: t('P3-Tool'), value: '3' },
  { label: t('P4-Other'), value: '4' },
]))

const columns = computed<TableColumnsType>(() => [
  {
    title: t('ID'),
    dataIndex: 'data_id',
    key: 'id',
    width: columnWidths.id,
    ellipsis: true,
  },
  {
    title: t('Priority'),
    key: 'priority',
    width: columnWidths.priority,
    align: 'center',
  },
  {
    title: t('Status'),
    key: 'status',
    width: columnWidths.status,
    align: 'center',
  },
  {
    title: t('Type'),
    key: 'type',
    width: columnWidths.type,
    align: 'center',
  },
  {
    title: t('Call Relationship'),
    key: 'relation',
    width: columnWidths.relation,
  },
  {
    title: t('Question'),
    key: 'question',
    width: columnWidths.question,
    ellipsis: true,
  },
  {
    title: t('Group / Trace'),
    key: 'groupTrace',
    width: columnWidths.groupTrace,
  },
  {
    title: t('Time'),
    key: 'time',
    width: columnWidths.time,
  },
  {
    title: t('Action'),
    key: 'action',
    width: columnWidths.action,
    align: 'center',
    fixed: 'right',
  },
])

function getPriorityStyle(priority?: number | null) {
  const value = priority ?? 4
  if (value === 0)
    return { backgroundColor: '#EF4444', color: '#FFFFFF' }
  if (value === 1)
    return { backgroundColor: '#FEF3C7', color: '#D97706' }
  if (value === 2)
    return { backgroundColor: '#DBEAFE', color: '#2563EB' }
  if (value === 3)
    return { backgroundColor: '#E0E7FF', color: '#4F46E5' }
  return { backgroundColor: '#F3F4F6', color: '#6B7280' }
}

function getStatusStyle(status?: string | null) {
  const map: Record<string, { backgroundColor: string, color: string }> = {
    pending: { backgroundColor: '#FEF3C7', color: '#D97706' },
    annotated: { backgroundColor: '#D1FAE5', color: '#059669' },
    approved: { backgroundColor: '#10B981', color: '#FFFFFF' },
    rejected: { backgroundColor: '#FEE2E2', color: '#DC2626' },
    kb_ingested: { backgroundColor: '#C7D2FE', color: '#4338CA' },
    kb_failed: { backgroundColor: '#FEE2E2', color: '#DC2626' },
  }
  return map[status || 'pending'] || { backgroundColor: '#F3F4F6', color: '#6B7280' }
}

function getDataTypeStyle(type?: string | null) {
  const map: Record<string, { backgroundColor: string, color: string }> = {
    e2e: { backgroundColor: '#FEE2E2', color: '#DC2626' },
    agent: { backgroundColor: '#DBEAFE', color: '#2563EB' },
    llm: { backgroundColor: '#EDE9FE', color: '#7C3AED' },
    tool: { backgroundColor: '#D1FAE5', color: '#059669' },
    custom: { backgroundColor: '#F3F4F6', color: '#6B7280' },
  }
  return map[type || 'custom'] || { backgroundColor: '#F3F4F6', color: '#6B7280' }
}

function getAvatarInitial(name?: string | null) {
  if (!name)
    return '?'
  return name.slice(0, 1).toUpperCase()
}

type AnnotationRow = Partial<AnnotationItem> & Record<string, any>

function getCaller(record: AnnotationRow) {
  return record.caller || t('User')
}

function getCallee(record: AnnotationRow) {
  return record.callee || t('Unknown')
}

function startSidebarResize(event: MouseEvent) {
  sidebarResizing.value = true
  sidebarStartX = event.clientX
  sidebarStartWidth = sidebarWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onSidebarResizeMove)
  window.addEventListener('mouseup', stopSidebarResize)
}

function onSidebarResizeMove(event: MouseEvent) {
  if (!sidebarResizing.value)
    return
  const diff = event.clientX - sidebarStartX
  const next = Math.min(sidebarMaxWidth, Math.max(sidebarMinWidth, sidebarStartWidth + diff))
  sidebarWidth.value = next
}

function stopSidebarResize() {
  if (!sidebarResizing.value)
    return
  sidebarResizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onSidebarResizeMove)
  window.removeEventListener('mouseup', stopSidebarResize)
}

function startColumnResize(key: string, event: MouseEvent) {
  event.stopPropagation()
  resizingColumnKey.value = key
  columnStartX = event.clientX
  columnStartWidth = columnWidths[key as keyof typeof columnWidths]
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onColumnResizeMove)
  window.addEventListener('mouseup', stopColumnResize)
}

function onColumnResizeMove(event: MouseEvent) {
  if (!resizingColumnKey.value)
    return
  const diff = event.clientX - columnStartX
  const next = Math.max(60, columnStartWidth + diff)
  columnWidths[resizingColumnKey.value as keyof typeof columnWidths] = next
}

function stopColumnResize() {
  if (!resizingColumnKey.value)
    return
  resizingColumnKey.value = null
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onColumnResizeMove)
  window.removeEventListener('mouseup', stopColumnResize)
}

function rowClassName(record: AnnotationItem) {
  return selectedId.value === record.data_id ? 'bg-blue-50' : ''
}

function handleTableRow(record: AnnotationItem) {
  return {
    onClick: () => openDetail(record.data_id),
  }
}

onMounted(() => {
  setDefaultTimeRange()
  loadStats()
  loadData()
})

onBeforeUnmount(() => {
  stopSidebarResize()
  stopColumnResize()
})
</script>

<template>
  <div class="flex h-full flex-col gap-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="m-0 text-2xl font-semibold text-gray-800">
          {{ t('QA Annotation Platform') }}
        </h1>
        <p class="m-0 text-sm text-gray-400">
          {{ t('Manage QA annotation tasks with filtering, review, and assets base ingestion.') }}
        </p>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 gap-4">
      <div
        class="relative flex-shrink-0"
        :style="{ width: `${sidebarWidth}px` }"
      >
        <a-card
          class="h-full overflow-hidden"
          :body-style="{ height: '100%', padding: '16px', display: 'flex', flexDirection: 'column' }"
        >
          <div class="flex flex-1 flex-col gap-4 overflow-y-auto pr-1">
            <a-collapse default-active-key="filter" expand-icon-position="end" ghost>
              <a-collapse-panel key="filter">
                <template #header>
                  <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                    <span>🔍</span>
                    <span>{{ t('Filter Conditions') }}</span>
                  </div>
                </template>
                <a-form layout="vertical" class="filter-form">
                  <a-form-item :label="t('Time Range')">
                    <a-range-picker
                      v-model:value="filters.timeRange"
                      show-time
                      format="YYYY-MM-DD HH:mm"
                      class="w-full"
                    />
                  </a-form-item>
                  <a-form-item :label="t('Type & Status')">
                    <div class="flex flex-col gap-2">
                      <a-select
                        v-model:value="filters.dataType"
                        :options="dataTypeOptions"
                        @change="applyFilters"
                      />
                      <a-select
                        v-model:value="filters.status"
                        :options="statusOptions"
                        @change="applyFilters"
                      />
                      <a-select
                        v-model:value="filters.priority"
                        :options="priorityOptions"
                        @change="applyFilters"
                      />
                    </div>
                  </a-form-item>
                  <a-form-item :label="t('Caller')">
                    <a-input
                      v-model:value="filters.caller"
                      :placeholder="t('Enter Caller...')"
                    >
                      <template #suffix>
                        <search-outlined
                          class="cursor-pointer text-gray-400 transition hover:text-blue-500"
                          @click="applyFilters"
                        />
                      </template>
                    </a-input>
                  </a-form-item>
                  <a-form-item :label="t('Callee')">
                    <a-input
                      v-model:value="filters.callee"
                      :placeholder="t('Enter Callee...')"
                    >
                      <template #suffix>
                        <search-outlined
                          class="cursor-pointer text-gray-400 transition hover:text-blue-500"
                          @click="applyFilters"
                        />
                      </template>
                    </a-input>
                  </a-form-item>
                  <a-form-item :label="t('Group ID')">
                    <a-input
                      v-model:value="filters.groupId"
                      :placeholder="t('Enter Group ID...')"
                    >
                      <template #suffix>
                        <search-outlined
                          class="cursor-pointer text-gray-400 transition hover:text-blue-500"
                          @click="applyFilters"
                        />
                      </template>
                    </a-input>
                  </a-form-item>
                  <a-form-item :label="t('Trace ID')">
                    <a-input
                      v-model:value="filters.traceId"
                      :placeholder="t('Enter Trace ID...')"
                    >
                      <template #suffix>
                        <search-outlined
                          class="cursor-pointer text-gray-400 transition hover:text-blue-500"
                          @click="applyFilters"
                        />
                      </template>
                    </a-input>
                  </a-form-item>
                  <a-form-item :label="t('Search')">
                    <a-input
                      v-model:value="filters.search"
                      :placeholder="t('Search Question content...')"
                    >
                      <template #suffix>
                        <search-outlined
                          class="cursor-pointer text-gray-400 transition hover:text-blue-500"
                          @click="applyFilters"
                        />
                      </template>
                    </a-input>
                  </a-form-item>
                  <div class="flex flex-col gap-2">
                    <a-button type="primary" block @click="applyFilters">
                      {{ t('Search') }}
                    </a-button>
                    <a-button block @click="resetFilters">
                      {{ t('Reset') }}
                    </a-button>
                  </div>
                </a-form>
              </a-collapse-panel>
            </a-collapse>

            <a-collapse default-active-key="progress" expand-icon-position="end" ghost>
              <a-collapse-panel key="progress">
                <template #header>
                  <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                    <span>📊</span>
                    <span>{{ t('Annotation Progress') }}</span>
                  </div>
                </template>
                <a-spin :spinning="statsLoading">
                  <div class="grid grid-cols-2 gap-3">
                    <div class="rounded-lg bg-amber-50 p-3 text-center">
                      <div class="text-lg font-semibold text-amber-600">
                        {{ stats.pending }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('Pending') }}
                      </div>
                    </div>
                    <div class="rounded-lg bg-emerald-50 p-3 text-center">
                      <div class="text-lg font-semibold text-emerald-600">
                        {{ stats.annotated }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('Annotated') }}
                      </div>
                    </div>
                    <div class="rounded-lg bg-emerald-500/10 p-3 text-center">
                      <div class="text-lg font-semibold text-emerald-600">
                        {{ stats.approved }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('Approved') }}
                      </div>
                    </div>
                    <div class="rounded-lg bg-red-50 p-3 text-center">
                      <div class="text-lg font-semibold text-red-500">
                        {{ stats.rejected }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('Rejected') }}
                      </div>
                    </div>
                    <div class="rounded-lg bg-indigo-50 p-3 text-center">
                      <div class="text-lg font-semibold text-indigo-600">
                        {{ stats.kb_ingested }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('KB Ingested') }}
                      </div>
                    </div>
                    <div class="rounded-lg bg-rose-50 p-3 text-center">
                      <div class="text-lg font-semibold text-rose-500">
                        {{ stats.kb_failed }}
                      </div>
                      <div class="text-xs text-gray-500">
                        {{ t('KB Failed') }}
                      </div>
                    </div>
                  </div>

                  <div class="mt-4 space-y-3">
                    <div class="text-xs font-medium text-gray-600">
                      {{ t('Annotation Progress') }}
                    </div>
                    <div class="h-2 overflow-hidden rounded-full bg-gray-100">
                      <div class="flex h-full">
                        <div
                          class="h-full bg-emerald-500"
                          :style="{ width: `${progressSegments.approved}%` }"
                        />
                        <div
                          class="h-full bg-emerald-300"
                          :style="{ width: `${progressSegments.annotated}%` }"
                        />
                        <div
                          class="h-full bg-red-400"
                          :style="{ width: `${progressSegments.rejected}%` }"
                        />
                        <div
                          class="h-full bg-amber-400"
                          :style="{ width: `${progressSegments.pending}%` }"
                        />
                        <div
                          class="h-full bg-indigo-400"
                          :style="{ width: `${progressSegments.kb_ingested}%` }"
                        />
                        <div
                          class="h-full bg-rose-400"
                          :style="{ width: `${progressSegments.kb_failed}%` }"
                        />
                      </div>
                    </div>
                    <div class="flex flex-wrap gap-3 text-xs text-gray-500">
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-amber-400" />
                        {{ t('Pending') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-emerald-300" />
                        {{ t('Annotated') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-emerald-500" />
                        {{ t('Approved') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-red-400" />
                        {{ t('Rejected') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-indigo-400" />
                        {{ t('KB Ingested') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <span class="h-2 w-2 rounded-full bg-rose-400" />
                        {{ t('KB Failed') }}
                      </span>
                    </div>
                  </div>
                </a-spin>
              </a-collapse-panel>
            </a-collapse>
          </div>
        </a-card>

        <div
          class="annotation-sidebar-resizer"
          :class="{ active: sidebarResizing }"
          @mousedown="startSidebarResize"
        />
      </div>

      <div class="flex min-w-0 flex-1 flex-col">
        <a-card
          class="h-full"
          :body-style="{ height: '100%', padding: '16px', display: 'flex', flexDirection: 'column' }"
        >
          <div class="mb-4 flex items-center justify-between">
            <div>
              <h2 class="m-0 text-lg font-semibold text-gray-800">
                {{ t('Data List') }}
              </h2>
              <p class="m-0 text-xs text-gray-400">
                {{ t('{count} items', { count: pagination.total }) }}
              </p>
            </div>
            <div class="flex items-center gap-2">
              <a-button @click="applyFilters">
                <template #icon>
                  <reload-outlined />
                </template>
                {{ t('Refresh') }}
              </a-button>
            </div>
          </div>

          <a-table
            :columns="columns"
            :data-source="dataList"
            :loading="loading"
            :pagination="false"
            :row-key="record => (record as AnnotationItem).data_id"
            :row-class-name="rowClassName"
            :custom-row="handleTableRow"
            size="middle"
            table-layout="fixed"
            :scroll="{ x: 1260 }"
          >
            <template #headerCell="{ column }">
              <div class="flex w-full items-center justify-between gap-2">
                <span class="text-xs font-semibold text-gray-600">
                  {{ column.title }}
                </span>
                <span
                  class="annotation-column-resizer"
                  @mousedown="startColumnResize(String(column.key), $event)"
                />
              </div>
            </template>

            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'priority'">
                <a-tag :style="getPriorityStyle(record.priority)" bordered>
                  P{{ record.priority ?? 4 }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :style="getStatusStyle(record.status)" bordered>
                  {{ getStatusLabel(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'type'">
                <a-tag :style="getDataTypeStyle(record.data_type)" bordered>
                  {{ getDataTypeLabel(record.data_type) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'relation'">
                <a-tooltip :title="`${getCaller(record)} → ${getCallee(record)}`">
                  <div class="flex items-center gap-2 text-xs text-gray-600">
                    <template v-if="record.data_type === 'e2e'">
                      <span>{{ t('User') }}</span>
                      <span>→</span>
                      <a-avatar
                        size="small"
                        :style="getAgentAvatarStyle(getCallee(record))"
                      >
                        {{ getAvatarInitial(getCallee(record)) }}
                      </a-avatar>
                      <span class="truncate">
                        {{ getCallee(record) }}
                      </span>
                    </template>
                    <template v-else>
                      <a-avatar
                        size="small"
                        :style="getAgentAvatarStyle(getCaller(record))"
                      >
                        {{ getAvatarInitial(getCaller(record)) }}
                      </a-avatar>
                      <span class="truncate">
                        {{ getCaller(record) }}
                      </span>
                      <span>→</span>
                      <template v-if="record.data_type === 'tool'">
                        <span>🔧</span>
                        <span class="truncate">
                          {{ getCallee(record) }}
                        </span>
                      </template>
                      <template v-else>
                        <a-avatar
                          size="small"
                          :style="getAgentAvatarStyle(getCallee(record))"
                        >
                          {{ getAvatarInitial(getCallee(record)) }}
                        </a-avatar>
                        <span class="truncate">
                          {{ getCallee(record) }}
                        </span>
                      </template>
                    </template>
                  </div>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'question'">
                <a-tooltip :title="record.question || '-'">
                  <div class="truncate text-xs text-gray-600">
                    {{ record.question || '-' }}
                  </div>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'groupTrace'">
                <a-tooltip
                  :title="t('Group: {group}\\nTrace: {trace}', { group: record.source_group_id || '-', trace: record.source_trace_id || '-' })"
                >
                  <div class="space-y-1 text-xs">
                    <div class="flex items-center gap-1">
                      <span class="text-gray-400">{{ t('G:') }}</span>
                      <span class="truncate text-emerald-600">
                        {{ record.source_group_id || '-' }}
                      </span>
                    </div>
                    <div class="flex items-center gap-1">
                      <span class="text-gray-400">{{ t('T:') }}</span>
                      <span class="truncate text-blue-600">
                        {{ record.source_trace_id || '-' }}
                      </span>
                    </div>
                  </div>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'time'">
                <span class="text-xs text-gray-500">
                  {{ formatDateShort(record.created_at) }}
                </span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  size="small"
                  type="primary"
                  @click.stop="openDetail(record.data_id)"
                >
                  {{ t('Annotate') }}
                </a-button>
              </template>
            </template>

            <template #emptyText>
              <div class="py-8">
                <a-empty>
                  <template #description>
                    <div class="text-sm text-gray-500">
                      <div>{{ t('No data available. Please inject data via API.') }}</div>
                      <div class="mt-2 text-xs text-gray-400">
                        {{ t('POST /api/v1/deposit') }}
                      </div>
                    </div>
                  </template>
                </a-empty>
              </div>
            </template>
          </a-table>

          <div class="mt-4 flex items-center justify-between">
            <span class="text-xs text-gray-500">
              {{ paginationInfo }}
            </span>
            <a-pagination
              :current="pagination.page"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              :show-size-changer="false"
              @change="loadData"
            />
          </div>
        </a-card>
      </div>
    </div>

    <a-drawer
      :open="drawerOpen"
      placement="right"
      :width="drawerWidth"
      :destroy-on-close="true"
      :title="t('Data Annotation')"
      @close="closeDrawer"
    >
      <a-spin :spinning="detailLoading">
        <div v-if="selectedData" class="space-y-4">
          <a-card>
            <a-descriptions bordered size="small" :column="1">
              <a-descriptions-item :label="t('Label')">
                <div class="flex flex-wrap gap-2">
                  <a-tag :style="getPriorityStyle(detailTags.priority)" bordered>
                    P{{ detailTags.priority }}
                  </a-tag>
                  <a-tag :style="getStatusStyle(detailTags.status)" bordered>
                    {{ getStatusLabel(detailTags.status) }}
                  </a-tag>
                  <a-tag :style="getDataTypeStyle(detailTags.dataType)" bordered>
                    {{ getDataTypeLabel(detailTags.dataType) }}
                  </a-tag>
                </div>
              </a-descriptions-item>
              <a-descriptions-item :label="t('Group')">
                <span class="font-mono text-xs text-emerald-600">
                  {{ selectedData.source_group_id || '-' }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item :label="t('Trace')">
                <span class="font-mono text-xs text-blue-600">
                  {{ selectedData.source_trace_id || '-' }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item :label="t('Time')">
                <span class="font-mono text-xs text-gray-600">
                  {{ formatDateTimeFull(selectedData.created_at) }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item :label="t('Call Relationship')">
                <div class="flex items-center gap-2 text-xs text-gray-600">
                  <a-avatar
                    size="small"
                    :style="getAgentAvatarStyle(getCaller(selectedData))"
                  >
                    {{ getAvatarInitial(getCaller(selectedData)) }}
                  </a-avatar>
                  <span>{{ getCaller(selectedData) }}</span>
                  <span>→</span>
                  <a-avatar
                    v-if="selectedData.data_type !== 'tool'"
                    size="small"
                    :style="getAgentAvatarStyle(getCallee(selectedData))"
                  >
                    {{ getAvatarInitial(getCallee(selectedData)) }}
                  </a-avatar>
                  <span v-else>🔧</span>
                  <span>{{ getCallee(selectedData) }}</span>
                </div>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card>
            <template #title>
              <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                <span>💬</span>
                <span>{{ t('QA Content') }}</span>
              </div>
            </template>
            <div class="space-y-4">
              <div>
                <div class="mb-2 text-xs font-semibold text-gray-500">
                  {{ t('Question') }}
                </div>
                <div class="rounded bg-gray-50 p-3 text-xs text-gray-700">
                  <template v-if="qaContent.question">
                    <pre class="whitespace-pre-wrap font-mono">{{ qaContent.question }}</pre>
                  </template>
                  <template v-else>
                    <span class="italic text-gray-400">{{ t('No content available') }}</span>
                  </template>
                </div>
              </div>
              <div>
                <div class="mb-2 text-xs font-semibold text-gray-500">
                  {{ t('Answer') }}
                </div>
                <div class="rounded bg-gray-50 p-3 text-xs text-gray-700">
                  <template v-if="qaContent.answer">
                    <pre class="whitespace-pre-wrap font-mono">{{ qaContent.answer }}</pre>
                  </template>
                  <template v-else>
                    <span class="italic text-gray-400">{{ t('No content available') }}</span>
                  </template>
                </div>
              </div>
            </div>
          </a-card>

          <a-card v-if="hasAnnotationResult">
            <template #title>
              <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                <span>📋</span>
                <span>{{ t('Annotated Result') }}</span>
              </div>
            </template>
            <a-table
              :columns="[
                { title: t('Key'), dataIndex: 'key', key: 'key', width: 140 },
                { title: t('Value'), dataIndex: 'value', key: 'value' },
              ]"
              :data-source="annotationEntries.map(([key, value]) => ({ key, value }))"
              :pagination="false"
              size="small"
              :row-key="record => record.key"
            />
          </a-card>

          <a-alert
            v-if="hasRejectReason"
            type="error"
            show-icon
            :message="t('Reject Reason')"
            :description="selectedData.reject_reason || '-'"
          />

          <a-card v-if="showAnnotationForm">
            <template #title>
              <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                <span>✏️</span>
                <span>{{ t('Annotation') }}</span>
              </div>
            </template>
            <a-form layout="vertical">
              <a-form-item :label="t('Corrected Question')">
                <a-textarea
                  v-model:value="annotationForm.question"
                  :rows="3"
                  :placeholder="t('Optional, enter corrected Question...')"
                />
              </a-form-item>
              <a-form-item :label="t('Corrected Answer')">
                <a-textarea
                  v-model:value="annotationForm.answer"
                  :rows="4"
                  :placeholder="t('Optional, enter corrected Answer...')"
                />
              </a-form-item>
              <a-form-item>
                <template #label>
                  <span class="flex items-center gap-1 text-gray-600">
                    <span>{{ t('Quality Score') }}</span>
                    <span class="text-red-500">*</span>
                  </span>
                </template>
                <a-select v-model:value="annotationForm.score" :placeholder="t('Please select')">
                  <a-select-option value="1">
                    {{ t('Excellent (1.0)') }}
                  </a-select-option>
                  <a-select-option value="0.8">
                    {{ t('Good (0.8)') }}
                  </a-select-option>
                  <a-select-option value="0.6">
                    {{ t('Fair (0.6)') }}
                  </a-select-option>
                  <a-select-option value="0.4">
                    {{ t('Poor (0.4)') }}
                  </a-select-option>
                  <a-select-option value="0.2">
                    {{ t('Very Poor (0.2)') }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item :label="t('Annotation Comment')">
                <a-textarea
                  v-model:value="annotationForm.comment"
                  :rows="3"
                  :placeholder="t('Optional, enter comments...')"
                />
              </a-form-item>
              <a-button type="primary" block @click="submitAnnotation(selectedData.data_id)">
                💾 {{ t('Submit Annotation') }}
              </a-button>
            </a-form>
          </a-card>

          <a-card v-if="showReviewSection">
            <template #title>
              <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                <span>👁️</span>
                <span>{{ selectedData.status === 'approved' ? t('Knowledge Base') : t('Annotation Review') }}</span>
              </div>
            </template>
            <div class="flex flex-col gap-3">
              <template v-if="selectedData.status === 'annotated'">
                <a-button type="primary" block @click="approveAnnotation(selectedData.data_id)">
                  ✅ {{ t('Approve Annotation') }}
                </a-button>
                <a-button danger block @click="openRejectModal(selectedData.data_id)">
                  ❌ {{ t('Reject Annotation') }}
                </a-button>
              </template>
              <template v-else>
                <a-button type="primary" block @click="ingestToKb(selectedData.data_id)">
                  📤 {{ t('Approve & Ingest to KB') }}
                </a-button>
              </template>
            </div>
          </a-card>

          <a-card v-if="showKbSection">
            <template #title>
              <div class="flex items-center gap-2 text-sm font-medium text-gray-700">
                <span>📚</span>
                <span>{{ t('Knowledge Base') }}</span>
              </div>
            </template>
            <a-alert
              v-if="selectedData.status === 'kb_ingested'"
              type="success"
              show-icon
              :message="t('Knowledge Base Ingestion Successful')"
            />
            <div v-else-if="selectedData.status === 'kb_failed'" class="space-y-3">
              <a-alert
                type="error"
                show-icon
                :message="t('KB Ingestion Failed, Please Retry')"
              />
              <a-button type="primary" block @click="ingestToKb(selectedData.data_id, true)">
                🔄 {{ t('Retry KB Ingestion') }}
              </a-button>
            </div>
          </a-card>
        </div>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="rejectModalOpen"
      :title="t('Reject Annotation')"
      :ok-text="t('Confirm Reject')"
      :cancel-text="t('Cancel')"
      ok-type="danger"
      @ok="confirmReject"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('Please enter reject reason:')">
          <a-textarea
            v-model:value="rejectReason"
            :rows="4"
            :placeholder="t('Please enter reject reason...')"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.annotation-sidebar-resizer {
  position: absolute;
  right: -4px;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
}

.annotation-sidebar-resizer::after {
  content: '';
  width: 2px;
  height: 40px;
  background: #cbd5f5;
  border-radius: 999px;
  transition: background 0.2s ease;
}

.annotation-sidebar-resizer:hover::after,
.annotation-sidebar-resizer.active::after {
  background: #3b82f6;
}

.annotation-column-resizer {
  width: 6px;
  height: 100%;
  cursor: col-resize;
  position: relative;
}

.annotation-column-resizer::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 0;
  width: 2px;
  height: 16px;
  background: #d1d5db;
  transform: translateY(-50%);
  border-radius: 999px;
}

/* Force vertical center for annotation result table cells */
.annotation-result-table :deep(.ant-table-cell) {
  vertical-align: middle !important;
}

.annotation-result-table :deep(.ant-table-cell pre) {
  margin: 0;
  display: inline-block;
  vertical-align: middle;
}

/* Reduce spacing between filter form items */
.filter-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.filter-form :deep(.ant-form-item:last-of-type) {
  margin-bottom: 16px;
}
</style>
