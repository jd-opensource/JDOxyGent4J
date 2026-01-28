<script setup lang="ts">
import {
  CodeOutlined,
  SendOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import axios, { type AxiosError } from 'axios'
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useRequest } from 'alova/client'
import KnowledgeSidebar from '../detail/components/KnowledgeSidebar.vue'
import type { KnowledgeBase } from '../types'
import { useI18n } from '@/locales'

const route = useRoute()
const { t } = useI18n()

const knowledgeId = computed(() => route.params.id as string)
const kbName = computed(() => {
  const q = route.query.kb_name
  if (Array.isArray(q))
    return q[0] ?? ''
  return typeof q === 'string' ? q : ''
})

const knowledge = computed<KnowledgeBase | null>(() => {
  const name = kbName.value || knowledgeId.value
  return {
    id: knowledgeId.value,
    name,
    description: '',
    documentCount: 0,
    totalCharacters: 0,
    hitCount: 0,
    status: 'active',
    createdAt: '',
    updatedAt: '',
  }
})

// --- Request Configuration ---

// 真实接口信息（根据 kb_name 拉取）
const {
  data: interfaceData,
  loading: interfacesLoading,
  error: interfacesError,
} = useRequest(() => Apis.knowledgeBaseRetrievalMethodDynamicManagement.get_kb_query_interface_api_v1_query_interface__kb_name__get({
  pathParams: {
    kb_name: kbName.value,
  },
}))

type UnknownRecord = Record<string, any>

interface QueryInterfaceItem {
  name?: string
  method?: string
  url?: string
  path?: string
  params?: unknown
  parameters?: unknown
}

function getInterfaceName(item: UnknownRecord): string {
  return String(
    item?.name
    ?? item?.interface_name
    ?? item?.endpoint_name
    ?? item?.operationId
    ?? item?.url
    ?? item?.path
    ?? '',
  )
}

function getInterfaceUrl(item: UnknownRecord): string {
  const raw = String(item?.url ?? item?.path ?? '')
  return raw
    .replace('{id}', knowledgeId.value)
    .replace('{kb_name}', kbName.value)
}

function getInterfaceMethod(item: UnknownRecord): string {
  return String(item?.method ?? 'POST').toUpperCase()
}

const rawInterfaces = computed<QueryInterfaceItem[]>(() => {
  const data = (interfaceData.value as any)?.data
  return Array.isArray(data) ? (data as QueryInterfaceItem[]) : []
})

const interfaceSelectOptions = computed(() => {
  return rawInterfaces.value
    .map((it) => {
      const name = getInterfaceName(it as UnknownRecord)
      return name
        ? { label: name, value: name }
        : null
    })
    .filter(Boolean) as { label: string, value: string }[]
})

const selectedInterfaceName = ref<string>('')

const selectedInterface = computed<QueryInterfaceItem | null>(() => {
  if (!rawInterfaces.value.length)
    return null
  const found = rawInterfaces.value.find(it => getInterfaceName(it as UnknownRecord) === selectedInterfaceName.value)
  return found ?? rawInterfaces.value[0]!
})

watch(
  () => interfaceSelectOptions.value,
  (opts) => {
    if (!selectedInterfaceName.value && opts.length > 0)
      selectedInterfaceName.value = opts[0]!.value
  },
  { immediate: true },
)

// Parameters
interface ParamItem {
  key: string
  value: string
  description?: string
  required?: boolean
}

const params = ref<ParamItem[]>([])

const hiddenParamKeys = new Set(['rerank', 'rerank_enabled', 'enable_rerank'])

function normalizeParams(raw: unknown): ParamItem[] {
  const items: ParamItem[] = []

  if (Array.isArray(raw)) {
    for (const p of raw as UnknownRecord[]) {
      const key = String(p?.key ?? p?.name ?? p?.field ?? '')
      if (!key || hiddenParamKeys.has(key))
        continue
      items.push({
        key,
        value: String(p?.default ?? p?.value ?? ''),
        description: p?.description ?? p?.desc,
        required: Boolean(p?.required ?? p?.is_required),
      })
    }
    return items
  }

  if (raw && typeof raw === 'object') {
    for (const [key, v] of Object.entries(raw as UnknownRecord)) {
      if (!key || hiddenParamKeys.has(key))
        continue
      if (v && typeof v === 'object') {
        items.push({
          key,
          value: String((v as UnknownRecord).default ?? (v as UnknownRecord).value ?? ''),
          description: (v as UnknownRecord).description ?? (v as UnknownRecord).desc,
          required: Boolean((v as UnknownRecord).required ?? (v as UnknownRecord).is_required),
        })
      }
      else {
        items.push({ key, value: String(v ?? '') })
      }
    }
    return items
  }

  return items
}

// --- Response Handling ---

const loading = ref(false)
const responseStatus = ref<number | null>(null)
const responseTime = ref<string>('')
const responseData = ref<any>(null)

function buildParamsObject() {
  return Object.fromEntries(
    params.value
      .filter(p => p.key)
      .map(p => [p.key, p.value]),
  ) as Record<string, any>
}

async function handleSend() {
  const activeParams = params.value.filter(p => p.key)
  const missingRequired = activeParams.find(p => p.required && !p.value.trim())
  if (missingRequired) {
    message.warning(t('参数 {key} 不能为空', { key: missingRequired.key }))
    return
  }

  loading.value = true
  responseData.value = null
  responseStatus.value = null
  responseTime.value = ''

  const startTime = performance.now()

  try {
    if (!selectedInterface.value)
      throw new Error(t('未选择接口'))

    const url = getInterfaceUrl(selectedInterface.value as UnknownRecord)
    const method = getInterfaceMethod(selectedInterface.value as UnknownRecord)
    const payload = buildParamsObject()
    const token = localStorage.getItem('token')
    const isGet = method === 'GET'

    const res = await axios.request({
      url,
      method: method as any,
      params: isGet ? payload : undefined,
      data: isGet ? undefined : payload,
      withCredentials: true,
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })

    const endTime = performance.now()
    responseTime.value = `${(endTime - startTime).toFixed(0)}ms`
    responseStatus.value = res.status
    responseData.value = res.data
    message.success(t('请求成功'))
  }
  catch (e) {
    const endTime = performance.now()
    responseTime.value = `${(endTime - startTime).toFixed(0)}ms`

    const err = e as AxiosError
    responseStatus.value = err.response?.status ?? null
    responseData.value = err.response?.data ?? { message: err.message }
    message.error(t('请求失败'))
  }
  finally {
    loading.value = false
  }
}

watch(
  () => selectedInterface.value,
  (it) => {
    const raw = (it as UnknownRecord | null)?.params
      ?? (it as UnknownRecord | null)?.parameters
      ?? (it as UnknownRecord | null)?.request_params
      ?? (it as UnknownRecord | null)?.requestParams
    params.value = normalizeParams(raw)
  },
  { immediate: true },
)

function filterInterfaceOption(input: string, option?: { label?: string }) {
  return (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}
</script>

<template>
  <div class="-m-4 flex h-[calc(100vh-112px)] overflow-hidden bg-gray-50">
    <!-- Sidebar -->
    <div class="w-64 shrink-0 bg-white">
      <knowledge-sidebar :knowledge="knowledge" />
    </div>

    <!-- Main Content Area -->
    <div class="flex flex-1 overflow-hidden">
      <!-- Left Pane: Request Config -->
      <div class="flex w-1/2 flex-col border-r border-gray-200 bg-white">
        <!-- Interface Select (单独一行) -->
        <div class="border-b border-gray-200 p-3">
          <a-select
            v-model:value="selectedInterfaceName"
            class="w-full"
            show-search
            :options="interfaceSelectOptions"
            :filter-option="filterInterfaceOption"
            :loading="interfacesLoading"
            :placeholder="t('请选择接口')"
          />
          <div v-if="interfacesError" class="mt-2 text-xs text-red-500">
            {{ t('接口列表加载失败') }}
          </div>
        </div>

        <!-- Toolbar / URL Bar -->
        <div class="flex items-center gap-2 border-b border-gray-200 p-3">
          <!-- Method & URL Display -->
          <div class="flex flex-1 items-center overflow-hidden rounded border border-gray-300 bg-gray-50">
            <span class="border-r border-gray-300 bg-gray-100 px-3 py-1.5 text-xs font-bold text-green-600">
              {{ selectedInterface ? getInterfaceMethod(selectedInterface as any) : 'POST' }}
            </span>
            <span class="truncate px-3 py-1.5 text-sm text-gray-600 font-mono">
              {{ selectedInterface ? getInterfaceUrl(selectedInterface as any) : '-' }}
            </span>
          </div>

          <a-button type="primary" :loading="loading" @click="handleSend">
            <template #icon>
              <send-outlined />
            </template>
            {{ t('发送') }}
          </a-button>
        </div>

        <!-- Parameters Section -->
        <div class="flex-1 overflow-y-auto p-4">
          <div class="mb-2 flex items-center justify-between">
            <h3 class="font-medium text-gray-700">
              {{ t('Params') }}
            </h3>
          </div>

          <div class="space-y-2">
            <div v-if="params.length === 0" class="py-8 text-center text-gray-400">
              {{ t('暂无参数') }}
            </div>
            <div
              v-for="(param, index) in params"
              :key="index"
              class="flex items-center gap-2 rounded border border-transparent p-1 hover:border-gray-200 hover:bg-gray-50"
            >
              <div class="flex flex-1 flex-col gap-1 sm:flex-row">
                <a-input
                  :value="param.key"
                  :placeholder="t('Key')"
                  size="small"
                  class="flex-1 font-mono text-xs"
                  :readonly="true"
                />
                <a-input
                  v-model:value="param.value"
                  :placeholder="t('Value')"
                  size="small"
                  class="flex-[2]"
                />
              </div>

              <a-tooltip v-if="param.description" :title="param.description">
                <span class="cursor-help text-xs text-gray-400">
                  <thunderbolt-outlined />
                </span>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Pane: Response Display -->
      <div class="flex w-1/2 flex-col bg-white">
        <!-- Response Header -->
        <div class="flex h-[58px] items-center justify-between border-b border-gray-200 px-4 py-2">
          <div class="flex items-center gap-4">
            <span class="text-sm font-medium text-gray-700">{{ t('Response') }}</span>
            <span v-if="responseStatus" class="rounded bg-green-100 px-2 py-0.5 text-xs font-semibold text-green-700">
              {{ responseStatus }} OK
            </span>
            <span v-if="responseTime" class="text-xs text-gray-500">
              {{ t('Time: {time}', { time: responseTime }) }}
            </span>
          </div>
          <div class="flex items-center gap-2">
            <!-- View Options (Placeholder) -->
            <span class="text-xs text-gray-400">{{ t('JSON') }}</span>
          </div>
        </div>

        <!-- Response Body -->
        <div class="flex-1 overflow-auto bg-gray-50 p-0">
          <div v-if="!responseData && !loading" class="flex h-full flex-col items-center justify-center text-gray-400">
            <code-outlined class="mb-2 text-4xl opacity-20" />
            <p class="text-sm">
              {{ t('点击{action}获取响应结果', { action: t('发送') }) }}
            </p>
          </div>

          <div v-else-if="loading" class="flex h-full items-center justify-center">
            <a-spin :tip="t('请求处理中...')" />
          </div>

          <div v-else class="h-full w-full overflow-auto p-4">
            <pre class="font-mono text-xs leading-relaxed text-gray-800">{{ JSON.stringify(responseData, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

  <style scoped>
  /* Custom scrollbar for pre block */
pre {
  white-space: pre-wrap; /* css-3 */
  white-space: -moz-pre-wrap; /* Mozilla, since 1999 */
  white-space: -pre-wrap; /* Opera 4-6 */
  white-space: -o-pre-wrap; /* Opera 7 */
  word-wrap: break-word; /* Internet Explorer 5.5+ */
}
</style>
