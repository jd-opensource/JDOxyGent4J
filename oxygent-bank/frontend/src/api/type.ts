import type { AxiosResponseHeaders } from 'axios'

// FastAPI 验证错误项
export interface ValidationErrorItem {
  loc: (string | number)[]
  msg: string
  type: string
}

// FastAPI 响应结构
export interface AlovaResponse<T = any> {
  code?: number
  message?: string
  data: T
  throwableDetail?: string
  $headers?: AxiosResponseHeaders
  // FastAPI 错误: string 为普通错误, ValidationErrorItem[] 为验证错误
  detail?: string | ValidationErrorItem[]
}

// 格式化验证错误为可读字符串
export function formatValidationError(detail: ValidationErrorItem[]): string {
  return detail.map(err => `${err.loc.join('.')}: ${err.msg}`).join('; ')
}
