<template>
  <div class="ai-chat-page">
    <!-- 顶部栏：返回 + 标题 + 编辑 ｜ 内容由AI生成 ｜ 砂立 + 分享 -->
    <header class="chat-header">
      <div class="header-left">
        <button class="icon-btn" @click="goBack" title="返回">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <h1 class="chat-title">{{ chatTitle }}</h1>
        <button class="icon-btn small" @click="renameChat" title="重命名">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
          </svg>
        </button>
      </div>
      <div class="header-center">内容由AI生成</div>
      <div class="header-right">
        <span class="credits-badge">
          <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
            <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 16.8 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
          </svg>
          <span><b>{{ credits }}</b> 砂立</span>
        </span>
        <button class="share-btn" @click="shareChat" title="分享">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
            <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8" />
            <polyline points="16 6 12 2 8 6" />
            <line x1="12" y1="2" x2="12" y2="15" />
          </svg>
          <span>分享</span>
        </button>
      </div>
    </header>

    <!-- 主体：消息区 -->
    <main class="chat-body" ref="bodyRef">
      <!-- 欢迎态（无消息时） -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-watermark">内容由AI生成</div>
        <div class="welcome-title">开始和 AI 对话</div>
        <div class="welcome-desc">描述你的需求，AI 会基于上下文给出回答</div>
      </div>

      <!-- 消息列表 -->
      <div class="messages" ref="messagesContainer">
        <div v-for="(message, index) in messages" :key="index" class="msg-block" :class="message.type">
          <!-- 用户消息 -->
          <div v-if="message.type === 'user'" class="user-msg">
            {{ message.content }}
          </div>

          <!-- AI 消息 -->
          <div v-else class="ai-msg">
            <MarkdownRenderer v-if="message.content" :content="message.content" />
            <div v-if="message.loading" class="loading">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="loading-text">AI 正在思考中…</span>
            </div>
            <div v-if="message.stopped" class="stopped-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>生成已停止，秒哒稍后返还。请告诉我接下来想怎么做～</span>
            </div>

            <!-- 操作行：点赞 / 点踩 / 复制（始终可见） -->
            <div v-if="message.content && !message.loading" class="msg-actions">
              <button class="msg-action" title="有帮助" @click="likeMessage(index)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                  <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3z" />
                  <path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
                </svg>
              </button>
              <button class="msg-action" title="没帮助" @click="dislikeMessage(index)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                  <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3z" />
                  <path d="M17 2h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17" />
                </svg>
              </button>
              <button class="msg-action" title="复制" @click="copyMessage(message.content)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
              </button>
            </div>
          </div>
        </div>
        <div class="bottom-pad" />
      </div>
    </main>

    <!-- 底部输入区 -->
    <footer class="chat-footer">
      <div class="input-card">
        <textarea
          v-model="userInput"
          class="chat-input"
          placeholder="请输入修改描述，@快捷调用技能"
          rows="1"
          @keydown="handleKeydown"
          :disabled="isGenerating"
          @input="autoResize"
          ref="textareaRef"
        />
        <div class="input-bar">
          <div class="bar-left">
            <button class="bar-chip" title="深度模式">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
              </svg>
              <span>深度</span>
              <svg class="caret" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="10" height="10">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <button class="bar-chip plus" title="添加">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="16" height="16">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          </div>
          <div class="bar-right">
            <button class="bar-icon" title="附件" @click="attachFile">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
              </svg>
            </button>
            <button class="bar-icon" title="语音">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                <line x1="12" y1="19" x2="12" y2="23" />
                <line x1="8" y1="23" x2="16" y2="23" />
              </svg>
            </button>
            <button
              v-if="!isGenerating"
              class="send-btn"
              :disabled="!userInput.trim()"
              @click="sendMessage"
              title="发送"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="18" height="18">
                <line x1="12" y1="19" x2="12" y2="5" />
                <polyline points="5 12 12 5 19 12" />
              </svg>
            </button>
            <button v-else class="stop-btn" @click="stopGeneration" title="停止">
              <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                <rect x="6" y="6" width="12" height="12" rx="2" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  stopped?: boolean
}

const router = useRouter()
const chatTitle = ref('AI 对话')
const credits = ref(319)

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const bodyRef = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()

// 停止标志
let stopFlag = false
let currentAiIndex = -1

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

const renameChat = () => {
  // 占位：后续可接入重命名弹窗
  message.info('重命名功能开发中')
}

const shareChat = () => {
  // 占位：复制当前链接 / 调起分享面板
  try {
    navigator.clipboard?.writeText(window.location.href)
    message.success('链接已复制，去分享给好友吧～')
  } catch {
    message.warning('复制失败')
  }
}

const attachFile = () => {
  message.info('附件上传功能开发中')
}

const likeMessage = (_idx: number) => {
  message.success('感谢你的反馈～')
}

const dislikeMessage = (_idx: number) => {
  message.success('已记录，会持续改进')
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 键盘事件
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isGenerating.value) {
    e.preventDefault()
    stopGeneration()
    return
  }
  if (e.key === 'Enter' && !e.shiftKey && !isGenerating.value) {
    e.preventDefault()
    sendMessage()
  }
}

// 自动调整高度
const autoResize = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
  }
}

// 发送消息
const sendMessage = async () => {
  const content = userInput.value.trim()
  if (!content || isGenerating.value) return

  stopFlag = false

  messages.value.push({
    type: 'user',
    content,
  })

  userInput.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  currentAiIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true

  try {
    await simulateAiResponse(content, currentAiIndex)
  } catch (error) {
    if (stopFlag) {
      messages.value[currentAiIndex].stopped = true
    } else {
      messages.value[currentAiIndex].content = '抱歉，出现了一些问题，请稍后重试。'
    }
    messages.value[currentAiIndex].loading = false
  } finally {
    isGenerating.value = false
    stopFlag = false
    await nextTick()
    scrollToBottom()
  }
}

// 停止生成
const stopGeneration = () => {
  if (!isGenerating.value) return
  stopFlag = true
}

// 模拟 AI 回复（流式效果）
const simulateAiResponse = async (userMessage: string, aiIndex: number) => {
  const responses: Record<string, string> = {
    default: `你好！我收到了你的消息："${userMessage}"

这是一个演示对话页面。在实际项目中，你可以：

- 接入真实的 AI API 接口
- 实现流式响应
- 支持多轮对话记忆
- 添加更多功能

有什么我可以帮助你的吗？`,
  }

  let response = responses.default

  if (userMessage.includes('待办') || userMessage.includes('网页')) {
    response = `好的，我来帮你设计一个待办事项网页！

## 功能规划

1. **添加任务** - 输入框 + 添加按钮
2. **任务列表** - 展示所有待办事项
3. **完成状态** - 点击切换完成/未完成
4. **删除任务** - 移除不需要的任务
5. **筛选功能** - 全部/已完成/未完成

## 技术栈建议

- HTML + CSS + JavaScript（原生）
- Vue 3 + Vite
- React + Create React App

你希望使用哪种技术栈来实现呢？`
  } else if (userMessage.includes('闭包')) {
    response = `## 什么是闭包？

闭包（Closure）是 JavaScript 中一个非常重要的概念。

### 定义

闭包是指**一个函数能够访问其外部作用域中的变量**，即使外部函数已经执行完毕。

### 简单例子

\`\`\`javascript
function outer() {
  let count = 0;

  function inner() {
    count++;
    console.log(count);
  }

  return inner;
}

const counter = outer();
counter(); // 1
counter(); // 2
counter(); // 3
\`\`\`

### 常见用途

1. **数据私有化** - 创建私有变量
2. **函数工厂** - 生成特定功能的函数
3. **回调函数** - 保留外部作用域引用
4. **模块化** - 实现模块模式

理解了吗？还想深入了解哪个方面？`
  } else if (userMessage.includes('秋天') || userMessage.includes('诗')) {
    response = `## 秋意浓

> 金风送爽入画楼，
> 霜叶红于二月花。
> 雁阵排空天际远，
> 稻香满野话丰年。

---

## 秋日私语

秋天是一首写不完的诗，
每一片落叶都是韵脚。
阳光穿过稀疏的枝叶，
在地面铺陈金色的思考。

你喜欢秋天吗？最喜欢秋天的什么呢？`
  } else if (userMessage.toLowerCase().includes('python') || userMessage.includes('快速排序')) {
    response = `## Python 快速排序实现

\`\`\`python
def quick_sort(arr):
    """
    快速排序算法
    时间复杂度: 平均 O(n log n)，最坏 O(n²)
    空间复杂度: O(log n)
    """
    if len(arr) <= 1:
        return arr

    pivot = arr[len(arr) // 2]

    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]

    return quick_sort(left) + middle + quick_sort(right)


if __name__ == "__main__":
    test_arr = [64, 34, 25, 12, 22, 11, 90]
    print("原始数组:", test_arr)
    sorted_arr = quick_sort(test_arr)
    print("排序结果:", sorted_arr)
\`\`\`

### 代码说明

1. **基准选择**：选取中间元素作为 pivot
2. **分区操作**：将数组分为小于、等于、大于基准三部分
3. **递归排序**：对左右两部分递归调用快速排序
4. **合并结果**：将三部分拼接起来

需要我解释其他排序算法吗？`
  }

  // 模拟流式输出
  let currentText = ''
  for (let i = 0; i < response.length; i++) {
    if (stopFlag) {
      messages.value[aiIndex].loading = false
      messages.value[aiIndex].stopped = true
      throw new Error('STOPPED_BY_USER')
    }

    currentText += response[i]
    messages.value[aiIndex].content = currentText
    await new Promise((resolve) => setTimeout(resolve, 10))
    if (i % 5 === 0) {
      scrollToBottom()
    }
  }
  messages.value[aiIndex].loading = false
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 全局键盘监听（Esc 停止）
const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isGenerating.value) {
    stopGeneration()
  }
}

onMounted(() => {
  textareaRef.value?.focus()
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style scoped>
.ai-chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  font-size: 14px;
  color: #1f2329;
}

/* ===== 顶部栏 ===== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #ffffff;
  border-bottom: 1px solid #eef0f4;
  flex-shrink: 0;
  min-height: 48px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.header-center {
  flex: 0 0 auto;
  font-size: 12px;
  color: #9ca3af;
  user-select: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  justify-content: flex-end;
}
.chat-title {
  margin: 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 360px;
}
.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, color 0.15s ease;
  flex-shrink: 0;
}
.icon-btn:hover {
  background: #f2f4f8;
  color: #1f2329;
}
.icon-btn svg {
  width: 18px;
  height: 18px;
}
.icon-btn.small {
  width: 26px;
  height: 26px;
}
.icon-btn.small svg {
  width: 14px;
  height: 14px;
}
.credits-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #f0f5ff;
  color: #4080ff;
  font-size: 12px;
  font-weight: 500;
  user-select: none;
}
.credits-badge b {
  font-weight: 700;
  color: #1f2329;
}
.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #1f2329;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.share-btn:hover {
  background: #f8fafc;
  border-color: #d1d5db;
}

/* ===== 主体 ===== */
.chat-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.welcome-watermark {
  font-size: 12px;
  color: #c9cdd4;
  margin-bottom: 12px;
}
.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 6px;
}
.welcome-desc {
  font-size: 14px;
  color: #8a8f99;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  scroll-behavior: smooth;
}
.msg-block {
  width: 100%;
  max-width: 720px;
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
}
.msg-block.user {
  align-items: flex-end;
}
.msg-block.ai {
  align-items: flex-start;
}
.user-msg {
  max-width: 75%;
  padding: 9px 14px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  font-size: 14px;
  color: #1f2329;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  word-wrap: break-word;
  overflow-wrap: break-word;
}
.ai-msg {
  width: 100%;
  font-size: 14px;
  line-height: 1.75;
  color: #1f2329;
  word-wrap: break-word;
  overflow-wrap: break-word;
}
.loading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #8a8f99;
  padding: 6px 0;
}
.loading .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9cdd4;
  animation: bounce 1.4s infinite ease-in-out both;
}
.loading .dot:nth-child(1) { animation-delay: -0.32s; }
.loading .dot:nth-child(2) { animation-delay: -0.16s; }
.loading-text { margin-left: 4px; font-size: 14px; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}
.stopped-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 10px;
  color: #d46b08;
  font-size: 13px;
  width: 100%;
}
.msg-actions {
  display: flex;
  gap: 2px;
  margin-top: 10px;
  align-items: center;
}
.msg-action {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, color 0.15s ease;
}
.msg-action:hover {
  background: #f2f4f8;
  color: #1f2329;
}
.bottom-pad {
  height: 16px;
  flex-shrink: 0;
}

/* ===== 底部输入区 ===== */
.chat-footer {
  flex-shrink: 0;
  padding: 12px 20px 20px;
  background: #f7f8fa;
}
.input-card {
  max-width: 720px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 10px 14px 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.input-card:focus-within {
  border-color: #c9d2e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.chat-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
  max-height: 180px;
  font-family: inherit;
  color: #1f2329;
  background: transparent;
  padding: 4px 2px;
}
.chat-input::placeholder {
  color: #b0b6c2;
}
.chat-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.input-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding-top: 6px;
  border-top: 1px solid #f2f3f5;
}
.bar-left,
.bar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bar-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  background: #f7f8fa;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.bar-chip:hover {
  background: #eef0f4;
  color: #1f2329;
}
.bar-chip.plus {
  width: 26px;
  height: 26px;
  padding: 0;
  justify-content: center;
}
.bar-icon {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, color 0.15s ease;
}
.bar-icon:hover {
  background: #f2f4f8;
  color: #1f2329;
}
.send-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #1f1f1f;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease, opacity 0.15s ease;
}
.send-btn:hover:not(:disabled) {
  background: #2c2c2c;
}
.send-btn:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}
.stop-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #e23b3b;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}
.stop-btn:hover {
  background: #cc2f2f;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-header { padding: 8px 12px; }
  .header-center { display: none; }
  .chat-title { max-width: 160px; font-size: 14px; }
  .credits-badge { padding: 4px 8px; font-size: 11px; }
  .share-btn span { display: none; }
  .share-btn { padding: 6px 8px; }
  .messages { padding: 16px 12px 8px; }
  .chat-footer { padding: 10px 12px 16px; }
}
</style>
