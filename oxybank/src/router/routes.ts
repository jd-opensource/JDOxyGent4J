import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'

/**
 * 路由配置
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: {
          titleKey: '首页',
        },
      },
      {
        path: '/knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/index.vue'),
        meta: {
          titleKey: '资产库',
        },
      },
      {
        path: '/annotation',
        name: 'AnnotationPlatform',
        component: () => import('@/views/annotation/index.vue'),
        meta: {
          titleKey: '标注',
        },
      },
      {
        path: '/knowledge/create',
        name: 'KnowledgeCreate',
        component: () => import('@/views/knowledge/create/index.vue'),
        meta: {
          titleKey: '新建资产库',
        },
      },
      {
        path: '/knowledge/:id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/knowledge/detail/index.vue'),
        meta: {
          titleKey: '资产库详情',
        },
      },
      {
        path: '/knowledge/:id/document/:docId',
        name: 'DocumentChunks',
        component: () => import('@/views/knowledge/chunks/index.vue'),
        meta: {
          titleKey: 'Chunk 详情',
        },
      },
      {
        path: '/knowledge/:id/setting',
        name: 'KnowledgeSetting',
        component: () => import('@/views/knowledge/setting/index.vue'),
        meta: {
          titleKey: '资产库设置',
        },
      },
      {
        path: '/knowledge/:id/recall',
        name: 'KnowledgeRecall',
        component: () => import('@/views/knowledge/recall/index.vue'),
        meta: {
          titleKey: '召回测试',
        },
      },
    ],
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: {
      titleKey: '页面不存在',
    },
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/500.vue'),
    meta: {
      titleKey: '服务器错误',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]
