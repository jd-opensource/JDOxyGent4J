<script setup lang="ts">
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import { computed, ref, watch } from 'vue'
import type { ChunkConfig, ParserType } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  modelValue: ChunkConfig
  files: File[]
  kbName?: string
  schemaExists?: boolean
  schemaChecking?: boolean
}

interface ChunkNextPayload {
  config: ChunkConfig
  schemaExists?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: ChunkConfig): void
  (e: 'prev'): void
  (e: 'next', payload: ChunkNextPayload): void
}>()

const { t } = useI18n()

// 本地配置
const config = ref<ChunkConfig>({ ...props.modelValue })

// 监听 props 变化
watch(
  () => props.modelValue,
  (val) => {
    config.value = { ...val }
  },
  { deep: true },
)

// 更新配置
function updateConfig() {
  emit('update:modelValue', config.value)
}

// 分段策略选项
const parserTypeOptions = [
  {
    key: 'smart',
    title: '智能',
    description: '自动选择更合适的切分方式',
  },
  {
    key: 'token',
    title: '字符',
    description: '按 token 数量进行切分',
  },
  {
    key: 'sentence',
    title: '句子',
    description: '按句子边界进行切分',
  },
  {
    key: 'markdown',
    title: 'Markdown',
    description: '按 Markdown 结构进行切分',
  },
  {
    key: 'html',
    title: 'HTML',
    description: '按 HTML 标签结构进行切分',
  },
  {
    key: 'json',
    title: 'JSON',
    description: '按 JSON 层级结构进行切分',
  },
]

// 检索模式选项
const retrievalModes = [
  {
    key: 'vector',
    title: '向量检索',
    description: '通过生成查询嵌入并查询与其向量表示最相似的文本分段',
    tag: '默认',
  },
  {
    key: 'fulltext',
    title: '全文检索',
    description: '索引文档中的所有词汇，从而允许用户查询任意词汇，并返回包含这些词汇的文本片段',
    tag: '默认',
  },
  // {
  //   key: 'hybrid',
  //   title: '混合检索',
  //   tag: '推荐',
  //   description: '同时执行全文检索和向量检索，并应用重排序步骤，从两类查询结果中选择匹配用户问题的最佳结果。',
  // },
]

// 当前文件预览
// const currentFileIndex = ref(0)
// const currentFile = computed(() => props.files[currentFileIndex.value])

// 主按钮文案
const primaryButtonText = computed(() => (props.schemaExists ? t('确认导入') : t('保存并处理')))

// 选择分段策略
function selectParserType(type: ParserType) {
  config.value.parserType = type
  updateConfig()
}

// 上一步
function handlePrev() {
  emit('prev')
}

// 下一步（保存并处理）
function handleNext() {
  updateConfig()
  emit('next', {
    config: config.value,
    schemaExists: props.schemaExists,
  })
}
</script>

<template>
  <div class="mx-auto max-w-[1200px]">
    <div class="flex gap-6">
      <!-- 左侧配置区 -->
      <div class="flex-1 min-w-0">
        <a-alert
          v-if="kbName"
          :type="props.schemaExists ? 'success' : 'info'"
          show-icon
          class="mb-4"
          :message="props.schemaExists ? t('检测到资产库已存在 Schema，本次仅需确认导入。') : t('资产库暂无 Schema，分段配置保存后将立即开始处理。')"
        />

        <!-- 分段设置 -->
        <div
          class="bg-white rounded-lg border border-gray-100 p-5 mb-4"
          :class="props.schemaExists ? 'opacity-60 pointer-events-none' : ''"
        >
          <h3 class="text-base font-semibold text-gray-800 mb-4 flex items-center">
            {{ t('分段设置') }}
          </h3>

          <!-- 分段策略选择 -->
          <div class="mb-4">
            <div class="mb-2 text-sm font-medium text-gray-700">
              {{ t('切分策略') }}
            </div>
            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div
                v-for="mode in parserTypeOptions"
                :key="mode.key"
                class="cursor-pointer rounded-lg border-2 p-4 transition-all"
                :class="{
                  'border-blue-500 bg-blue-50': config.parserType === mode.key,
                  'border-gray-200 hover:border-gray-300': config.parserType !== mode.key,
                }"
                @click="selectParserType(mode.key as ParserType)"
              >
                <div class="font-medium text-gray-800">
                  {{ t(mode.title) }}
                </div>
                <div class="text-xs text-gray-400">
                  {{ t(mode.description) }}
                </div>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label class="flex items-center text-[13px] text-gray-500 mb-2">
                {{ t('Chunk 大小') }}
                <a-tooltip :title="t('单个分段的 token 数量')">
                  <info-circle-outlined class="ml-1 text-gray-400" />
                </a-tooltip>
              </label>
              <a-input-number
                v-model:value="config.chunkSize"
                :min="100"
                :max="4000"
                style="width: 100%"
                :addon-after="t('tokens')"
                @change="updateConfig"
              />
            </div>
            <div>
              <label class="flex items-center text-[13px] text-gray-500 mb-2">
                {{ t('重叠长度') }}
                <a-tooltip :title="t('相邻分段之间的重叠 token 数量')">
                  <info-circle-outlined class="ml-1 text-gray-400" />
                </a-tooltip>
              </label>
              <a-input-number
                v-model:value="config.chunkOverlap"
                :min="0"
                :max="1000"
                style="width: 100%"
                :addon-after="t('tokens')"
                @change="updateConfig"
              />
            </div>
            <div>
              <label class="flex items-center text-[13px] text-gray-500 mb-2">
                {{ t('分隔符') }}
                <a-tooltip :title="t('用于切分文本的分隔符')">
                  <info-circle-outlined class="ml-1 text-gray-400" />
                </a-tooltip>
              </label>
              <a-input
                v-model:value="config.separator"
                :placeholder="t('空格或\\n\\n')"
                @change="updateConfig"
              />
            </div>
          </div>

          <!-- 文本预处理规则 TODO: 后续支持 -->
          <!-- <div class="flex flex-col gap-2">
            <label class="flex items-center text-[13px] text-gray-500 mb-2">文本预处理规则</label>
            <a-checkbox
              v-model:checked="config.cleanRules.replaceSpaces"
              @change="updateConfig"
            >
              替换掉连续的空格、换行符和制表符
            </a-checkbox>
            <a-checkbox
              v-model:checked="config.cleanRules.removeUrls"
              class="ml-0 mt-2"
              @change="updateConfig"
            >
              删除所有 URL 和电子邮件地址
            </a-checkbox>
          </div> -->
        </div>

        <!-- 检索设置 -->
        <div
          class="bg-white rounded-lg border border-gray-100 p-5 mb-4"
          :class="props.schemaExists ? 'opacity-60 pointer-events-none' : ''"
        >
          <h3 class="text-base font-semibold text-gray-800 mb-4 flex items-center">
            {{ t('检索设置') }}
          </h3>
          <div class="flex flex-col gap-3 pointer-events-none opacity-70">
            <div
              v-for="mode in retrievalModes"
              :key="mode.key"
              class="rounded-lg border-2 p-4 transition-all"
              :class="{
                'border-blue-500 bg-blue-50': config.retrievalMode === mode.key,
                'border-gray-200 hover:border-gray-300': config.retrievalMode !== mode.key,
              }"
            >
              <div class="mb-1 flex items-center gap-2">
                <span class="font-medium text-gray-800">{{ t(mode.title) }}</span>
                <a-tag v-if="mode.tag" color="blue" size="small">
                  {{ t(mode.tag) }}
                </a-tag>
              </div>
              <div class="text-xs text-gray-400">
                {{ t(mode.description) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="mt-6 flex justify-between border-t border-gray-100 pt-6">
          <a-button @click="handlePrev">
            {{ t('上一步') }}
          </a-button>
          <a-button type="primary" :loading="schemaChecking" @click="handleNext">
            {{ primaryButtonText }}
          </a-button>
        </div>
      </div>

      <!-- 右侧预览区 TODO: 后续支持 -->
      <!-- <div class="w-80 flex-shrink-0 overflow-hidden rounded-lg border border-gray-200 bg-gray-50">
        <div class="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3">
          <span class="font-medium text-gray-800">预览</span>
          <a-select
            v-if="files.length > 1"
            v-model:value="currentFileIndex"
            size="small"
            style="width: 150px"
          >
            <a-select-option v-for="(file, index) in files" :key="index" :value="index">
              {{ file.name }}
            </a-select-option>
          </a-select>
        </div>
        <div class="p-4">
          <div v-if="currentFile" class="mb-4 flex items-center gap-2 rounded-md bg-white p-3">
            <file-outlined class="text-xl text-green-500" />
            <span class="flex-1 truncate text-sm text-gray-800">{{ currentFile.name }}</span>
            <span class="text-xs text-gray-400">0 预览块</span>
          </div>
          <div class="py-12 text-center text-sm text-gray-400">
            <p>点击左侧的"预览块"按钮来加载预览</p>
          </div>
        </div>
      </div> -->
    </div>
  </div>
</template>
