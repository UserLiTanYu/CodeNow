import request from '@/utils/request'

/** 查询管理员后台维护的站点公开简介。 */
export const getAdminSiteProfile = () => request.get('/admin/site-profile')

/** 更新管理员在博客首页展示的个人简介。 */
export const updateAdminSiteProfile = (data) => request.put('/admin/site-profile', data)
