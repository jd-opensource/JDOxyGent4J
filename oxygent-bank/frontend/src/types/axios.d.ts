import 'axios'

declare module 'axios' {
  // 在原有的 AxiosRequestConfig 基础上，新增一个 rawResponse?: boolean
  interface AxiosRequestConfig {
    rawResponse?: boolean
  }
}
