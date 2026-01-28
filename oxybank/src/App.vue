<script setup lang="ts">
import { ConfigProvider } from 'ant-design-vue'
import enUS from 'ant-design-vue/es/locale/en_US'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { themeConfig } from './config/theme'
import { useI18n } from '@/locales'

const { locale } = useI18n()

const antdLocale = computed(() => (locale.value === 'en-US' ? enUS : zhCN))
</script>

<template>
  <config-provider :theme="themeConfig" :locale="antdLocale">
    <div id="app">
      <router-view />
    </div>
  </config-provider>
</template>

<style scoped>
#app {
  width: 100%;
  height: 100%;
}
</style>
