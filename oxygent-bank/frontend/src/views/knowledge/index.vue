<script setup lang="ts">
import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePagination } from 'alova/client'
import { KnowledgeTypeEnum } from '@/config/enums'
import { useI18n } from '@/locales'
import CreateKbModal from '@/views/knowledge/components/CreateKbModal.vue'
import Apis from '@/api'

const router = useRouter()
const createModalRef = ref<InstanceType<typeof CreateKbModal> | null>(null)
const { t } = useI18n()

//  获取资产库列表
const { data: knowledgeList, loading, page, total, reload } = usePagination(
  (page, pageSize) => Apis.knowledgeBaseManagement.get_all_knowledge_base_api_v1_kb_base_get({
    params: {
      page,
      size: pageSize,
    },
  }),
  {
    data: response => response.data?.items || [],
    total: response => response.data?.total || 0,
    initialData: {
      data: [],
      total: 0,
    },
    initialPage: 1,
    initialPageSize: 20,
    append: true,
  },
)

const searchText = ref('')

// 过滤后的数据
const filteredList = computed(() => {
  if (!searchText.value) {
    return knowledgeList.value
  }
  return knowledgeList.value.filter(
    item =>
      item.kb_name.toLowerCase().includes(searchText.value.toLowerCase())
      || (item.kb_description && item.kb_description.toLowerCase().includes(searchText.value.toLowerCase())),
  )
})

// 是否还有更多数据
const hasMore = computed(() => {
  return knowledgeList.value.length < (total.value || 0)
})

// 加载更多
function loadMore() {
  page.value++
}

// 新建资产库
function handleCreate() {
  createModalRef.value?.show()
}

function handleCreateSuccess(id: string) {
  router.push({ path: '/knowledge/create', query: { kb_id: id } })
}

// 查看详情
function handleDetail(record: Record<string, any>) {
  router.push({
    path: `/knowledge/${record.kb_id}`,
    query: {
      kb_type: record.kb_type,
      kb_name: record.kb_name,
    },
  })
}

// 编辑 (暂时禁用)
function handleEdit(_record: Record<string, any>) {
  message.info(t('编辑功能暂未开放'))
}

// 删除 (暂时禁用)
function handleDelete(record: Record<string, any>) {
  Modal.confirm({
    title: t('确认删除'),
    content: t('确定要删除资产库「{name}」吗？此操作不可恢复。', { name: record.kb_name }),
    okText: t('删除'),
    okType: 'danger',
    cancelText: t('取消'),
    async onOk() {
      try {
        await Apis.knowledgeBaseManagement.delete_knowledge_base_api_v1_kb_base__kb_name__delete(
          {
            pathParams: {
              kb_name: record.kb_name,
            },
          },
        )
        message.success(t('删除成功'))
        reload()
      }
      catch {
        message.error(t('删除失败'))
      }
    },
  })
}

// 获取资产库类型标签
function getTypeLabel(type: string) {
  return t(KnowledgeTypeEnum.getLabelByValue(type as any) || type)
}

// 获取资产库类型颜色
function getTypeColor(type: string) {
  return KnowledgeTypeEnum.getColorByValue(type as any) || 'default'
}
</script>

<template>
  <div class="knowledge-list-page">
    <!-- 页面头部 -->
    <div class="mb-6 flex items-start justify-between">
      <a-input
        v-model:value="searchText"
        :placeholder="t('搜索资产库名称或描述')"
        allow-clear
        class="w-full sm:w-80"
      >
        <template #prefix>
          <search-outlined class="text-gray-400" />
        </template>
      </a-input>
      <div>
        <a-button type="primary" @click="handleCreate">
          <template #icon>
            <plus-outlined />
          </template>
          {{ t('新建资产库') }}
        </a-button>
      </div>
    </div>

    <!-- 加载中状态 (仅首次) -->
    <div v-if="loading && page === 1 && knowledgeList.length === 0" class="flex justify-center py-32">
      <a-spin size="large" :tip="t('加载中...')" />
    </div>

    <!-- 卡片列表 -->
    <div v-else-if="filteredList.length > 0">
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <div
          v-for="item in filteredList"
          :key="item.kb_id"
          class="group relative flex cursor-pointer flex-col rounded-xl border border-gray-100 bg-white p-5 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-lg hover:shadow-indigo-50"
          @click="handleDetail(item)"
        >
          <!-- 头部: 图标与名称 -->
          <div class="mb-4 flex items-start gap-3">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 transition-colors group-hover:bg-indigo-600 group-hover:text-white">
              <file-text-outlined class="text-xl" />
            </div>
            <div class="min-w-0 flex-1">
              <h3 class="mb-1 truncate text-lg font-bold text-gray-800 transition-colors group-hover:text-indigo-600" :title="item.kb_name">
                {{ item.kb_name }}
              </h3>
              <a-tag :color="getTypeColor(item.kb_type)" class="mr-0 border-0">
                {{ getTypeLabel(item.kb_type) }}
              </a-tag>
            </div>
          </div>

          <!-- 描述 -->
          <p class="mb-4 line-clamp-3 h-[60px] text-sm leading-relaxed text-gray-500" :title="item.kb_description">
            {{ item.kb_description || t('暂无描述') }}
          </p>

          <!-- 操作栏 -->
          <div class="mt-auto flex items-center justify-between border-t border-gray-50 pt-4" @click.stop>
            <a-tooltip :title="t('查看详情')">
              <a-button type="link" size="small" class="!px-0 text-gray-500 hover:text-indigo-600" @click="handleDetail(item)">
                <template #icon>
                  <eye-outlined class="mr-1" />
                </template>
                {{ t('查看') }}
              </a-button>
            </a-tooltip>

            <div class="flex gap-2">
              <a-tooltip :title="t('编辑功能暂未开放')">
                <a-button type="text" size="small" disabled class="text-gray-400 hover:text-blue-600" @click="handleEdit(item)">
                  <template #icon>
                    <edit-outlined />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="t('删除资产库')">
                <a-button type="text" size="small" danger class="hover:bg-red-50" @click="handleDelete(item)">
                  <template #icon>
                    <delete-outlined />
                  </template>
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 / 到底提示 -->
      <div class="mt-10 flex justify-center pb-10">
        <a-button
          v-if="hasMore"
          :loading="loading"
          size="large"
          class="min-w-[160px] rounded-full shadow-sm hover:shadow-md"
          @click="loadMore"
        >
          {{ loading ? t('加载中...') : t('加载更多') }}
        </a-button>
        <div v-else class="flex items-center gap-2 text-gray-400">
          <span class="h-px w-12 bg-gray-200" />
          <span class="text-sm">{{ t('没有更多数据了') }}</span>
          <span class="h-px w-12 bg-gray-200" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="flex flex-col items-center justify-center py-24 text-center">
      <div class="mb-6 inline-flex h-24 w-24 items-center justify-center rounded-full bg-indigo-50">
        <file-text-outlined class="text-5xl text-indigo-200" />
      </div>
      <h3 class="mb-2 text-xl font-semibold text-gray-900">
        {{ searchText ? t('未找到相关资产库') : t('暂无资产库') }}
      </h3>
      <p class="mb-8 text-gray-500">
        {{ searchText ? t('请尝试更换搜索关键词') : t('开始创建您的第一个资产库，管理和检索您的文档。') }}
      </p>
      <a-button v-if="!searchText" type="primary" size="large" class="h-10 px-8" @click="handleCreate">
        <template #icon>
          <plus-outlined />
        </template>
        {{ t('创建第一个资产库') }}
      </a-button>
      <a-button v-else @click="searchText = ''">
        {{ t('清除搜索') }}
      </a-button>
    </div>

    <create-kb-modal ref="createModalRef" @success="handleCreateSuccess" />
  </div>
</template>

<style scoped>
/*
  Reset or scoped styles if needed.
  Tailwind handles most things.
*/
</style>
