import myAxios from '@/request'

export interface OrderVO {
  id: number
  orderNo: string
  userId?: number
  userAccount?: string
  productType?: string
  productName?: string
  quantity?: number
  amount?: number
  currency?: string
  status?: string
  channel?: string
  createTime?: string
  expireTime?: string
  payTime?: string
  payUrl?: string
  qrCode?: string
  channelParams?: Record<string, unknown>
}

export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export function createOrder(params: {
  productType: string
  productCode: string
  quantity?: number
}): Promise<BaseResponse<OrderVO>> {
  return myAxios
    .post('/payment/order/create', params)
    .then((r) => r.data as BaseResponse<OrderVO>)
}

export function mockPay(params: { id: number }): Promise<BaseResponse<OrderVO>> {
  return myAxios
    .post('/payment/order/mock-pay', params)
    .then((r) => r.data as BaseResponse<OrderVO>)
}

export function getOrder(id: string | number): Promise<BaseResponse<OrderVO>> {
  return myAxios
    .get('/payment/order/get', { params: { id } })
    .then((r) => r.data as BaseResponse<OrderVO>)
}

/** 管理后台分页查询全部订单（仅管理员） */
export function listOrdersAdminByPage(params: {
  pageNum?: number
  pageSize?: number
  orderNo?: string
  userId?: string | number
  productType?: string
  status?: string
}): Promise<BaseResponse<PageOrderVO>> {
  return myAxios
    .post('/payment/order/admin/list/page/vo', params)
    .then((r) => r.data as BaseResponse<PageOrderVO>)
}

/** 管理后台查看订单详情（仅管理员） */
export function adminGetOrder(id: number | string): Promise<BaseResponse<OrderVO>> {
  return myAxios
    .get('/payment/order/admin/get', { params: { id } })
    .then((r) => r.data as BaseResponse<OrderVO>)
}

/** 管理后台取消订单（仅管理员） */
export function adminCancelOrder(id: number): Promise<BaseResponse<boolean>> {
  return myAxios
    .post('/payment/order/admin/cancel', { id })
    .then((r) => r.data as BaseResponse<boolean>)
}

/** 管理后台强制标记已支付（仅管理员） */
export function adminMarkPaid(id: number): Promise<BaseResponse<OrderVO>> {
  return myAxios
    .post('/payment/order/admin/mark-paid', { id })
    .then((r) => r.data as BaseResponse<OrderVO>)
}

export interface PageOrderVO {
  records?: OrderVO[]
  pageNumber?: number
  pageSize?: number
  totalPage?: number
  totalRow?: number
}
