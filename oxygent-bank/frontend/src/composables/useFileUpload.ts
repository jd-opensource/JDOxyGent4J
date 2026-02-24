import { ref } from 'vue'
import Apis from '@/api'
import { t } from '@/locales'

export interface UploadFileItem {
  id: string
  name: string
  size: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  progress: number
  file: File
  binaryContent?: ArrayBuffer
  // 上传成功后的响应数据
  fileId?: string
  filePath?: string
  md5?: string
  errorMessage?: string
}

export interface UploadResult {
  fileId: string
  filePath: string
  fileName: string
  fileType: string
  fileSize: number
  md5: string
  uploadTime: string
}

export function readFileAsBinary(file: File): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result
      if (!result) {
        reject(new Error(t('文件读取失败')))
        return
      }
      resolve(result instanceof ArrayBuffer ? result : new TextEncoder().encode(String(result)).buffer)
    }
    reader.onerror = () => reject(reader.error || new Error(t('文件读取失败')))
    reader.readAsArrayBuffer(file)
  })
}

export async function toBinaryFile(file: File): Promise<{ file: File, binary: ArrayBuffer }> {
  const binary = await readFileAsBinary(file)
  const normalizedFile = new File([binary], file.name, { type: file.type })
  return { file: normalizedFile, binary }
}

export function useFileUpload() {
  const fileList = ref<UploadFileItem[]>([])
  const uploading = ref(false)

  /**
   * 添加文件到待上传列表（不自动上传）
   */
  async function addFiles(files: File[] | FileList) {
    const newFiles: UploadFileItem[] = []

    for (const file of Array.from(files)) {
      let binaryContent: ArrayBuffer | undefined
      try {
        const { binary, file: normalized } = await toBinaryFile(file)
        binaryContent = binary
        newFiles.push({
          id: `${Date.now()}_${Math.random().toString(36).slice(2)}`,
          name: normalized.name,
          size: normalized.size,
          status: 'pending' as const,
          progress: 0,
          file: normalized,
          binaryContent,
        })
      }
      catch (error) {
        console.error('文件读取失败:', error)
        newFiles.push({
          id: `${Date.now()}_${Math.random().toString(36).slice(2)}`,
          name: file.name,
          size: file.size,
          status: 'pending' as const,
          progress: 0,
          file,
        })
      }
    }

    fileList.value.push(...newFiles)
    return newFiles
  }

  /**
   * 上传单个文件到资产库
   */
  async function uploadSingleFile(kbId: string, fileItem: UploadFileItem): Promise<UploadResult | null> {
    fileItem.status = 'uploading'
    fileItem.progress = 0

    try {
      // 模拟进度更新（实际进度取决于 axios 配置）
      const progressInterval = setInterval(() => {
        if (fileItem.progress < 90) {
          fileItem.progress += 10
        }
      }, 100)

      const res = await Apis.knowledgeBaseFileManagement.upload_kb_file_api_v1_kb_base__kb_id__upload_file_post({
        pathParams: { kb_id: kbId },
        data: { file: fileItem.file },
      })

      clearInterval(progressInterval)
      fileItem.progress = 100
      fileItem.status = 'success'

      // 提取响应数据
      const data = res.data as any // 类型断言，确保访问后端返回的字段
      if (data) {
        fileItem.fileId = data.file_id
        fileItem.filePath = data.file_path
        fileItem.md5 = data.md5

        return {
          fileId: data.file_id || '',
          filePath: data.file_path || '',
          fileName: data.file_name || fileItem.name,
          fileType: data.file_type || '',
          fileSize: data.file_size || 0,
          md5: data.md5 || '',
          uploadTime: data.upload_time || '',
        }
      }

      return null
    }
    catch (error) {
      fileItem.status = 'error'
      fileItem.errorMessage = (error as Error).message || t('上传失败')
      console.error('文件上传失败:', error)
      return null
    }
  }

  /**
   * 批量上传所有待上传的文件
   */
  async function uploadAllFiles(kbId: string): Promise<UploadResult[]> {
    const pendingFiles = fileList.value.filter(f => f.status === 'pending')

    if (pendingFiles.length === 0) {
      return []
    }

    uploading.value = true
    const results: UploadResult[] = []

    try {
      for (const fileItem of pendingFiles) {
        const result = await uploadSingleFile(kbId, fileItem)
        if (result) {
          results.push(result)
        }
      }
    }
    finally {
      uploading.value = false
    }

    return results
  }

  /**
   * 删除文件
   */
  function removeFile(id: string) {
    const index = fileList.value.findIndex(f => f.id === id)
    if (index !== -1) {
      fileList.value.splice(index, 1)
    }
  }

  /**
   * 清空文件列表
   */
  function clearFiles() {
    fileList.value = []
  }

  /**
   * 格式化文件大小
   */
  function formatFileSize(bytes: number): string {
    if (bytes === 0)
      return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return `${(bytes / k ** i).toFixed(2)} ${sizes[i]}`
  }

  /**
   * 检查是否有上传中的文件
   */
  function hasUploadingFiles(): boolean {
    return fileList.value.some(f => f.status === 'uploading')
  }

  /**
   * 检查是否有上传失败的文件
   */
  function hasErrorFiles(): boolean {
    return fileList.value.some(f => f.status === 'error')
  }

  /**
   * 获取所有上传成功的文件结果
   */
  function getSuccessfulUploads(): UploadResult[] {
    return fileList.value
      .filter(f => f.status === 'success' && f.fileId && f.filePath)
      .map(f => ({
        fileId: f.fileId!,
        filePath: f.filePath!,
        fileName: f.name,
        fileType: f.name.split('.').pop() || '',
        fileSize: f.size,
        md5: f.md5 || '',
        uploadTime: '', // 列表中可能未保存完整时间，可根据需求扩展 UploadFileItem
      }))
  }

  return {
    fileList,
    uploading,
    addFiles,
    uploadSingleFile,
    uploadAllFiles,
    removeFile,
    clearFiles,
    formatFileSize,
    hasUploadingFiles,
    hasErrorFiles,
    getSuccessfulUploads,
    readFileAsBinary, // 暴露给外部使用
  }
}
