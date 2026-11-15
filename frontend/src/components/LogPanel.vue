<template>
    <div class="log-panel">
        <div class="log-toolbar">
            <div class="log-status">
                <span class="log-status-dot" :class="{ connected }"></span>
                {{ connected ? '实时连接中' : '等待连接' }}
            </div>
            <button class="log-clear" type="button" @click="clearLogs">
                清空
            </button>
        </div>

        <div ref="logListRef" class="log-list">
            <div v-if="logs.length === 0" class="log-empty">
                暂无日志
            </div>
            <div
                v-for="item in logs"
                :key="item.id"
                class="log-item"
                :class="`log-item--${item.level.toLowerCase()}`"
            >
                <div class="log-meta">
                    <span class="log-time">{{ item.time }}</span>
                    <span class="log-level">[{{ item.level }}]</span>
                </div>
                <div class="log-message">{{ item.message }}</div>
                <div v-if="item.throwable" class="log-error">
                    {{ item.throwable }}
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

interface LogItem {
    id: number
    time: string
    level: string
    message: string
    userId: number
    throwable?: string | null
}

const props = defineProps<{
    userId: number | null
}>()

const emit = defineEmits<{
    usage: []
    connected: []
}>()

const maxLogs = 200
const logs = ref<LogItem[]>([])
const connected = ref(false)
const logListRef = ref<HTMLElement | null>(null)

let eventSource: EventSource | null = null

// Each user keeps a single EventSource while this component is mounted.
const close = () => {
    connected.value = false
    eventSource?.close()
    eventSource = null
}

const scrollToBottom = async () => {
    await nextTick()
    const logList = logListRef.value
    if (logList) {
        logList.scrollTop = logList.scrollHeight
    }
}

const connect = () => {
    close()
    if (props.userId == null) {
        return
    }

    eventSource = new EventSource(
        `/api/logs/stream?userId=${encodeURIComponent(String(props.userId))}`
    )

    eventSource.onopen = () => {
        connected.value = true
    }

    eventSource.onerror = () => {
        // EventSource 会自动重连，不在这里重复创建连接。
        connected.value = false
    }

    eventSource.addEventListener('connected', () => {
        connected.value = true
        emit('connected')
    })

    eventSource.addEventListener('usage', () => {
        emit('usage')
    })

    eventSource.addEventListener('log', (event) => {
        try {
            const item = JSON.parse((event as MessageEvent<string>).data) as LogItem
            logs.value.push(item)
            if (logs.value.length > maxLogs) {
                logs.value.splice(0, logs.value.length - maxLogs)
            }
            scrollToBottom()
        } catch {
            // 单条日志格式异常时跳过，保持 SSE 连接。
        }
    })
}

const clearLogs = () => {
    logs.value = []
}

watch(
    () => props.userId,
    connect,
    { immediate: true }
)

onBeforeUnmount(close)
</script>

<style scoped>
.log-panel {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    min-height: 0;
    overflow: hidden;
}

.log-toolbar {
    display: flex;
    flex: 0 0 auto;
    align-items: center;
    justify-content: space-between;
    min-height: 24px;
}

.log-status {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #64748b;
    font-size: 12px;
}

.log-status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #cbd5e1;
}

.log-status-dot.connected {
    background: #10b981;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.log-clear {
    padding: 3px 10px;
    border: 1px solid #e5e7eb;
    border-radius: 999px;
    color: #64748b;
    background: #fff;
    font-size: 11px;
    cursor: pointer;
}

.log-clear:hover {
    color: #334155;
    background: #f8fafc;
}

.log-list {
    flex: 1 1 0;
    min-height: 0;
    overflow-y: auto;
    padding: 8px;
    box-sizing: border-box;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #f8fafc;
    overscroll-behavior: contain;
    contain: layout paint;
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
}

.log-list::-webkit-scrollbar {
    width: 6px;
}

.log-list::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: #cbd5e1;
}

.log-item {
    padding: 7px 8px;
    margin-bottom: 4px;
    border-radius: 7px;
    color: #334155;
    background: #fff;
    font-family: Consolas, "Microsoft YaHei", monospace;
    font-size: 12px;
    line-height: 1.45;
}

.log-item:last-child {
    margin-bottom: 0;
}

.log-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 2px;
}

.log-time {
    color: #94a3b8;
    font-size: 11px;
}

.log-level {
    padding: 1px 5px;
    border-radius: 4px;
    color: #475569;
    background: #f1f5f9;
    font-size: 10px;
    font-weight: 700;
}

.log-message {
    overflow-wrap: anywhere;
}

.log-error {
    margin-top: 3px;
    color: #dc2626;
    font-size: 11px;
}

.log-item--info {
    background: rgba(16, 185, 129, 0.045);
}

.log-item--info .log-level {
    color: #047857;
    background: #d1fae5;
}

.log-item--warn {
    background: rgba(245, 158, 11, 0.055);
}

.log-item--warn .log-level {
    color: #b45309;
    background: #fef3c7;
}

.log-item--error {
    background: rgba(239, 68, 68, 0.055);
}

.log-item--error .log-level {
    color: #b91c1c;
    background: #fee2e2;
}

.log-item--error .log-message {
    color: #dc2626;
}

.log-item--debug {
    background: rgba(59, 130, 246, 0.045);
}

.log-item--debug .log-level {
    color: #1d4ed8;
    background: #dbeafe;
}

.log-item--trace {
    background: rgba(148, 163, 184, 0.06);
}

.log-item--trace .log-level {
    color: #64748b;
    background: #e2e8f0;
}

.log-empty {
    display: grid;
    height: 100%;
    min-height: 80px;
    place-items: center;
    color: #a8abb2;
    font-size: 12px;
}
</style>
