<template>
    <div class="main-container">
        <div class="pagehead">
            <span style="font-size: 25px;margin-left: 20px;font-weight: bold;">🤖 Bot Console</span>
            <el-button type="danger" style="margin-left: auto;margin-right: 20px;"
             @click="loginVisible = true">
                退出登录
            </el-button>
            <div style="margin-right: 20px;">
                <el-avatar :size="50" />
            </div>
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
                        <qrcode-vue v-if="qrContent" :value="qrContent" :size="150" level="H" />
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
                    <div style="width: 100%;">
                        <div class="title">Bot Switch</div>
                    </div>
                </div>
                <div class="tab-base" style="flex: 1;margin-top: 20px;">
                    <div style="width: 100%;">
                        <div class="title">Bot Log</div>
                    </div>
                </div>
            </div>
            <!-- 第三列 -->
            <div class="tab-base" style="flex: 1;">
                <div style="width: 100%;">
                    <div class="title">AI Setting</div>
                </div>
            </div>
        </div>
        <LoginDialog v-model="loginVisible" />
    </div>
</template>

<script setup lang="ts">
import botApi from '@/api/bot';
import LoginDialog from '@/components/LoginDialog.vue';
import { ElMessage } from 'element-plus';
import QrcodeVue from 'qrcode.vue';
import { ref } from 'vue';

const loginVisible = ref(false)
const qrContent = ref("")
const createBot = async () => {
    const res = await botApi.createBot()
    if (res.data.code == '200') {
        qrContent.value = res.data.data
        // console.log(res.data.data)
        ElMessage.success("二维码已创建")
    }
}
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
