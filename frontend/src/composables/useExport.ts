import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportExcel, type ExportColumn } from '@/utils/export'

/**
 * 通用导出逻辑
 * @param columns 导出列定义
 * @param filename 导出文件名（不含扩展名）
 * @returns selection（勾选数组）、onSelectionChange、handleExport
 */
export function useExport(columns: ExportColumn[], filename: string) {
  const selection = ref<any[]>([])

  function onSelectionChange(rows: any[]) {
    selection.value = rows
  }

  function handleExport(records: any[]) {
    const data = selection.value.length ? selection.value : records
    if (!data.length) {
      ElMessage.warning('没有可导出的数据')
      return
    }
    exportExcel(columns, data, filename)
    ElMessage.success(`已导出 ${data.length} 条数据`)
  }

  return { selection, onSelectionChange, handleExport }
}
