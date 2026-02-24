import axios from 'axios'

axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'
axios.defaults.headers.post['Content-Type'] = 'application/json; charset=UTF-8'

axios.defaults.baseURL = '/'
axios.defaults.withCredentials = true
axios.defaults.responseType = 'json'

const customAxios = axios.create()

customAxios.interceptors.request.use(
  async (config) => {
    if (!config.params) {
      config.params = {}
    }

    // FormData 时删除 Content-Type，让浏览器自动设置 multipart/form-data 及 boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    return config
  },
  error => Promise.reject(error),
)

customAxios.interceptors.response.use(
  response => response.data,
  (error) => {
    // 保留原始错误信息，由 alova 层统一处理
    return Promise.reject(error)
  },
)

export default customAxios
