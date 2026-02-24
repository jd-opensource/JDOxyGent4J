<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useI18n } from '@/locales'
import Apis from '@/api'

const emit = defineEmits<{
  (e: 'success', id: string): void
  (e: 'cancel'): void
}>()

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const { t } = useI18n()

const formState = reactive({
  kb_name: '',
  kb_type: 'structured',
  kb_path: '',
  kb_description: '',
})

const rules: Record<string, any[]> = {
  kb_name: [{ required: true, message: t('请输入资产库名称'), trigger: 'blur' }],
  kb_type: [{ required: true, message: t('请选择资产库类型'), trigger: 'change' }],
  kb_path: [{ required: true, message: t('请输入存储路径'), trigger: 'blur' }],
}

function show() {
  formState.kb_name = ''
  formState.kb_type = 'structured'
  formState.kb_path = ''
  formState.kb_description = ''
  visible.value = true
}

function handleCancel() {
  visible.value = false
  emit('cancel')
}

async function handleOk() {
  try {
    await formRef.value?.validate()
    loading.value = true

    const response = await Apis.knowledgeBaseManagement.create_knowledge_base_api_v1_kb_base_post({
      data: {
        kb_name: formState.kb_name,
        kb_type: formState.kb_type,
        // kb_path: formState.kb_path
        kb_description: formState.kb_description || '',
      },
    })

    const kbId = response.data
    if (kbId) {
      message.success(t('资产库创建成功'))
      visible.value = false
      emit('success', kbId)
    }
    else {
      throw new Error(t('未返回资产库ID'))
    }
  }
  catch (error) {
    console.error(error)
    message.error(t('创建失败: {message}', { message: (error as any).message || t('未知错误') }))
  }
  finally {
    loading.value = false
  }
}

defineExpose({
  show,
})
</script>

<template>
  <a-modal
    v-model:open="visible"
    :title="t('创建资产库')"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
    >
      <a-form-item :label="t('资产库名称')" name="kb_name">
        <a-input v-model:value="formState.kb_name" :placeholder="t('请输入资产库名称')" />
      </a-form-item>

      <a-form-item :label="t('资产库类型')" name="kb_type">
        <a-select v-model:value="formState.kb_type">
          <a-select-option value="structured">
            {{ t('结构化 (Excel/CSV)') }}
          </a-select-option>
          <a-select-option value="unstructured">
            {{ t('非结构化 (PDF/TXT/MD...)') }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <!-- <a-form-item label="存储路径" name="kb_path">
        <a-input v-model:value="formState.kb_path" placeholder="例如: /data/kb/my_kb" />
        <div class="text-xs text-gray-400 mt-1">
          资产库文件存储的物理路径
        </div>
      </a-form-item> -->

      <a-form-item :label="t('描述')" name="kb_description">
        <a-textarea v-model:value="formState.kb_description" :placeholder="t('请输入资产库描述（可选）')" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
