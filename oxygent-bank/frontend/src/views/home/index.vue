<script setup lang="ts">
import {
  AppstoreOutlined,
  ArrowRightOutlined,
  ContainerOutlined,
  DatabaseOutlined,
  EditOutlined,
  ShareAltOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/locales'

const { t } = useI18n()
const router = useRouter()

const mainActions = computed(() => [
  {
    icon: DatabaseOutlined,
    title: t('Home_Assets_Title'),
    description: t('Home_Assets_Desc'),
    action: t('Home_Assets_Action'),
    route: '/knowledge',
    color: '#4285F4',
  },
  {
    icon: EditOutlined,
    title: t('Home_Annotation_Title'),
    description: t('Home_Annotation_Desc'),
    action: t('Home_Annotation_Action'),
    route: '/annotation',
    color: '#34A853',
  },
])

const features = computed(() => [
  {
    icon: AppstoreOutlined,
    title: t('Home_Feature_Storage'),
    description: t('Home_Feature_Storage_Desc'),
  },
  {
    icon: ShareAltOutlined,
    title: t('Home_Feature_Routing'),
    description: t('Home_Feature_Routing_Desc'),
  },
  {
    icon: ThunderboltOutlined,
    title: t('Home_Feature_Interface'),
    description: t('Home_Feature_Interface_Desc'),
  },
  {
    icon: ContainerOutlined,
    title: t('Home_Feature_Audit'),
    description: t('Home_Feature_Audit_Desc'),
  },
])

function navigateTo(route: string) {
  router.push(route)
}
</script>

<template>
  <div class="home-container">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          {{ t('Home_Title') }}
        </h1>
        <p class="hero-subtitle">
          {{ t('Home_Subtitle') }}
        </p>
        <p class="hero-description">
          {{ t('Home_Description') }}
        </p>
      </div>
    </section>

    <!-- Main Actions Section -->
    <section class="actions-section">
      <div
        v-for="(action, index) in mainActions"
        :key="index"
        class="action-card"
        @click="navigateTo(action.route)"
      >
        <div class="action-icon" :style="{ backgroundColor: `${action.color}15` }">
          <component :is="action.icon" :style="{ color: action.color }" />
        </div>
        <div class="action-content">
          <h3 class="action-title">
            {{ action.title }}
          </h3>
          <p class="action-description">
            {{ action.description }}
          </p>
          <div class="action-button">
            <span>{{ action.action }}</span>
            <arrow-right-outlined />
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div
        v-for="(feature, index) in features"
        :key="index"
        class="feature-card"
      >
        <div class="feature-icon">
          <component :is="feature.icon" />
        </div>
        <h4 class="feature-title">
          {{ feature.title }}
        </h4>
        <p class="feature-description">
          {{ feature.description }}
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  font-family:
    'Plus Jakarta Sans',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
  padding: 4rem 2rem 6rem;
  overflow-x: hidden;
}

/* Hero Section */
.hero-section {
  max-width: 900px;
  margin: 0 auto 5rem;
  text-align: center;
  padding: 3rem 0;
  animation: fadeInUp 0.8s ease-out;
}

.hero-content {
  position: relative;
}

.hero-title {
  font-size: 4rem;
  font-weight: 700;
  color: #202124;
  margin: 0 0 1rem;
  letter-spacing: -0.02em;
  line-height: 1.1;
  animation: fadeInUp 0.8s ease-out 0.1s backwards;
}

.hero-subtitle {
  font-size: 1.75rem;
  font-weight: 500;
  color: #4285f4;
  margin: 0 0 1.5rem;
  animation: fadeInUp 0.8s ease-out 0.2s backwards;
}

.hero-description {
  font-size: 1.125rem;
  font-weight: 400;
  color: #5f6368;
  line-height: 1.6;
  margin: 0;
  animation: fadeInUp 0.8s ease-out 0.3s backwards;
}

/* Main Actions Section */
.actions-section {
  max-width: 1200px;
  margin: 0 auto 6rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 2rem;
  padding: 0 1rem;
}

.action-card {
  background: white;
  border-radius: 16px;
  padding: 3rem;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.06),
    0 1px 2px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  position: relative;
  overflow: hidden;
  animation: fadeInUp 0.8s ease-out backwards;
}

.action-card:nth-child(1) {
  animation-delay: 0.4s;
}

.action-card:nth-child(2) {
  animation-delay: 0.5s;
}

.action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--card-color, #4285f4), var(--card-color-light, #669df6));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.action-card:hover::before {
  opacity: 1;
}

.action-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.12),
    0 8px 16px rgba(0, 0, 0, 0.08);
}

.action-icon {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  transition: transform 0.3s ease;
}

.action-card:hover .action-icon {
  transform: scale(1.1);
}

.action-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.action-title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #202124;
  margin: 0;
  letter-spacing: -0.01em;
}

.action-description {
  font-size: 1rem;
  font-weight: 400;
  color: #5f6368;
  line-height: 1.6;
  margin: 0;
  flex: 1;
}

.action-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 500;
  color: #4285f4;
  transition: gap 0.3s ease;
}

.action-card:hover .action-button {
  gap: 1rem;
}

/* Features Section */
.features-section {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
  padding: 0 1rem;
}

.feature-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  animation: fadeInUp 0.8s ease-out backwards;
}

.feature-card:nth-child(1) {
  animation-delay: 0.6s;
}

.feature-card:nth-child(2) {
  animation-delay: 0.7s;
}

.feature-card:nth-child(3) {
  animation-delay: 0.8s;
}

.feature-card:nth-child(4) {
  animation-delay: 0.9s;
}

.feature-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #4285f4;
  margin: 0 auto 1rem;
  transition: all 0.3s ease;
}

.feature-card:hover .feature-icon {
  background: linear-gradient(135deg, #4285f4 0%, #669df6 100%);
  color: white;
  transform: rotateY(180deg);
}

.feature-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #202124;
  margin: 0 0 0.5rem;
  letter-spacing: -0.01em;
}

.feature-description {
  font-size: 0.875rem;
  font-weight: 400;
  color: #5f6368;
  line-height: 1.5;
  margin: 0;
}

/* Animations */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive Design */
@media (max-width: 1024px) {
  .hero-title {
    font-size: 3rem;
  }

  .hero-subtitle {
    font-size: 1.5rem;
  }

  .actions-section {
    grid-template-columns: 1fr;
    max-width: 600px;
  }
}

@media (max-width: 768px) {
  .home-container {
    padding: 2rem 1rem 4rem;
  }

  .hero-section {
    margin-bottom: 3rem;
    padding: 1rem 0;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.25rem;
  }

  .hero-description {
    font-size: 1rem;
  }

  .actions-section {
    gap: 1.5rem;
    margin-bottom: 4rem;
  }

  .action-card {
    padding: 2rem;
  }

  .action-title {
    font-size: 1.5rem;
  }

  .features-section {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
  }

  .feature-card {
    padding: 1.5rem;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 2rem;
  }

  .hero-subtitle {
    font-size: 1.125rem;
  }

  .actions-section {
    grid-template-columns: 1fr;
  }

  .features-section {
    grid-template-columns: 1fr;
  }
}
</style>
