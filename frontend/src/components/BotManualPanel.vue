<template>
    <div class="manual-panel">
        <div class="manual-hero">
            <span class="manual-hero__tag">WXBOT GUIDE</span>
            <div class="manual-hero__title">一千万以内最好的微信机器人</div>
            <p class="manual-hero__text">
                直接发送消息即可开始对话，支持上下文、图片理解和常用快捷命令。
            </p>
        </div>

        <section class="manual-section">
            <div class="manual-section__title">输入支持</div>
            <div class="manual-capabilities">
                <div class="manual-capability">
                    <span class="manual-capability__name">文字</span>
                    <span class="manual-capability__desc">自然语言提问与连续对话</span>
                </div>
                <div class="manual-capability">
                    <span class="manual-capability__name">语音</span>
                    <span class="manual-capability__desc">识别语音转写后的文字内容</span>
                </div>
                <div class="manual-capability">
                    <span class="manual-capability__name">图片</span>
                    <span class="manual-capability__desc">支持 JPEG、PNG、WebP</span>
                </div>
            </div>
        </section>

        <section class="manual-section">
            <div class="manual-section__title">输出支持</div>
            <div class="manual-output">
                <span class="manual-output__type">文字</span>
                <span>回复支持文字消息发送，且适配Markdown 格式文本。</span>
            </div>
        </section>

        <section class="manual-section">
            <div class="manual-section__title">可用命令</div>
            <div class="manual-commands">
                <div class="manual-command">
                    <code>/help</code>
                    <span>查看全部命令和功能说明</span>
                </div>
                <div class="manual-command">
                    <code>/reload</code>
                    <span>清空当前对话上下文</span>
                </div>
                <div class="manual-command">
                    <code>/usage 或 /token</code>
                    <span>查看今日和历史 Token 消耗</span>
                </div>
                <div class="manual-command">
                    <code>/AI</code>
                    <span>查看当前模型和思考强度</span>
                </div>
                <div class="manual-command">
                    <code>/fly</code>
                    <span>开启飞行模式</span>
                </div>
                <div class="manual-command">
                    <code>/genshin 或 /原神</code>
                    <span>懂你意思</span>
                </div>
            </div>
        </section>

        <div class="manual-tips">
            <span>命令不区分大小写，也可以直接发送 <code>/help</code> 获取帮助。</span>
            <span>图片和消息会发送至已配置的 AI 服务用于生成回复。</span>
        </div>
    </div>
</template>

<style scoped>
.manual-panel {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 14px;
    width: 100%;
    min-height: 0;
    padding-right: 3px;
    overflow-y: auto;
    box-sizing: border-box;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

.manual-panel::-webkit-scrollbar {
    width: 6px;
}

.manual-panel::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #cbd5e1;
}

.manual-hero {
    padding: 14px 16px;
    border: 1px solid #dbeafe;
    border-radius: 14px;
    background: linear-gradient(135deg, #f5f3ff 0%, #ecfeff 100%);
}

.manual-hero__tag {
    color: #4f46e5;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.12em;
}

.manual-hero__title {
    margin-top: 5px;
    color: #1f2937;
    font-size: 17px;
    font-weight: 700;
    line-height: 1.35;
}

.manual-hero__text {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 11px;
    line-height: 1.6;
}

.manual-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.manual-section__title {
    color: #303133;
    font-size: 13px;
    font-weight: 600;
}

.manual-capabilities {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
}

.manual-capability {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 4px;
    padding: 9px 10px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #f8fafc;
}

.manual-capability__name {
    color: #4338ca;
    font-size: 11px;
    font-weight: 700;
}

.manual-capability__desc {
    color: #64748b;
    font-size: 10px;
    line-height: 1.45;
}

.manual-output {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 9px 10px;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    color: #64748b;
    background: #f8fafc;
    font-size: 11px;
    line-height: 1.5;
}

.manual-output__type {
    flex: 0 0 auto;
    padding: 1px 6px;
    border-radius: 5px;
    color: #047857;
    background: #d1fae5;
    font-size: 10px;
    font-weight: 600;
}

.manual-commands {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #f8fafc;
}

.manual-command {
    display: grid;
    grid-template-columns: minmax(100px, auto) minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    padding: 7px 10px;
    border-bottom: 1px solid #eef0f3;
    font-size: 10px;
}

.manual-command:last-child {
    border-bottom: 0;
}

.manual-command code,
.manual-tips code {
    color: #4338ca;
    font-family: Consolas, "Microsoft YaHei", monospace;
}

.manual-command span {
    min-width: 0;
    overflow: hidden;
    color: #64748b;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.manual-tips {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-top: 2px;
    color: #94a3b8;
    font-size: 9px;
    line-height: 1.5;
}

@media (max-width: 1200px) {
    .manual-capabilities {
        grid-template-columns: 1fr;
    }
}
</style>
