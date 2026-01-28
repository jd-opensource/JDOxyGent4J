<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppFooter from './AppFooter.vue'
import AppHeader from './AppHeader.vue'
// import AppSider from './AppSider.vue'
// import { useAppStore } from '@/stores/modules/app'

// const appStore = useAppStore()
// const collapsed = computed(() => appStore.collapsed)

const route = useRoute()
const isHomePage = computed(() => route.name === 'Home')
</script>

<template>
  <a-layout class="h-screen overflow-hidden">
    <!-- <app-sider :collapsed="collapsed" /> -->
    <a-layout class="flex flex-col h-full">
      <app-header />
      <a-layout-content
        class="flex-1 overflow-y-auto flex flex-col"
        :class="[isHomePage ? 'bg-[#000810]' : 'bg-gray-50']"
      >
        <div class="flex-1" :class="{ 'p-4': !isHomePage }">
          <router-view />
        </div>
        <app-footer v-if="!isHomePage" />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped></style>
