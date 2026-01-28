<script setup lang="ts">
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import StepDataSource from './components/StepDataSource.vue'
import StepStructureConfig from './components/StepStructureConfig.vue'
import StepRetrievalConfig from './components/StepRetrievalConfig.vue'
import StepChunkConfig from './components/StepChunkConfig.vue'
import { useKnowledgeCreate } from '@/views/knowledge/create/composables/useKnowledgeCreate'
import { useI18n } from '@/locales'

const {
  // 状态
  loading,
  currentStep,
  steps,
  isImportMode,

  // 数据
  file,
  fileName,
  fileType,
  filePath,
  fileId,
  kbInfo,
  kbId,
  schemaColumns,
  retrievalConfig,
  chunkConfig,
  kbSchemaExists,
  loadingSchema,
  showStepsBar,

  // 方法
  goBack,
  handleDataSourceNext,
  handleStructureNext,
  handleChunkNext,
  handlePrev,
  handleRetrievalPrev,
  handleRetrievalComplete,
} = useKnowledgeCreate()

const { t } = useI18n()
</script>

<template>
  <div class="mx-auto max-w-6xl px-4 py-8">
    <div class="min-h-[600px] overflow-hidden rounded-xl bg-white shadow-sm ring-1 ring-gray-100">
      <!-- 头部区域：返回按钮 + 步骤条 -->
      <div class="flex items-center gap-6 border-b border-gray-100 bg-gray-50/30 px-8 py-5">
        <a-button type="text" class="flex items-center px-0 text-gray-500 hover:text-indigo-600" @click="goBack">
          <template #icon>
            <arrow-left-outlined />
          </template>
          {{ isImportMode ? t('返回资产库') : t('返回列表') }}
        </a-button>

        <div v-if="showStepsBar" class="flex-1 flex justify-center">
          <div class="flex items-center gap-4">
            <!-- 动态渲染步骤条 -->
            <template v-for="(step, index) in steps" :key="index">
              <!-- Step Item -->
              <div class="flex items-center gap-2" :class="currentStep >= index ? 'text-indigo-600' : 'text-gray-400'">
                <div v-if="currentStep === index" class="rounded-full bg-indigo-600 px-3 py-0.5 text-xs font-bold text-white shadow-sm shadow-indigo-200">
                  STEP {{ index + 1 }}
                </div>
                <div
                  v-else class="flex h-6 w-6 items-center justify-center rounded-full border text-xs font-medium"
                  :class="currentStep > index ? 'border-indigo-600 bg-indigo-600 text-white' : 'border-gray-300 bg-white'"
                >
                  <span v-if="currentStep > index">✓</span>
                  <span v-else>{{ index + 1 }}</span>
                </div>
                <span class="font-medium">{{ step.title }}</span>
              </div>

              <!-- Divider (not after last step) -->
              <div v-if="index < steps.length - 1" class="h-px w-8 bg-gray-200" />
            </template>
          </div>
        </div>

        <!-- 占位，保持步骤条居中 -->
        <div class="w-20" />
      </div>

      <!-- 内容区域 -->
      <div class="p-8">
        <a-spin :spinning="loading || loadingSchema">
          <div class="mx-auto max-w-5xl">
            <!-- Step 0: 选择数据源 -->
            <step-data-source
              v-if="currentStep === 0"
              :kb-id="kbId"
              :kb-type="(kbInfo.kb_type as '' | 'structured' | 'unstructured')"
              :schema-exists="kbSchemaExists"
              @next="handleDataSourceNext"
            />

            <!-- Schema 不存在：完整配置流程 -->
            <template v-if="!kbSchemaExists">
              <!-- Unstructured Flow -->
              <template v-if="kbInfo.kb_type === 'unstructured'">
                <!-- Step 1: Chunk Config -->
                <step-chunk-config
                  v-if="currentStep === 1"
                  :model-value="chunkConfig"
                  :files="file ? [file] : []"
                  :kb-name="kbInfo.kb_name"
                  :schema-exists="kbSchemaExists"
                  :schema-checking="loadingSchema"
                  @prev="handlePrev"
                  @next="handleChunkNext"
                />
              </template>

              <!-- Structured Flow -->
              <template v-else>
                <!-- Step 1: 结构化配置 -->
                <step-structure-config
                  v-if="currentStep === 1"
                  :file="file"
                  :file-name="fileName"
                  :file-type="fileType"
                  :file-path="filePath"
                  :file-id="fileId"
                  :kb-info="kbInfo"
                  :kb-id="kbId"
                  :initial-columns="schemaColumns"
                  @prev="handlePrev"
                  @next="handleStructureNext"
                />

                <!-- Step 2: 检索策略 -->
                <step-retrieval-config
                  v-else-if="currentStep === 2"
                  :columns="schemaColumns"
                  :initial-config="retrievalConfig"
                  @prev="handleRetrievalPrev"
                  @complete="handleRetrievalComplete"
                />
              </template>
            </template>
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
