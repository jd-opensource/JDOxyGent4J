/**
 * EnumHelper - 类型安全的枚举帮助工具
 *
 * @description
 * 提供了一套完整的枚举管理方案，支持类型安全、下拉选项、标签获取等功能。
 * 特别适用于与 Ant Design Vue 等 UI 库配合使用。
 *
 * @example
 * ```typescript
 * // 定义枚举配置
 * const StatusEnum = new EnumHelper({
 *   PENDING: { label: '待处理', value: 0, color: 'orange' },
 *   APPROVED: { label: '已通过', value: 1, color: 'green' },
 *   REJECTED: { label: '已拒绝', value: 2, color: 'red' },
 * })
 *
 * // 使用枚举值
 * const status = StatusEnum.enum.PENDING // 0
 *
 * // 获取标签
 * StatusEnum.getLabelByValue(0) // '待处理'
 *
 * // 获取下拉选项
 * StatusEnum.options // [{ label: '待处理', value: 0, color: 'orange' }, ...]
 *
 * // 获取颜色
 * StatusEnum.getColorByValue(1) // 'green'
 * ```
 *
 * @author OXY Team
 * @license MIT
 */

import type { TagProps } from 'ant-design-vue'

/**
 * 基础原始类型
 */
type Primitive = string | number | boolean

/**
 * 枚举项接口定义
 * @template T 枚举值的类型，必须是原始类型
 */
export interface EnumItem<T extends Primitive = number | string | boolean> {
  /** 显示标签 */
  label: string
  /** 枚举值 */
  value: T
  /** 是否禁用 */
  disabled?: boolean
  /** 标签颜色（适用于 Ant Design Tag 组件） */
  color?: TagProps['color']
  /** 图标（可选） */
  icon?: string
  /** 描述信息（可选） */
  description?: string
}

/**
 * 从枚举配置中提取值类型
 */
type ExtractEnumValue<T> = T extends EnumItem<infer V> ? V : never

/**
 * 枚举配置类型（只读）
 */
type EnumConfigType<T extends Record<string, EnumItem>> = {
  readonly [K in keyof T]: T[K]
}

/**
 * 从枚举配置生成枚举值对象
 */
type EnumFromConfig<T extends Record<string, EnumItem>> = {
  readonly [K in keyof T]: T[K]['value']
}

/**
 * 枚举帮助类
 *
 * @template T 枚举配置对象类型
 * @template V 枚举值类型
 *
 * @example
 * ```typescript
 * const PermissionEnum = new EnumHelper({
 *   HIGH: { label: '高', value: 'high', color: 'red' },
 *   MEDIUM: { label: '中', value: 'medium', color: 'orange' },
 *   LOW: { label: '低', value: 'low', color: 'green' },
 * })
 *
 * // 访问枚举值
 * PermissionEnum.enum.HIGH // 'high'
 *
 * // 获取所有标签
 * PermissionEnum.getAllLabels() // ['高', '中', '低']
 *
 * // 检查值是否存在
 * PermissionEnum.hasValue('high') // true
 * ```
 */
export class EnumHelper<
  T extends Record<string, EnumItem>,
  V extends ExtractEnumValue<T[keyof T]> = ExtractEnumValue<T[keyof T]>,
> {
  /** 枚举配置（冻结的只读对象） */
  private enumConfig: EnumConfigType<T>

  /** 枚举值对象（冻结的只读对象） */
  private enumValue: EnumFromConfig<T>

  /**
   * 构造函数
   * @param config 枚举配置对象
   */
  constructor(config: T) {
    this.enumConfig = Object.freeze({ ...config }) as EnumConfigType<T>
    this.enumValue = Object.entries(config).reduce((acc, [key, item]) => {
      return { ...acc, [key]: item.value }
    }, {} as EnumFromConfig<T>)
    Object.freeze(this.enumValue)
  }

  /**
   * 获取下拉选项列表
   * @param keys 可选的键过滤器，只返回指定键的选项
   * @returns 枚举项数组，适用于下拉框等组件
   *
   * @example
   * ```typescript
   * // 获取所有选项
   * StatusEnum.getOptions()
   *
   * // 获取指定选项
   * StatusEnum.getOptions(['PENDING', 'APPROVED'])
   * ```
   */
  getOptions(keys?: Array<keyof T>): EnumItem<V>[] {
    return Object.entries(this.enumConfig)
      .filter(([key]) => (keys ? keys.includes(key) : true))
      .map(([, item]) => item)
  }

  /**
   * 根据枚举值获取标签文本
   * @param value 枚举值
   * @returns 对应的标签文本，未找到则返回空字符串
   *
   * @example
   * ```typescript
   * StatusEnum.getLabelByValue(0) // '待处理'
   * StatusEnum.getLabelByValue(999) // ''
   * ```
   */
  getLabelByValue(value?: V): string {
    const item = Object.values(this.enumConfig).find(item => item.value === value)
    return item?.label || ''
  }

  /**
   * 根据值获取完整的枚举项配置
   * @param value 枚举值
   * @returns 完整的 EnumItem 对象，未找到则返回 undefined
   *
   * @example
   * ```typescript
   * StatusEnum.getConfigByValue(0)
   * // { label: '待处理', value: 0, color: 'orange' }
   * ```
   */
  getConfigByValue(value: V): EnumItem<V> | undefined {
    return Object.values(this.enumConfig).find(item => item.value === value)
  }

  /**
   * 根据键获取枚举项配置
   * @param key 枚举键
   * @returns 对应的 EnumItem 对象
   *
   * @example
   * ```typescript
   * StatusEnum.getConfigByKey('PENDING')
   * // { label: '待处理', value: 0, color: 'orange' }
   * ```
   */
  getConfigByKey(key: keyof T): T[keyof T] {
    return this.enumConfig[key]
  }

  /**
   * 根据值获取颜色
   * @param value 枚举值
   * @returns 对应的颜色值，未找到则返回 undefined
   *
   * @example
   * ```typescript
   * StatusEnum.getColorByValue(0) // 'orange'
   * ```
   */
  getColorByValue(value: V): TagProps['color'] | undefined {
    return this.getConfigByValue(value)?.color
  }

  /**
   * 根据值获取图标
   * @param value 枚举值
   * @returns 对应的图标，未找到则返回 undefined
   */
  getIconByValue(value: V): string | undefined {
    return this.getConfigByValue(value)?.icon
  }

  /**
   * 根据值获取描述
   * @param value 枚举值
   * @returns 对应的描述信息，未找到则返回 undefined
   */
  getDescriptionByValue(value: V): string | undefined {
    return this.getConfigByValue(value)?.description
  }

  /**
   * 获取所有枚举值
   * @returns 所有枚举值的数组
   *
   * @example
   * ```typescript
   * StatusEnum.getAllValues() // [0, 1, 2]
   * ```
   */
  getAllValues(): V[] {
    return Object.values(this.enumConfig).map(item => item.value as V)
  }

  /**
   * 获取所有标签
   * @returns 所有标签的数组
   *
   * @example
   * ```typescript
   * StatusEnum.getAllLabels() // ['待处理', '已通过', '已拒绝']
   * ```
   */
  getAllLabels(): string[] {
    return Object.values(this.enumConfig).map(item => item.label)
  }

  /**
   * 检查值是否存在于枚举中
   * @param value 待检查的值
   * @returns 是否存在
   *
   * @example
   * ```typescript
   * StatusEnum.hasValue(0) // true
   * StatusEnum.hasValue(999) // false
   * ```
   */
  hasValue(value: V): boolean {
    return this.getAllValues().includes(value)
  }

  /**
   * 获取所有下拉选项（快捷访问器）
   * @returns 枚举项数组
   */
  get options(): EnumItem<V>[] {
    return this.getOptions()
  }

  /**
   * 获取默认值（第一个选项的值）
   * @returns 第一个枚举项的值
   *
   * @example
   * ```typescript
   * StatusEnum.default // 0
   * ```
   */
  get default(): V | undefined {
    return this.options[0]?.value
  }

  /**
   * 获取枚举值对象（类似传统 enum 的使用方式）
   * @returns 枚举值对象
   *
   * @example
   * ```typescript
   * StatusEnum.enum.PENDING // 0
   * StatusEnum.enum.APPROVED // 1
   * ```
   */
  get enum(): EnumFromConfig<T> {
    return this.enumValue
  }

  /**
   * 获取枚举项数量
   * @returns 枚举项总数
   */
  get size(): number {
    return this.options.length
  }
}
