/**
 * 本地存储工具
 */

const PREFIX = 'oxybank_'

export const storage = {
  /**
   * 设置存储
   */
  set(key: string, value: any, expire?: number): void {
    const data = {
      value,
      expire: expire ? Date.now() + expire : null,
    }
    localStorage.setItem(`${PREFIX}${key}`, JSON.stringify(data))
  },

  /**
   * 获取存储
   */
  get<T = any>(key: string): T | null {
    const item = localStorage.getItem(`${PREFIX}${key}`)
    if (!item)
      return null

    try {
      const data = JSON.parse(item)
      // 检查是否过期
      if (data.expire && Date.now() > data.expire) {
        this.remove(key)
        return null
      }
      return data.value
    }
    catch {
      return null
    }
  },

  /**
   * 移除存储
   */
  remove(key: string): void {
    localStorage.removeItem(`${PREFIX}${key}`)
  },

  /**
   * 清空所有存储
   */
  clear(): void {
    const keys = Object.keys(localStorage)
    keys.forEach((key) => {
      if (key.startsWith(PREFIX)) {
        localStorage.removeItem(key)
      }
    })
  },
}
