import { axiosRequestAdapter } from '@alova/adapter-axios'

import type { AxiosError } from 'axios'
import { createAlova } from 'alova'
import vueHook from 'alova/vue'
import { message } from 'ant-design-vue'
import type { AlovaResponse, ValidationErrorItem } from './type'
import { formatValidationError } from './type'

import customAxios from './axios'
import { createApis, withConfigType } from './createApis'
import { t } from '@/locales'

// 解析错误消息
function parseErrorMessage(response: AlovaResponse): string {
  const { detail } = response

  if (!detail) {
    return response.message || t('请求失败')
  }

  // FastAPI 验证错误 (detail 为数组)
  if (Array.isArray(detail)) {
    return formatValidationError(detail as ValidationErrorItem[])
  }

  // 普通字符串错误
  return detail
}

// 处理网络错误
function handleNetworkError(error: AxiosError): string {
  if (error.code === 'ECONNABORTED') {
    return t('请求超时，请稍后重试')
  }
  if (!error.response) {
    return t('网络连接异常，请检查网络')
  }
  const status = error.response.status
  const statusMessages: Record<number, string> = {
    400: t('请求参数错误'),
    401: t('未授权，请重新登录'),
    403: t('拒绝访问'),
    404: t('请求资源不存在'),
    500: t('服务器内部错误'),
    502: t('网关错误'),
    503: t('服务不可用'),
  }
  return statusMessages[status] || t('请求失败 ({status})', { status })
}

export const alovaInstance = createAlova({
  baseURL: '',
  statesHook: vueHook,
  requestAdapter: axiosRequestAdapter({
    axios: customAxios,
  }),
  cacheFor: null,
  beforeRequest(method) {
    const token = localStorage.getItem('token')

    method.config.headers = {
      ...method.config.headers,
      'X-Requested-With': 'XMLHttpRequest',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }

    // FormData 时不设置 Content-Type，让浏览器自动处理
    if (!(method.data instanceof FormData)) {
      method.config.headers['Content-Type'] = 'application/json'
    }
  },
  responded: {
    onError(error: AxiosError) {
      const errMsg = handleNetworkError(error)
      message.error(errMsg)
      return Promise.reject(error)
    },
    onSuccess(response: AlovaResponse, method) {
      // rawResponse 模式：直接返回原始响应
      if (method.config?.rawResponse) {
        return response
      }

      // 业务错误处理
      if (response.code !== undefined && response.code !== 200) {
        const errMsg = parseErrorMessage(response)
        message.error(errMsg)
        throw new Error(errMsg)
      }

      return response
    },
    onComplete() {},
  },
})

export const $$userConfigMap = withConfigType({
  // 'general.get_kb_schema_api_v1_kb_base__kb_name__schema_exists_get': {
  //   rawResponse: true,
  // },
})

const Apis = createApis(alovaInstance, $$userConfigMap)
globalThis.Apis = Apis
export default Apis
