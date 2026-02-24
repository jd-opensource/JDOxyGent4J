/**
 * 资产库创建 Composable
 *
 * @description
 * 封装资产库创建流程的所有业务逻辑，包括：
 * - 创建资产库
 * - 上传文件
 * - 更新 Schema
 * - 导入文件（不携带 Schema 参数）
 */

import { message } from 'ant-design-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRetrival } from './useRetrival'
import type { ChunkConfig, ColumnSchema, KnowledgeBaseInfo, RetrievalConfig } from '@/views/knowledge/types'
import { defaultChunkConfig, defaultRetrievalConfig } from '@/views/knowledge/types'
import { useI18n } from '@/locales'
import Apis from '@/api'

export function useKnowledgeCreate() {
  const { t } = useI18n()
  const router = useRouter()
  const route = useRoute()

  // 状态
  const loading = ref(false)
  const currentStep = ref(0)

  // 数据
  const file = ref<File | null>(null)
  const fileName = ref<string>('')
  const fileType = ref<string>('')
  const kbId = ref<string>(typeof route.query.kb_id === 'string' ? route.query.kb_id : '')
  const fileId = ref<string>('')
  const filePath = ref<string>('')
  const kbInfo = ref<KnowledgeBaseInfo>({
    kb_name: '',
    kb_type: 'structured',
  })
  const schemaColumns = ref<ColumnSchema[]>([])
  const retrievalConfig = ref<RetrievalConfig>(JSON.parse(JSON.stringify(defaultRetrievalConfig)))
  const chunkConfig = ref<ChunkConfig>({ ...defaultChunkConfig })
  const kbSchemaExists = ref(false)
  const loadingSchema = ref(false)
  const shouldCreateQueryInterface = ref(false)
  const schemaChecked = ref(!kbId.value)

  const { checkSchema, updateOrCreateSchema } = useRetrival()

  const baseUnstructuredSchema = {
    fields: [
      {
        field_name: 'chunk_to_return',
        field_type: 'string',
        field_desc: '',
      },
      {
        field_name: 'chunk_to_emb',
        field_type: 'string',
        field_desc: '',
      },
    ],
    match_rules: [
      {
        match_policies: [
          {
            mode: 'es_text',
            input_fields: [
              'chunk_to_emb',
            ],
          },
        ],
        output_fields: [
          'chunk_to_return',
        ],
      },
      {
        match_policies: [
          {
            mode: 'vearch_vector',
            input_fields: [
              'chunk_to_emb',
            ],
            embedding_model: 'default',
          },
        ],
        output_fields: [
          'chunk_to_return',
        ],
      },
    ],
  }

  function buildParserConfig(config: ChunkConfig) {
    return {
      parser_type: config.parserType,
      chunk_size: config.chunkSize,
      chunk_overlap: config.chunkOverlap,
      separator: config.separator,
      splitter_type: 'sentence',
      include_metadata: true,
      include_prev_next_rel: true,
    }
  }

  // 是否为导入模式 (已有资产库 ID)
  const isImportMode = computed(() => !!kbId.value)

  // 动态步骤配置
  // 根据 kb_type 和 kbSchemaExists 返回不同的步骤
  const steps = computed(() => {
    // Schema 已存在时，只需要一步：上传文件
    if (kbSchemaExists.value) {
      return [
        { title: t('上传文件') },
      ]
    }

    const baseStep = { title: t('选择数据源') }

    // Schema 不存在时，需要完整流程
    if (kbInfo.value.kb_type === 'unstructured') {
      return [
        baseStep,
        { title: t('分段设置') },
      ]
    }

    // Structured，三步流程
    return [
      baseStep,
      { title: t('结构化配置') },
      { title: t('检索策略') },
    ]
  })

  // 初始化
  onMounted(() => {
    const queryKbId = route.query.kb_id as string
    if (queryKbId) {
      kbId.value = queryKbId
      schemaChecked.value = false
      loadKnowledgeBaseInfo(queryKbId)
    }
  })

  watch(
    () => route.query.kb_id,
    (newKbId) => {
      if (typeof newKbId === 'string') {
        kbId.value = newKbId
        schemaChecked.value = false
        loadKnowledgeBaseInfo(newKbId)
      }
    },
  )

  async function refreshSchemaStatus(targetKbName?: string) {
    const name = targetKbName || kbInfo.value.kb_name
    if (!name) {
      kbSchemaExists.value = false
      schemaChecked.value = true
      return
    }
    loadingSchema.value = true
    try {
      kbSchemaExists.value = await checkSchema(name)
    }
    catch (error) {
      kbSchemaExists.value = false
      console.error('检查资产库 Schema 状态失败', error)
    }
    finally {
      loadingSchema.value = false
      schemaChecked.value = true
    }
  }

  /**
   * 根据 kb_id 获取资产库信息
   */
  async function loadKnowledgeBaseInfo(id: string) {
    try {
      const res = await Apis.knowledgeBaseManagement.get_all_knowledge_base_api_v1_kb_base_get({
        params: {
          page: 1,
          size: 200,
        },
      })
      const target = res.data?.items?.find(item => item.kb_id === id)
      if (target) {
        kbInfo.value = {
          kb_name: target.kb_name || '',
          kb_type: target.kb_type || 'structured',
        }
        await refreshSchemaStatus(target.kb_name || '')
      }
    }
    catch (error) {
      console.error('获取资产库信息失败', error)
    }
  }

  /**
   * 返回资产库列表
   */
  function goBack() {
    if (isImportMode.value) {
      router.push(`/knowledge/${kbId.value}`)
    }
    else {
      router.push('/knowledge')
    }
  }

  /**
   * 创建资产库（保留供空资产库创建使用）
   * 返回格式: { code: 200, msg: "创建成功", data: "kb_id" }
   */
  async function createKnowledgeBase(info: KnowledgeBaseInfo): Promise<string | null> {
    const createRes = await Apis.knowledgeBaseManagement.create_knowledge_base_api_v1_kb_base_post({
      data: {
        kb_name: info.kb_name,
        kb_type: info.kb_type,
      },
    })
    return createRes.data || null
  }

  /**
   * 构建 KB Schema
   */
  function buildKbSchema(columns: ColumnSchema[], retrievalConfig?: RetrievalConfig) {
    const kbSchema: Record<string, any> = {
      fields: columns.map(col => ({
        field_name: col.name,
        field_type: col.type,
        field_desc: col.description,
      })),
    }

    if (retrievalConfig && retrievalConfig.length > 0) {
      kbSchema.match_rules = retrievalConfig.map((rule) => {
        const matchPolicies: any[] = []

        // 精确匹配策略
        rule.preciseStrategies
          .filter(s => (s.input_fields || []).length > 0)
          .forEach((s) => {
            matchPolicies.push({
              mode: 'precise',
              input_fields: s.input_fields || [],
            })
          })

        // 主策略
        if (rule.mainStrategy.type === 'es') {
          matchPolicies.push({
            mode: 'es_text',
            input_fields: rule.mainStrategy.input_fields || [],
          })
        }
        else if (rule.mainStrategy.type === 'vearch') {
          matchPolicies.push({
            mode: 'vearch_vector',
            input_fields: rule.mainStrategy.input_fields || [],
            embedding_model: rule.mainStrategy.embedding_model,
          })
        }

        return {
          match_policies: matchPolicies,
          output_fields: rule.output_fields || [],
        }
      })
    }

    return kbSchema
  }

  /**
   * 导入文件数据（不再传递 kb_schema）
   * @param options 导入选项
   * @param options.kbId 资产库 ID
   * @param options.fileId 文件 ID
   * @param options.filePath 文件路径
   * @param options.fileType 文件类型
   */
  async function ingestFile(options: {
    kbId: string
    fileId: string
    filePath: string
    fileType: string
    fileName?: string
    kbName?: string
  }): Promise<void> {
    const {
      kbId: kbIdParam,
      fileId: fileIdParam,
      filePath: filePathParam,
      fileType: fileTypeParam,
      fileName: fileNameParam,
      // kbName,
    } = options

    const resolvedFileName = fileNameParam
      || fileName.value
      || file.value?.name
      || filePathParam.split('/').pop()
      || ''

    const requestData: any = {
      file_upload_info: {
        file_id: fileIdParam,
        file_type: fileTypeParam,
        file_path: filePathParam,
        file_name: resolvedFileName,
      },
    }

    await Apis.knowledgeBaseFileManagement.ingest_kb_file_api_v1_kb_base__kb_id__ingest_file_post({
      pathParams: { kb_id: kbIdParam } as any,
      data: requestData,
    })
    // TODO: 暂时无需支持query_interface接口
    // const targetKbName = kbName || kbInfo.value.kb_name
    // if (shouldCreateQueryInterface.value && targetKbName) {
    //   await Apis.general.create_kb_query_interface_api_v1_query_interface__kb_name__post(
    //     {
    //       pathParams: {
    //         kb_name: targetKbName,
    //       },
    //     },
    //   )
    //   shouldCreateQueryInterface.value = false
    // }
  }

  /**
   * 上传结果数据类型
   */
  interface UploadResultData {
    info: KnowledgeBaseInfo
    file: File
    kbId: string
    fileId: string
    filePath: string
    fileName: string
    fileType: string
  }

  interface ChunkNextPayload {
    config: ChunkConfig
    schemaExists?: boolean
  }

  /**
   * Step 1 -> Step 2: 接收已上传的文件信息，进入下一步
   * 文件上传已在 StepDataSource 组件中完成
   *
   * 流程逻辑：
   * - 导入模式：先检查 schema 是否存在，然后进入下一步
   * - 新建模式：schema 不存在，直接进入下一步
   *
   * UI 会根据 kbSchemaExists 状态展示不同的界面
   */
  async function handleDataSourceNext(data: UploadResultData) {
    // 保存所有数据
    kbInfo.value = isImportMode.value
      ? {
          kb_name: kbInfo.value.kb_name || data.info.kb_name,
          kb_type: kbInfo.value.kb_type || data.info.kb_type,
        }
      : data.info
    file.value = data.file
    kbId.value = data.kbId
    fileId.value = data.fileId
    filePath.value = data.filePath
    fileName.value = data.fileName
    fileType.value = data.fileType

    // 检查 schema 是否存在
    // 只有在导入模式（已有资产库）下才需要检查
    if (isImportMode.value) {
      // 等待 schema 检查完成后再进入下一步
      await refreshSchemaStatus(kbInfo.value.kb_name)
    }
    else {
      // 新建资产库，schema 不存在
      kbSchemaExists.value = false
      schemaChecked.value = true
    }

    shouldCreateQueryInterface.value = !kbSchemaExists.value

    // Schema 存在时，直接执行导入逻辑
    if (kbSchemaExists.value) {
      await handleQuickImport()
    }
    else {
      // 进入下一步，UI 会根据 kbSchemaExists 展示不同界面
      currentStep.value = 1
    }
  }

  /**
   * Schema 存在时的快速导入（仅导入文件）
   * 用于 UI 上的"确认导入"按钮
   */
  async function handleQuickImport() {
    loading.value = true
    try {
      await ingestFile({
        kbId: kbId.value,
        fileId: fileId.value,
        filePath: filePath.value,
        fileType: fileType.value || fileName.value.split('.').pop() || '',
      })

      message.success(t('文件导入成功'))
      goBack()
    }
    catch (error) {
      console.error('导入失败', error)
      message.error(t('导入失败: {message}', { message: (error as any).message || String(error) }))
    }
    finally {
      loading.value = false
    }
  }

  /**
   * 创建空资产库
   */
  async function handleCreateEmpty(info: KnowledgeBaseInfo) {
    loading.value = true
    try {
      await createKnowledgeBase(info)
      message.success(t('空资产库创建成功'))
      router.push('/knowledge')
    }
    catch (error) {
      console.error(error)
      message.error(t('创建失败: {message}', { message: (error as any).message || String(error) }))
    }
    finally {
      loading.value = false
    }
  }

  /**
   * Step 2 (Structured) -> Step 3: 结构配置完成
   */
  function handleStructureNext(columns: ColumnSchema[]) {
    if (kbInfo.value.kb_type !== 'structured') {
      message.error(t('当前数据源类型为非结构化，无法进行结构化配置'))
      return
    }
    schemaColumns.value = columns
    currentStep.value = 2
  }

  /**
   * Step 2 (Unstructured) -> Finish: 分段配置完成并处理
   */
  async function handleChunkNext(payload: ChunkConfig | ChunkNextPayload) {
    if (kbInfo.value.kb_type !== 'unstructured') {
      message.error(t('当前数据源类型为结构化，无法进行分段配置'))
      return
    }
    const nextConfig = 'config' in payload ? payload.config : payload
    const schemaExists = ('config' in payload ? payload.schemaExists : undefined) ?? kbSchemaExists.value

    kbSchemaExists.value = !!schemaExists
    chunkConfig.value = nextConfig
    loading.value = true

    try {
      const isSchemaMissing = !schemaExists && kbInfo.value.kb_type === 'unstructured'

      // 非结构化数据：schema 不存在时先创建/更新 schema，再导入文件
      if (isSchemaMissing) {
        if (!kbInfo.value.kb_name)
          throw new Error(t('资产库名称缺失，无法更新 Schema'))
        const unstructuredSchema = {
          ...baseUnstructuredSchema,
          parser_config: buildParserConfig(nextConfig),
        }
        await updateOrCreateSchema(kbInfo.value.kb_name, unstructuredSchema as any)
        kbSchemaExists.value = true
      }

      // 非结构化数据导入（仅导入文件）
      await ingestFile({
        kbId: kbId.value,
        fileId: fileId.value,
        filePath: filePath.value,
        fileType: fileType.value || fileName.value.split('.').pop() || '',
      })

      message.success(t('文件导入并开始处理'))
      goBack()
    }
    catch (error) {
      console.error(error)
      message.error(t('处理失败: {message}', { message: (error as any).message || String(error) }))
    }
    finally {
      loading.value = false
    }
  }

  /**
   * 返回上一步
   */
  function handlePrev() {
    if (currentStep.value === 1) {
      // 如果是第一步（上传文件后），返回则重置文件
      // 如果是导入模式，直接返回即可（因为文件已上传但未处理，是否需要删除？暂不）
      currentStep.value = 0
      // kbId 保留
      fileId.value = ''
    }
    else if (currentStep.value === 2) {
      currentStep.value = 1
    }
  }

  /**
   * 从 Step 3 返回上一步，同时保存检索配置
   */
  function handleRetrievalPrev(config: RetrievalConfig) {
    retrievalConfig.value = config
    currentStep.value = 1
  }

  /**
   * Step 3 (Structured) -> 完成: 导入文件（包含检索配置）
   */
  async function handleRetrievalComplete(config: RetrievalConfig) {
    if (kbInfo.value.kb_type !== 'structured') {
      message.error(t('当前数据源类型为非结构化，无法配置检索策略'))
      return
    }
    retrievalConfig.value = config
    loading.value = true

    try {
      const kbSchema = buildKbSchema(schemaColumns.value, config)
      const schemaAlreadyExists = kbSchemaExists.value

      // 如果 schema 不存在，先创建/更新 schema
      if (!schemaAlreadyExists) {
        if (!kbInfo.value.kb_name)
          throw new Error(t('资产库名称缺失，无法更新 Schema'))
        await updateOrCreateSchema(kbInfo.value.kb_name, kbSchema as any)
        kbSchemaExists.value = true
      }

      // 导入文件（仅导入文件）
      await ingestFile({
        kbId: kbId.value,
        fileId: fileId.value,
        filePath: filePath.value,
        fileType: fileType.value || fileName.value.split('.').pop() || '',
      })

      message.success(t('导入成功'))
      goBack()
    }
    catch (error) {
      console.error(error)
      message.error(t('处理失败: {message}', { message: (error as any).message || String(error) }))
    }
    finally {
      loading.value = false
    }
  }

  return {
    // 状态
    loading,
    currentStep,
    steps,
    isImportMode,

    // 数据
    file,
    fileName,
    fileType,
    kbId,
    fileId,
    filePath,
    kbInfo,
    schemaColumns,
    retrievalConfig,
    chunkConfig,
    kbSchemaExists,
    loadingSchema,
    schemaChecked,
    showStepsBar: computed(() => schemaChecked.value && steps.value.length > 1),

    // 方法
    goBack,
    handleDataSourceNext,
    handleCreateEmpty,
    handleStructureNext,
    handleChunkNext,
    handlePrev,
    handleRetrievalPrev,
    handleRetrievalComplete,
    handleQuickImport, // Schema 存在时的快速导入
  }
}
