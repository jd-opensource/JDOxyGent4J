<script setup lang="ts">
import {
  FileTextOutlined,
  InfoCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { KnowledgeBase } from '../types'
import { mockKnowledgeBases } from '../mock'
import KnowledgeSidebar from '../detail/components/KnowledgeSidebar.vue'
import { useI18n } from '@/locales'

const route = useRoute()
const { t } = useI18n()

// 资产库ID
const knowledgeId = computed(() => route.params.id as string)

// 资产库数据
const knowledge = ref<KnowledgeBase | null>(null)
const loading = ref(false)
const saving = ref(false)

// 表单数据
const formData = reactive({
  name: '',
  description: '',
  visibility: 'only_me',
  chunkMode: 'general',
  indexMode: 'high_quality',
  keywordCount: 10,
  embeddingModel: 'netease-youdao/bce-embedding-base_v1',
  retrievalMode: 'vector',
  rerankEnabled: true,
  rerankModel: 'netease-youdao/bce-reranker-base_v1',
  topK: 3,
  scoreThresholdEnabled: false,
  scoreThreshold: 0.5,
})

// 可见权限选项
const visibilityOptions = computed(() => ([
  { value: 'only_me', label: t('只有我') },
  { value: 'team', label: t('团队') },
  { value: 'public', label: t('公开') },
]))

// 分段模式选项
const chunkModes = [
  {
    key: 'general',
    icon: SettingOutlined,
    title: 'General',
    description: '通用文本分块模式，检索和召回的块是相同的',
  },
  {
    key: 'parent_child',
    icon: SettingOutlined,
    title: 'Parent-Child',
    description: '使用父子模式时，子块用于检索，父块用作上下文',
    disabled: true,
  },
  {
    key: 'qa',
    icon: SettingOutlined,
    title: 'Q&A',
    description: '使用 Q&A 模式时，块将被拆分为问题和答案对。检索时将使用问题部分进行检索，答案部分将作为上下文返回。',
    disabled: true,
  },
]

// 索引模式选项
const indexModes = [
  {
    key: 'high_quality',
    title: '高质量',
    tag: '推荐',
    description: '调用嵌入模型来处理文档以实现更精确的检索，可以帮助大语言模型生成高质量的回答。',
  },
  {
    key: 'economy',
    title: '经济',
    description: '每个块使用 10 个关键词进行检索，不消耗 tokens，但会降低检索准确性。',
  },
]

// Embedding 模型选项
const embeddingModels = [
  { value: 'netease-youdao/bce-embedding-base_v1', label: 'netease-youdao/bce-embedding-base_v1' },
  { value: 'text-embedding-ada-002', label: 'text-embedding-ada-002' },
  { value: 'text-embedding-3-small', label: 'text-embedding-3-small' },
]

// Rerank 模型选项
const rerankModels = [
  { value: 'netease-youdao/bce-reranker-base_v1', label: 'netease-youdao/bce-reranker-base_v1' },
  { value: 'cohere-rerank-v2', label: 'cohere-rerank-v2' },
]

// 检索模式选项
const retrievalModes = [
  {
    key: 'vector',
    title: '向量检索',
    description: '通过生成查询嵌入并查询与其向量表示最相似的文本分段',
  },
  {
    key: 'fulltext',
    title: '全文检索',
    description: '索引文档中的所有词汇，从而允许用户查询任意词汇，并返回包含这些词汇的文本片段',
  },
  {
    key: 'hybrid',
    title: '混合检索',
    tag: '推荐',
    description: '同时执行全文检索和向量检索，并应用重排序步骤，从两类查询结果中选择匹配用户问题的最佳结果。',
  },
]

// 加载资产库数据
function loadKnowledge() {
  loading.value = true
  setTimeout(() => {
    const found = mockKnowledgeBases.find((kb: KnowledgeBase) => kb.id === knowledgeId.value)
    knowledge.value = found ?? mockKnowledgeBases[0] ?? null

    // 填充表单
    if (knowledge.value) {
      formData.name = knowledge.value.name
      formData.description = knowledge.value.description
    }

    loading.value = false
  }, 300)
}

// 保存设置
function handleSave() {
  saving.value = true
  setTimeout(() => {
    saving.value = false
    message.success(t('设置已保存'))
  }, 500)
}

onMounted(() => {
  loadKnowledge()
})
</script>

<template>
  <div class="-m-4 flex h-[calc(100vh-112px)] bg-white">
    <!-- 左侧边栏 -->
    <knowledge-sidebar :knowledge="knowledge" />

    <!-- 右侧主内容 -->
    <div class="flex-1 overflow-y-auto p-6">
      <a-spin :spinning="loading">
        <!-- 页面标题 -->
        <div class="mb-6">
          <h2 class="m-0 mb-2 text-xl font-semibold text-gray-800">
            {{ t('资产库设置') }}
          </h2>
          <p class="m-0 text-[13px] text-gray-400">
            {{ t('在这里，您可以修改此资产库的属性和检索设置') }}
          </p>
        </div>

        <!-- 基本信息 -->
        <div class="mb-8">
          <!-- 名称和图标 -->
          <div class="mb-6 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">{{ t('名称和图标') }}</label>
            <div class="flex flex-1 items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 text-xl text-white">
                <file-text-outlined />
              </div>
              <a-input v-model:value="formData.name" class="flex-1" />
            </div>
          </div>

          <!-- 描述 -->
          <div class="mb-6 flex gap-6">
            <label class="w-32 flex-shrink-0 pt-2 text-sm text-gray-500">{{ t('描述') }}</label>
            <a-textarea
              v-model:value="formData.description"
              class="flex-1"
              :rows="3"
              :placeholder="t('请输入资产库描述')"
            />
          </div>

          <!-- 可见权限 -->
          <div class="mb-6 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">{{ t('可见权限') }}</label>
            <a-select
              v-model:value="formData.visibility"
              :options="visibilityOptions"
              class="flex-1"
            />
          </div>
        </div>

        <!-- 分段模式 -->
        <div class="mb-8">
          <div class="mb-4 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">
              {{ t('分段模式') }}
              <a href="#" class="ml-1 text-blue-500">{{ t('了解更多') }}</a>{{ t('关于分段模式。') }}
            </label>
          </div>
          <div class="ml-[152px] flex flex-col gap-3">
            <div
              v-for="mode in chunkModes"
              :key="mode.key"
              class="cursor-pointer rounded-lg border-2 p-4 transition-all"
              :class="{
                'border-blue-500 bg-blue-50': formData.chunkMode === mode.key,
                'border-gray-200 hover:border-gray-300': formData.chunkMode !== mode.key && !mode.disabled,
                'cursor-not-allowed border-gray-100 opacity-50': mode.disabled,
              }"
              @click="!mode.disabled && (formData.chunkMode = mode.key)"
            >
              <div class="flex items-center gap-2">
                <component :is="mode.icon" class="text-base text-blue-500" />
                <span class="font-medium text-gray-800">{{ t(mode.title) }}</span>
              </div>
              <p class="m-0 mt-1 text-xs text-gray-400">
                {{ t(mode.description) }}
              </p>
            </div>
          </div>
        </div>

        <!-- 索引模式 -->
        <div class="mb-8">
          <div class="mb-4 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">{{ t('索引模式') }}</label>
          </div>
          <div class="ml-[152px] flex flex-col gap-3">
            <div
              v-for="mode in indexModes"
              :key="mode.key"
              class="cursor-pointer rounded-lg border-2 p-4 transition-all"
              :class="{
                'border-blue-500 bg-blue-50': formData.indexMode === mode.key,
                'border-gray-200 hover:border-gray-300': formData.indexMode !== mode.key,
              }"
              @click="formData.indexMode = mode.key"
            >
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-800">{{ t(mode.title) }}</span>
                <a-tag v-if="mode.tag" color="blue" size="small">
                  {{ t(mode.tag) }}
                </a-tag>
              </div>
              <p class="m-0 mt-1 text-xs text-gray-400">
                {{ t(mode.description) }}
              </p>
            </div>

            <!-- 经济模式关键词数量 -->
            <div v-if="formData.indexMode === 'economy'" class="mt-2 flex items-center gap-4">
              <span class="text-sm text-gray-500">
                {{ t('关键词数量') }}
                <a-tooltip :title="t('每个块提取的关键词数量')">
                  <info-circle-outlined class="ml-1 text-gray-400" />
                </a-tooltip>
              </span>
              <a-slider v-model:value="formData.keywordCount" :min="1" :max="20" class="w-48" />
              <a-input-number v-model:value="formData.keywordCount" :min="1" :max="20" class="w-16" size="small" />
            </div>
          </div>
        </div>

        <!-- Embedding 模型 -->
        <div class="mb-8">
          <div class="mb-4 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">{{ t('Embedding 模型') }}</label>
            <a-select
              v-model:value="formData.embeddingModel"
              :options="embeddingModels"
              class="flex-1"
            />
          </div>
        </div>

        <!-- 检索设置 -->
        <div class="mb-8">
          <div class="mb-4 flex items-center gap-6">
            <label class="w-32 flex-shrink-0 text-sm text-gray-500">
              {{ t('检索设置') }}
              <a href="#" class="ml-1 text-blue-500">{{ t('了解更多') }}</a>{{ t('关于检索方法。') }}
            </label>
          </div>
          <div class="ml-[152px] flex flex-col gap-3">
            <div
              v-for="mode in retrievalModes"
              :key="mode.key"
              class="cursor-pointer rounded-lg border-2 p-4 transition-all"
              :class="{
                'border-blue-500 bg-blue-50': formData.retrievalMode === mode.key,
                'border-gray-200 hover:border-gray-300': formData.retrievalMode !== mode.key,
              }"
              @click="formData.retrievalMode = mode.key"
            >
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-800">{{ t(mode.title) }}</span>
                <a-tag v-if="mode.tag" color="blue" size="small">
                  {{ t(mode.tag) }}
                </a-tag>
              </div>
              <p class="m-0 mt-1 text-xs text-gray-400">
                {{ t(mode.description) }}
              </p>

              <!-- 向量检索的 Rerank 设置 -->
              <div v-if="formData.retrievalMode === mode.key && mode.key === 'vector'" class="mt-4 space-y-4 border-t border-gray-100 pt-4">
                <!-- Rerank 开关 -->
                <div class="flex items-center gap-3">
                  <a-switch v-model:checked="formData.rerankEnabled" size="small" />
                  <span class="text-sm text-gray-600">
                    {{ t('Rerank 模型') }}
                    <a-tooltip :title="t('使用 Rerank 模型对检索结果进行重排序')">
                      <info-circle-outlined class="ml-1 text-gray-400" />
                    </a-tooltip>
                  </span>
                </div>

                <!-- Rerank 模型选择 -->
                <div v-if="formData.rerankEnabled">
                  <a-select
                    v-model:value="formData.rerankModel"
                    :options="rerankModels"
                    class="w-full"
                  />
                </div>

                <!-- Top K 和 Score 阈值 -->
                <div class="flex gap-8">
                  <div class="flex items-center gap-3">
                    <span class="text-sm text-gray-500">
                      {{ t('Top K') }}
                      <a-tooltip :title="t('返回的最相似结果数量')">
                        <info-circle-outlined class="ml-1 text-gray-400" />
                      </a-tooltip>
                    </span>
                    <a-input-number v-model:value="formData.topK" :min="1" :max="20" class="w-20" size="small" />
                    <a-slider v-model:value="formData.topK" :min="1" :max="20" class="w-32" />
                  </div>
                  <div class="flex items-center gap-3">
                    <a-checkbox v-model:checked="formData.scoreThresholdEnabled">
                      {{ t('Score 阈值') }}
                      <a-tooltip :title="t('只返回相似度高于阈值的结果')">
                        <info-circle-outlined class="ml-1 text-gray-400" />
                      </a-tooltip>
                    </a-checkbox>
                    <a-input-number
                      v-model:value="formData.scoreThreshold"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      :disabled="!formData.scoreThresholdEnabled"
                      class="w-20"
                      size="small"
                    />
                    <a-slider
                      v-model:value="formData.scoreThreshold"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      :disabled="!formData.scoreThresholdEnabled"
                      class="w-32"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 保存按钮 -->
          <div class="ml-[152px]">
            <a-button type="primary" size="large" :loading="saving" @click="handleSave">
              {{ t('保存') }}
            </a-button>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>
