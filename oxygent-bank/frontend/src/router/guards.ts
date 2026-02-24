import type { Router } from 'vue-router'
import { t } from '@/locales'
import { auth } from '@/utils/auth'

/**
 * 路由守卫配置
 */
export function setupRouterGuards(router: Router) {
  // 全局前置守卫
  router.beforeEach((to, _from, next) => {
    // 设置页面标题
    const titleKey = (to.meta.titleKey || to.meta.title) as string | undefined
    if (titleKey) {
      document.title = `${t(titleKey)} - OxyBank`
    }

    // 检查是否需要登录
    if (to.meta.requiresAuth && !auth.isAuthenticated()) {
      // 可以在这里跳转到登录页
      // next({ name: 'Login', query: { redirect: to.fullPath } })
      next()
    }
    else {
      next()
    }
  })

  // 全局后置守卫
  router.afterEach(() => {
    // 可以在这里添加页面访问统计等
  })
}
