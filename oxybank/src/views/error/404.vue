<script setup lang="ts">
import { HomeOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/locales'

const router = useRouter()
const { t } = useI18n()

function goHome() {
  router.push('/knowledge')
}
</script>

<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">
        404
      </div>
      <div class="error-title">
        {{ t('页面不存在') }}
      </div>
      <div class="error-description">
        {{ t('抱歉，您访问的页面不存在或已被移除') }}
      </div>
      <a-button type="primary" size="large" @click="goHome">
        <template #icon>
          <home-outlined />
        </template>
        {{ t('返回首页') }}
      </a-button>
    </div>
    <div class="error-illustration">
      <svg viewBox="0 0 400 300" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="50" y="80" width="300" height="180" rx="8" fill="#f5f5f5" stroke="#d9d9d9" stroke-width="2" />
        <circle cx="200" cy="140" r="40" fill="#1890ff" opacity="0.2" />
        <path d="M180 130 L200 150 L220 130" stroke="#1890ff" stroke-width="4" stroke-linecap="round" fill="none" />
        <path d="M180 150 L200 170 L220 150" stroke="#1890ff" stroke-width="4" stroke-linecap="round" fill="none" />
        <rect x="120" y="200" width="160" height="20" rx="4" fill="#e6e6e6" />
        <rect x="140" y="230" width="120" height="12" rx="2" fill="#f0f0f0" />
      </svg>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}

.error-content {
  text-align: center;
  color: #fff;
  margin-bottom: 48px;
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  font-family:
    'SF Pro Display',
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  margin-top: 16px;
  margin-bottom: 8px;
}

.error-description {
  font-size: 16px;
  opacity: 0.8;
  margin-bottom: 32px;
}

.error-illustration {
  width: 300px;
  max-width: 100%;
}

.error-illustration svg {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 10px 30px rgba(0, 0, 0, 0.2));
}
</style>
