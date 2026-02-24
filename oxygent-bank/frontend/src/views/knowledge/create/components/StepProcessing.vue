<script setup lang="ts">
import {
  ApiOutlined,
  BookOutlined,
  CheckCircleFilled,
  FileExcelOutlined,
  FileOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileWordOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { ChunkConfig } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  files: File[]
  config: ChunkConfig
  knowledgeId?: string
}

const props = defineProps<Props>()

defineEmits<{
  (e: 'complete'): void
}>()

const router = useRouter()
const { t } = useI18n()

// 资产库名称
const knowledgeName = ref('')

// 处理进度
interface FileProgress {
  name: string
  progress: number
  status: 'pending' | 'processing' | 'completed' | 'error'
}

const fileProgresses = ref<FileProgress[]>([])

// 整体状态
const isCompleted = computed(() =>
  fileProgresses.value.every(f => f.status === 'completed'),
)

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

// 格式化检索模式
function formatRetrievalMode(mode: string) {
  const map: Record<string, string> = {
    vector: t('向量检索'),
    fulltext: t('全文检索'),
    hybrid: t('混合检索'),
  }
  return map[mode] || mode
}

function formatParserType(type: string) {
  const map: Record<string, string> = {
    smart: t('智能'),
    token: t('字符'),
    sentence: t('句子'),
    markdown: 'Markdown',
    html: 'HTML',
    json: 'JSON',
  }
  return map[type] || type
}

// 初始化
onMounted(() => {
  // 使用第一个文件名作为默认资产库名称
  const firstFile = props.files[0]
  if (firstFile) {
    knowledgeName.value = firstFile.name.replace(/\.[^.]+$/, '')
  }

  // 初始化文件进度
  fileProgresses.value = props.files.map(f => ({
    name: f.name,
    progress: 0,
    status: 'pending' as const,
  }))

  // 模拟处理进度
  simulateProgress()
})

// 模拟处理进度
function simulateProgress() {
  fileProgresses.value.forEach((fileProgress, index) => {
    setTimeout(() => {
      fileProgress.status = 'processing'

      const interval = setInterval(() => {
        if (fileProgress.progress < 100) {
          fileProgress.progress += Math.random() * 15
          if (fileProgress.progress >= 100) {
            fileProgress.progress = 100
            fileProgress.status = 'completed'
            clearInterval(interval)
          }
        }
      }, 200)
    }, index * 500)
  })
}

// 前往文档
function goToDocuments() {
  const id = props.knowledgeId || 'kb-new'
  router.push(`/knowledge/${id}`)
}

// 查看 API
function viewAPI() {
  // TODO: 打开 API 弹窗
}
</script>

<template>
  <div class="step-processing">
    <div class="processing-layout">
      <!-- 左侧主内容 -->
      <div class="main-content">
        <!-- 成功提示 -->
        <div class="success-header">
          <span class="success-icon">🎉</span>
          <h2 class="success-title">
            {{ t('资产库已创建') }}
          </h2>
          <p class="success-desc">
            {{ t('我们自动为该资产库起了个名称，您也可以随时修改') }}
          </p>
        </div>

        <!-- 资产库名称 -->
        <div class="name-section">
          <div class="name-label">
            <book-outlined class="name-icon" />
            {{ t('资产库名称') }}
          </div>
          <a-input
            v-model:value="knowledgeName"
            size="large"
            :placeholder="t('请输入资产库名称')"
          />
        </div>

        <!-- 处理进度 -->
        <div class="progress-section">
          <div class="progress-header">
            <loading-outlined v-if="!isCompleted" class="spin-icon" />
            <check-circle-filled v-else class="check-icon" />
            <span>{{ isCompleted ? t('处理完成') : t('嵌入处理中...') }}</span>
          </div>

          <div class="file-list">
            <div
              v-for="file in fileProgresses"
              :key="file.name"
              class="file-item"
            >
              <component
                :is="getFileIcon(file.name)"
                class="file-icon"
                :style="{ color: getFileIconColor(file.name) }"
              />
              <span class="file-name">{{ file.name }}</span>
              <span class="file-progress">{{ Math.round(file.progress) }}%</span>
            </div>
          </div>
        </div>

        <!-- 配置摘要 -->
        <div class="config-summary">
          <a-descriptions
            :column="2"
            bordered
            size="small"
          >
            <a-descriptions-item :label="t('切分策略')">
              {{ formatParserType(config.parserType) }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('Chunk 大小')">
              {{ config.chunkSize }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('重叠长度')">
              {{ config.chunkOverlap }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('分隔符')">
              {{ config.separator }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('检索设置')">
              <a-tag color="blue">
                {{ formatRetrievalMode(config.retrievalMode) }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- 操作按钮 -->
        <div class="actions">
          <a-button size="large" @click="viewAPI">
            <template #icon>
              <api-outlined />
            </template>
            {{ t('Access the API') }}
          </a-button>
          <a-button type="primary" size="large" @click="goToDocuments">
            {{ t('前往文档') }}
          </a-button>
        </div>
      </div>

      <!-- 右侧提示卡片 -->
      <div class="tips-card">
        <div class="tips-icon">
          <book-outlined />
        </div>
        <h3 class="tips-title">
          {{ t('接下来做什么') }}
        </h3>
        <p class="tips-content">
          {{ t('当文档完成索引后，您可以管理和编辑文档、运行检索测试以及修改资产库设置。资产库即可集成到应用程序中作为上下文使用，因此请调整检索设置以确保最佳性能。') }}
        </p>
        <a-button type="link" class="tips-link">
          {{ t('了解更多') }}
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.step-processing {
  max-width: 1000px;
  margin: 0 auto;
}

.processing-layout {
  display: flex;
  gap: 24px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.tips-card {
  width: 280px;
  flex-shrink: 0;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  padding: 24px;
  align-self: flex-start;
}

.success-header {
  text-align: center;
  margin-bottom: 32px;
}

.success-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 12px;
}

.success-title {
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 8px 0;
}

.success-desc {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
}

.name-section {
  margin-bottom: 24px;
}

.name-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 8px;
}

.name-icon {
  font-size: 20px;
  color: #faad14;
}

.progress-section {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 12px;
}

.spin-icon {
  color: #1890ff;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.check-icon {
  color: #52c41a;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-radius: 6px;
}

.file-icon {
  font-size: 18px;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-progress {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  min-width: 40px;
  text-align: right;
}

.config-summary {
  margin-bottom: 24px;
}

.actions {
  display: flex;
  gap: 12px;
}

.tips-icon {
  width: 48px;
  height: 48px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #52c41a;
  margin-bottom: 16px;
}

.tips-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 12px 0;
}

.tips-content {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.tips-link {
  padding: 0;
  height: auto;
}
</style>
