import request from '../utils/request'

export function getInferenceTaskList(params) {
  return request({
    url: '/inference_tasks',
    method: 'get',
    params
  })
}

export function getInferenceTaskById(id) {
  return request({
    url: '/inference_tasks/' + id,
    method: 'get'
  })
}

export function createInferenceTask(data) {
  return request({
    url: '/inference_tasks',
    method: 'post',
    data
  })
}

export function updateInferenceTask(id, data) {
  return request({
    url: '/inference_tasks/' + id,
    method: 'put',
    data
  })
}

export function deleteInferenceTask(id) {
  return request({
    url: '/inference_tasks/' + id,
    method: 'delete'
  })
}

export function uploadInferenceImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/inference/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function uploadInferenceVideo(file, interval = 2) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('interval', interval)
  return request({
    url: '/inference/upload-video',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000
  })
}
