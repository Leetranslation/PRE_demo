import * as XLSX from 'xlsx'

export interface ExportColumn {
  /** 字段名（对应数据里的 key） */
  prop: string
  /** 表头中文名 */
  label: string
  /** 可选的格式化函数 */
  formatter?: (row: any) => string
}

/**
 * 导出数据为 Excel
 * @param columns 列定义（含中文表头）
 * @param rows 数据行
 * @param filename 文件名（不含扩展名）
 */
export function exportExcel(columns: ExportColumn[], rows: any[], filename: string) {
  // 构建表头
  const header = columns.map((c) => c.label)
  // 构建数据
  const data = rows.map((row) =>
    columns.map((c) => {
      if (c.formatter) {
        return c.formatter(row)
      }
      const v = row[c.prop]
      return v === null || v === undefined ? '' : v
    })
  )

  const ws = XLSX.utils.aoa_to_sheet([header, ...data])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, `${filename}.xlsx`)
}
