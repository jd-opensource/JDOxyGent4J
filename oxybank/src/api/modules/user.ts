export interface UserInfo {
  id: string
  username: string
  nickname?: string
  avatar?: string
  email?: string
}

interface LoginResponse {
  token?: string
}

interface ApiResponse<T> {
  data?: T
}

export const userApi = {
  async getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return Promise.resolve({
      data: {
        id: 'mock-user',
        username: 'mock',
      },
    })
  },
  async login(_username: string, _password: string): Promise<ApiResponse<LoginResponse>> {
    return Promise.resolve({
      data: {
        token: 'mock-token',
      },
    })
  },
  async logout(): Promise<ApiResponse<null>> {
    return Promise.resolve({ data: null })
  },
}
