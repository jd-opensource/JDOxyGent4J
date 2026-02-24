// ============ 全局通用泛型工具类型 ============

/**
 * 提取API返回值类型
 * @template T - API函数类型
 * @example
 * type MyApiResponse = ApiResponse<typeof FBApis.default_.post_v2_example>
 */
export type ApiResponse<T extends (...args: any[]) => any> = Awaited<ReturnType<T>>

/**
 * 提取API返回的data字段类型
 * @template T - API函数类型
 * @example
 * type MyApiData = ApiData<typeof FBApis.default_.post_v2_example>
 */
export type ApiData<T extends (...args: any[]) => any> = NonNullable<ApiResponse<T>['data']>

export type ApiDataItem<T extends (...args: any[]) => any> = NonNullable<ApiData<T>>[number]

/**
 * 提取分页列表中的单个项目类型
 * @template T - API函数类型
 * @example
 * type MyListItem = ApiListItem<typeof FBApis.default_.post_v2_example_list>
 */
export type ApiListItem<T extends (...args: any[]) => any> = NonNullable<
  NonNullable<ApiData<T>['items']>[number]
>

/**
 * 提取嵌套对象中的列表项类型
 * @template T - API函数类型
 * @template K - 嵌套对象的key
 * @example
 * type MyNestedItem = ApiNestedListItem<typeof FBApis.default_.post_v2_example, 'items'>
 */
export type ApiNestedListItem<
  T extends (...args: any[]) => any,
  K extends keyof ApiData<T>,
> = ApiData<T>[K] extends (infer U)[] ? U : never
