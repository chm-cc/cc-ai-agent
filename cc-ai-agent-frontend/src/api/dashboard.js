import request from './request'

/**
 * 获取用量统计数据
 * @param {number} days - 统计天数 (7/30/90)
 */
export async function fetchStatistics(days = 30) {
  const res = await request.get('/v1/statistics', { params: { days } })
  return res.data.data
}
