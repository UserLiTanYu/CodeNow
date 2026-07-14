import request from '@/utils/request'

export const getTags = () => request.get('/tags')
export const createTag = (data) => request.post('/tags', data)
export const deleteTag = (id) => request.delete(`/tags/${id}`)
