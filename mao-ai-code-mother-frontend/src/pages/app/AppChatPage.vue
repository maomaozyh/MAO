<template>
  <div class="chat-page-wrapper">
    <div class="chat-container">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <div class="header-left">
          <button class="icon-btn" @click="goBack" title="返回">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
          </button>
          <div class="header-title">
            <h1 class="title-text">{{ appInfo?.appName || '代码生成助手' }}</h1>
          </div>
          <button class="icon-btn" @click="renameApp" title="修改名字">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
            </svg>
          </button>
        </div>
        <div class="header-right">
          <span class="balance-badge" title="当前积分余额">
            <svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
              <path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4L12 16.8 5.8 21.3l2.4-7.4L2 9.4h7.6L12 2z" />
            </svg>
            <span>积分 {{ balanceText }}</span>
          </span>
          <button class="action-btn primary" @click="deployApp" :disabled="deploying">
            <svg v-if="!deploying" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <span v-if="deploying">部署中</span>
            <span v-else>部署</span>
          </button>
        </div>
      </div>

      <!-- 主体区域 -->
      <div class="chat-body">
        <!-- 中间对话区 -->
        <div class="chat-main">
          <div class="messages-scroll" ref="messagesContainer">
            <!-- 欢迎页 -->
            <div v-if="messages.length === 0" class="welcome-section">
              <div class="welcome-icon">✨</div>
              <h2 class="welcome-title">{{ appInfo?.appName || '代码生成助手' }}</h2>
              <p class="welcome-desc">{{ appInfo?.description || '描述你想要的网站，AI 帮你一键生成' }}</p>
              <div class="quick-chips">
                <div v-for="(p, i) in quickPrompts" :key="i" class="chip" @click="sendQuickPrompt(p)">
                  {{ p }}
                </div>
              </div>
            </div>

            <!-- 消息列表 -->
            <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.type">
              <div v-if="msg.type === 'user'" class="user-bubble">
                {{ msg.content }}
              </div>
              <div v-else class="ai-block">
                <div class="ai-content">
                  <template v-if="isFullHtmlDoc(msg.content)">
                    <div class="gen-success">
                      <span class="success-icon">✅</span>
                      <span>网站已生成，右侧预览已自动更新</span>
                      <button
                        class="selfcheck-btn"
                        :disabled="selfChecking || isGenerating"
                        @click="handleSelfCheck(msg)"
                        title="AI 检查代码常见错误并尝试修复"
                      >
                        {{ selfChecking ? '检查中…' : '自查修复' }}
                      </button>
                    </div>
                    <div v-if="msg.selfCheck" class="selfcheck-result" :class="{ ok: !msg.selfCheck.hasIssue }">
                      <div class="selfcheck-title">
                        <span>{{ msg.selfCheck.hasIssue ? '⚠️ 发现 ' + msg.selfCheck.issues.length + ' 个问题' : '✅ 未发现明显问题' }}</span>
                        <span v-if="msg.selfCheck.fixedCode" class="selfcheck-fix" @click="applySelfCheckFix(msg)">应用修复后的代码</span>
                      </div>
                      <ul v-if="msg.selfCheck.hasIssue" class="selfcheck-issues">
                        <li v-for="(issue, i) in msg.selfCheck.issues" :key="i">{{ issue }}</li>
                      </ul>
                    </div>
                    <details class="code-wrap">
                      <summary>查看完整代码</summary>
                      <pre class="code-block"><code>{{ msg.content }}</code></pre>
                    </details>
                  </template>
                  <MarkdownRenderer v-else-if="msg.content && !isPptSlides(msg.content)" :content="msg.content" />
                  <PptExportCard v-else-if="isPptSlides(msg.content)" :slides-json="extractPptJson(msg.content)" />
                  <span v-if="msg.loading && msg.content" class="typing-cursor"></span>
                  <div v-if="msg.loading && !msg.content" class="thinking" @click="stopGeneration" title="点击停止生成">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="thinking-text">AI 正在思考中...（点击或按 Esc 停止）</span>
                  </div>
                  <div v-if="msg.stopped" class="stopped-box">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="12" y1="8" x2="12" y2="12"/>
                      <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <span>生成已停止，积分稍后返还。请告诉我接下来想怎么做～</span>
                  </div>
                  <div v-if="msg.failed" class="failed-box">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="12" y1="8" x2="12" y2="12"/>
                      <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    <span>{{ msg.content }}</span>
                    <button class="retry-btn" @click="retryMessage(idx)">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="23 4 23 10 17 10"/>
                        <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                      </svg>
                      <span>重试</span>
                    </button>
                  </div>
                </div>
                <div v-if="msg.content && !msg.loading && !msg.stopped && !msg.failed" class="ai-actions">
                  <button class="act-btn" title="有帮助" @click="likeMessage(idx)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                    </svg>
                  </button>
                  <button class="act-btn" title="没帮助" @click="dislikeMessage(idx)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3zm7-13h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17"/>
                    </svg>
                  </button>
                  <button class="act-btn" title="复制" @click="copyMessage(msg.content)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <div class="bottom-pad"></div>
          </div>

          <!-- 底部输入框 -->
          <div class="input-section">
            <div class="input-card" @click="closeDropdowns">
              <!-- 顶部胶囊工具栏 -->
              <div class="chip-bar">
                <div class="chip-bar-left">
                  <div class="chip-dropdown-wrap">
                    <button class="chip-btn chip-plus" @click="togglePlusDropdown">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <line x1="12" y1="5" x2="12" y2="19"/>
                        <line x1="5" y1="12" x2="19" y2="12"/>
                      </svg>
                    </button>
                    <div v-if="showPlusDropdown" class="chip-dropdown chip-dropdown-down" @click.stop>
                      <div class="chip-dropdown-item" @click="openMaterialModal">
                        <span class="chip-dropdown-icon">🗂️</span>
                        <span class="chip-dropdown-text">从素材库中添加</span>
                      </div>
                      <div class="chip-dropdown-item" @click="triggerFileUpload">
                        <span class="chip-dropdown-icon">📁</span>
                        <span class="chip-dropdown-text">上传文件或者图片</span>
                      </div>
                    </div>
                  </div>
                  <button
                    class="chip-btn chip-expand"
                    :disabled="expanding || isGenerating"
                    @click="handleExpandPrompt"
                    title="AI 描述智能扩写"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 3l1.9 5.6L19.5 10l-5.6 1.9L12 17.5l-1.9-5.6L4.5 10l5.6-1.4z"/>
                      <path d="M19 15l.7 2.3L22 18l-2.3.7L19 21l-.7-2.3L16 18l2.3-.7z"/>
                    </svg>
                    <span>{{ expanding ? '扩写中' : '扩写' }}</span>
                  </button>
                  <button class="chip-btn chip-skill" title="技能" @click="openSkillModal">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
                    </svg>
                    <span>技能</span>
                  </button>
                  <div class="chip-dropdown-wrap">
                    <button class="chip-btn chip-deep" @click="toggleDeepDropdown">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <line x1="12" y1="5" x2="12" y2="19"/>
                        <line x1="5" y1="12" x2="19" y2="12"/>
                      </svg>
                      <span>{{ genMode === 'fast' ? '快速开发' : '深度开发' }}</span>
                      <svg class="chip-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="6 9 12 15 18 9"/>
                      </svg>
                    </button>
                    <div v-if="showDeepDropdown" class="chip-dropdown chip-dropdown-down" @click.stop>
                      <div class="chip-dropdown-item" :class="{ active: genMode === 'deep' }" @click="selectMode('deep')">
                        <span class="chip-dropdown-icon">🧠</span>
                        <span class="chip-dropdown-text">深度开发</span>
                      </div>
                      <div class="chip-dropdown-item" :class="{ active: genMode === 'fast' }" @click="selectMode('fast')">
                        <span class="chip-dropdown-icon">🔍</span>
                        <span class="chip-dropdown-text">快速开发</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <textarea
                  v-model="userInput"
                  class="chat-input"
                  :placeholder="getPlaceholder()"
                  rows="1"
                  @keydown="handleKeydown"
                  :disabled="!isOwner && !isAdmin"
                  @input="autoResize"
                  ref="textareaRef"
              ></textarea>
              <div class="input-bar">
                <div class="bar-left"></div>
                <div class="bar-right">
                  <button
                      v-if="!isGenerating"
                      class="send-btn"
                      :disabled="!userInput.trim() || !isOwner"
                      @click="sendMessage"
                      title="发送"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <line x1="12" y1="19" x2="12" y2="5"/>
                      <polyline points="5 12 12 5 19 12"/>
                    </svg>
                  </button>
                  <button
                      v-else
                      class="stop-btn"
                      @click="stopGeneration"
                      title="停止生成"
                  >
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <rect x="6" y="6" width="12" height="12" rx="2"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <p v-if="isGenerating" class="hint-text">
              点击「生成中」文字或按 <kbd>Esc</kbd> 停止生成
            </p>
            <p v-else class="hint-text">
              AI 生成内容仅供参考，请核实重要信息
            </p>
          </div>
        </div>

        <!-- 右侧预览面板 -->
        <div class="preview-panel" :class="{ collapsed: !previewOpen }">
          <button class="collapse-btn" @click="previewOpen = !previewOpen">
            <svg v-if="previewOpen" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </button>
          <div v-if="previewOpen" class="preview-content">
            <div class="preview-header">
              <span class="preview-label">实时预览</span>
              <div class="preview-header-actions">
                <button class="action-btn preview-action" @click="downloadCode" :disabled="downloading || !isOwner" title="下载代码">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  <span>下载</span>
                </button>
                <a-button v-if="previewUrl" type="link" size="small" @click="openInNewTab">
                  <template #icon><ExportOutlined /></template>
                  新窗口
                </a-button>
              </div>
            </div>
            <div class="preview-body">
              <div v-if="!previewUrl && !isGenerating" class="preview-empty">
                <span class="empty-ico">🖼️</span>
                <p>生成网站后在此预览</p>
              </div>
              <div v-else-if="isGenerating" class="preview-loading">
                <div class="loader"></div>
                <p class="generating-clickable" @click="stopGeneration" title="点击停止生成">正在生成...（点击或按 Esc 停止）</p>
                <button class="stop-mini-btn" @click="stopGeneration">停止生成</button>
              </div>
              <iframe
                  v-else
                  :src="previewUrl"
                  class="preview-frame"
                  frameborder="0"
                  @load="onIframeLoad"
              ></iframe>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改名字弹窗 -->
    <a-modal
        v-model:open="renameVisible"
        title="修改名字"
        :confirm-loading="renaming"
        ok-text="保存"
        cancel-text="取消"
        :mask-closable="!renaming"
        width="420"
        @ok="confirmRename"
    >
      <a-input
          v-model:value="renameName"
          placeholder="请输入应用名称"
          :maxlength="50"
          show-count
          @press-enter="confirmRename"
      />
    </a-modal>
    <DeploySuccessModal
        v-model:open="deployModalVisible"
        :deploy-url="deployUrl"
        @open-site="openDeployedSite"
    />

    <!-- 素材库选择弹窗 -->
    <a-modal
        v-model:open="materialModalVisible"
        title="从素材库中添加"
        :footer="null"
        width="720"
        @cancel="materialModalVisible = false"
    >
      <div v-if="materialLoading" class="picker-loading">加载中…</div>
      <div v-else-if="materialList.length === 0" class="picker-empty">暂无素材，请先到素材库上传</div>
      <div v-else class="picker-grid">
        <div
            v-for="m in materialList"
            :key="m.id"
            class="picker-card"
            @click="selectMaterial(m)"
        >
          <img v-if="m.type && m.type.startsWith('image')" :src="m.url" class="picker-thumb" alt="" />
          <div v-else class="picker-thumb picker-file">{{ (m.type || 'file').slice(0, 3).toUpperCase() }}</div>
          <div class="picker-name" :title="m.name">{{ m.name }}</div>
        </div>
      </div>
      <div v-if="materialHasMore" class="picker-more" @click="loadMaterials(materialPage + 1)">加载更多</div>
    </a-modal>

    <!-- 技能选择弹窗 -->
    <a-modal
        v-model:open="skillModalVisible"
        title="选择技能"
        :footer="null"
        width="720"
        @cancel="skillModalVisible = false"
    >
      <div v-if="skillLoading" class="picker-loading">加载中…</div>
      <div v-else-if="skillList.length === 0" class="picker-empty">暂无可用技能</div>
      <div v-else class="picker-grid">
        <div
            v-for="s in skillList"
            :key="s.id"
            class="picker-card"
            @click="selectSkill(s)"
        >
          <div class="picker-thumb picker-skill">{{ s.icon || '🧩' }}</div>
          <div class="picker-name" :title="s.skillName">{{ s.skillName }}</div>
          <div class="picker-desc" :title="s.skillDesc">{{ s.skillDesc }}</div>
        </div>
      </div>
      <div v-if="skillHasMore" class="picker-more" @click="loadSkills(skillPage + 1)">加载更多</div>
    </a-modal>

    <!-- 隐藏的文件上传 input -->
    <input
        ref="fileInputRef"
        type="file"
        accept="image/*,.pdf,.doc,.docx,.txt,.md,.json,.zip,.csv,.xlsx,.pptx"
        style="display: none"
        @change="onFileSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useRecentProjectsStore } from '@/stores/recentProjects'
import { refreshBalance } from '@/composables/useBalance'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
  expandPrompt,
  selfCheckAppCode,
  updateApp,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import PptExportCard from '@/components/PptExportCard.vue'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'
import { ExportOutlined } from '@ant-design/icons-vue'
import {
  uploadMaterial,
  listMyMaterialVoByPage,
  getMaterialVOById,
} from '@/api/materialController'
import { listSkillVOByPage } from '@/api/skillController'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
// 进入对话页即视为"与 AI 对话"，记录到最近项目（store 会同步刷新侧边栏列表）
const recentStore = useRecentProjectsStore()

const appInfo = ref<API.AppVO>()
const appId = ref<any>()

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  stopped?: boolean
  failed?: boolean
  prompt?: string
  createTime?: string
  selfCheck?: API.SelfCheckResultVO
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
// 生成代码（GEN_CODE，SSE 流式）结束时刷新积分余额，让侧边栏/会员页实时更新
watch(isGenerating, (now, prev) => {
  if (prev && !now) refreshBalance()
})
const messagesContainer = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()

// 描述智能扩写
const expanding = ref(false)

// 工具栏下拉菜单
const showPlusDropdown = ref(false)
const showDeepDropdown = ref(false)
const closeDropdowns = () => {
  showPlusDropdown.value = false
  showDeepDropdown.value = false
}
const togglePlusDropdown = (e: MouseEvent) => {
  e.stopPropagation()
  showDeepDropdown.value = false
  showPlusDropdown.value = !showPlusDropdown.value
}
const toggleDeepDropdown = (e: MouseEvent) => {
  e.stopPropagation()
  showPlusDropdown.value = false
  showDeepDropdown.value = !showDeepDropdown.value
}

// ===== 输入栏芯片功能 =====
// 生成模式：deep=深度开发 / fast=快速开发（传入后端 gen/code 的 mode 参数）
const genMode = ref<'deep' | 'fast'>('deep')
const selectMode = (mode: 'deep' | 'fast') => {
  genMode.value = mode
  showDeepDropdown.value = false
}

// 把引用文本插入到输入框
const insertIntoPrompt = (text: string) => {
  const cur = userInput.value || ''
  const sep = cur && !cur.endsWith('\n') ? '\n' : ''
  userInput.value = cur + sep + text + '\n'
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
      textareaRef.value.focus()
    }
  })
}

// —— 上传文件或者图片 ——
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)
const triggerFileUpload = () => {
  showPlusDropdown.value = false
  fileInputRef.value?.click()
}
const onFileSelected = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadMaterial(formData)
    if (res.data?.code === 0 && res.data.data) {
      const vo = await getMaterialVOById({ id: res.data.data })
      const m = vo.data?.data
      if (m?.url) {
        const refText =
          m.type && m.type.startsWith('image')
            ? `![${m.name || file.name}](${m.url})`
            : `素材「${m.name || file.name}」：${m.url}`
        insertIntoPrompt(refText)
        message.success('已添加到输入框')
      } else {
        message.error('素材信息缺失')
      }
    } else {
      message.error(res.data?.message || '上传失败')
    }
  } catch (err: any) {
    message.error(err?.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
    target.value = ''
  }
}

// —— 从素材库中添加 ——
const materialModalVisible = ref(false)
const materialList = ref<API.MaterialVO[]>([])
const materialLoading = ref(false)
const materialPage = ref(1)
const materialHasMore = ref(false)
const openMaterialModal = () => {
  showPlusDropdown.value = false
  materialModalVisible.value = true
  if (materialList.value.length === 0) loadMaterials(1)
}
const loadMaterials = async (page: number) => {
  materialLoading.value = true
  try {
    const res = await listMyMaterialVoByPage({ pageNum: page, pageSize: 20 })
    const records = res.data?.data?.records || []
    if (page === 1) materialList.value = records
    else materialList.value = [...materialList.value, ...records]
    materialPage.value = page
    materialHasMore.value = records.length >= 20
  } catch (err: any) {
    message.error(err?.response?.data?.message || '加载素材失败')
  } finally {
    materialLoading.value = false
  }
}
const selectMaterial = (m: API.MaterialVO) => {
  if (m.url) {
    const refText =
      m.type && m.type.startsWith('image')
        ? `![${m.name}](${m.url})`
        : `素材「${m.name}」：${m.url}`
    insertIntoPrompt(refText)
  }
  materialModalVisible.value = false
  message.success('已添加到输入框')
}

// —— 技能 ——
const skillModalVisible = ref(false)
const skillList = ref<API.SkillVO[]>([])
const skillLoading = ref(false)
const skillPage = ref(1)
const skillHasMore = ref(false)
const openSkillModal = () => {
  skillModalVisible.value = true
  if (skillList.value.length === 0) loadSkills(1)
}
const loadSkills = async (page: number) => {
  skillLoading.value = true
  try {
    const res = await listSkillVOByPage({ pageNum: page, pageSize: 20, status: 1 })
    const records = res.data?.data?.records || []
    if (page === 1) skillList.value = records
    else skillList.value = [...skillList.value, ...records]
    skillPage.value = page
    skillHasMore.value = records.length >= 20
  } catch (err: any) {
    message.error(err?.response?.data?.message || '加载技能失败')
  } finally {
    skillLoading.value = false
  }
}
const selectSkill = (s: API.SkillVO) => {
  if (s.skillName) {
    insertIntoPrompt(`请使用技能「${s.skillName}」来完成以下需求：`)
  }
  skillModalVisible.value = false
  message.success('已添加技能引用')
}

// AI 代码自查修复
const selfChecking = ref(false)
const handleSelfCheck = async (msg: Message) => {
  if (!appId.value || selfChecking.value || isGenerating.value) return
  selfChecking.value = true
  try {
    const res = await selfCheckAppCode({ appId: appId.value })
    if (res.data.code === 0 && res.data.data) {
      msg.selfCheck = res.data.data
      if (res.data.data.hasIssue) {
        message.warning('发现 ' + (res.data.data.issues?.length || 0) + ' 个问题')
      } else {
        message.success('未发现明显问题')
      }
      // 代码自查会扣积分，刷新余额
      refreshBalance()
    } else {
      message.error(res.data.message || '自查失败')
    }
  } catch {
    message.error('自查失败，请稍后再试')
  } finally {
    selfChecking.value = false
  }
}

// 应用 AI 修复后的代码（更新消息内容 + 用 srcDoc 内联刷新预览）
const applySelfCheckFix = (msg: Message) => {
  const fixed = msg.selfCheck?.fixedCode
  if (!fixed) return
  msg.content = fixed
  msg.selfCheck = { ...msg.selfCheck, fixedCode: '' }
  message.success('已应用修复后的代码，预览已更新')
  try {
    const previewFrame = document.querySelector('.preview-frame') as HTMLIFrameElement | null
    if (previewFrame) {
      previewFrame.setAttribute('srcdoc', fixed)
    }
  } catch {
    /* 静默 */
  }
}

const handleExpandPrompt = async () => {
  const raw = userInput.value.trim()
  if (!raw) {
    message.warning('请先输入要扩写的内容')
    return
  }
  expanding.value = true
  try {
    const res = await expandPrompt({ prompt: raw })
    if (res.data.code === 0 && res.data.data) {
      userInput.value = res.data.data
      message.success('已智能扩写，检查后可发送')
      if (textareaRef.value) {
        textareaRef.value.style.height = 'auto'
        textareaRef.value.style.height = textareaRef.value.scrollHeight + 'px'
      }
      // 智能扩写会扣积分，刷新余额
      refreshBalance()
    } else {
      message.error(res.data.message || '扩写失败')
    }
  } catch {
    message.error('扩写失败，请稍后再试')
  } finally {
    expanding.value = false
  }
}

let eventSource: EventSource | null = null

// 流式无数据超时保护（安全网，避免一直卡在"生成中"）
const STREAM_TIMEOUT = 180000
let inactivityTimer: ReturnType<typeof setTimeout> | null = null
let streamCompleted = false
const clearInactivityTimer = () => {
  if (inactivityTimer) {
    clearTimeout(inactivityTimer)
    inactivityTimer = null
  }
}
const armInactivityTimer = (aiMessageIndex: number) => {
  clearInactivityTimer()
  inactivityTimer = setTimeout(() => {
    if (streamCompleted || !isGenerating.value) return
    messages.value[aiMessageIndex].loading = false
    messages.value[aiMessageIndex].failed = true
    messages.value[aiMessageIndex].content = '生成超时，连接可能已断开，请点击重试。'
    isGenerating.value = false
    eventSource?.close()
    eventSource = null
    message.error('生成超时，请重试')
  }, STREAM_TIMEOUT)
}

const previewOpen = ref(true)

const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

const previewUrl = ref('')
const previewReady = ref(false)

const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

const downloading = ref(false)

const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 积分余额（购买 secondsBalance + 赠送 giftSecondsBalance，与侧边栏/会员中心口径一致）
const balanceText = computed(() => {
  const purchased = Number(loginUserStore.loginUser?.secondsBalance ?? 0)
  const gift = Number(loginUserStore.loginUser?.giftSecondsBalance ?? 0)
  return purchased + gift
})

const renameVisible = ref(false)
const renameName = ref('')
const renaming = ref(false)

const quickPrompts = [
  '生成一个精美的个人主页',
  '做一个待办事项应用',
  '创建一个产品展示页',
  '设计一个登录注册页面',
]


const renameApp = () => {
  renameName.value = appInfo.value?.appName || ''
  renameVisible.value = true
}

const confirmRename = async () => {
  const newName = (renameName.value || '').trim()
  if (!newName) {
    message.warning('应用名称不能为空')
    return
  }
  const id = appInfo.value?.id
  if (!id) return
  // 名字没变就直接关闭，不发请求
  if (newName === (appInfo.value?.appName || '')) {
    renameVisible.value = false
    return
  }
  renaming.value = true
  try {
    const res = await updateApp({ id, appName: newName })
    if (res.data.code === 0) {
      if (appInfo.value) appInfo.value.appName = newName
      message.success('修改成功')
      renameVisible.value = false
    } else {
      message.error('修改失败：' + (res.data.message || ''))
    }
  } catch (err) {
    console.error('修改名字失败：', err)
    message.error('修改失败')
  } finally {
    renaming.value = false
  }
}

const goBack = () => {
  router.back()
}

const sendQuickPrompt = (text: string) => {
  userInput.value = text
  sendMessage()
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 反馈：点赞 / 点踩（前端提示，与 AiChatPage 行为一致）
const likeMessage = (_idx: number) => {
  message.success('感谢你的反馈～')
}

const dislikeMessage = (_idx: number) => {
  message.success('已记录，会持续改进')
}

const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        const historyMessages: Message[] = chatHistories
            .map((chat) => ({
              type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
              content: chat.message || '',
              createTime: chat.createTime,
            }))
            .reverse()
        if (isLoadMore) {
          messages.value.unshift(...historyMessages)
        } else {
          messages.value = historyMessages
        }
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 记录到"最近项目"（best-effort，fire-and-forget，不阻塞首屏渲染）
      // id 为 19 位雪花 ID，保持字符串传递，禁止 Number() 转换
      recentStore.recordOpen(id)

      await loadChatHistory()
      if (messages.value.length >= 2) {
        updatePreview()
      }
      if (
          appInfo.value.initPrompt &&
          isOwner.value &&
          messages.value.length === 0 &&
          historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

const sendInitialMessage = async (prompt: string) => {
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    failed: false,
    prompt: prompt,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

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

const autoResize = () => {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 200) + 'px'
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) return

  const messageContent = userInput.value.trim()
  userInput.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  messages.value.push({
    type: 'user',
    content: messageContent,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    failed: false,
    prompt: messageContent,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  await generateCode(messageContent, aiMessageIndex)
}

const stopGeneration = () => {
  if (!isGenerating.value || !eventSource) return

  eventSource.close()
  eventSource = null
  clearInactivityTimer()

  const currentAiMsg = messages.value[messages.value.length - 1]
  if (currentAiMsg && currentAiMsg.type === 'ai' && currentAiMsg.loading) {
    currentAiMsg.loading = false
    currentAiMsg.stopped = true
  }

  isGenerating.value = false
  message.info('已停止生成')
}

const retryMessage = async (idx: number) => {
  const msg = messages.value[idx]
  if (!msg || msg.type !== 'ai') return
  const prompt = msg.prompt || ''
  if (!prompt) {
    message.error('缺少原始提示词，无法重试')
    return
  }
  // 复位该条失败消息的状态，重新发起生成
  msg.failed = false
  msg.stopped = false
  msg.loading = true
  msg.content = ''
  await nextTick()
  scrollToBottom()
  isGenerating.value = true
  await generateCode(prompt, idx)
}

const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  streamCompleted = false

  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })
    if (genMode.value) params.set('mode', genMode.value)

    const url = `${baseURL}/app/chat/gen/code?${params}`

    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    armInactivityTimer(aiMessageIndex)

    let fullContent = ''

    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        if (content !== undefined && content !== null) {
          fullContent += content
          if (
            !streamCompleted &&
            fullContent.includes('<title>对话停止页面</title>')
          ) {
            streamCompleted = true
            isGenerating.value = false
            eventSource?.close()
            eventSource = null
            router.push('/app/stop')
            return
          }
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
          // 每收到一块数据就重置超时计时，只有长时间无数据才判定超时
          armInactivityTimer(aiMessageIndex)
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()
      eventSource = null
      clearInactivityTimer()

      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        const errorMessage = errorData.message || '生成过程中出现错误'
        if (isImageRelatedError(errorMessage)) {
          streamCompleted = true
          isGenerating.value = false
          eventSource?.close()
          eventSource = null
          clearInactivityTimer()
          router.push('/app/stop')
          return
        }
        const friendly = toUserFriendlyError(errorMessage)
        messages.value[aiMessageIndex].content = `❌ ${friendly}`
        messages.value[aiMessageIndex].loading = false
        message.error(friendly)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
        eventSource = null
        clearInactivityTimer()
      } catch (parseError) {
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
        eventSource = null
        clearInactivityTimer()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

const handleError = (error: unknown, aiMessageIndex: number) => {
  if (isImageRelatedError(error)) {
    isGenerating.value = false
    eventSource?.close()
    eventSource = null
    router.push('/app/stop')
    return
  }
  const friendly = toUserFriendlyError(error)
  messages.value[aiMessageIndex].content = `❌ ${friendly}`
  messages.value[aiMessageIndex].loading = false
  messages.value[aiMessageIndex].failed = true
  message.error(friendly)
  isGenerating.value = false
  eventSource?.close()
  eventSource = null
}

const isImageRelatedError = (err: unknown): boolean => {
  const raw = err instanceof Error ? err.message : String(err ?? '')
  const lower = raw.toLowerCase()
  return (
    lower.includes('image') ||
    lower.includes('clipboard') ||
    lower.includes('剪贴板') ||
    lower.includes('图片') ||
    lower.includes('vision') ||
    lower.includes('视觉')
  )
}

const toUserFriendlyError = (err: unknown): string => {
  const raw = err instanceof Error ? err.message : String(err ?? '')
  const lower = raw.toLowerCase()
  if (
    lower.includes('image') ||
    lower.includes('clipboard') ||
    lower.includes('剪贴板') ||
    lower.includes('图片') ||
    lower.includes('vision') ||
    lower.includes('视觉')
  ) {
    return '当前模型暂不支持图片 / 视觉输入，请改用文字描述你的需求 🙏'
  }
  if (lower.includes('not support') || lower.includes('不支持')) {
    return '当前模型暂不支持该输入方式，请调整后重试'
  }
  return '抱歉，生成过程中出现了错误，请稍后重试。'
}

const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

const isFullHtmlDoc = (content?: string): boolean => {
  if (!content) return false
  let c = content.trim()
  c = c.replace(/^```(?:html)?\s*/i, '').replace(/```\s*$/i, '').trim()
  const startsWithHtml = /^<!DOCTYPE html/i.test(c) || /^<html[\s>]/i.test(c)
  const hasHtmlClose = c.includes('</html>')
  return startsWithHtml && hasHtmlClose
}

// PPT 导出：检测 AI 消息是否包含结构化幻灯片 JSON（后端 generatePptMessage 产出）
const PPT_FENCE = '```pptx-slides'
const isPptSlides = (content?: string): boolean => {
  return !!content && content.includes(PPT_FENCE)
}
const extractPptJson = (content?: string): string => {
  if (!content) return ''
  const start = content.indexOf(PPT_FENCE)
  if (start < 0) return ''
  const afterFence = content.indexOf('\n', start) + 1
  const end = content.indexOf('```', afterFence)
  const json = end > afterFence ? content.substring(afterFence, end) : content.substring(afterFence)
  return json.trim()
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-frame') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

const getPlaceholder = () => {
  if (isGenerating.value) {
    return 'AI 正在生成中...'
  }
  if (!isOwner.value && !isAdmin.value) {
    return '无法在别人的作品下对话哦~'
  }
  return '请输入修改描述，@快捷调用技能'
}

const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isGenerating.value) {
    stopGeneration()
  }
}

onMounted(() => {
  fetchAppInfo()
  window.addEventListener('keydown', handleGlobalKeydown)

  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
  clearInactivityTimer()
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
})
</script>

<style scoped>
.chat-page-wrapper {
  width: 100%;
  /* 该页是独立整页路由（无侧边栏/无 .content 内边距包裹），须直接铺满视口；
     旧值 height:calc(100vh-48px) + margin:-24px 是「错误地按 BasicLayout 内 24px 内边距」写的，
     会导致整页（含顶部栏）被上移 24px 而顶部被遮挡、底部留 48px 空白 */
  height: 100vh;
  margin: 0;
  padding: 0;
  background: #f7f8fa;
  overflow: hidden;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

/* 顶部栏 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #eef0f5;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #555;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: #f2f3f5;
  color: #1a1a1a;
}

.icon-btn svg {
  width: 18px;
  height: 18px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.title-text {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

.gen-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  background: #fff7e6;
  color: #fa8c16;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #fa8c16;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.balance-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 8px;
  background: #fff7e8;
  border: 1px solid #ffe2b8;
  color: #d48806;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.balance-badge svg {
  color: #faad14;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid #e5e6eb;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  border-color: #c9cdd4;
  color: #1d2129;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary {
  background: #165dff;
  border-color: #165dff;
  color: #fff;
}

.action-btn.primary:hover:not(:disabled) {
  background: #0e42d2;
  border-color: #0e42d2;
}

.action-btn svg {
  width: 15px;
  height: 15px;
}

/* 主体区域 */
.chat-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* 中间对话区 */
.chat-main {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.messages-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px 59px;
  display: flex;
  flex-direction: column;
  align-items: center;
  scroll-behavior: smooth;
}

/* 欢迎页 */
.welcome-section {
  text-align: center;
  max-width: 560px;
  margin: 40px auto 60px;
}

.welcome-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 28px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 8px;
}

.welcome-desc {
  font-size: 14px;
  color: #86909c;
  margin: 0 0 24px;
  line-height: 1.6;
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chip {
  padding: 7px 14px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 20px;
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chip:hover {
  border-color: #165dff;
  color: #165dff;
  background: #f2f3ff;
}

/* 消息行 */
.msg-row {
  width: 100%;
  max-width: 720px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
}

.msg-row.user {
  align-items: flex-end;
}

.msg-row.ai {
  align-items: flex-start;
}

.user-bubble {
  max-width: 70%;
  padding: 9px 16px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 18px;
  font-size: 14px;
  color: #1d2129;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  word-wrap: break-word;
  overflow-wrap: break-word;
}

/* AI 消息 */
.ai-block {
  width: 100%;
}

.ai-content {
  font-size: 15px;
  line-height: 1.75;
  color: #1d2129;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #86909c;
  font-size: 14px;
  padding: 8px 0;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9cdd4;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

.thinking-text {
  margin-left: 4px;
}

.thinking {
  cursor: pointer;
  transition: color 0.2s ease;
}

.thinking:hover .thinking-text {
  color: #ff4d4f;
}

/* 流式生成中的打字光标 */
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: -0.1em;
  background: #6c3ce0;
  border-radius: 1px;
  animation: cursor-blink 0.8s steps(2, start) infinite;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.stopped-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
  color: #d46b08;
  font-size: 13px;
  margin-top: 10px;
}

.stopped-box svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.failed-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  border-radius: 8px;
  color: #cf1322;
  font-size: 13px;
  margin-top: 10px;
}

.failed-box svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  padding: 5px 12px;
  background: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: #ff4d4f;
  color: #fff;
}

.retry-btn svg {
  width: 13px;
  height: 13px;
}

.gen-success {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  color: #389e0d;
  font-size: 13px;
  margin-bottom: 14px;
}

.selfcheck-btn {
  margin-left: auto;
  padding: 3px 10px;
  border: 1px solid #7c3aed;
  color: #7c3aed;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
}

.selfcheck-btn:hover:not(:disabled) {
  background: #7c3aed;
  color: #fff;
}

.selfcheck-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.selfcheck-result {
  margin: 0 0 12px;
  padding: 10px 12px;
  background: #fff1f0;
  border: 1px solid #ffa39e;
  border-radius: 6px;
  font-size: 13px;
}

.selfcheck-result.ok {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.selfcheck-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #d4380d;
  font-weight: 500;
}

.selfcheck-result.ok .selfcheck-title {
  color: #389e0d;
}

.selfcheck-fix {
  color: #7c3aed;
  cursor: pointer;
  font-weight: 500;
  text-decoration: underline;
}

.selfcheck-issues {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #cf1322;
  line-height: 1.6;
}

.selfcheck-result.ok .selfcheck-issues {
  color: #389e0d;
}

.success-icon {
  font-size: 15px;
}

.code-wrap {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 8px 14px;
  margin: 10px 0;
}

.code-wrap summary {
  cursor: pointer;
  color: #409eff;
  font-size: 13px;
  user-select: none;
  list-style: none;
}

.code-wrap summary::before {
  content: '▶ ';
  display: inline-block;
  font-size: 9px;
  transition: transform 0.2s;
}

.code-wrap[open] summary::before {
  transform: rotate(90deg);
}

.code-block {
  margin: 10px 0 0;
  max-height: 380px;
  overflow: auto;
  color: #d4d4d4;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.ai-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  opacity: 1;
}

.act-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #c9cdd4;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.act-btn:hover {
  background: #f2f3f5;
  color: #4e5969;
}

.act-btn svg {
  width: 14px;
  height: 14px;
}

.bottom-pad {
  height: 0;
  flex-shrink: 0;
}

/* 底部输入区域 */
.input-section {
  flex-shrink: 0;
  margin-top: -59px;
  padding: 2px 20px 0;
  background: linear-gradient(180deg, rgba(247, 248, 250, 0) 0%, rgba(247, 248, 250, 0.95) 40%, #f7f8fa 100%);
}

.input-card {
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 16px;
  padding: 7px 12px 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.input-card:focus-within {
  border-color: #165dff;
  box-shadow: 0 4px 24px rgba(22, 93, 255, 0.12);
}

.chat-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  max-height: 180px;
  font-family: inherit;
  color: #1d2129;
  background: transparent;
  padding: 2px 0;
}

.chat-input::placeholder {
  color: #c9cdd4;
}

.chat-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
  padding-top: 3px;
  border-top: 1px solid #f2f3f5;
}

.bar-left,
.bar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.bar-left {
  flex: 1;
  overflow-x: auto;
  overflow-y: visible;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.bar-left::-webkit-scrollbar {
  display: none;
}

/* 胶囊工具栏 */
.chip-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 3px;
}

.chip-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: visible;
  flex-wrap: nowrap;
}

/* 胶囊按钮风格 */
.chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: #f7f8fa;
  border: none;
  border-radius: 999px;
  color: #4e5969;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  height: 28px;
}

.chip-btn:hover {
  filter: brightness(0.97);
}

.chip-btn svg {
  width: 16px;
  height: 16px;
}

.chip-plus {
  width: 34px;
  height: 34px;
  padding: 0;
  justify-content: center;
  background: #fff;
  border: 1.5px solid #e5e6eb;
  color: #1d2129;
}
.chip-plus:hover {
  border-color: #c9cdd4;
  background: #f7f8fa;
}
.chip-plus svg {
  width: 18px;
  height: 18px;
}

.chip-expand {
  background: #fff0f5;
  color: #f53f6b;
}

.chip-expand:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.chip-skill {
  background: #e8fff3;
  color: #00b42a;
}

.chip-deep {
  background: linear-gradient(135deg, #6b5bff 0%, #4e3bff 100%);
  color: #fff;
  padding: 7px 18px;
}

.chip-deep:hover {
  filter: brightness(1.05);
}

.chip-arrow {
  width: 12px !important;
  height: 12px !important;
  margin-left: 2px;
}

/* 下拉菜单容器 */
.chip-dropdown-wrap {
  position: relative;
  flex-shrink: 0;
}

/* 下拉菜单 */
.chip-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 220px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 8px;
  z-index: 1000;
  animation: dropdownFadeIn 0.18s ease;
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chip-dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.chip-dropdown-item:hover {
  background: #f2f3f5;
}

.chip-dropdown-icon {
  font-size: 22px;
  line-height: 1;
}

.chip-dropdown-text {
  font-size: 14px;
  color: #1d2129;
  font-weight: 500;
}

.chip-dropdown-item.active {
  background: #eaf2ff;
}

.chip-dropdown-item.active .chip-dropdown-text {
  color: #165dff;
}

/* 素材/技能选择弹窗 */
.picker-loading,
.picker-empty {
  padding: 40px 0;
  text-align: center;
  color: #86909c;
  font-size: 14px;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 4px;
}

.picker-card {
  border: 1px solid #eef0f5;
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.picker-card:hover {
  border-color: #165dff;
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.12);
}

.picker-thumb {
  width: 100%;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  background: #f2f3f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  overflow: hidden;
}

.picker-file {
  background: #f2f3f5;
  color: #86909c;
}

.picker-skill {
  font-size: 28px;
}

.picker-name {
  width: 100%;
  font-size: 13px;
  color: #1d2129;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-desc {
  width: 100%;
  font-size: 11px;
  color: #86909c;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.picker-more {
  text-align: center;
  padding: 12px 0 4px;
  color: #165dff;
  font-size: 13px;
  cursor: pointer;
}

.send-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: #165dff;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  background: #0e42d2;
}

.send-btn:disabled {
  background: #c9cdd4;
  cursor: not-allowed;
}

.send-btn svg {
  width: 15px;
  height: 15px;
}

.stop-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: #ff4d4f;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  animation: stopGlow 2s ease-in-out infinite;
}

.stop-btn:hover {
  background: #cf1322;
}

.stop-btn svg {
  width: 13px;
  height: 13px;
}

@keyframes stopGlow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(255, 77, 79, 0); }
}

.hint-text {
  text-align: center;
  font-size: 12px;
  color: #c9cdd4;
  margin: 8px 0 0;
}

.hint-text kbd {
  display: inline-block;
  padding: 2px 5px;
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-family: monospace;
  font-size: 10px;
  color: #86909c;
}

/* 右侧预览面板 */
.preview-panel {
  flex: 1 1 0;
  min-width: 0;
  background: #fff;
  border-left: 1px solid #eef0f5;
  display: flex;
  flex-direction: column;
  transition: flex-basis 0.3s ease, width 0.3s ease;
  overflow: hidden;
}

.preview-panel.collapsed {
  flex: 0 0 28px;
  width: 28px;
}

.collapse-btn {
  width: 28px;
  height: 44px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-right: none;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #86909c;
  flex-shrink: 0;
  margin: 20px 0 0 -1px;
  position: absolute;
  left: 0;
  top: 50px;
  z-index: 2;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  color: #165dff;
  width: 32px;
}

.collapse-btn svg {
  width: 13px;
  height: 13px;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 320px;
  height: 100%;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid #f2f3f5;
  flex-shrink: 0;
}

.preview-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-action {
  padding: 4px 10px;
  font-size: 12px;
}

.preview-action svg {
  width: 13px;
  height: 13px;
}

.preview-label {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.preview-body {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: #f7f8fa;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #86909c;
}

.empty-ico {
  font-size: 36px;
  margin-bottom: 10px;
}

.preview-empty p {
  margin: 0;
  font-size: 13px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #86909c;
  gap: 10px;
}

.loader {
  width: 26px;
  height: 26px;
  border: 2px solid #e5e6eb;
  border-top-color: #165dff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.preview-loading p {
  margin: 0;
  font-size: 13px;
}

.preview-loading .generating-clickable {
  cursor: pointer;
  transition: color 0.2s ease;
}

.preview-loading .generating-clickable:hover {
  color: #ff4d4f;
}

.stop-mini-btn {
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.stop-mini-btn:hover {
  background: #fff1f0;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

/* 响应式 */
@media (max-width: 1200px) {
  .preview-content {
    min-width: 280px;
  }
}

@media (max-width: 900px) {
  .preview-panel {
    display: none;
  }
}

@media (max-width: 768px) {
  .chat-page-wrapper {
    height: 100vh;
  }

  .chat-header {
    padding: 10px 14px;
  }

  .title-text {
    max-width: 160px;
    font-size: 14px;
  }

  .action-btn span {
    display: none;
  }

  .action-btn {
    padding: 7px 10px;
  }

  .messages-scroll {
    padding: 20px 14px 0;
  }

  .welcome-section {
    margin: 20px auto 40px;
  }

  .welcome-title {
    font-size: 20px;
  }

  .user-bubble {
    max-width: 85%;
  }

  .input-section {
    padding: 10px 12px 16px;
  }

  .bottom-pad {
    height: 16px;
  }
}
</style>
