<template>
    <div class="library-section">
        <div class="library-head">
            <span class="library-label">知识库</span>
            <el-button class="library-add-button" circle size="small" aria-label="添加知识库"
             @click="openCreate">
                +
            </el-button>
        </div>

        <div class="library-panel">
            <div class="library-empty" v-if="itemList.length === 0">暂无知识库</div>
            <div v-for="item in itemList" :key="item.id" class="library-item">
                <div class="library-item__actions">
                    <el-button
                        class="library-item__action library-item__action--edit"
                        circle
                        text
                        aria-label="编辑知识库"
                        @click.stop="openEdit(item)"
                    >
                        <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button
                        class="library-item__action library-item__action--delete"
                        circle
                        text
                        aria-label="删除知识库"
                        @click.stop="deleteItem(item)"
                    >
                        <el-icon><Delete /></el-icon>
                    </el-button>
                </div>
                <div class="library-item__icon">📖</div>
                <div class="library-item__title">{{ item.title || '未命名' }}</div>
            </div>
        </div>

        <el-dialog v-model="dialogVisible" class="knowledge-dialog" width="700px" align-center
            :close-on-click-modal="false" :show-close="false" @closed="resetForm" append-to-body>
            <template #header>
                <div class="dialog-title">{{ editingId === null ? '新建知识库' : '编辑知识库' }}</div>
            </template>

            <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" hide-required-asterisk>
                <el-form-item label="标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入知识库标题" clearable />
                </el-form-item>
                <el-form-item label="描述" prop="description">
                    <el-input v-model="form.description" placeholder="请输入知识库描述(可空)" clearable />
                </el-form-item>
                <el-form-item label="内容" prop="content">
                    <el-input v-model="form.content" type="textarea" :rows="11" resize="none"
                     placeholder="请输入知识库内容" maxlength="10000" show-word-limit/>
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button class="dialog-button dialog-button--cancel" @click="dialogVisible = false">
                    取消
                </el-button>
                <el-button class="dialog-button dialog-button--submit" @click="confirm">
                    保存
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import userApi from '@/api/user'
import { Delete, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref ,onMounted} from 'vue'

interface LibraryItem {
    id: number
    userId: number
    title: string
    description: string | null
    content: string
    createAt: string
}

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const itemList = ref<LibraryItem[]>([])
const formRef = ref<FormInstance>()
const form = reactive({ title: '', description: '', content: '' })

const rules: FormRules = {
    title: [{ required: true, whitespace: true, message: '请输入标题', trigger: 'blur' }],
    content: [{ required: true, whitespace: true, message: '请输入内容', trigger: 'blur' }],
}

const openCreate = () => {
    editingId.value = null
    Object.assign(form, { title: '', description: '', content: '' })
    dialogVisible.value = true
}

const openEdit = (item: LibraryItem) => {
    editingId.value = item.id
    Object.assign(form, {
        title: item.title,
        description: item.description || '',
        content: item.content,
    })
    dialogVisible.value = true
}

const deleteItem = async (item: LibraryItem) => {
    try {
        await ElMessageBox.confirm(
            `确定删除知识库“${item.title || '未命名'}”吗？`,
            '删除确认',
            {
                type: 'warning',
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                showClose: false,
                closeOnClickModal: false,
                customClass: 'library-delete-confirm',
            }
        )
    } catch {
        return
    }

    const res = await userApi.deleteLibrary(item.id)
    if (res.data.code == "200") {
        ElMessage.success('知识库删除成功')
        await getLibrary()
    } else {
        ElMessage.error(res.data.msg)
    }
}

const getLibrary = async () => {
    const res = await userApi.getLibrary()
    if (res.data.code == "200") {
        itemList.value = res.data.data
    } else { ElMessage.error(res.data.msg) }
}
const confirm = async () => {
    try {
        await formRef.value?.validate()
    } catch {
        return
    }

    const res = editingId.value === null
        ? await userApi.addLibrary(form)
        : await userApi.updateLibrary({ id: editingId.value, ...form })
    if (res.data.code == "200") {
        ElMessage.success(editingId.value === null ? '知识库添加成功' : '知识库修改成功')
        dialogVisible.value = false
        await getLibrary()
    } else {
        ElMessage.error(res.data.msg)
    }
}

const resetForm = () => {
    formRef.value?.resetFields()
}
onMounted(() => {
    getLibrary()
})
</script>

<style scoped>
.library-section {
    display: flex;
    flex-direction: column;
    gap: 7px;
    width: 100%;
    min-width: 0;
}

.library-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.library-label {
    color: #303133;
    font-size: 14px;
    font-weight: 600;
}

.library-add-button {
    width: 24px;
    height: 24px;
    padding: 0;
    border-color: #e5e7eb;
    color: #4f46e5;
    background: #dfdfdf;
    font-size: 16px;
    line-height: 1;
}

.library-add-button:hover {
    border-color: #c7d2fe;
    color: #4338ca;
    background: #eef2ff;
}

.library-panel {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: flex-start;
    gap: 12px;
    width: 100%;
    min-width: 0;
    max-width: 100%;
    min-height: 112px;
    padding: 8px;
    box-sizing: border-box;
    overflow-x: auto;
    border: 1px dashed #d9dde3;
    border-radius: 10px;
    background: rgba(247, 248, 249, 0.95);
}

.library-item {
    position: relative;
    display: flex;
    flex: 0 0 124px;
    height: 96px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    padding: 24px 8px 9px;
    overflow: hidden;
    border: 1.5px solid #cbd5e1;
    border-radius: 12px;
    background: #fff;
}

.library-item__actions {
    position: absolute;
    top: 5px;
    right: 5px;
    display: flex;
    gap: 2px;
}

.library-item__action {
    width: 24px;
    height: 24px;
    margin: 0;
    padding: 0;
    border: 0;
    background: transparent;
}

.library-item__action--delete {
    color: #dc2626;
}

.library-item__action--delete:hover {
    color: #b91c1c;
    background: #fef2f2;
}

.library-item__action--edit {
    color: #4f46e5;
}

.library-item__action--edit:hover {
    color: #4338ca;
    background: #eef2ff;
}

.library-item__icon {
    font-size: 30px;
    line-height: 1;
}

.library-item__title {
    width: 100%;
    margin-top: 6px;
    overflow: hidden;
    color: #303133;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.3;
    text-align: center;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.library-empty {
    width: 100%;
    color: #a8abb2;
    font-size: 12px;
    text-align: center;
}

.library-panel {
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

.library-panel::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}

.library-panel::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #cbd5e1;
}

.library-panel::-webkit-scrollbar-track {
    background: transparent;
}

.dialog-title {
    color: #303133;
    font-size: 21px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-align: center;
}

:global(.library-delete-confirm) {
    width: 380px;
    max-width: calc(100vw - 32px);
    padding: 0;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.85);
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.98);
    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
}

:global(.library-delete-confirm .el-message-box__header) {
    display: flex;
    justify-content: center;
    padding: 22px 24px 8px;
}

:global(.library-delete-confirm .el-message-box__title) {
    width: 100%;
    color: #303133;
    font-size: 18px;
    font-weight: 700;
    text-align: center;
    justify-content: center;
}

:global(.library-delete-confirm .el-message-box__content) {
    padding: 10px 24px 20px;
    color: #606266;
}

:global(.library-delete-confirm .el-message-box__btns) {
    gap: 10px;
    padding: 14px 24px 20px;
    border-top: 1px solid #f0f2f5;
}

:global(.library-delete-confirm .el-button) {
    min-width: 88px;
    height: 38px;
    margin: 0;
    border-radius: 10px;
    font-weight: 600;
}

:global(.library-delete-confirm .el-button:not(.el-button--primary)) {
    border: 0;
    color: #606266;
    background: #f2f3f5;
}

:global(.library-delete-confirm .el-button--primary) {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #f05252, #dc2626);
    box-shadow: 0 6px 16px rgba(220, 38, 38, 0.22);
}

:global(.library-delete-confirm .el-button--primary:hover) {
    color: #fff;
    background: linear-gradient(135deg, #ef4444, #b91c1c);
}

:global(.knowledge-dialog) {
    max-width: calc(100vw - 32px);
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.85);
    border-radius: 25px;
    background: rgba(255, 255, 255, 0.97);
    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
    backdrop-filter: blur(14px);
}

:global(.knowledge-dialog .el-dialog__header) {
    margin: 0;
    padding: 24px 30px 8px;
}

:global(.knowledge-dialog .el-dialog__body) {
    padding: 14px 30px 4px;
}

:global(.knowledge-dialog .el-dialog__footer) {
    padding: 16px 30px 24px;
    border-top: 1px solid #f0f2f5;
}

:global(.knowledge-dialog .el-form-item) {
    margin-bottom: 18px;
}

:global(.knowledge-dialog .el-form-item__label) {
    height: 38px;
    align-items: center;
    color: #606266;
    font-weight: 500;
    line-height: 38px;
}

:global(.knowledge-dialog .el-input__wrapper),
:global(.knowledge-dialog .el-textarea__inner) {
    border-radius: 10px;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e5e7eb inset;
    transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

:global(.knowledge-dialog .el-input__wrapper) {
    padding: 4px 12px;
}

:global(.knowledge-dialog .el-textarea__inner) {
    padding: 10px 12px;
    color: #303133;
    font-family: inherit;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

:global(.knowledge-dialog .el-textarea__inner::-webkit-scrollbar) {
    width: 6px;
    height: 6px;
}

:global(.knowledge-dialog .el-textarea__inner::-webkit-scrollbar-thumb) {
    border-radius: 999px;
    background: #cbd5e1;
}

:global(.knowledge-dialog .el-textarea__inner::-webkit-scrollbar-track) {
    background: transparent;
}

:global(.knowledge-dialog .el-input__wrapper:hover),
:global(.knowledge-dialog .el-textarea__inner:hover) {
    box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:global(.knowledge-dialog .el-input__wrapper.is-focus),
:global(.knowledge-dialog .el-textarea__inner:focus) {
    background: #fff;
    box-shadow:
        0 0 0 1px #06b6d4 inset,
        0 0 0 4px rgba(6, 182, 212, 0.08);
}

:global(.knowledge-dialog .dialog-button) {
    min-width: 92px;
    height: 38px;
    border-radius: 10px;
    font-weight: 600;
}

:global(.knowledge-dialog .dialog-button--cancel) {
    border: 0;
    color: #606266;
    background: #f2f3f5;
}

:global(.knowledge-dialog .dialog-button--cancel:hover) {
    color: #303133;
    background: #e9ebef;
}

:global(.knowledge-dialog .dialog-button--submit) {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #06b6d4);
    box-shadow: 0 6px 16px rgba(79, 70, 229, 0.22);
}

:global(.knowledge-dialog .dialog-button--submit:hover) {
    color: #fff;
    background: linear-gradient(135deg, #6366f1, #22d3ee);
}
</style>
