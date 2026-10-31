<template>
    <div style="display: flex;justify-content: center;flex-wrap: wrap;
    align-items: center;gap: 20px;">
        <div class="pagehead">
            <span style="color: green;">WXBOT</span>
        </div>
        <div>
            <el-button type="primary" @click="createBot">生成二维码</el-button>
        </div>
        <qrcode-vue v-if="qrContent" :value="qrContent" :size="150" level="H" />
    </div>
</template>

<script setup lang="ts">
import botApi from '@/api/bot';
import { ElMessage } from 'element-plus';
import QrcodeVue from 'qrcode.vue';
import { ref } from 'vue';
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
.pagehead{
    width: 100%;
    height: 50px;
    background-color: rgb(247, 243, 243);
    padding: 10px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
}
</style>
