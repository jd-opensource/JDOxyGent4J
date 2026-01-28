import type { Dayjs } from 'dayjs'

export type AnnotationStatus =
  | 'pending'
  | 'annotated'
  | 'approved'
  | 'rejected'
  | 'kb_ingested'
  | 'kb_failed'

export type AnnotationDataType = 'e2e' | 'agent' | 'llm' | 'tool' | 'custom'

export interface AnnotationItem {
  data_id: string
  priority?: number | null
  status: AnnotationStatus
  data_type?: AnnotationDataType | string | null
  caller?: string | null
  callee?: string | null
  question?: string | Record<string, unknown> | null
  answer?: string | Record<string, unknown> | null
  source_group_id?: string | null
  source_trace_id?: string | null
  created_at?: string | null
  annotation?: Record<string, unknown> | null
  reject_reason?: string | null
}

export interface AnnotationStats {
  pending: number
  annotated: number
  approved: number
  rejected: number
  kb_ingested: number
  kb_failed: number
}

export interface AnnotationFilters {
  timeRange: [Dayjs, Dayjs] | undefined
  dataType: string
  status: string
  priority: string
  caller: string
  callee: string
  groupId: string
  traceId: string
  search: string
}
