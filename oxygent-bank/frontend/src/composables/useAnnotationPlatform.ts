import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import utc from 'dayjs/plugin/utc'
import { computed, reactive, ref, watch } from 'vue'
import type {
  AnnotationFilters,
  AnnotationItem,
  AnnotationStats,
} from '@/views/annotation/types'
import { t } from '@/locales'

const API_BASE = '/api/v1'

const statusLabels: Record<string, string> = {
  pending: 'Pending',
  annotated: 'Annotated',
  approved: 'Approved',
  rejected: 'Rejected',
  kb_ingested: 'KB Ingested',
  kb_failed: 'KB Failed',
}

const dataTypeLabels: Record<string, string> = {
  e2e: 'E2E',
  agent: 'Agent',
  llm: 'LLM',
  tool: 'Tool',
  custom: 'Custom',
}

dayjs.extend(utc)

const agentColorMap = [
  { bgColor: '#FEEAD4', color: '#7d4303' },
  { bgColor: '#E4FBCC', color: '#417609' },
  { bgColor: '#D3F8DF', color: '#116e30' },
  { bgColor: '#E0F2FE', color: '#044c7c' },
  { bgColor: '#E0EAFF', color: '#002980' },
  { bgColor: '#EFF1F5', color: '#313b4e' },
  { bgColor: '#FBE8FF', color: '#690080' },
  { bgColor: '#FBE7F6', color: '#6d1257' },
  { bgColor: '#FEF7C4', color: '#7d6e02' },
  { bgColor: '#E6F4D7', color: '#41641b' },
  { bgColor: '#D5F5F6', color: '#166669' },
  { bgColor: '#D2E9FF', color: '#004180' },
  { bgColor: '#D1DFFF', color: '#002780' },
  { bgColor: '#D5D9EB', color: '#293156' },
  { bgColor: '#EBE9FE', color: '#11067a' },
  { bgColor: '#FFE4E8', color: '#800013' },
]

function hashCode(str: string) {
  let hash = 0
  for (let i = 0; i < str.length; i += 1) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash &= hash
  }
  return hash
}

function getAgentAvatarStyle(name: string, size = 20) {
  if (!name) {
    return {
      backgroundColor: '#eee',
      color: '#666',
      fontSize: `${size * 0.5}px`,
    }
  }
  const idx = Math.abs(hashCode(name)) % agentColorMap.length
  const cur = agentColorMap[idx]
  return {
    backgroundColor: cur?.bgColor || '#eee',
    color: cur?.color || '#666',
    fontSize: `${size * 0.5}px`,
  }
}

function normalizeDateInput(value?: string | null) {
  if (!value)
    return null
  return value.includes('T') ? value : value.replace(' ', 'T')
}

async function requestJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })
  if (!response.ok) {
    throw new Error(`API call failed: ${response.status}`)
  }
  const result = await response.json()
  // Handle API response format: { code, msg, data }
  if (result && typeof result === 'object' && 'data' in result) {
    return result.data as T
  }
  return result as T
}

function buildQuery(params: Record<string, string | number | undefined | null>) {
  const url = new URL(`${API_BASE}/data`, window.location.origin)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.append(key, String(value))
    }
  })
  return url
}

export function useAnnotationPlatform() {
  const dataList = ref<AnnotationItem[]>([])
  const stats = reactive<AnnotationStats>({
    pending: 0,
    annotated: 0,
    approved: 0,
    rejected: 0,
    kb_ingested: 0,
    kb_failed: 0,
  })

  const filters = reactive<AnnotationFilters>({
    timeRange: undefined,
    dataType: '',
    status: '',
    priority: '',
    caller: '',
    callee: '',
    groupId: '',
    traceId: '',
    search: '',
  })

  const pagination = reactive({
    page: 1,
    pageSize: 20,
    total: 0,
  })

  const loading = ref(false)
  const statsLoading = ref(false)
  const detailLoading = ref(false)

  const drawerOpen = ref(false)
  const selectedId = ref<string | null>(null)
  const selectedData = ref<AnnotationItem | null>(null)

  const annotationForm = reactive({
    question: '',
    answer: '',
    score: undefined as string | undefined,
    comment: '',
  })

  const rejectModalOpen = ref(false)
  const rejectReason = ref('')
  const pendingRejectId = ref<string | null>(null)
  const kbEnabled = ref(true)

  const totalPages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)))

  const paginationInfo = computed(
    () => t('Page {page}/{total}, {count} items', {
      page: pagination.page,
      total: totalPages.value,
      count: pagination.total,
    }),
  )

  const progressTotal = computed(
    () =>
      stats.pending
      + stats.annotated
      + stats.approved
      + stats.rejected
      + stats.kb_ingested
      + stats.kb_failed,
  )

  const progressSegments = computed(() => {
    const total = progressTotal.value
    const calc = (value: number) => (total > 0 ? (value / total) * 100 : 0)
    return {
      pending: calc(stats.pending),
      annotated: calc(stats.annotated),
      approved: calc(stats.approved),
      rejected: calc(stats.rejected),
      kb_ingested: calc(stats.kb_ingested),
      kb_failed: calc(stats.kb_failed),
    }
  })

  function setDefaultTimeRange() {
    const now = dayjs().utcOffset(8 * 60)
    const roundedNow = now.second(0).millisecond(0).add(1, 'minute')
    const threeDaysAgo = roundedNow.subtract(3, 'day')
    filters.timeRange = [threeDaysAgo, roundedNow]
  }

  function formatForBackend(value?: Dayjs | null) {
    if (!value)
      return ''
    return value.utcOffset(8 * 60).format('YYYY-MM-DDTHH:mm:ss')
  }

  function getStatusLabel(status?: string | null) {
    if (!status)
      return '-'
    return t(statusLabels[status] || status)
  }

  function getDataTypeLabel(type?: string | null) {
    if (!type)
      return '-'
    return t(dataTypeLabels[type] || type)
  }

  function formatDateShort(value?: string | null) {
    if (!value)
      return '-'
    const normalized = normalizeDateInput(value)
    return dayjs(normalized).format('MM-DD HH:mm:ss')
  }

  function formatDateTimeFull(value?: string | null) {
    if (!value)
      return '-'
    const normalized = normalizeDateInput(value)
    return dayjs(normalized).format('YYYY-MM-DD HH:mm')
  }

  function isJsonString(value?: unknown) {
    if (!value || typeof value !== 'string')
      return false
    try {
      JSON.parse(value)
      return true
    }
    catch {
      return false
    }
  }

  function formatContent(value?: unknown) {
    if (value === null || value === undefined || value === '') {
      return null
    }
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2)
    }
    if (isJsonString(value)) {
      return JSON.stringify(JSON.parse(value as string), null, 2)
    }
    return String(value)
  }

  const annotationEntries = computed(() => {
    if (!selectedData.value?.annotation) {
      return []
    }
    const entries = Object.entries(selectedData.value.annotation)
    return entries.sort(([a], [b]) => {
      const keyA = a.toLowerCase()
      const keyB = b.toLowerCase()
      if (keyA === 'question')
        return -1
      if (keyB === 'question')
        return 1
      if (keyA === 'content')
        return -1
      if (keyB === 'content')
        return 1
      if (keyA === 'comment')
        return 1
      if (keyB === 'comment')
        return -1
      return keyA.localeCompare(keyB)
    })
  })

  const qaContent = computed(() => ({
    question: formatContent(selectedData.value?.question),
    answer: formatContent(selectedData.value?.answer),
  }))

  const detailTags = computed(() => ({
    priority: selectedData.value?.priority ?? 4,
    status: selectedData.value?.status,
    dataType: selectedData.value?.data_type,
  }))

  const hasAnnotationResult = computed(
    () => !!selectedData.value?.annotation && Object.keys(selectedData.value.annotation).length > 0,
  )

  const hasRejectReason = computed(
    () => selectedData.value?.status === 'rejected' && !!selectedData.value?.reject_reason,
  )

  const showAnnotationForm = computed(() => selectedData.value?.status === 'pending')
  const showReviewSection = computed(
    () => selectedData.value?.status === 'annotated' || selectedData.value?.status === 'approved',
  )
  const showKbSection = computed(
    () =>
      selectedData.value?.status === 'approved'
      || selectedData.value?.status === 'kb_ingested'
      || selectedData.value?.status === 'kb_failed',
  )

  async function loadStats() {
    statsLoading.value = true
    try {
      const start = filters.timeRange?.[0]
      const end = filters.timeRange?.[1]
      const url = new URL(`${API_BASE}/stats`, window.location.origin)
      url.searchParams.set('start_time', start ? formatForBackend(start) : '')
      url.searchParams.set('end_time', end ? formatForBackend(end) : '')
      const response = await requestJson<{
        pending_count?: number
        annotated_count?: number
        approved_count?: number
        rejected_count?: number
        kb_ingested_count?: number
        kb_failed_count?: number
      }>(url.toString())
      stats.pending = response.pending_count ?? 0
      stats.annotated = response.annotated_count ?? 0
      stats.approved = response.approved_count ?? 0
      stats.rejected = response.rejected_count ?? 0
      stats.kb_ingested = response.kb_ingested_count ?? 0
      stats.kb_failed = response.kb_failed_count ?? 0
    }
    catch (error) {
      console.error('Failed to fetch statistics:', error)
    }
    finally {
      statsLoading.value = false
    }
  }

  async function loadData(page = 1) {
    loading.value = true
    try {
      const start = filters.timeRange?.[0]
      const end = filters.timeRange?.[1]
      const url = buildQuery({
        data_type: filters.dataType,
        status: filters.status,
        priority: filters.priority,
        caller: filters.caller,
        callee: filters.callee,
        group_id: filters.groupId,
        trace_id: filters.traceId,
        search: filters.search,
        start_time: start ? formatForBackend(start) : '',
        end_time: end ? formatForBackend(end) : '',
        page,
        page_size: pagination.pageSize,
      })

      const response = await requestJson<{
        items?: AnnotationItem[]
        total?: number
        page?: number
        total_pages?: number
      }>(url.toString())

      dataList.value = response.items || []
      pagination.total = response.total ?? 0
      pagination.page = response.page ?? page
      if (response.total_pages) {
        pagination.page = Math.min(pagination.page, response.total_pages)
      }
    }
    catch (error: any) {
      console.error('Failed to fetch data list:', error)
      message.error(t('Failed to fetch data list'))
    }
    finally {
      loading.value = false
    }
  }

  function applyFilters() {
    loadStats()
    loadData(1)
  }

  function resetFilters() {
    filters.dataType = ''
    filters.status = ''
    filters.priority = ''
    filters.caller = ''
    filters.callee = ''
    filters.groupId = ''
    filters.traceId = ''
    filters.search = ''
    setDefaultTimeRange()
    applyFilters()
  }

  async function openDetail(dataId: string) {
    detailLoading.value = true
    selectedId.value = dataId
    try {
      const url = new URL(`${API_BASE}/data/${dataId}`, window.location.origin)
      const response = await requestJson<AnnotationItem>(url.toString())
      selectedData.value = response
      drawerOpen.value = true
    }
    catch (error: any) {
      console.error('Failed to fetch data details:', error)
      message.error(t('Failed to fetch data details'))
    }
    finally {
      detailLoading.value = false
    }
  }

  function closeDrawer() {
    drawerOpen.value = false
    selectedData.value = null
    selectedId.value = null
  }

  function openRejectModal(dataId: string) {
    pendingRejectId.value = dataId
    rejectReason.value = ''
    rejectModalOpen.value = true
  }

  async function confirmReject() {
    if (!pendingRejectId.value)
      return
    try {
      await requestJson(`${API_BASE}/data/${pendingRejectId.value}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reject_reason: rejectReason.value }),
      })
      message.success(t('Rejected'))
      rejectModalOpen.value = false
      pendingRejectId.value = null
      closeDrawer()
      loadData(pagination.page)
      loadStats()
    }
    catch (error) {
      console.error('Operation failed:', error)
      message.error(t('Operation failed'))
    }
  }

  async function approveAnnotation(dataId: string) {
    try {
      await requestJson(`${API_BASE}/data/${dataId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      })
      message.success(t('Approved'))
      closeDrawer()
      loadData(pagination.page)
      loadStats()
    }
    catch (error) {
      console.error('Operation failed:', error)
      message.error(t('Operation failed'))
    }
  }

  async function ingestToKb(dataId: string, retry = false) {
    if (!kbEnabled.value) {
      message.warning(
        t('Knowledge Base is not configured. Please configure QA_KB_ENDPOINT and QA_KB_ID.'),
      )
      return
    }
    try {
      message.info(retry ? t('Retrying KB ingestion...') : t('Ingesting to Knowledge Base...'))
      const response = await requestJson<{ success?: boolean, message?: string }>(
        `${API_BASE}/kb/data/${dataId}/ingest`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(retry ? { retry: true } : {}),
        },
      )
      if (response?.success) {
        message.success(t('Successfully ingested to Knowledge Base'))
      }
      else {
        message.error(t('Failed to ingest: {message}', { message: response?.message || t('Unknown error') }))
      }
      closeDrawer()
      loadData(pagination.page)
      loadStats()
    }
    catch (error: any) {
      console.error('KB ingestion failed:', error)
      message.error(t('KB ingestion failed: {message}', { message: error.message }))
    }
  }

  async function submitAnnotation(dataId: string) {
    if (!annotationForm.score) {
      message.warning(t('Please select quality score'))
      return
    }
    if (!annotationForm.question && !annotationForm.answer && !annotationForm.comment) {
      message.warning(t('Please fill in at least one correction or comment'))
      return
    }
    try {
      await requestJson(`${API_BASE}/data/${dataId}/annotate`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          status: 'annotated',
          annotation: {
            content: annotationForm.answer,
            question: annotationForm.question,
            score: Number(annotationForm.score),
            comment: annotationForm.comment || '',
          },
          scores: { overall_score: Number(annotationForm.score) },
        }),
      })
      message.success(t('Annotation submitted successfully'))
      closeDrawer()
      loadData(pagination.page)
      loadStats()
    }
    catch (error: any) {
      console.error('Annotation failed:', error)
      message.error(t('Annotation failed: {message}', { message: error.message }))
    }
  }

  watch(
    () => selectedData.value,
    (value) => {
      if (!value)
        return
      annotationForm.question = formatContent(value.question) || ''
      annotationForm.answer = formatContent(value.answer) || ''
      annotationForm.score = undefined
      annotationForm.comment = ''
    },
  )

  watch(rejectModalOpen, (open) => {
    if (!open) {
      pendingRejectId.value = null
    }
  })

  return {
    approveAnnotation,
    annotationEntries,
    annotationForm,
    applyFilters,
    closeDrawer,
    confirmReject,
    dataList,
    detailLoading,
    detailTags,
    drawerOpen,
    filters,
    formatContent,
    formatDateShort,
    formatDateTimeFull,
    getAgentAvatarStyle,
    getDataTypeLabel,
    getStatusLabel,
    hasAnnotationResult,
    hasRejectReason,
    ingestToKb,
    kbEnabled,
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
    totalPages,
  }
}
