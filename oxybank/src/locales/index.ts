import { ref, watch } from 'vue'
import enUS from './en-US'
import zhCN from './zh-CN'

export type Locale = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'oxygent-locale'

const messages: Record<Locale, Record<string, string>> = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

function getInitialLocale(): Locale {
  if (typeof window === 'undefined') {
    return 'zh-CN'
  }
  const stored = window.localStorage.getItem(STORAGE_KEY) as Locale | null
  if (stored === 'zh-CN' || stored === 'en-US') {
    return stored
  }
  const browser = window.navigator.language.toLowerCase()
  return browser.startsWith('en') ? 'en-US' : 'zh-CN'
}

const locale = ref<Locale>(getInitialLocale())

function interpolate(message: string, params?: Record<string, string | number>) {
  if (!params) {
    return message
  }
  return message.replace(/\{(\w+)\}/g, (_match, key) => {
    const value = params[key]
    return value === undefined ? `{${key}}` : String(value)
  })
}

export function t(key: string, params?: Record<string, string | number>) {
  const message = messages[locale.value]?.[key] || key
  return interpolate(message, params)
}

export function setLocale(value: Locale) {
  locale.value = value
}

export const availableLocales: Array<{ label: string, value: Locale }> = [
  { label: '中文', value: 'zh-CN' },
  { label: 'EN', value: 'en-US' },
]

watch(
  locale,
  (value) => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_KEY, value)
      document.documentElement.lang = value
    }
  },
  { immediate: true },
)

export function useI18n() {
  return {
    locale,
    setLocale,
    t,
    availableLocales,
  }
}
