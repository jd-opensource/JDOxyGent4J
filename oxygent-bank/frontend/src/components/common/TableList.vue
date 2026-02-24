<script setup lang="ts" generic="T extends Record<string, any>">
import type { TableColumnsType, TableProps } from 'ant-design-vue'
import { useI18n } from '@/locales'

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  rowKey: 'id',
})

const { t } = useI18n()

interface Props {
  columns: TableColumnsType<T>
  dataSource: T[]
  loading?: boolean
  rowKey?: string
  rowSelection?: TableProps['rowSelection']
}

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: computed(() => props.dataSource.length),
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => t('共 {total} 条', { total }),
  pageSizeOptions: ['10', '20', '50', '100'],
})

// 处理分页变化
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
}
</script>

<template>
  <div class="table-list-wrapper">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :row-key="rowKey"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1500 }"
      @change="handleTableChange"
    >
      <template #bodyCell="slotProps">
        <slot name="bodyCell" v-bind="slotProps" />
      </template>
    </a-table>
  </div>
</template>

<style scoped>
.table-list-wrapper {
  background: white;
  padding: 16px;
  border-radius: 8px;
}

.table-list-wrapper :deep(.ant-table-wrapper) {
  background: white;
}
</style>
