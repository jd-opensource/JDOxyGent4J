<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  CheckOutlined,
  DeleteOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { ColumnSchema, RetrievalConfig, RetrievalRule } from '../../types'
import { defaultRetrievalConfig } from '../../types'
import { useI18n } from '@/locales'

interface Props {
  columns: ColumnSchema[]
  /** 已缓存的检索配置，用于步骤切换时保持数据 */
  initialConfig?: RetrievalConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'prev', config: RetrievalConfig): void
  (e: 'complete', config: RetrievalConfig): void
}>()

const { t } = useI18n()

// Helper to generate IDs
function generateId() {
  return Math.random().toString(36).substring(2, 9)
}

// Initialize config
const config = ref<RetrievalConfig>([])

function initConfig() {
  if (props.initialConfig && props.initialConfig.length > 0) {
    // Deep copy to avoid mutating prop
    config.value = JSON.parse(JSON.stringify(props.initialConfig))
  }
  else {
    // Clone default and assign new IDs to be safe
    const defaults = JSON.parse(JSON.stringify(defaultRetrievalConfig)) as RetrievalConfig
    defaults.forEach((rule) => {
      rule.id = generateId()
    })
    config.value = defaults
  }
}

initConfig()

// Options
const mainStrategyOptions = computed(() => ([
  { label: t('全文检索 (ES)'), value: 'es' },
  { label: t('向量检索 (Vearch)'), value: 'vearch' },
]))

// All available fields from schema
const allFieldOptions = computed(() => {
  return props.columns.map(col => ({ label: col.name, value: col.name, type: col.type }))
})

// --- Rule Management ---

function addRule() {
  config.value.push({
    id: generateId(),
    mainStrategy: {
      type: 'es',
      input_fields: [],
      output_fields: [],
    },
    preciseStrategies: [
      {
        type: 'precise',
        input_fields: [],
        output_fields: [],
      },
    ],
    output_fields: [],
  })
}

function removeRule(index: number) {
  config.value.splice(index, 1)
}

// --- Strategy Management within Rule ---

function addPreciseStrategy(ruleIndex: number) {
  const rule = config.value[ruleIndex]
  if (!rule)
    return

  rule.preciseStrategies.push({
    type: 'precise',
    input_fields: [],
    output_fields: [],
  })
}

function removePreciseStrategy(ruleIndex: number, strategyIndex: number) {
  const rule = config.value[ruleIndex]
  if (!rule)
    return
  rule.preciseStrategies.splice(strategyIndex, 1)
}

// --- Helpers for Inputs ---

/**
 * Get available options for Precise Strategy Input
 * Filters out fields already selected in OTHER precise strategies in the same rule
 */
function getPreciseInputOptions(ruleIndex: number, currentStrategyIndex: number) {
  const rule = config.value[ruleIndex]
  if (!rule)
    return allFieldOptions.value

  // Collect all selected values in precise strategies (except current one)
  const selectedInOthers = new Set<string>()
  rule.preciseStrategies.forEach((s, idx) => {
    if (idx !== currentStrategyIndex && s.input_fields.length > 0) {
      const v = s.input_fields[0]
      if (v)
        selectedInOthers.add(v)
    }
  })

  // Filter options
  return allFieldOptions.value.filter(opt => !selectedInOthers.has(opt.value))
}

/**
 * Get options for Main Strategy
 * If Vearch, only allow string fields (if that requirement persists, else all)
 * Assuming Vearch usually needs text, but let's stick to "string" type filter if logic dictates.
 * The previous code filtered 'string' for Vearch.
 */
function getMainInputOptions(rule: RetrievalRule) {
  if (rule.mainStrategy.type === 'vearch') {
    return allFieldOptions.value.filter(opt => opt.type === 'string')
  }
  return allFieldOptions.value
}

// Handle Main Strategy Type Change
function handleMainTypeChange(rule: RetrievalRule) {
  // If changing to Vearch, ensure inputs are valid (string only)
  if (rule.mainStrategy.type === 'vearch') {
    const stringFields = allFieldOptions.value.filter(o => o.type === 'string').map(o => o.value)
    // Filter input_fields
    rule.mainStrategy.input_fields = rule.mainStrategy.input_fields.filter(f => stringFields.includes(f))

    if (!rule.mainStrategy.embedding_model) {
      rule.mainStrategy.embedding_model = 'text-embedding-v3-small'
    }
  }
}

// --- Navigation ---

function handlePrev() {
  emit('prev', config.value)
}

function validate() {
  for (let i = 0; i < config.value.length; i++) {
    const rule = config.value[i]
    if (!rule)
      continue
    if (rule.output_fields.length === 0) {
      message.error(t('规则 #{index} 未选择输出字段', { index: i + 1 }))
      return false
    }
  }
  return true
}

function handleComplete() {
  if (!validate())
    return
  // 精确匹配策略可选：提交前过滤掉未选择输入字段的策略，避免后端收到空策略
  const normalized = config.value.map(rule => ({
    ...rule,
    preciseStrategies: (rule.preciseStrategies || []).filter(s => (s.input_fields || []).length > 0),
  }))
  emit('complete', normalized)
}
</script>

<template>
  <div class="mx-auto max-w-[1000px] pb-12">
    <div class="mb-6">
      <h3 class="text-lg font-medium text-gray-800">
        {{ t('检索策略配置') }}
      </h3>
      <a-alert
        :message="t('配置检索规则，组合全文检索/向量检索与精确匹配。全文/向量检索可不指定输入字段。')"
        type="info"
        show-icon
        class="mt-2"
      />
    </div>

    <!-- Rules List -->
    <div class="space-y-6">
      <div
        v-for="(rule, rIndex) in config" :key="rule.id"
        class="rounded-lg border border-gray-200 bg-white shadow-sm transition-all hover:shadow-md"
      >
        <!-- Rule Header -->
        <div class="flex items-center justify-between border-b border-gray-100 bg-gray-50 px-4 py-3">
          <div class="font-medium text-gray-700">
            {{ t('检索规则 #{index}', { index: rIndex + 1 }) }}
          </div>
          <a-button
            v-if="config.length > 1"
            type="text"
            danger
            size="small"
            @click="removeRule(rIndex)"
          >
            <template #icon>
              <delete-outlined />
            </template>
            {{ t('删除规则') }}
          </a-button>
        </div>

        <div class="p-4">
          <!-- Strategies Table-like Grid -->
          <div class="mb-4 rounded border border-gray-100">
            <!-- Header Row -->
            <div class="grid grid-cols-12 gap-4 bg-gray-50 px-4 py-2 text-xs font-medium text-gray-500">
              <div class="col-span-3">
                {{ t('检索策略') }}
              </div>
              <div class="col-span-8">
                {{ t('输入字段') }}
              </div>
              <div class="col-span-1 text-center">
                {{ t('操作') }}
              </div>
            </div>

            <!-- Main Strategy Row -->
            <div class="grid grid-cols-12 items-start gap-4 border-t border-gray-100 px-4 py-3">
              <div class="col-span-3 pt-1">
                <a-select
                  v-model:value="rule.mainStrategy.type"
                  :options="mainStrategyOptions"
                  style="width: 100%"
                  @change="handleMainTypeChange(rule)"
                />
                <!-- Embedding Model for Vearch (temporarily hidden)
                <div v-if="rule.mainStrategy.type === 'vearch'" class="mt-2 rounded bg-blue-50 p-2">
                  <div class="mb-1 text-[10px] font-medium text-blue-600">
                    {{ t('Embedding Model') }}
                  </div>
                  <a-input
                    v-model:value="rule.mainStrategy.embedding_model"
                    :placeholder="t('text-embedding-v3-small')"
                    size="small"
                    class="text-xs"
                  />
                </div>
                -->
              </div>
              <div class="col-span-8 pt-1">
                <!-- Single Select wrapped in array logic -->
                <a-select
                  :value="rule.mainStrategy.input_fields[0]"
                  :options="getMainInputOptions(rule)"
                  :placeholder="rule.mainStrategy.type === 'precise' ? t('选择字段 (可选)') : t('选择字段 (必选)')"
                  style="width: 100%"
                  allow-clear
                  @update:value="(val) => rule.mainStrategy.input_fields = typeof val === 'string' && val ? [val] : []"
                />
              </div>
              <div class="col-span-1 text-center text-gray-400 pt-2">
                -
              </div>
            </div>

            <!-- Precise Strategies Rows -->
            <div
              v-for="(precise, pIndex) in rule.preciseStrategies"
              :key="pIndex"
              class="grid grid-cols-12 items-start gap-4 border-t border-gray-100 px-4 py-3"
            >
              <div class="col-span-3 pt-1">
                <a-select
                  value="precise"
                  :options="[{ label: t('精确匹配'), value: 'precise' }]"
                  disabled
                  style="width: 100%"
                />
              </div>
              <div class="col-span-8 pt-1">
                <a-select
                  :value="precise.input_fields[0]"
                  :options="getPreciseInputOptions(rIndex, pIndex)"
                  :placeholder="t('选择字段 (可选)')"
                  style="width: 100%"
                  allow-clear
                  @update:value="(val) => precise.input_fields = typeof val === 'string' && val ? [val] : []"
                />
              </div>
              <div class="col-span-1 text-center pt-1">
                <a-button type="text" danger size="small" @click="removePreciseStrategy(rIndex, pIndex)">
                  <template #icon>
                    <delete-outlined />
                  </template>
                </a-button>
              </div>
            </div>
          </div>

          <!-- Add Strategy Button -->
          <div class="mb-6">
            <a-button
              type="dashed"
              block
              class="border-indigo-600 text-indigo-600 hover:border-indigo-500 hover:text-indigo-500"
              @click="addPreciseStrategy(rIndex)"
            >
              <plus-outlined /> {{ t('添加策略') }}
            </a-button>
          </div>

          <!-- Output Fields -->
          <div class="rounded bg-indigo-50/50 p-3">
            <div class="mb-1 flex items-center gap-1 text-sm font-medium text-gray-700">
              {{ t('输出字段') }}
              <span class="text-red-500">*</span>
              <a-tooltip :title="t('该规则命中后返回的字段内容')">
                <question-circle-outlined class="text-gray-400" />
              </a-tooltip>
            </div>
            <a-select
              v-model:value="rule.output_fields"
              mode="multiple"
              :options="allFieldOptions"
              :placeholder="t('选择输出字段')"
              style="width: 100%"
              allow-clear
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Add Rule Button -->
    <div class="mt-6 text-center">
      <a-button
        type="dashed"
        class="border-indigo-600 text-indigo-600 hover:border-indigo-500 hover:text-indigo-500 w-full"
        @click="addRule"
      >
        <plus-outlined /> {{ t('添加规则') }}
      </a-button>
    </div>

    <!-- Footer Actions -->
    <div class="mt-10 flex justify-between border-t border-gray-100 pt-6">
      <a-button @click="handlePrev">
        {{ t('上一步') }}
      </a-button>
      <a-button type="primary" @click="handleComplete">
        <template #icon>
          <check-outlined />
        </template>
        {{ t('完成并导入') }}
      </a-button>
    </div>
  </div>
</template>
