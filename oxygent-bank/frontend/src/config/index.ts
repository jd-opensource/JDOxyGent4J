/**
 * 应用配置
 */

export const appConfig = {
  title: import.meta.env.VITE_APP_TITLE || 'OxyBank',
  version: import.meta.env.VITE_APP_VERSION || '1.0.0',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
} as const
