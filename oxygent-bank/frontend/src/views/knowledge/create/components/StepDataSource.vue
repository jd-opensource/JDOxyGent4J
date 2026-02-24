<script setup lang="ts">
import {
  CloudUploadOutlined,
  FileExcelOutlined,
  FileTextOutlined,
  InboxOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import type { UploadChangeParam, UploadFile, UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { KnowledgeBaseInfo } from '../../types'
import CreateKbModal from '@/views/knowledge/components/CreateKbModal.vue'
import { useI18n } from '@/locales'
import Apis from '@/api'

interface Props {
  kbId?: string // 可选：如果已有资产库 ID，则使用它
  kbType?: 'structured' | 'unstructured' | ''
  schemaExists?: boolean
}

// 上传结果数据
interface UploadResultData {
  info: KnowledgeBaseInfo
  file: File
  kbId: string
  fileId: string
  filePath: string
  fileName: string
  fileType: string
}

const props = withDefaults(defineProps<Props>(), {
  kbId: '',
  kbType: '',
  schemaExists: false,
})

const emit = defineEmits<{
  (e: 'next', data: UploadResultData): void
}>()

const router = useRouter()
const { t } = useI18n()

// KB Form Data
const formState = reactive<KnowledgeBaseInfo>({
  kb_name: '',
  kb_type: 'structured',
})

const createModalRef = ref()

// Data Source Selection
const dataSourceType = ref<'structured' | 'unstructured'>('structured')

const dataSources = [
  {
    key: 'structured',
    title: '结构化数据',
    desc: 'Excel / CSV',
    icon: FileExcelOutlined,
  },
  {
    key: 'unstructured',
    title: '非结构化数据',
    desc: 'PDF / TXT / MD / Word',
    icon: FileTextOutlined,
  },
]

const isSourceLocked = computed(() => !!props.kbId && !!props.kbType)
const availableDataSources = computed(() => {
  if (!isSourceLocked.value)
    return dataSources
  return dataSources.filter(s => s.key === props.kbType)
})

watch(
  () => props.kbType,
  (type) => {
    if (!isSourceLocked.value || !type)
      return
    if (dataSourceType.value !== type) {
      dataSourceType.value = type as any
      resetFileState()
    }
  },
  { immediate: true },
)

// File Upload State
const fileList = ref<UploadFile[]>([])
const rawFile = ref<File | null>(null)
const uploading = ref(false)
const uploadedKbId = ref('')
const uploadedFileId = ref('')
const uploadedFilePath = ref('')
const acceptFormatsStructured = '.xlsx,.xls,.csv'
const acceptFormatsUnstructured = '.txt,.md,.markdown,.pdf,.html,.htm,.docx,.doc'

const acceptFormats = computed(() => {
  return dataSourceType.value === 'structured' ? acceptFormatsStructured : acceptFormatsUnstructured
})

const nextHint = computed(() => {
  if (props.schemaExists)
    return t('✓ 文件已上传成功，可点击{action}完成', { action: t('确认导入') })
  return dataSourceType.value === 'unstructured'
    ? t('✓ 文件已上传成功，下一步进入分段设置并开始处理')
    : t('✓ 文件已上传成功，下一步进入结构化配置并继续检索策略')
})

// 是否已完成上传
const isUploaded = computed(() => !!uploadedFileId.value)

/**
 * 上传文件到服务器
 * 手动创建 FormData 以确保文件以 multipart/form-data 格式上传
 */
async function uploadFileToServer(kbIdParam: string, file: File): Promise<{ fileId: string, filePath: string } | null> {
  // 手动创建 FormData 对象，确保 Content-Type 为 multipart/form-data
  const formData = new FormData()
  formData.append('file', file)

  const uploadRes = await Apis.knowledgeBaseFileManagement.upload_kb_file_api_v1_kb_base__kb_id__upload_file_post({
    pathParams: { kb_id: kbIdParam },
    data: formData as any,
  })

  const data = uploadRes.data
  const fileId = data?.file_id || ''
  const filePath = data?.file_path || ''

  if (!fileId)
    return null

  return { fileId, filePath }
}

/**
 * 自定义上传请求 - 在文件选择时立即执行上传
 */
async function customUploadRequest(options: any) {
  const { file, onSuccess, onError, onProgress } = options
  const uploadFile = file as File

  // 验证文件大小
  const isLt15M = uploadFile.size / 1024 / 1024 < 15
  if (!isLt15M) {
    message.error(t('{name} 文件大小超过 15MB 限制', { name: uploadFile.name }))
    onError?.(new Error(t('文件大小超过限制')))
    return
  }

  uploading.value = true
  rawFile.value = uploadFile

  try {
    if (isSourceLocked.value) {
      dataSourceType.value = props.kbType as any
    }

    onProgress?.({ percent: 10 })

    // 1. 设置表单数据（用文件名作为资产库名）
    const fileName = uploadFile.name
    const nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.')) || fileName
    formState.kb_name = nameWithoutExt
    // formState.kb_path = `/${nameWithoutExt}`
    formState.kb_type = dataSourceType.value

    onProgress?.({ percent: 30 })

    // 2. 必须先创建空资产库获取 kb_id
    const targetKbId = props.kbId || uploadedKbId.value
    if (!targetKbId)
      throw new Error(t('请先创建空资产库'))

    uploadedKbId.value = targetKbId

    onProgress?.({ percent: 50 })

    // 3. 上传文件（直接传 File 对象，它本身就是 Blob）
    const uploadResult = await uploadFileToServer(targetKbId, uploadFile)
    if (!uploadResult) {
      throw new Error(t('文件上传失败'))
    }

    uploadedFileId.value = uploadResult.fileId
    uploadedFilePath.value = uploadResult.filePath

    onProgress?.({ percent: 100 })
    onSuccess?.(uploadResult, uploadFile)

    message.success(t('{name} 上传成功', { name: uploadFile.name }))
  }
  catch (error) {
    console.error('上传失败:', error)
    message.error(t('上传失败: {message}', { message: (error as Error).message || t('未知错误') }))
    onError?.(error)
    resetFileState()
  }
  finally {
    uploading.value = false
  }
}

function handleFileChange(info: UploadChangeParam) {
  // 只保留最新的一个文件
  fileList.value = info.fileList.slice(-1)
}

function handleRemove() {
  resetFileState()
}

function resetFileState() {
  fileList.value = []
  rawFile.value = null
  uploadedFileId.value = ''
  uploadedFilePath.value = ''
  uploadedKbId.value = ''
}

function handleSourceSelect(type: 'structured' | 'unstructured') {
  if (isSourceLocked.value)
    return
  dataSourceType.value = type
  resetFileState()
}

const uploadProps = computed<UploadProps>(() => ({
  multiple: false,
  accept: acceptFormats.value,
  customRequest: customUploadRequest,
  showUploadList: {
    showRemoveIcon: true,
  },
}))

// Actions
async function handleNext() {
  try {
    if (!rawFile.value) {
      message.warning(t('请上传文件'))
      return
    }

    if (!isUploaded.value) {
      message.warning(t('文件正在上传中，请稍候'))
      return
    }

    formState.kb_type = dataSourceType.value

    emit('next', {
      info: { ...formState },
      file: rawFile.value,
      kbId: uploadedKbId.value,
      fileId: uploadedFileId.value,
      filePath: uploadedFilePath.value,
      fileName: rawFile.value.name,
      fileType: rawFile.value.name.split('.').pop() || '',
    })
  }
  catch (error) {
    console.error(error)
  }
}

function handleCreateEmpty() {
  createModalRef.value?.show()
}

function handleCreateSuccess(id: string) {
  router.push({ path: '/knowledge/create', query: { kb_id: id } })
}
</script>

<template>
  <div class="step-data-source">
    <!-- Data Source Selection -->
    <div class="mb-6">
      <h3 class="mb-4 text-base font-medium text-gray-900">
        {{ t('选择数据源') }}
      </h3>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="source in availableDataSources"
          :key="source.key"
          class="group relative flex cursor-pointer items-center gap-4 rounded-xl border p-4 transition-all hover:shadow-md"
          :class="dataSourceType === source.key
            ? 'border-indigo-500 bg-indigo-50/30 ring-1 ring-indigo-500'
            : 'border-gray-200 bg-white hover:border-indigo-300'"
          @click="handleSourceSelect(source.key as any)"
        >
          <div
            class="flex h-10 w-10 items-center justify-center rounded-lg transition-colors"
            :class="dataSourceType === source.key ? 'bg-indigo-100 text-indigo-600' : 'bg-gray-100 text-gray-500 group-hover:bg-indigo-50 group-hover:text-indigo-500'"
          >
            <component :is="source.icon" class="text-xl" />
          </div>
          <div>
            <div class="font-medium text-gray-900">
              {{ t(source.title) }}
            </div>
            <div class="text-xs text-gray-500">
              {{ t(source.desc) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Upload Area -->
    <div class="mb-8">
      <h3 class="mb-4 text-base font-medium text-gray-900">
        {{ t('上传文本文件') }}
      </h3>
      <div class="upload-wrapper">
        <a-upload-dragger
          v-model:file-list="fileList"
          v-bind="uploadProps"
          class="custom-dragger"
          :multiple="false"
          :max-count="1"
          :disabled="uploading"
          @change="handleFileChange"
          @remove="handleRemove"
        >
          <div class="py-8">
            <div class="mb-4 flex justify-center">
              <div class="flex h-12 w-12 items-center justify-center rounded-full bg-indigo-50 text-indigo-500">
                <loading-outlined v-if="uploading" class="text-2xl animate-spin" />
                <cloud-upload-outlined v-else class="text-2xl" />
              </div>
            </div>
            <p class="mb-2 text-base font-medium text-gray-800">
              <template v-if="uploading">
                {{ t('正在上传文件...') }}
              </template>
              <template v-else>
                {{ t('拖拽文件或文件夹至此，或者') }} <span class="text-indigo-600">{{ t('选择文件') }}</span>
              </template>
            </p>
            <p class="text-sm text-gray-400">
              {{ t('已支持 {formats} 等格式。', { formats: dataSourceType === 'structured' ? 'XLSX, XLS, CSV' : 'PDF, TXT, MD, DOCX, HTML' }) }}
              <br>{{ t('单次仅支持一个文件，每个文件不超过 15 MB。') }}
            </p>
            <!-- 上传成功提示 -->
            <p v-if="isUploaded" class="mt-2 text-sm text-green-600">
              {{ nextHint }}
            </p>
          </div>
        </a-upload-dragger>
      </div>
    </div>

    <!-- Footer Actions -->
    <div class="flex items-center justify-between border-t border-gray-100 pt-6">
      <div class="flex items-center">
        <a-button
          type="link"
          class="flex items-center px-0 text-gray-500 hover:text-indigo-600"
          @click="handleCreateEmpty"
        >
          <template #icon>
            <inbox-outlined />
          </template>
          {{ t('创建空资产库') }}
        </a-button>
      </div>

      <a-button type="primary" class="px-8" :disabled="uploading" @click="handleNext">
        {{ schemaExists ? t('确认导入') : t('下一步') }}
      </a-button>
    </div>

    <!-- Empty KB Modal -->
    <create-kb-modal ref="createModalRef" @success="handleCreateSuccess" />
  </div>
</template>

<style scoped>
.step-data-source {
  @apply max-w-4xl mx-auto;
}

.upload-wrapper :deep(.ant-upload.ant-upload-drag) {
  @apply border-2 border-dashed border-gray-200 bg-gray-50/50 rounded-xl transition-colors;
}

.upload-wrapper :deep(.ant-upload.ant-upload-drag:hover) {
  @apply border-indigo-400 bg-indigo-50/20;
}

.upload-wrapper :deep(.ant-upload-list-item) {
  @apply mt-4 border border-gray-200 rounded-lg px-4 py-2 bg-white;
}
</style>
