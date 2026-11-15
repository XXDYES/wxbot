import request from './request'

export interface TaskItem {
    id: number
    userId: number
    taskType: number
    taskName: string
    cronExpression: string
    status: number
    content: string
    createAt: string
}

export interface TaskPayload {
    taskName: string
    taskType: number
    cronExpression: string
    content: string
    status: number
}

interface ApiResult<T> {
    code: string
    data: T
    msg?: string
}

const taskApi = {
    getTasks() {
        return request.get<ApiResult<TaskItem[]>>('/tasks')
    },
    createTask(payload: TaskPayload) {
        return request.post<ApiResult<TaskItem>>('/tasks', payload)
    },
    updateTask(id: number, payload: TaskPayload) {
        return request.put<ApiResult<TaskItem>>(`/tasks/${id}`, payload)
    },
    updateTaskStatus(id: number, status: number) {
        return request.patch<ApiResult<void>>(`/tasks/${id}/status`, { status })
    },
    deleteTask(id: number) {
        return request.delete<ApiResult<void>>(`/tasks/${id}`)
    },
}

export default taskApi
