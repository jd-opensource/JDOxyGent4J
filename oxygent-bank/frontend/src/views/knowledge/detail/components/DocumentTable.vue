<script setup lang="ts">
import {
  DeleteOutlined,
  EditOutlined,
  FileExcelOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileWordOutlined,
  PlusOutlined,
  SearchOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { Modal, message } from 'ant-design-vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePagination } from 'alova/client'
import type { DocumentStatus } from '../../types'
import type { KnowledgeFileItem } from '@/api/globals'
import TableList from '@/components/common/TableList.vue'
import { useI18n } from '@/locales'
import Apis from '@/api'

interface Props {
  knowledgeId: string
  kbType: string
}

const props = defineProps<Props>()

const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()

// 获取文件列表 - 使用 usePagination
const {
  data: fileList,
  loading,
  page,
  pageSize,
  // reload,
} = usePagination(
  (page, pageSize) =>
    Apis.knowledgeBaseFileManagement.get_kb_files_api_v1_kb_base__kb_id__kb_file_get({
      pathParams: {
        kb_id: props.knowledgeId,
      },
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
    initialPageSize: 10,
  },
)

// 内部状态
const searchText = ref('')
const statusFilter = ref<DocumentStatus | 'all'>('all')

// 将 API 数据映射为组件内部使用的格式
interface MappedDocument {
  id: string
  name: string
  fileName: string
  chunkMode: string
  characterCount: number
  hitCount: number
  update_time: string
  status: DocumentStatus
}

const documents = computed<MappedDocument[]>(() => {
  return fileList.value.map((file: KnowledgeFileItem) => {
    // 从 file_extra_info 中提取信息，如果不存在则使用默认值
    const extraInfo = file.file_extra_info || {}
    const fileName = file.file_name

    return {
      id: file.ori_file_id,
      name: fileName,
      fileName, // 用于文件图标
      chunkMode: 'auto', // API 未提供，默认为 auto
      characterCount: (extraInfo.character_count as number) || 0,
      hitCount: (extraInfo.hit_count as number) || 0,
      update_time: (extraInfo.update_time as string) || file.update_time || '',
      status: mapFileStatus(extraInfo.status as string),
    }
  })
})

// 从文件路径中提取文件名
// function extractFileName(filePath: string): string {
//   const parts = filePath.split('/')
//   return parts[parts.length - 1] || filePath
// }

// 映射文件状态
function mapFileStatus(status: string | undefined): DocumentStatus {
  if (!status)
    return 'processing'
  if (status === 'enabled' || status === 'active')
    return 'enabled'
  if (status === 'disabled' || status === 'inactive')
    return 'disabled'
  if (status === 'error' || status === 'failed')
    return 'error'
  return 'processing'
}

// 状态选项
// const statusOptions = [
//   { value: 'all', label: 'All Status' },
//   { value: 'enabled', label: '可用' },
//   { value: 'disabled', label: '禁用' },
//   { value: 'processing', label: '处理中' },
//   { value: 'error', label: '错误' },
// ]

// 过滤后的数据
const filteredDocuments = computed(() => {
  let result = documents.value

  // 搜索过滤
  if (searchText.value) {
    result = result.filter(item =>
      item.name.toLowerCase().includes(searchText.value.toLowerCase()),
    )
  }

  // 状态过滤
  if (statusFilter.value !== 'all') {
    result = result.filter(item => item.status === statusFilter.value)
  }

  return result
})

// 表格列配置
const columns = computed<TableColumnsType<MappedDocument>>(() => [
  {
    title: '#',
    dataIndex: 'index',
    key: 'index',
    width: 60,
    align: 'center',
    customRender: ({ index }) => (page.value - 1) * pageSize.value + index + 1,
  },
  {
    title: t('名称'),
    dataIndex: 'name',
    key: 'name',
    width: 250,
  },
  {
    title: t('分段模式'),
    dataIndex: 'chunkMode',
    key: 'chunkMode',
    width: 100,
    align: 'center',
  },
  {
    title: t('字符数'),
    dataIndex: 'characterCount',
    key: 'characterCount',
    width: 100,
    align: 'right',
  },
  {
    title: t('召回次数'),
    dataIndex: 'hitCount',
    key: 'hitCount',
    width: 100,
    align: 'center',
  },
  {
    title: t('更新时间'),
    dataIndex: 'update_time',
    key: 'update_time',
    width: 180,
    // sorter: (a: MappedDocument, b: MappedDocument) =>
    //   new Date(a.update_time).getTime() - new Date(b.update_time).getTime(),
    // defaultSortOrder: 'descend',
  },
  // {
  //   title: t('状态'),
  //   dataIndex: 'status',
  //   key: 'status',
  //   width: 100,
  //   align: 'center',
  // },
  {
    title: t('操作'),
    key: 'action',
    width: 150,
    align: 'center',
    fixed: 'right',
  },
])

// 获取文件图标
function getFileIcon(filename: string) {
  const ext = filename.split('.').pop()?.toLowerCase()
  switch (ext) {
    case 'pdf':
      return FilePdfOutlined
    case 'doc':
    case 'docx':
      return FileWordOutlined
    case 'xls':
    case 'xlsx':
    case 'csv':
      return FileExcelOutlined
    case 'txt':
    case 'md':
      return FileTextOutlined
    default:
      return FileOutlined
  }
}

// 获取文件图标颜色
function getFileIconColor(filename: string) {
  const ext = filename.split('.').pop()?.toLowerCase()
  switch (ext) {
    case 'pdf':
      return '#ff4d4f'
    case 'doc':
    case 'docx':
      return '#1890ff'
    case 'xls':
    case 'xlsx':
    case 'csv':
      return '#52c41a'
    default:
      return '#1890ff'
  }
}

// 格式化字符数
function formatCharacters(num: number): string {
  if (num >= 10000) {
    if (locale.value === 'en-US') {
      return `${(num / 1000).toFixed(1)}k`
    }
    return `${(num / 10000).toFixed(1)}万`
  }
  if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`
  }
  return num.toString()
}

// 格式化分段模式
function formatChunkMode(mode: string) {
  return mode === 'auto' ? t('通用') : t('自定义')
}

// 切换状态
function handleToggleStatus(record: Record<string, any>) {
  if (record.status === 'processing') {
    message.warning(t('文档处理中，无法切换状态'))
    return
  }

  const newStatus = record.status === 'enabled' ? 'disabled' : 'enabled'

  // TODO: 调用后端接口更新文档状态
  // await Apis.general.update_kb_file_status_api({
  //   pathParams: { kb_id: props.knowledgeId, file_id: record.id },
  //   data: { status: newStatus }
  // })

  message.success(t('已{status}文档', { status: newStatus === 'enabled' ? t('启用') : t('禁用') }))
  // TODO: 刷新数据
}

// 查看 Chunk
function handleViewChunks(record: Record<string, any>) {
  router.push({
    path: `/knowledge/${props.knowledgeId}/document/${record.id}`,
    query: {
      ...route.query,
      kb_type: props.kbType,
    },
  })
}

// 重命名状态
const renameModalVisible = ref(false)
const renameForm = ref({
  id: '',
  name: '',
})

// 打开重命名弹窗
function handleRename(record: Record<string, any>) {
  renameForm.value = {
    id: record.id,
    name: record.name,
  }
  renameModalVisible.value = true
}

// 确认重命名
async function handleRenameConfirm() {
  // TODO: 调用后端接口更新文档名称
  // await Apis.general.update_kb_file_name_api({
  //   pathParams: { kb_id: props.knowledgeId, file_id: renameForm.value.id },
  //   data: { file_name: renameForm.value.name }
  // })

  message.success(t('重命名成功'))
  renameModalVisible.value = false
  // TODO: 刷新数据
}

// 删除文档
function handleDelete(record: Record<string, any>) {
  Modal.confirm({
    title: t('确认删除'),
    content: t('确定要删除文档「{name}」吗？此操作不可恢复。', { name: record.name }),
    okText: t('删除'),
    okType: 'danger',
    cancelText: t('取消'),
    async onOk() {
      // TODO: 调用后端接口删除文档
      // await Apis.general.delete_kb_file_api({
      //   pathParams: { kb_id: props.knowledgeId, file_id: record.id }
      // })

      message.success(t('删除成功'))
      // TODO: 刷新数据
    },
  })
}

// 添加文件
function handleAddFile() {
  router.push(`/knowledge/create?kb_id=${props.knowledgeId}`)
}

// 刷新
// function handleRefresh() {
//   reload()
//   message.success('刷新成功')
// }
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="mb-5">
      <h2 class="m-0 mb-2 text-xl font-semibold text-gray-800">
        {{ t('文档') }}
      </h2>
      <p class="m-0 text-[13px] text-gray-400">
        {{ t('资产库的所有文件都在这里显示。') }}
      </p>
    </div>

    <!-- 工具栏 -->
    <div class="mb-4 flex items-center justify-between">
      <div class="flex gap-3">
        <!-- <a-select
          v-model:value="statusFilter"
          :options="statusOptions"
          class="w-28"
        /> -->
        <a-input
          v-model:value="searchText"
          :placeholder="t('搜索')"
          allow-clear
          class="w-48"
        >
          <template #prefix>
            <search-outlined class="text-gray-400" />
          </template>
        </a-input>
      </div>
      <div class="flex gap-2">
        <!-- <a-button @click="handleRefresh">
          <template #icon>
            <reload-outlined />
          </template>
          元数据
        </a-button> -->
        <a-button type="primary" @click="handleAddFile">
          <template #icon>
            <plus-outlined />
          </template>
          {{ t('添加文件') }}
        </a-button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && filteredDocuments.length === 0" class="rounded-lg border border-gray-100 bg-white p-6 shadow-md">
      <div class="py-16 text-center">
        <div class="mb-4 inline-flex h-20 w-20 items-center justify-center rounded-full bg-indigo-50">
          <file-text-outlined class="text-4xl text-indigo-200" />
        </div>
        <h3 class="mb-2 text-lg font-medium text-gray-900">
          {{ t('暂无文档') }}
        </h3>
        <p class="mb-6 text-sm text-gray-500">
          {{ t('开始上传您的第一个文档到资产库') }}
        </p>
        <a-button type="primary" @click="handleAddFile">
          <template #icon>
            <plus-outlined />
          </template>
          {{ t('添加第一个文档') }}
        </a-button>
      </div>
    </div>

    <!-- 表格 - 使用 TableList 组件 -->
    <table-list
      v-else
      :columns="columns"
      :data-source="filteredDocuments"
      :loading="loading"
      row-key="id"
    >
      <!-- 名称列 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="flex cursor-pointer items-center gap-2" @click="handleViewChunks(record)">
            <component
              :is="getFileIcon(record.fileName)"
              class="text-lg"
              :style="{ color: getFileIconColor(record.fileName) }"
            />
            <span class="text-sm text-blue-500 transition-colors hover:text-blue-600 hover:underline">{{ record.name }}</span>
          </div>
        </template>

        <!-- 分段模式 -->
        <template v-else-if="column.key === 'chunkMode'">
          <a-tag>{{ formatChunkMode(record.chunkMode) }}</a-tag>
        </template>

        <!-- 字符数 -->
        <template v-else-if="column.key === 'characterCount'">
          {{ formatCharacters(record.characterCount) }}
        </template>

        <!-- 状态列 - 使用 Switch -->
        <template v-else-if="column.key === 'status'">
          <a-switch
            :checked="record.status === 'enabled'"
            size="small"
            :disabled="record.status === 'processing'"
            :loading="record.status === 'processing'"
            @change="handleToggleStatus(record)"
          />
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-tooltip :title="t('查看分段')">
              <a-button type="text" size="small" @click="handleViewChunks(record)">
                <template #icon>
                  <unordered-list-outlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip :title="t('重命名')">
              <a-button type="text" size="small" @click="handleRename(record)">
                <template #icon>
                  <edit-outlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip :title="t('删除')">
              <a-button type="text" danger size="small" @click="handleDelete(record)">
                <template #icon>
                  <delete-outlined />
                </template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </template>
    </table-list>

    <!-- 重命名弹窗 -->
    <a-modal
      v-model:open="renameModalVisible"
      :title="t('重命名文档')"
      :width="400"
      @ok="handleRenameConfirm"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('文档名称')">
          <a-input v-model:value="renameForm.name" :placeholder="t('请输入文档名称')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
