<script setup lang="ts">
import { HomeOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/locales'

const router = useRouter()
const { t } = useI18n()

function goHome() {
  router.push('/knowledge')
}

function reload() {
  window.location.reload()
}
</script>

<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">
        500
      </div>
      <div class="error-title">
        {{ t('服务器错误') }}
      </div>
      <div class="error-description">
        {{ t('抱歉，服务器发生了错误，请稍后再试') }}
      </div>
      <a-space>
        <a-button size="large" @click="reload">
          <template #icon>
            <reload-outlined />
          </template>
          {{ t('刷新页面') }}
        </a-button>
        <a-button type="primary" size="large" @click="goHome">
          <template #icon>
            <home-outlined />
          </template>
          {{ t('返回首页') }}
        </a-button>
      </a-space>
    </div>
    <div class="error-illustration">
      <svg viewBox="0 0 400 300" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="50" y="80" width="300" height="180" rx="8" fill="#fff5f5" stroke="#ffa39e" stroke-width="2" />
        <circle cx="200" cy="140" r="40" fill="#ff4d4f" opacity="0.2" />
        <path d="M185 125 L215 155" stroke="#ff4d4f" stroke-width="4" stroke-linecap="round" />
        <path d="M215 125 L185 155" stroke="#ff4d4f" stroke-width="4" stroke-linecap="round" />
        <rect x="120" y="200" width="160" height="20" rx="4" fill="#ffccc7" />
        <rect x="140" y="230" width="120" height="12" rx="2" fill="#fff1f0" />
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
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
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
