<script setup lang="ts">
import {
  ClockCircleOutlined,
  DeleteOutlined,
  FileTextOutlined,
  GlobalOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import { useI18n } from '@/locales'

interface Props {
  open: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const { t } = useI18n()

// 内置元数据字段
interface BuiltInField {
  key: string
  label: string
  type: string
  icon: any
  enabled: boolean
}

const builtInFields = ref<BuiltInField[]>([
  { key: 'document_name', label: 'document_name', type: 'string', icon: FileTextOutlined, enabled: false },
  { key: 'uploader', label: 'uploader', type: 'string', icon: UserOutlined, enabled: false },
  { key: 'upload_date', label: 'upload_date', type: 'time', icon: ClockCircleOutlined, enabled: false },
  { key: 'last_update_date', label: 'last_update_date', type: 'time', icon: ClockCircleOutlined, enabled: false },
  { key: 'source', label: 'source', type: 'string', icon: GlobalOutlined, enabled: false },
])

// 自定义元数据字段
interface CustomField {
  id: string
  name: string
  type: 'string' | 'number' | 'boolean' | 'date'
  value: string
}

const customFields = ref<CustomField[]>([])

// 添加元数据弹窗
const addModalVisible = ref(false)
const newField = ref({
  name: '',
  type: 'string' as CustomField['type'],
})

// 关闭
function handleClose() {
  emit('update:open', false)
}

// 切换内置字段启用状态
function toggleBuiltInField(field: BuiltInField) {
  field.enabled = !field.enabled
  message.success(t('已{status} {label}', { status: field.enabled ? t('启用') : t('禁用'), label: field.label }))
}

// 打开添加元数据弹窗
function handleOpenAddModal() {
  newField.value = {
    name: '',
    type: 'string',
  }
  addModalVisible.value = true
}

// 添加自定义元数据
function handleAddField() {
  if (!newField.value.name.trim()) {
    message.warning(t('请输入字段名称'))
    return
  }

  // 检查是否重复
  const exists = customFields.value.some(f => f.name === newField.value.name)
  if (exists) {
    message.warning(t('字段名称已存在'))
    return
  }

  customFields.value.push({
    id: `custom-${Date.now()}`,
    name: newField.value.name,
    type: newField.value.type,
    value: '',
  })

  message.success(t('添加成功'))
  addModalVisible.value = false
}

// 删除自定义元数据
function handleDeleteField(field: CustomField) {
  const index = customFields.value.findIndex(f => f.id === field.id)
  if (index > -1) {
    customFields.value.splice(index, 1)
    message.success(t('删除成功'))
  }
}

// 获取类型标签
function getTypeLabel(type: string) {
  const map: Record<string, string> = {
    string: 'string',
    number: 'number',
    boolean: 'boolean',
    date: 'date',
    time: 'time',
  }
  return map[type] || type
}
</script>

<template>
  <a-modal
    :open="open"
    :title="t('元数据')"
    :width="560"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="mb-4">
      <p class="m-0 text-sm text-gray-500">
        {{ t('元数据是关于文档的数据，用于描述文档的属性。元数据可以帮助您更好地组织和管理文档。') }}
      </p>
    </div>

    <!-- 添加元数据按钮 -->
    <a-button type="primary" class="mb-4" @click="handleOpenAddModal">
      <template #icon>
        <plus-outlined />
      </template>
      {{ t('添加元数据') }}
    </a-button>

    <!-- 自定义元数据列表 -->
    <div v-if="customFields.length > 0" class="mb-4">
      <div
        v-for="field in customFields"
        :key="field.id"
        class="flex items-center justify-between gap-3 rounded-lg border border-gray-200 bg-white p-3 mb-2"
      >
        <div class="flex items-center gap-3">
          <file-text-outlined class="text-gray-400" />
          <span class="font-medium text-gray-800">{{ field.name }}</span>
          <span class="text-xs text-gray-400">{{ getTypeLabel(field.type) }}</span>
        </div>
        <a-button type="text" danger size="small" @click="handleDeleteField(field)">
          <template #icon>
            <delete-outlined />
          </template>
        </a-button>
      </div>
    </div>

    <!-- 内置元数据 -->
    <div class="mb-2 flex items-center gap-2">
      <a-checkbox disabled />
      <span class="text-sm text-gray-600">{{ t('内置') }}</span>
      <a-tooltip :title="t('系统内置的元数据字段，可以选择是否启用')">
        <question-circle-outlined class="text-gray-400" />
      </a-tooltip>
    </div>

    <div class="space-y-2">
      <div
        v-for="field in builtInFields"
        :key="field.key"
        class="flex cursor-pointer items-center justify-between gap-3 rounded-lg border border-gray-200 bg-gray-50 p-3 transition-all hover:border-gray-300"
        @click="toggleBuiltInField(field)"
      >
        <div class="flex items-center gap-3">
          <component :is="field.icon" class="text-gray-400" />
          <span class="font-medium text-gray-600">{{ field.label }}</span>
          <span class="text-xs text-gray-400">{{ getTypeLabel(field.type) }}</span>
        </div>
        <span class="text-xs text-gray-400">{{ field.enabled ? t('已启用') : t('已禁用') }}</span>
      </div>
    </div>

    <!-- 添加元数据弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      :title="t('添加元数据')"
      :width="400"
      @ok="handleAddField"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('字段名称')" required>
          <a-input v-model:value="newField.name" :placeholder="t('请输入字段名称')" />
        </a-form-item>
        <a-form-item :label="t('字段类型')">
          <a-select v-model:value="newField.type" class="w-full">
            <a-select-option value="string">
              {{ t('字符串 (string)') }}
            </a-select-option>
            <a-select-option value="number">
              {{ t('数字 (number)') }}
            </a-select-option>
            <a-select-option value="boolean">
              {{ t('布尔值 (boolean)') }}
            </a-select-option>
            <a-select-option value="date">
              {{ t('日期 (date)') }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-modal>
</template>
