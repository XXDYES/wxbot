<template>
    <div class="main-container">
        <div class="pagehead">
            <span style="font-size: 25px;margin-left: 20px;font-weight: bold;">🤖 Bot Console</span>
            <el-button type="danger" style="margin-left: auto;margin-right: 20px;" @click="loginout()">
                退出登录
            </el-button>
            <!-- <div style="margin-right: 20px;">
                <el-avatar :size="50" class="user-avatar">
                    <span style="font-size: 35px;">{{ (userInfo?.username || '?').charAt(0) }}</span>
                </el-avatar>
            </div> -->
        </div>
        <div class="content-container">
            <!-- 第一列 -->
            <div class="line-item" style="width: 30%;">
                <div class="tab-base" style="width: 100%;height: 35vh;">
                    <div style="flex: 0 0 100%;">
                        <div class="title">Bot Status</div>
                    </div>
                    <div style="display: flex;flex-direction: column;gap: 10px;">
                        <el-button class="gradient-button" @click="createBot" style="width: 120px;margin: 0 auto;">
                            生成二维码</el-button>
                        <div class="qrcode-box" v-if="!qrContent">
                            暂无二维码<br>点击上方按钮生成
                        </div>
                        <qrcode-vue v-if="qrContent" :value="qrContent" :size="130" level="H" />
                        <div style="text-align: center;" v-if="qrContent">微信扫码</div>
                    </div>
                </div>
                <div class="tab-base" style="flex: 1;width: 100%;margin-top: 20px;">
                    <div style="width: 100%;">
                        <div class="title">Bot Manual</div>
                    </div>
                </div>
            </div>
            <!-- 第二列 -->
            <div class="line-item" style="width: 30%;">
                <div class="tab-base" style="height: 20vh;">
                    <div style="width: 100%;height: 100%;display:flex ;flex-direction: column;">
                        <div class="title">Bot Switch</div>
                        <div style="display: flex;gap: 20px;justify-content: center;
                        flex: 1;align-items: center;">
                            <el-tooltip class="box-item" effect="dark" content="测试连接" placement="bottom">
                                <el-button circle class="switch-button switch-button--green" aria-label="启动">
                                    <el-icon>
                                        <Switch />
                                    </el-icon>
                                </el-button>
                            </el-tooltip>
                            <el-tooltip class="box-item" effect="dark" content="重载配置" placement="bottom">
                                <el-button circle class="switch-button switch-button--yellow" aria-label="刷新">
                                    <el-icon>
                                        <RefreshRight />
                                    </el-icon>
                                </el-button>
                            </el-tooltip>
                            <el-tooltip class="box-item" effect="dark" content="解除绑定" placement="bottom">
                                <el-button circle class="switch-button switch-button--red" aria-label="关闭">
                                    <el-icon>
                                        <SwitchButton />
                                    </el-icon>
                                </el-button>
                            </el-tooltip>
                        </div>
                    </div>
                </div>
                <div class="tab-base" style="flex: 1;margin-top: 20px;">
                    <div style="width: 100%;">
                        <div class="title">Bot Log</div>
                    </div>
                </div>
            </div>
            <!-- 第三列 -->
            <div class="tab-base ai-settings-card">
                <div class="ai-title-row">
                    <div class="title">AI Setting</div>
                    <span class="ai-config-status">
                        <span class="ai-status-dot"></span>
                        AI 服务配置已加载
                    </span>
                </div>

                <div class="ai-settings-content">
                    <div class="ai-field-row">
                        <div class="ai-field">
                            <span class="ai-field-label">模型选择</span>
                            <el-select v-model="model" class="ai-select" placeholder="请选择模型" disabled>
                                <el-option label="deepseek-v4.1-flash" value="deepseek-v4.1-flash" />
                                <el-option label="Qwen-3.8-Flash" value="qwen-3.8-flash" />
                            </el-select>
                        </div>

                        <div class="ai-field">
                            <span class="ai-field-label">思考强度</span>
                            <el-select v-model="thinking" class="ai-select" placeholder="请选择思考强度" disabled>
                                <el-option label="None" value="none" />
                                <el-option label="Low" value="low" />
                                <el-option label="High" value="high" />
                                <el-option label="Max" value="max" />
                            </el-select>
                        </div>
                    </div>

                    <div class="ai-field">
                        <div class="ai-field-head">
                            <span class="ai-field-label">模型人设</span>
                            <!-- <span class="ai-field-hint">定义回复风格</span> -->
                            <el-button circle class="ai-field-hint" v-if="!editPersona" 
                            @click="editPersona = true;copyPersona = persona">
                                <el-icon size="15px" color="#00f">
                                    <Edit />
                                </el-icon></el-button>
                            <div v-if="editPersona">
                                <el-button @click="editPersona = false;persona = copyPersona" circle>
                                    <el-icon size="15px" color="#00f">
                                        <CloseBold />
                                    </el-icon>
                                </el-button>
                                <el-button @click="setPersona(persona)" circle>
                                    <el-icon size="15px" color="#00f">
                                        <Select />
                                    </el-icon>
                                </el-button>
                            </div>
                        </div>
                        <el-input v-model="persona" class="ai-persona" type="textarea" 
                        :rows="4" resize="none" :disabled="!editPersona"
                            placeholder="请输入机器人的人设与回复风格" />
                    </div>

                    <div class="ai-section">
                        <div class="ai-field-head">
                            <span class="ai-field-label">知识库</span>
                            <el-button class="ai-add-button" circle size="small" aria-label="添加知识库">
                                +
                            </el-button>
                        </div>
                        <div class="ai-panel" style="height: 15vh;">
                            <span>暂无知识库</span>
                        </div>
                    </div>

                    <div class="ai-section ai-schedule-section">
                        <div class="ai-field-head">
                            <span class="ai-field-label">定时任务</span>
                            <el-button class="ai-add-button" circle size="small" aria-label="添加定时任务">
                                +
                            </el-button>
                        </div>
                        <div class="ai-panel ai-schedule-panel">
                            <span>暂未配置定时任务</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <LoginDialog v-model="loginVisible" />
    </div>
</template>

<script setup lang="ts">
import botApi from '@/api/bot';
import userApi from '@/api/user';
import LoginDialog from '@/components/LoginDialog.vue';
import { Switch, RefreshRight, SwitchButton, Edit, Select, CloseBold } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import QrcodeVue from 'qrcode.vue';
import { onMounted, ref } from 'vue';
import axios from 'axios'

const userInfo = ref({ id: "", username: "" })
const model = ref("deepseek-v4.1-flash")
const thinking = ref("none")
const persona = ref("")
const copyPersona = ref("")
const loginVisible = ref(false)
const editPersona = ref(false)
const qrContent = ref("")
const createBot = async () => {
    const res = await botApi.createBot()
    if (res.data.code == '200') {
        qrContent.value = res.data.data
        // console.log(res.data.data)
        ElMessage.success("二维码已创建")
    }
}
const getUserInfo = async () => {
    try {
        const res = await userApi.getUserInfo()

        if (res.data.code === '200') {
            userInfo.value = res.data.data
            await getAiSetting()
        }
    } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
            loginVisible.value = true
        }
    }
}
const getAiSetting = async() => {
    const res = await userApi.getAiSetting()
    if (res.data.code === "200"){
        const aiSetting = res.data.data
        model.value = aiSetting.model
        thinking.value = aiSetting.reasoningEffort
        persona.value = aiSetting.persona
    }
}
const setPersona = async(persona: string) => {
    const res = await userApi.setPersona(persona)
    if(res.data.code == "200"){
        editPersona.value = false
        copyPersona.value = ""
        ElMessage.success("修改人设成功")
    }
}
const loginout = () => {
    loginVisible.value = true
    localStorage.removeItem("userToken")
}
onMounted(() => {
    getUserInfo()
})
</script>

<style scoped>
.main-container {
    display: flex;
    flex-direction: column;
    flex-wrap: wrap;
    width: 100vw;
    height: 100vh;
}

.pagehead {
    width: 100%;
    height: 70px;
    background-color: rgb(247, 243, 243);
    padding: 10px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    position: relative;
}

.pagehead::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, #51e546, #06b6d4, transparent);
    opacity: 0.5;
}

.user-avatar {
    color: #fff;
    font-weight: 700;
    background: linear-gradient(135deg, #59e546, #06b6d4);
    /* border: 2px solid rgba(255, 255, 255, 0.9); */
    box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.content-container {
    display: flex;
    padding: 20px 30px;
    flex-wrap: wrap;
    flex: 1;
    width: 100%;
    box-sizing: border-box;
    background-color: #f5f6f7;
    background-image:
        linear-gradient(rgba(6, 182, 212, 0.10) 1.5px, transparent 1.5px),
        linear-gradient(90deg, rgba(81, 229, 70, 0.10) 1.5px, transparent 1.5px);
    background-size: 24px 24px;
}

.tab-base {
    display: flex;
    flex-wrap: wrap;
    background-color: rgba(255, 255, 255, 0.99);
    border-radius: 25px;
    padding: 15px;
    backdrop-filter: blur(10px);
    border: 2px solid rgba(255, 255, 255, 0.3);
    box-sizing: border-box;
    box-shadow: 0 8px 12px 0 rgba(0, 0, 0, 0.1);
    align-items: flex-start;
    align-content: flex-start;
}

.title {
    font-size: 20px;
    font-weight: bold;
    position: relative;
    margin-right: auto;
    padding: 0 10px;
    line-height: 1;
    margin-bottom: 20px;
}

.title::before {
    content: "";
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-45%);
    width: 4px;
    height: 100%;
    background: linear-gradient(to bottom, #4f46e5, #06b6d4);
    border-radius: 2px;
}

.line-item {
    display: flex;
    flex-direction: column;
    margin-right: 40px;
    height: 100%;
}

.qrcode-box {
    width: 150px;
    height: 150px;
    border: 1px dashed #a8abb2;
    background-color: rgba(255, 255, 255, 0.6);
    border-radius: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    box-sizing: border-box;
    text-align: center;
    color: #a8abb2;
}

.switch-button {
    height: 100%;
    aspect-ratio: 1 / 1;
    width: auto !important;
    padding: 0 !important;
    border: 0;
    color: #fff;
    transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.switch-button .el-icon {
    font-size: clamp(18px, 2vw, 26px);
}

.switch-button--green {
    background: #25c45e;
    box-shadow: 0 8px 18px rgba(37, 196, 94, 0.28);
}

.switch-button--yellow {
    color: #614500;
    background: #f3c514;
    box-shadow: 0 8px 18px rgba(243, 197, 20, 0.28);
}

.switch-button--red {
    background: #f05252;
    box-shadow: 0 8px 18px rgba(240, 82, 82, 0.28);
}

.switch-button:hover {
    color: inherit;
    filter: brightness(1.05);
    transform: translateY(-2px);
}

.switch-button--green:hover {
    color: #fff;
    background: #20b455;
}

.switch-button--yellow:hover {
    color: #614500;
    background: #dfac0f;
}

.switch-button--red:hover {
    color: #fff;
    background: #e04444;
}

.ai-settings-card {
    flex: 1;
    min-width: 210px;
    flex-direction: column;
    flex-wrap: nowrap;
    align-items: stretch;
}

.ai-title-row {
    display: flex;
    align-items: center;
    width: 100%;
    margin-bottom: 16px;
}

.ai-title-row .title {
    flex: 0 0 auto;
    margin-bottom: 0;
    white-space: nowrap;
}

.ai-config-status {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
    margin-left: auto;
    padding: 2px 3px;
    border-radius: 999px;
    color: #475569;
    background: #f0fdf4;
    font-size: 9px;
    white-space: nowrap;
}

.ai-settings-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 14px;
    width: 100%;
    min-height: 0;
}

.ai-status-dot {
    width: 5px;
    height: 5px;
    flex: 0 0 5px;
    border-radius: 50%;
    background: #10b981;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.ai-field,
.ai-section {
    display: flex;
    flex-direction: column;
    gap: 7px;
    width: 100%;
}

.ai-field-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 10px;
    width: 100%;
}

.ai-field-row .ai-field {
    min-width: 0;
}

.ai-field-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.ai-field-label {
    color: #303133;
    font-size: 14px;
    font-weight: 600;
}

.ai-field-hint {
    color: #a8abb2;
    font-size: 11px;
}

.ai-add-button {
    width: 24px;
    height: 24px;
    padding: 0;
    border-color: #e5e7eb;
    color: #4f46e5;
    background: #dfdfdf;
    font-size: 16px;
    line-height: 1;
}

.ai-add-button:hover {
    border-color: #c7d2fe;
    color: #4338ca;
    background: #eef2ff;
}

.ai-select {
    width: 100%;
}

.ai-select :deep(.el-select__wrapper) {
    min-height: 36px;
    border-radius: 10px;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e5e7eb inset;
}

.ai-select :deep(.el-select__wrapper.is-focused) {
    box-shadow:
        0 0 0 1px #06b6d4 inset,
        0 0 0 4px rgba(6, 182, 212, 0.08);
}

.ai-persona :deep(.el-textarea__inner) {
    padding: 10px 12px;
    border-radius: 10px;
    color: #303133;
    background: #f8fafc;
    box-shadow: 0 0 0 1px #e5e7eb inset;
    resize: none;
}

.ai-persona :deep(.el-textarea__inner:focus) {
    background: #fff;
    box-shadow:
        0 0 0 1px #06b6d4 inset,
        0 0 0 4px rgba(6, 182, 212, 0.08);
}

.ai-panel {
    display: grid;
    min-height: 66px;
    place-items: center;
    border: 1px dashed #d9dde3;
    border-radius: 10px;
    color: #a8abb2;
    background: rgba(247, 248, 249, 0.95);
    font-size: 12px;
}

.ai-schedule-section {
    flex: 1;
    min-height: 0;
}

.ai-schedule-panel {
    flex: 1;
    min-height: 0;
}

.gradient-button {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #06b6d4);
}

.gradient-button:hover {
    border: 0;
    color: #fff;
    background: linear-gradient(135deg, #6366f1, #22d3ee);
}
</style>
