<script setup lang="ts">
import {
  EditOutlined,
} from '@ant-design/icons-vue'
import { ref } from 'vue'
import MetadataModal from './MetadataModal.vue'
import { useI18n } from '@/locales'

// 元数据弹窗
const metadataModalOpen = ref(false)
const { t } = useI18n()

// 开始标注 - 打开元数据管理弹窗
function handleStartAnnotation() {
  metadataModalOpen.value = true
}
</script>

<template>
  <div class="w-72 flex-shrink-0 overflow-y-auto border-l border-gray-100 bg-white">
    <!-- 元数据 -->
    <div class="border-b border-gray-100 p-4">
      <h4 class="m-0 mb-2 text-sm font-semibold text-gray-800">
        {{ t('元数据') }}
      </h4>
      <p class="m-0 mb-3 text-xs leading-relaxed text-gray-400">
        {{ t('元数据是关于文档的数据，用于描述文档的属性。元数据可以帮助您更好地组织和管理文档。') }}
      </p>
      <a-button type="primary" block @click="handleStartAnnotation">
        <template #icon>
          <edit-outlined />
        </template>
        {{ t('开始标注') }}
      </a-button>
    </div>

    <!-- 文档信息占位 -->
    <div class="flex flex-1 items-center justify-center p-4">
      <p class="text-xs text-gray-400">
        {{ t('选择一个分段查看详情') }}
      </p>
    </div>

    <!-- 元数据管理弹窗 -->
    <metadata-modal v-model:open="metadataModalOpen" />
  </div>
</template>
