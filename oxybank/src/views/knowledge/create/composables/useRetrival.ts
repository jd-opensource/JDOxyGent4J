import type { KBSchema } from '@/api/globals.d'
import Apis from '@/api'

/**
 * 资产库检索相关的 Composable
 *
 * @description
 * 封装资产库 Schema 相关的业务逻辑：
 * - 检查 Schema 是否存在
 * - 创建/更新 Schema
 */
export function useRetrival() {
  /**
   * 校验资产库的 Schema 是否存在
   * @param kbName 资产库名称
   * @returns 是否存在 Schema
   */
  async function checkSchema(kbName: string): Promise<boolean> {
    try {
      const response = await Apis.knowledgeBaseManagement.get_kb_schema_api_v1_kb_base__kb_name__schema_exists_get({
        pathParams: {
          kb_name: kbName,
        },
      })

      return response.data === true
    }
    catch (error) {
      console.error('checkSchema failed', error)
      throw error
    }
  }

  /**
   * 更新/创建资产库 Schema
   * @param kbName 资产库名称
   * @param schema KB Schema 数据
   * @returns 更新结果
   */
  async function updateOrCreateSchema(kbName: string, schema: KBSchema): Promise<string | null> {
    try {
      const response = await Apis.knowledgeBaseManagement.update_kb_schema_api_v1_kb_base__kb_name__schema_post({
        pathParams: {
          kb_name: kbName,
        },
        data: schema,
      })

      return response.data ?? null
    }
    catch (error) {
      console.error('updateOrCreateSchema failed', error)
      throw error
    }
  }

  return {
    checkSchema,
    updateOrCreateSchema,
  }
}
