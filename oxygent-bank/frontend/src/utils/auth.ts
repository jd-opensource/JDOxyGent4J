/**
 * 认证工具
 */

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'user_info'

export const auth = {
  /**
   * 获取 Token
   */
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  /**
   * 设置 Token
   */
  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },

  /**
   * 移除 Token
   */
  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY)
  },

  /**
   * 获取用户信息
   */
  getUserInfo<T = any>(): T | null {
    const userInfo = localStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
  },

  /**
   * 设置用户信息
   */
  setUserInfo(userInfo: any): void {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  /**
   * 移除用户信息
   */
  removeUserInfo(): void {
    localStorage.removeItem(USER_INFO_KEY)
  },

  /**
   * 是否已登录
   */
  isAuthenticated(): boolean {
    return !!this.getToken()
  },

  /**
   * 登出
   */
  logout(): void {
    this.removeToken()
    this.removeUserInfo()
  },
}
