<script setup lang="ts">
import { SaveOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { ref, watch } from 'vue'
import type { Chunk } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  open: boolean
  chunk: Chunk | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'save', chunk: Chunk): void
}>()

const { t } = useI18n()

// 编辑的内容
const editContent = ref('')

// 监听 chunk 变化
watch(
  () => props.chunk,
  (newChunk) => {
    if (newChunk) {
      editContent.value = newChunk.content
    }
  },
  { immediate: true },
)

// 关闭抽屉
function handleClose() {
  emit('update:open', false)
}

// 保存
function handleSave() {
  message.info(t('暂不支持保存，敬请期待后续版本'))
}
</script>

<template>
  <a-drawer
    :open="open"
    :title="t('编辑分段')"
    :width="600"
    placement="right"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <template #extra>
      <a-button type="primary" @click="handleSave">
        <template #icon>
          <save-outlined />
        </template>
        {{ t('保存') }}
      </a-button>
    </template>

    <div v-if="chunk" class="flex h-full flex-col">
      <!-- 分段信息 -->
      <div class="mb-4 flex items-center gap-4 rounded-lg bg-gray-50 p-3">
        <span class="text-sm font-medium text-gray-800">
          {{ t('分段-{index}', { index: String(chunk.index).padStart(2, '0') }) }}
        </span>
        <!-- TODO: 后续支持：字符数/召回次数/启用状态等信息 -->
      </div>

      <!-- 编辑区域 -->
      <div class="flex flex-1 flex-col">
        <div class="mb-2 flex items-center justify-between">
          <span class="text-sm font-medium text-gray-700">{{ t('分段内容') }}</span>
          <!-- TODO: 后续支持：字符数统计 -->
        </div>
        <a-textarea
          v-model:value="editContent"
          class="flex-1"
          :auto-size="{ minRows: 15, maxRows: 30 }"
          :placeholder="t('请输入分段内容')"
        />
      </div>

      <!-- 提示信息 -->
      <div class="mt-4 rounded-lg border border-blue-100 bg-blue-50 p-3">
        <p class="m-0 text-xs text-blue-600">
          {{ t('提示：编辑分段内容后，系统将自动重新计算嵌入向量，这可能需要一些时间。') }}
        </p>
      </div>
    </div>
  </a-drawer>
</template>
