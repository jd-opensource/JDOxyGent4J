/**
 * 资产库相关类型定义
 */

// 资产库状态
export type KnowledgeStatus = 'active' | 'inactive'

// 文档状态
export type DocumentStatus = 'enabled' | 'disabled' | 'processing' | 'error'

// Chunk 状态
export type ChunkStatus = 'enabled' | 'disabled'

// 分段模式
export type ChunkMode = 'auto' | 'custom'

// 分段策略类型
export type ParserType = 'token' | 'sentence' | 'markdown' | 'html' | 'json' | 'smart'

// 索引方式
export type IndexMode = 'high_quality' | 'economy'

// 检索方式
export type RetrievalMode = 'vector' | 'fulltext' | 'hybrid'

// 资产库接口
export interface KnowledgeBase {
  id: string
  name: string
  description: string
  documentCount: number
  totalCharacters: number
  hitCount: number
  status: KnowledgeStatus
  createdAt: string
  updatedAt: string
}

// 文档接口
export interface Document {
  id: string
  knowledgeId: string
  name: string
  chunkMode: ChunkMode
  characterCount: number
  chunkCount: number
  hitCount: number
  uploadedAt: string
  status: DocumentStatus
}

// Chunk 接口
export interface Chunk {
  id: string
  documentId: string
  index: number
  content: string
  characterCount: number
  hitCount: number
  status: ChunkStatus
}

// 创建资产库表单
export interface CreateKnowledgeForm {
  name: string
  description: string
  files: File[]
  chunkConfig: ChunkConfig
}

// 分段配置
export interface ChunkConfig {
  parserType: ParserType
  chunkSize: number
  chunkOverlap: number
  separator: string
  retrievalMode: RetrievalMode
}

// 默认分段配置
export const defaultChunkConfig: ChunkConfig = {
  parserType: 'smart',
  chunkSize: 500,
  chunkOverlap: 50,
  separator: ' ',
  retrievalMode: 'vector',
}

// --- New Types for Refactored Flow ---

export interface KnowledgeBaseInfo {
  kb_name: string
  kb_type: string
}

export interface ColumnSchema {
  name: string
  type: string
  description: string
  // originalType?: string
}

export interface StructuredFileConfig {
  sheetName?: string
  headerRow?: number
  columns: ColumnSchema[]
}

export interface RetrievalStrategy {
  type: 'precise' | 'es' | 'vearch'
  input_fields: string[]
  output_fields: string[]
  embedding_model?: string // only for vearch
}

export interface RetrievalRule {
  id: string
  // 第一行：全文检索或向量检索 (互斥)
  mainStrategy: RetrievalStrategy
  // 第二行及后续：精确匹配 (可多个)
  preciseStrategies: RetrievalStrategy[]
  // 规则共享的输出字段
  output_fields: string[]
}

export type RetrievalConfig = RetrievalRule[]

export const defaultRetrievalConfig: RetrievalConfig = [
  {
    id: 'default_rule',
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
  },
]
