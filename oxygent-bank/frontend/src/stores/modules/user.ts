import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserInfo } from '@/api/modules/user'
import { userApi } from '@/api/modules/user'
import { auth } from '@/utils/auth'

export const useUserStore = defineStore(
  'user',
  () => {
    const userInfo = ref<UserInfo | null>(null)
    const token = ref<string | null>(auth.getToken())

    /**
     * 设置用户信息
     */
    function setUserInfo(info: UserInfo | null) {
      userInfo.value = info
      if (info) {
        auth.setUserInfo(info)
      }
      else {
        auth.removeUserInfo()
      }
    }

    /**
     * 设置 Token
     */
    function setToken(newToken: string | null) {
      token.value = newToken
      if (newToken) {
        auth.setToken(newToken)
      }
      else {
        auth.removeToken()
      }
    }

    /**
     * 获取用户信息
     */
    async function fetchUserInfo() {
      try {
        const res = await userApi.getUserInfo()
        if (res.data) {
          setUserInfo(res.data)
        }
      }
      catch (error) {
        console.error('获取用户信息失败:', error)
      }
    }

    /**
     * 登录
     */
    async function login(username: string, password: string) {
      try {
        const res = await userApi.login(username, password)
        if (res.data?.token) {
          setToken(res.data.token)
          await fetchUserInfo()
          return true
        }
        return false
      }
      catch (error) {
        console.error('登录失败:', error)
        return false
      }
    }

    /**
     * 登出
     */
    async function logout() {
      try {
        await userApi.logout()
      }
      catch (error) {
        console.error('登出失败:', error)
      }
      finally {
        setToken(null)
        setUserInfo(null)
        auth.logout()
      }
    }

    /**
     * 是否已登录
     */
    const isAuthenticated = computed(() => !!token.value)

    return {
      userInfo,
      token,
      isAuthenticated,
      setUserInfo,
      setToken,
      fetchUserInfo,
      login,
      logout,
    }
  },
  {
    persist: {
      key: 'user-store',
      pick: ['token', 'userInfo'],
    },
  },
)
