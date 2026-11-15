<template>
    <el-dialog
        v-model="visible"
        class="login-dialog"
        width="420px"
        align-center
        :close-on-click-modal="false"
        :show-close="false"
    >
        <template #header>
            <div class="dialog-title">WXBOT</div>
        </template>

        <el-tabs v-model="activeTab" stretch>
            <el-tab-pane label="登录" name="login">
                <el-form
                    ref="loginFormRef"
                    :model="loginForm"
                    :rules="loginRules"
                    label-width="80px"
                    hide-required-asterisk
                >
                    <el-form-item label="账号" prop="username">
                        <el-input
                            v-model="loginForm.username"
                            placeholder="请输入账号"
                            clearable
                            autocomplete="username"
                        />
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input
                            v-model="loginForm.password"
                            type="password"
                            placeholder="请输入密码"
                            show-password
                            autocomplete="current-password"
                        />
                    </el-form-item>
                </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
                <el-form
                    ref="registerFormRef"
                    :model="registerForm"
                    :rules="registerRules"
                    label-width="80px"
                    hide-required-asterisk
                >
                    <el-form-item label="账号" prop="username">
                        <el-input
                            v-model="registerForm.username"
                            placeholder="请输入账号"
                            clearable
                            autocomplete="username"
                        />
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input
                            v-model="registerForm.password"
                            type="password"
                            placeholder="请输入密码"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>
                    <el-form-item label="确认密码" prop="repassword">
                        <el-input
                            v-model="registerForm.repassword"
                            type="password"
                            placeholder="请再次输入密码"
                            show-password
                            autocomplete="new-password"
                        />
                    </el-form-item>
                </el-form>
            </el-tab-pane>
        </el-tabs>

        <template #footer>
            <el-button class="dialog-button dialog-button--cancel" @click="resetActiveForm">
                重置
            </el-button>
            <el-button
                class="dialog-button dialog-button--submit"
                type="primary"
                @click="validateActiveForm"
            >
                {{ activeTab === 'login' ? '登录' : '注册' }}
            </el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import type { LoginForm, RegisterForm } from '@/api/user';
import userApi from '@/api/user';
import type { FormInstance, FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{
    loginSuccess: []
}>()
const activeTab = ref('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
    username: '',
    password: '',
})

const registerForm = reactive({
    username: '',
    password: '',
    repassword: '',
})

const loginRules: FormRules = {
    username: [
        {
            required: true,
            whitespace: true,
            message: '请输入账号',
            trigger: 'blur',
        },
    ],
    password: [
        {
            required: true,
            whitespace: true,
            message: '请输入密码',
            trigger: 'blur',
        },
    ],
}

const registerRules: FormRules = {
    username: [
        {
            required: true,
            whitespace: true,
            message: '请输入账号',
            trigger: 'blur',
        },
        {
            max: 10,
            message: '用户名长度不能超过10个字符',
            trigger: 'blur',
        },
    ],
    password: [
        {
            required: true,
            whitespace: true,
            message: '请输入密码',
            trigger: 'blur',
        },
        {
            max: 15,
            message: '密码不能超过15个字符',
            trigger: 'blur',
        },
    ],
    repassword: [
        {
            required: true,
            whitespace: true,
            message: '请再次输入密码',
            trigger: 'blur',
        },
        {
            validator: (_rule, value, callback) => {
                if (value && value !== registerForm.password) {
                    callback(new Error('两次输入的密码不一致'))
                    return
                }
                callback()
            },
            trigger: ['blur', 'change'],
        },
    ],
}

const validateActiveForm = async() => {
    const formRef = activeTab.value === 'login'
        ? loginFormRef.value
        : registerFormRef.value

    try{
        await formRef?.validate()
    }catch{return}

    if (activeTab.value === 'login') {
        await login(loginForm)            // 登录请求
    } else {
        await register(registerForm)         // 注册请求
    }
}

const resetActiveForm = () => {
    if (activeTab.value === 'login') {
        loginFormRef.value?.resetFields()
    } else {
        registerFormRef.value?.resetFields()
    }
}

const login = async(form:LoginForm) => {
    const res = await userApi.login(form)
    if (res.data.code == "200"){
        const token = res.data.data
        localStorage.setItem("userToken", token)
        ElMessage.success('登录成功')
        visible.value = false
        emit('loginSuccess')
    }else{ElMessage.error(res.data.msg)}
}
const register = async(form:RegisterForm) => {
    const res = await userApi.register(form)
    if(res.data.code == "200"){
        ElMessage.success("注册成功")
        activeTab.value = "login"
    }else{ElMessage.error(res.data.msg)}
}
</script>

<style scoped>
.dialog-title {
    text-align: center;
    font-size: 21px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #303133;
}

:global(.login-dialog) {
    max-width: calc(100vw - 32px);
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.85);
    border-radius: 25px;
    background: rgba(255, 255, 255, 0.97);
    box-shadow: 0 18px 50px rgba(15, 23, 42, 0.16);
    backdrop-filter: blur(14px);
}

:global(.login-dialog .el-dialog__header) {
    margin: 0;
    padding: 24px 30px 6px;
}

:global(.login-dialog .el-dialog__body) {
    padding: 10px 30px 4px;
}

:global(.login-dialog .el-dialog__footer) {
    padding: 16px 30px 24px;
    border-top: 1px solid #f0f2f5;
}

:global(.login-dialog .el-tabs__header) {
    margin: 0 0 22px;
}

:global(.login-dialog .el-tabs__nav-wrap::after) {
    height: 1px;
    background: #eef0f3;
}

:global(.login-dialog .el-tabs__active-bar) {
    height: 3px;
    border-radius: 999px;
    background: linear-gradient(90deg, #4f46e5, #06b6d4);
}

:global(.login-dialog .el-tabs__item) {
    height: 42px;
    color: #909399;
    font-size: 15px;
}

:global(.login-dialog .el-tabs__item.is-active) {
    color: #303133;
    font-weight: 600;
}

:global(.login-dialog .el-form-item) {
    margin-bottom: 18px;
}

:global(.login-dialog .el-form-item__label) {
    height: 38px;
    align-items: center;
    line-height: 38px;
    color: #606266;
    font-weight: 500;
}

:global(.login-dialog .el-input__wrapper) {
    padding: 4px 12px;
    border-radius: 10px;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e5e7eb inset;
    transition:
        background-color 0.2s ease,
        box-shadow 0.2s ease;
}

:global(.login-dialog .el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:global(.login-dialog .el-input__wrapper.is-focus) {
    background: #fff;
    box-shadow:
        0 0 0 1px #06b6d4 inset,
        0 0 0 4px rgba(6, 182, 212, 0.08);
}

:global(.login-dialog .el-input__inner) {
    font-family: inherit;
    line-height: 30px;
}

:global(.login-dialog .dialog-button) {
    min-width: 92px;
    height: 38px;
    border-radius: 10px;
    font-weight: 600;
}

:global(.login-dialog .dialog-button--cancel) {
    border: 0;
    color: #606266;
    background: #f2f3f5;
}

:global(.login-dialog .dialog-button--cancel:hover) {
    color: #303133;
    background: #e9ebef;
}

:global(.login-dialog .dialog-button--submit) {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #06b6d4);
    box-shadow: 0 6px 16px rgba(79, 70, 229, 0.22);
}

:global(.login-dialog .dialog-button--submit:hover) {
    color: #fff;
    background: linear-gradient(135deg, #6366f1, #22d3ee);
}
</style>
