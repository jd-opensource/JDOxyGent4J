<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { ColumnSchema, KnowledgeBaseInfo } from '../../types'
import { useI18n } from '@/locales'
import Apis from '@/api'

interface Props {
  file: File | null
  fileName: string
  fileType: string
  filePath: string
  fileId: string
  kbInfo: KnowledgeBaseInfo
  kbId: string
  /** 已缓存的列配置，用于步骤切换时保持数据 */
  initialColumns?: ColumnSchema[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'next', columns: ColumnSchema[]): void
}>()

const { t } = useI18n()

const loading = ref(false)
const columns = ref<ColumnSchema[]>([])

const typeOptions = [
  { labelKey: 'String', value: 'string' },
  { labelKey: 'Integer', value: 'integer' },
  { labelKey: 'Float', value: 'float' },
]

const tableColumns = computed(() => ([
  { title: t('原始列名'), dataIndex: 'originalName', key: 'originalName', width: 200 },
  { title: t('列名 (Editable)'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('数据类型'), dataIndex: 'type', key: 'type', width: 150 },
  { title: t('描述'), dataIndex: 'description', key: 'description' },
]))

onMounted(async () => {
  // 如果有缓存的列配置，直接使用（用于步骤切换时保持数据）
  if (props.initialColumns && props.initialColumns.length > 0) {
    columns.value = JSON.parse(JSON.stringify(props.initialColumns))
    return
  }

  // 否则从后端获取
  if (props.fileId && props.kbId) {
    await fetchFileColumns()
  }
  else {
    message.warning(t('文件信息丢失，请返回重新选择'))
  }
})

/**
 * 从后端接口获取文件列信息
 */
async function fetchFileColumns() {
  loading.value = true
  try {
    if (!props.fileId || !props.kbId) {
      throw new Error(t('文件ID或资产库ID不存在'))
    }
    // 调用后端接口获取文件列信息
    const response = await Apis.knowledgeBaseFileManagement.get_uploaded_file_info_api_v1_kb_base__kb_id__upload_file__file_id__get({
      pathParams: {
        kb_id: props.kbId,
        file_id: props.fileId,
      } as any,
      params: {
        file_type: props.fileType,
        file_path: props.filePath,
      },
    })

    // 转换后端返回的字段信息为前端格式
    if (response.data && Array.isArray(response.data)) {
      columns.value = response.data.map(field => ({
        name: field.field_name,
        originalName: field.field_name,
        type: field.field_type,
        description: field.field_desc || '',
      }))
    }
    else {
      throw new Error('接口返回数据格式错误')
    }
  }
  catch (error) {
    console.error(error)
    message.error(t('获取文件信息失败: {message}', { message: (error as any).message || String(error) }))
  }
  finally {
    loading.value = false
  }
}

function handlePrev() {
  emit('prev')
}

function handleNext() {
  emit('next', columns.value)
}
</script>

<template>
  <div class="step-structure-config">
    <div class="header">
      <h3>{{ t('结构化数据配置') }}</h3>
      <p v-if="fileName" class="file-info">
        {{ t('当前文件: {name}', { name: fileName }) }}
      </p>
    </div>

    <a-spin :spinning="loading">
      <a-table
        :data-source="columns"
        :columns="tableColumns"
        :pagination="false"
        row-key="originalName"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-input v-model:value="record.name" />
          </template>
          <template v-if="column.key === 'type'">
            <a-select v-model:value="record.type" style="width: 100%">
              <a-select-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
                {{ t(opt.labelKey) }}
              </a-select-option>
            </a-select>
          </template>
          <template v-if="column.key === 'description'">
            <a-input v-model:value="record.description" :placeholder="t('请输入描述')" />
          </template>
        </template>
      </a-table>
    </a-spin>

    <div class="actions">
      <a-button @click="handlePrev">
        {{ t('上一步') }}
      </a-button>
      <a-button type="primary" @click="handleNext">
        {{ t('下一步') }}
      </a-button>
    </div>
  </div>
</template>

<style scoped>
.step-structure-config {
  max-width: 1000px;
  margin: 0 auto;
}
.header {
  margin-bottom: 24px;
}
.file-info {
  color: #666;
  font-size: 14px;
}
.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  border-top: 1px solid #f0f0f0;
  padding-top: 24px;
}
</style>
