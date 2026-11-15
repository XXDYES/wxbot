<template>
    <div class="task-panel">
        <div v-if="loading" class="task-empty">任务加载中...</div>
        <div v-else-if="loadFailed" class="task-empty">任务加载失败</div>
        <div v-else-if="tasks.length === 0" class="task-empty">暂无定时任务</div>
        <div v-else class="task-list">
            <article v-for="task in tasks" :key="task.id" class="task-item">
                <div class="task-item__header">
                    <div class="task-item__summary">
                        <span class="task-item__name" :title="task.taskName">
                            {{ task.taskName }}
                        </span>
                        <span
                            class="task-item__cron"
                            :title="`Cron: ${task.cronExpression}\n格式：秒 分 时 日 月 周`"
                        >
                            触发:{{ formatCron(task.cronExpression) }}
                        </span>
                    </div>
                    <div class="task-item__actions">
                        <el-button
                            class="task-item__action task-item__action--edit"
                            circle
                            text
                            size="small"
                            aria-label="编辑任务"
                            @click.stop="openEdit(task)"
                        >
                            <el-icon><Edit /></el-icon>
                        </el-button>
                        <el-button
                            class="task-item__action task-item__action--delete"
                            circle
                            text
                            size="small"
                            aria-label="删除任务"
                            :disabled="pendingTaskIds.has(task.id)"
                            @click.stop="deleteTask(task)"
                        >
                            <el-icon><Delete /></el-icon>
                        </el-button>
                    </div>
                    <el-switch
                        class="task-item__switch"
                        size="small"
                        :model-value="task.status === 1"
                        :loading="pendingTaskIds.has(task.id)"
                        :disabled="pendingTaskIds.has(task.id)"
                        @change="toggleTaskStatus(task, $event)"
                    />
                </div>
                <div class="task-item__details">
                    <span class="task-item__type">
                        {{ task.taskType === 1 ? '固定消息' : 'AI 回复' }}
                    </span>
                    <span class="task-item__content" :title="task.content">
                        消息:{{ task.content }}
                    </span>
                </div>
            </article>
        </div>

        <el-dialog
            v-model="dialogVisible"
            class="task-dialog"
            width="620px"
            align-center
            :close-on-click-modal="false"
            :show-close="false"
            append-to-body
            @closed="resetForm"
        >
            <template #header>
                <div class="task-dialog__title">
                    {{ editingId === null ? '新建定时任务' : '编辑定时任务' }}
                </div>
            </template>

            <el-form
                ref="formRef"
                :model="form"
                :rules="rules"
                label-width="82px"
                hide-required-asterisk
            >
                <el-form-item label="任务名称" prop="taskName">
                    <el-input
                        v-model="form.taskName"
                        maxlength="50"
                        show-word-limit
                        placeholder="请输入任务名称"
                        clearable
                    />
                </el-form-item>
                <el-form-item label="任务类型" prop="taskType">
                    <el-select v-model="form.taskType" class="task-dialog__select">
                        <el-option label="发送固定消息" :value="1" />
                        <el-option label="调用 AI 回复" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="触发时间" prop="cronExpression">
                    <el-input
                        v-model="form.cronExpression"
                        placeholder="例如：0 0 15 * * *"
                        clearable
                    />
                    <div class="task-dialog__hint">
                        格式：秒 分 时 日 月 周 示例：0 0 12 * * 1 = 每周一12:00</div>
                </el-form-item>
                <el-form-item label="消息内容" prop="content">
                    <el-input
                        v-model="form.content"
                        type="textarea"
                        :rows="5"
                        resize="none"
                        placeholder="请输入要发送的消息或 AI 提示词"
                    />
                </el-form-item>
                <el-form-item label="启用状态">
                    <el-switch
                        v-model="form.status"
                        :active-value="1"
                        :inactive-value="0"
                        inline-prompt
                        active-text="启用"
                        inactive-text="停用"
                    />
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button class="task-dialog__button task-dialog__button--cancel"
                    @click="dialogVisible = false">
                    取消
                </el-button>
                <el-button class="task-dialog__button task-dialog__button--submit"
                    :loading="submitting" @click="submitForm">
                    {{ editingId === null ? '创建' : '保存' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import taskApi, { type TaskItem } from '@/api/task'
import { Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

interface TaskForm {
    taskName: string
    taskType: number
    cronExpression: string
    content: string
    status: number
}

const tasks = ref<TaskItem[]>([])
const loading = ref(true)
const loadFailed = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const pendingTaskIds = ref(new Set<number>())
const form = reactive<TaskForm>({
    taskName: '',
    taskType: 1,
    cronExpression: '',
    content: '',
    status: 1,
})

const rules: FormRules = {
    taskName: [
        { required: true, whitespace: true, message: '请输入任务名称', trigger: 'blur' },
    ],
    taskType: [
        { required: true, message: '请选择任务类型', trigger: 'change' },
    ],
    cronExpression: [
        { required: true, whitespace: true, message: '请输入触发时间', trigger: 'blur' },
    ],
    content: [
        { required: true, whitespace: true, message: '请输入任务内容', trigger: 'blur' },
    ],
}

const weekNames: Record<string, string> = {
    '0': '日',
    '1': '一',
    '2': '二',
    '3': '三',
    '4': '四',
    '5': '五',
    '6': '六',
    '7': '日',
}

const padTime = (value: string) => value.padStart(2, '0')

const formatCron = (cron: string) => {
    const fields = cron.trim().split(/\s+/)
    if (fields.length !== 6) {
        return cron
    }

    const second = fields[0] ?? ''
    const minute = fields[1] ?? ''
    const hour = fields[2] ?? ''
    const day = fields[3] ?? ''
    const month = fields[4] ?? ''
    const week = fields[5] ?? ''
    if (![second, minute, hour].every((value) => /^\d+$/.test(value))) {
        return cron
    }

    const time = second === '0'
        ? `${padTime(hour)}:${padTime(minute)}`
        : `${padTime(hour)}:${padTime(minute)}:${padTime(second)}`

    if (day === '*' && month === '*' && (week === '*' || week === '?')) {
        return `每天 ${time}`
    }
    if (day === '*' && month === '*' && week === '1-5') {
        return `工作日 ${time}`
    }
    const weekName = weekNames[week]
    if (day === '*' && month === '*' && weekName) {
        return `每周${weekName} ${time}`
    }
    if (/^\d+$/.test(day) && month === '*' && (week === '*' || week === '?')) {
        return `每月${day}日 ${time}`
    }
    return cron
}

const openCreate = () => {
    editingId.value = null
    Object.assign(form, {
        taskName: '',
        taskType: 1,
        cronExpression: '',
        content: '',
        status: 1,
    })
    dialogVisible.value = true
}

const openEdit = (task: TaskItem) => {
    editingId.value = task.id
    Object.assign(form, {
        taskName: task.taskName,
        taskType: task.taskType,
        cronExpression: task.cronExpression,
        content: task.content,
        status: task.status,
    })
    dialogVisible.value = true
}

const resetForm = () => {
    formRef.value?.clearValidate()
}

const submitForm = async () => {
    try {
        await formRef.value?.validate()
    } catch {
        return
    }

    submitting.value = true
    try {
        const payload = { ...form }
        const res = editingId.value === null
            ? await taskApi.createTask(payload)
            : await taskApi.updateTask(editingId.value, payload)
        if (res.data.code !== '200') {
            ElMessage.error(res.data.msg || '定时任务保存失败')
            return
        }
        ElMessage.success(editingId.value === null ? '定时任务创建成功' : '定时任务修改成功')
        dialogVisible.value = false
        await getTasks()
    } catch {
        // 请求拦截器已统一提示错误，弹窗保持打开以便修改。
    } finally {
        submitting.value = false
    }
}

defineExpose({ openCreate })

const setPending = (taskId: number, pending: boolean) => {
    const next = new Set(pendingTaskIds.value)
    if (pending) {
        next.add(taskId)
    } else {
        next.delete(taskId)
    }
    pendingTaskIds.value = next
}

const toggleTaskStatus = async (
    task: TaskItem,
    value: string | number | boolean,
) => {
    const status = value === true ? 1 : 0
    if (status === task.status) {
        return
    }

    setPending(task.id, true)
    try {
        const res = await taskApi.updateTaskStatus(task.id, status)
        if (res.data.code !== '200') {
            ElMessage.error(res.data.msg || '任务状态修改失败')
            await getTasks()
            return
        }
        task.status = status
        // ElMessage.success(status === 1 ? '任务已启用' : '任务已停用')
    } catch {
        await getTasks()
    } finally {
        setPending(task.id, false)
    }
}

const deleteTask = async (task: TaskItem) => {
    try {
        await ElMessageBox.confirm(
            `确定删除任务“${task.taskName}”吗？`,
            '删除确认',
            {
                type: 'warning',
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                showClose: false,
                closeOnClickModal: false,
                customClass: 'task-delete-confirm',
            },
        )
    } catch {
        return
    }

    setPending(task.id, true)
    try {
        const res = await taskApi.deleteTask(task.id)
        if (res.data.code !== '200') {
            ElMessage.error(res.data.msg || '定时任务删除失败')
            return
        }
        tasks.value = tasks.value.filter((item) => item.id !== task.id)
        ElMessage.success('定时任务删除成功')
    } catch {
        // 请求拦截器已统一提示错误。
    } finally {
        setPending(task.id, false)
    }
}

const getTasks = async () => {
    loading.value = true
    loadFailed.value = false
    try {
        const res = await taskApi.getTasks()
        if (res.data.code === '200') {
            tasks.value = res.data.data ?? []
        } else {
            loadFailed.value = true
        }
    } catch {
        loadFailed.value = true
    } finally {
        loading.value = false
    }
}

onMounted(getTasks)
</script>

<style scoped>
.task-panel {
    display: flex;
    flex: 1 1 0;
    flex-direction: column;
    width: 100%;
    height: 100%;
    min-width: 0;
    min-height: 0;
    overflow: hidden;
    align-self: stretch;
}

.task-list {
    display: flex;
    flex: 1 1 0;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    min-height: 0;
    padding: 9px;
    overflow-x: hidden;
    overflow-y: auto;
    box-sizing: border-box;
    border: 1px dashed #d9dde3;
    border-radius: 10px;
    background: rgba(247, 248, 249, 0.95);
    overscroll-behavior: contain;
    contain: layout paint;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

.task-list::-webkit-scrollbar {
    width: 6px;
}

.task-list::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #cbd5e1;
}

.task-item {
    display: flex;
    flex: 0 0 auto;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    padding: 10px 12px;
    overflow: hidden;
    box-sizing: border-box;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #fff;
}

.task-item__header,
.task-item__details {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    min-width: 0;
}

.task-item__summary {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.task-item__name {
    flex: 0 1 auto;
    min-width: 0;
    max-width: 220px;
    overflow: hidden;
    color: #303133;
    font-size: 13px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.task-item__actions {
    display: flex;
    flex: 0 0 auto;
    gap: 2px;
}

.task-item__action {
    width: 24px;
    height: 24px;
    margin: 0;
    padding: 0;
}

.task-item__action.el-button.is-disabled {
    opacity: 0.75;
}

.task-item__action--edit {
    color: #4f46e5;
}

.task-item__action--edit:hover {
    color: #4338ca;
    background: #eef2ff;
}

.task-item__action--delete {
    color: #dc2626;
}

.task-item__action--delete:hover {
    color: #b91c1c;
    background: #fef2f2;
}

.task-item__switch {
    flex: 0 0 auto;
}

.task-item__type {
    flex: 0 0 auto;
    padding: 2px 7px;
    border-radius: 5px;
    color: #4338ca;
    background: #eef2ff;
    font-size: 10px;
}

.task-item__cron {
    flex: 0 1 auto;
    min-width: 0;
    overflow: hidden;
    color: #64748b;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.task-item__content {
    flex: 1 1 0;
    min-width: 0;
    overflow: hidden;
    color: #64748b;
    font-size: 11px;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.task-empty {
    display: grid;
    flex: 1 1 0;
    width: 100%;
    min-height: 0;
    place-items: center;
    border: 1px dashed #d9dde3;
    border-radius: 10px;
    color: #a8abb2;
    background: rgba(247, 248, 249, 0.95);
    font-size: 12px;
}

:global(.task-delete-confirm) {
    width: 380px;
    max-width: calc(100vw - 32px);
    padding: 0;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.85);
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.98);
    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
}

:global(.task-delete-confirm .el-message-box__header) {
    display: flex;
    justify-content: center;
    padding: 22px 24px 8px;
}

:global(.task-delete-confirm .el-message-box__title) {
    width: 100%;
    color: #303133;
    font-size: 18px;
    font-weight: 700;
    text-align: center;
    justify-content: center;
}

:global(.task-delete-confirm .el-message-box__content) {
    padding: 10px 24px 20px;
    color: #606266;
}

:global(.task-delete-confirm .el-message-box__btns) {
    gap: 10px;
    padding: 14px 24px 20px;
    border-top: 1px solid #f0f2f5;
}

:global(.task-delete-confirm .el-button) {
    min-width: 88px;
    height: 38px;
    margin: 0;
    border-radius: 10px;
    font-weight: 600;
}

:global(.task-delete-confirm .el-button:not(.el-button--primary)) {
    border: 0;
    color: #606266;
    background: #f2f3f5;
}

:global(.task-delete-confirm .el-button--primary) {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #f05252, #dc2626);
    box-shadow: 0 6px 16px rgba(220, 38, 38, 0.22);
}

:global(.task-delete-confirm .el-button--primary:hover) {
    color: #fff;
    background: linear-gradient(135deg, #ef4444, #b91c1c);
}

:global(.task-dialog) {
    max-width: calc(100vw - 32px);
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.85);
    border-radius: 25px;
    background: rgba(255, 255, 255, 0.98);
    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
    backdrop-filter: blur(14px);
}

:global(.task-dialog .el-dialog__header) {
    margin: 0;
    padding: 24px 30px 8px;
}

:global(.task-dialog .el-dialog__body) {
    padding: 14px 30px 4px;
}

:global(.task-dialog .el-dialog__footer) {
    padding: 16px 30px 24px;
    border-top: 1px solid #f0f2f5;
}

.task-dialog__title {
    color: #303133;
    font-size: 21px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-align: center;
}

.task-dialog__select {
    width: 100%;
}

.task-dialog__hint {
    width: 100%;
    margin-top: 4px;
    color: #a8abb2;
    font-size: 11px;
    line-height: 1.4;
}

:global(.task-dialog .el-form-item) {
    margin-bottom: 18px;
}

:global(.task-dialog .el-form-item__label) {
    height: 38px;
    align-items: center;
    color: #606266;
    font-weight: 500;
    line-height: 38px;
}

:global(.task-dialog .el-input__wrapper),
:global(.task-dialog .el-select__wrapper),
:global(.task-dialog .el-textarea__inner) {
    border-radius: 10px;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e5e7eb inset;
}

:global(.task-dialog .el-input__wrapper),
:global(.task-dialog .el-select__wrapper) {
    min-height: 38px;
}

:global(.task-dialog .el-textarea__inner) {
    padding: 10px 12px;
    color: #303133;
    font-family: inherit;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

:global(.task-dialog .el-input__wrapper.is-focus),
:global(.task-dialog .el-select__wrapper.is-focused),
:global(.task-dialog .el-textarea__inner:focus) {
    background: #fff;
    box-shadow:
        0 0 0 1px #06b6d4 inset,
        0 0 0 4px rgba(6, 182, 212, 0.08);
}

:global(.task-dialog .task-dialog__button) {
    min-width: 92px;
    height: 38px;
    border-radius: 10px;
    font-weight: 600;
}

:global(.task-dialog .task-dialog__button--cancel) {
    border: 0;
    color: #606266;
    background: #f2f3f5;
}

:global(.task-dialog .task-dialog__button--submit) {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #06b6d4);
    box-shadow: 0 6px 16px rgba(79, 70, 229, 0.22);
}

:global(.task-dialog .task-dialog__button--submit:hover) {
    color: #fff;
    background: linear-gradient(135deg, #6366f1, #22d3ee);
}
</style>
