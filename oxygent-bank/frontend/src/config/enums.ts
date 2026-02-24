/**
 * 应用枚举配置
 *
 * @description
 * 定义应用中使用的所有枚举值，统一管理状态、类型等配置项。
 * 使用 EnumHelper 提供类型安全和便捷的访问方式。
 */

import { EnumHelper } from '@/utils/enumHelper'

/**
 * 资产库状态枚举
 *
 * @description 定义资产库的状态
 */
export const KnowledgeStatusEnum = new EnumHelper({
  ACTIVE: {
    label: '启用',
    value: 'active',
    color: 'green',
    description: '资产库处于启用状态',
  },
  INACTIVE: {
    label: '停用',
    value: 'inactive',
    color: 'default',
    description: '资产库处于停用状态',
  },
})

/**
 * 资产库类型枚举
 *
 * @description 定义资产库的类型
 */
export const KnowledgeTypeEnum = new EnumHelper({
  STRUCTURED: {
    label: '结构化',
    value: 'structured',
    color: 'blue',
    description: '结构化数据资产库 (Excel/CSV)',
  },
  UNSTRUCTURED: {
    label: '非结构化',
    value: 'unstructured',
    color: 'purple',
    description: '非结构化数据资产库 (文档/文本)',
  },
})

/**
 * 文档权限枚举
 *
 * @description 定义资产库和文档的访问权限级别
 */
export const PermissionEnum = new EnumHelper({
  HIGH: {
    label: '高',
    value: 'high',
    color: 'red',
    description: '高权限，仅管理员可访问',
  },
  MEDIUM: {
    label: '中',
    value: 'medium',
    color: 'orange',
    description: '中等权限，团队成员可访问',
  },
  LOW: {
    label: '低',
    value: 'low',
    color: 'green',
    description: '低权限，所有人可访问',
  },
})

/**
 * 文档学习状态枚举
 *
 * @description 文档在资产库中的学习处理状态
 */
export const LearningStatusEnum = new EnumHelper({
  COMPLETED: {
    label: '学习完成',
    value: 'completed',
    color: 'green',
    description: '文档已成功学习并可用',
  },
  LEARNING: {
    label: '学习中',
    value: 'learning',
    color: 'blue',
    description: '文档正在学习处理中',
  },
  PENDING: {
    label: '待学习',
    value: 'pending',
    color: 'default',
    description: '文档等待学习处理',
  },
  FAILED: {
    label: '学习失败',
    value: 'failed',
    color: 'red',
    description: '文档学习处理失败',
  },
})

/**
 * 文档来源类型枚举
 *
 * @description 文档的获取来源方式
 */
export const DocumentSourceEnum = new EnumHelper({
  ONLINE: {
    label: '在线获取',
    value: 'online',
    description: '通过链接在线获取文档',
  },
  LOCAL: {
    label: '本地上传',
    value: 'local',
    description: '从本地上传文档文件',
  },
  CUSTOM: {
    label: '自定义',
    value: 'custom',
    description: '手动输入自定义文档内容',
  },
})

/**
 * 文档类型枚举
 *
 * @description 文档的存储和来源类型
 */
export const DocumentTypeEnum = new EnumHelper({
  JOYSPACE: {
    label: 'JoySpace',
    value: 'joyspace',
    description: 'JoySpace 平台文档',
  },
  DOCUMENT: {
    label: '文档',
    value: 'document',
    description: '通用文档类型',
  },
  SSC: {
    label: 'SSC',
    value: 'ssc',
    description: 'SSC 平台文档',
  },
  EIT: {
    label: 'EIT',
    value: 'eit',
    description: 'EIT 平台文档',
  },
  MIAOXUN: {
    label: 'MIAOXUN',
    value: 'miaoxun',
    description: '妙寻平台文档',
  },
  OTHER: {
    label: '其他',
    value: 'other',
    description: '其他类型文档',
  },
})

/**
 * 文档分段方式枚举
 *
 * @description 文档内容的分段处理方式
 */
export const SegmentMethodEnum = new EnumHelper({
  AUTO: {
    label: '自动分段',
    value: 'auto',
    description: '系统自动识别并分段',
  },
  MANUAL: {
    label: '手动分段',
    value: 'manual',
    description: '用户手动设置分段',
  },
})

/**
 * 上传方式枚举
 *
 * @description 文档上传的方式选择
 */
export const UploadTypeEnum = new EnumHelper({
  LOCAL: {
    label: '本地上传',
    value: 'local',
    icon: 'FolderOutlined',
    description: '上传本地文件或文件夹',
  },
  ONLINE: {
    label: '在线获取',
    value: 'online',
    icon: 'LinkOutlined',
    description: '通过URL在线获取文档',
  },
  CUSTOM: {
    label: '自定义',
    value: 'custom',
    icon: 'FileOutlined',
    description: '手动输入自定义内容',
  },
})

/**
 * 在线文档类型枚举
 *
 * @description 在线获取文档的类型
 */
export const OnlineDocTypeEnum = new EnumHelper({
  JOYSPACE: {
    label: 'JoySpace',
    value: 'joyspace',
    description: '在线获取JoySpace文档内容，进行解析和智能化拆分的处理',
  },
  OSS: {
    label: 'OSS文件',
    value: 'oss',
    description: '获取OSS地址中的文件内容，进行解析和智能化拆分的处理',
  },
  API: {
    label: 'API',
    value: 'api',
    description: '获取API接口内的内容，进行解析和分段处理',
  },
  WEBPAGE: {
    label: '网页',
    value: 'webpage',
    description: '获取网页内容，进行解析和分段处理',
  },
  DOCUMENT: {
    label: '神灯文档',
    value: 'document',
    description: '在线获取神灯文档内容，进行解析和智能化拆分的处理',
  },
})

/**
 * 文档更新策略枚举
 *
 * @description 文档自动更新的时间策略
 */
export const UpdateStrategyEnum = new EnumHelper({
  MANUAL: {
    label: '不自动更新',
    value: 'manual',
    description: '手动触发更新',
  },
  DAILY: {
    label: '每1天自动更新',
    value: 'daily',
    description: '每天自动更新一次',
  },
  THREE_DAYS: {
    label: '每3天自动更新',
    value: 'three_days',
    description: '每3天自动更新一次',
  },
  WEEKLY: {
    label: '每7天自动更新',
    value: 'weekly',
    description: '每7天自动更新一次',
  },
})

// 导出所有枚举的类型，方便在组件中使用
export type KnowledgeStatusValue = typeof KnowledgeStatusEnum.enum[keyof typeof KnowledgeStatusEnum.enum]
export type KnowledgeTypeValue = typeof KnowledgeTypeEnum.enum[keyof typeof KnowledgeTypeEnum.enum]
export type PermissionValue = typeof PermissionEnum.enum[keyof typeof PermissionEnum.enum]
export type LearningStatusValue = typeof LearningStatusEnum.enum[keyof typeof LearningStatusEnum.enum]
export type DocumentSourceValue = typeof DocumentSourceEnum.enum[keyof typeof DocumentSourceEnum.enum]
export type DocumentTypeValue = typeof DocumentTypeEnum.enum[keyof typeof DocumentTypeEnum.enum]
export type SegmentMethodValue = typeof SegmentMethodEnum.enum[keyof typeof SegmentMethodEnum.enum]
export type UploadTypeValue = typeof UploadTypeEnum.enum[keyof typeof UploadTypeEnum.enum]
export type OnlineDocTypeValue = typeof OnlineDocTypeEnum.enum[keyof typeof OnlineDocTypeEnum.enum]
export type UpdateStrategyValue = typeof UpdateStrategyEnum.enum[keyof typeof UpdateStrategyEnum.enum]
