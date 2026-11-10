/**
 * 单据选择器的列配置（多处复用，别每个页面抄一遍）。
 * 原则：选单据的时候必须看得见「谁、什么货、多少钱、什么状态」，不能只给一个编号。
 */
import type { DocPickColumn } from './DocPicker.vue'

const APPROVE: Record<string, string> = { pending: '待提交', approving: '审批中', approved: '已审批' }
const BIZ: Record<string, string> = {
  pending_inbound: '待入库',
  partial_inbound: '部分入库',
  completed: '已完成',
  pending: '待开始',
  started: '生产中',
  finished: '已完工'
}

export const approveText = (v?: string) => (v ? APPROVE[v] || v : '')
export const bizText = (v?: string) => (v ? BIZ[v] || v : '')

/** 采购订单 */
export const PURCHASE_ORDER_COLS: DocPickColumn[] = [
  { prop: 'orderNo', label: '采购单号', width: '170' },
  { prop: 'supplierName', label: '供应商', minWidth: '140' },
  { prop: 'materialCode', label: '物料编号', width: '130' },
  { prop: 'materialName', label: '物料名称', minWidth: '130' },
  { prop: 'orderDate', label: '订单日期', width: '110' },
  { prop: 'totalAmount', label: '金额', width: '110', align: 'right' },
  { prop: 'approveStatus', label: '审批状态', width: '95', format: (r) => approveText(r.approveStatus) },
  { prop: 'bizStatus', label: '业务状态', width: '95', format: (r) => bizText(r.bizStatus) }
]

/** 销售订单 */
export const SALES_ORDER_COLS: DocPickColumn[] = [
  { prop: 'orderNo', label: '销售单号', width: '170' },
  { prop: 'customerName', label: '客户', minWidth: '140' },
  { prop: 'productCode', label: '产品编号', width: '130' },
  { prop: 'productName', label: '产品名称', minWidth: '130' },
  { prop: 'orderDate', label: '订单日期', width: '110' },
  { prop: 'totalAmount', label: '金额', width: '110', align: 'right' },
  { prop: 'approveStatus', label: '审批状态', width: '95', format: (r) => approveText(r.approveStatus) },
  { prop: 'bizStatus', label: '业务状态', width: '95', format: (r) => bizText(r.bizStatus) }
]

/** 采购/销售发票（结构一样，核销用） */
export const INVOICE_COLS: DocPickColumn[] = [
  { prop: 'invoiceNo', label: '发票号', width: '170' },
  { prop: 'sourceOrderNo', label: '来源订单号', width: '170' },
  { prop: 'invoiceDate', label: '开票日期', width: '110' },
  { prop: 'amount', label: '发票金额', width: '120', align: 'right' },
  { prop: 'verifiedAmount', label: '已核销', width: '110', align: 'right' },
  { prop: 'approveStatus', label: '审批状态', width: '95', format: (r) => approveText(r.approveStatus) }
]

/** 外协单 */
export const OUTSOURCE_ORDER_COLS: DocPickColumn[] = [
  { prop: 'orderNo', label: '外协单号', width: '170' },
  { prop: 'type', label: '类型', width: '95', format: (r) => (r.type === 'whole' ? '整单外协' : '工序外协') },
  { prop: 'orderNo2', label: '生产工单', width: '160', format: (r) => r.productionOrderNo || r.sourceOrderNo || '' },
  { prop: 'supplierName', label: '供应商', minWidth: '130' },
  { prop: 'quantity', label: '数量', width: '90', align: 'right' },
  { prop: 'deliveryDate', label: '交货日期', width: '110' },
  { prop: 'approveStatus', label: '审批状态', width: '95', format: (r) => approveText(r.approveStatus) }
]
