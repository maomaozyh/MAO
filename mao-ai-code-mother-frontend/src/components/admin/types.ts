/**
 * 通用后台表格组件的列/筛选项类型定义
 *
 * 放在独立文件而不是 AdminTable.vue 的 <script setup> 里，
 * 因为 <script setup> 不允许 ES 模块导出，各管理页需要 import 这些类型。
 */
export interface AdminColumn {
  title: string
  dataIndex: string
  key?: string
  /** 渲染类型，不传则按纯文本渲染 */
  type?: 'text' | 'image' | 'time' | 'tag' | 'money'
  width?: number | string
  ellipsis?: boolean
  fixed?: 'left' | 'right' | boolean
  /** type=time 时的格式，默认 YYYY-MM-DD HH:mm:ss */
  format?: string
  /** type=tag 时的 值 → {文案, 颜色} 映射 */
  tags?: { value: any; label: string; color: string }[]
  /** type=money 时的除数，默认 100（分 → 元） */
  divide?: number
  /** 自定义渲染函数，优先级高于 type */
  customRender?: (params: { record: any; text: any; index: number }) => any
}

export interface AdminFilter {
  label: string
  field: string
  type?: 'input' | 'select'
  options?: { label: string; value: any }[]
  placeholder?: string
}
