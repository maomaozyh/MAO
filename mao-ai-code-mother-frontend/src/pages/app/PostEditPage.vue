<template>
  <div class="post-edit-page">
    <!-- 顶部返回导航 -->
    <div class="nav-bar">
      <span class="back-link" @click="goBack">
        <span class="back-icon">&lt;</span>
        返回
      </span>
      <span class="nav-title">社区首页</span>
    </div>

    <!-- 发布卡片 -->
    <div class="post-card">
      <h1 class="page-title">发布帖子</h1>

      <!-- 标题 -->
      <div class="form-group">
        <label class="form-label">标题</label>
        <div class="title-input-wrap">
          <input
            v-model="form.title"
            class="title-input"
            placeholder="请概述帖子主题..."
            maxlength="150"
          />
          <span class="char-count">{{ form.title.length }}/150</span>
        </div>
      </div>

      <!-- 板块 -->
      <div class="form-group">
        <label class="form-label">板块</label>
        <div class="select-wrap">
          <select v-model="form.category" class="category-select">
            <option value="">点击选择其他板块</option>
            <option v-for="cat in categories" :key="cat.key" :value="cat.key">{{ cat.label }}</option>
          </select>
          <span class="select-arrow">▾</span>
        </div>
      </div>

      <!-- 标签 -->
      <div class="form-group">
        <label class="form-label">标签（最多 5 个）</label>
        <div class="tags-input-wrap">
          <div class="tags-list">
            <span v-for="(tag, index) in form.tags" :key="index" class="tag-item">
              #{{ tag }}
              <span class="tag-remove" @click="removeTag(index)">×</span>
            </span>
          </div>
          <input
            v-model="tagInput"
            class="tag-input"
            placeholder="输入标签后按回车添加"
            @keydown.enter.prevent="addTag"
            @keydown.backspace="handleBackspace"
          />
        </div>
      </div>

      <!-- 内容编辑器（富文本） -->
      <div class="form-group">
        <label class="form-label">内容</label>
        <div class="editor-wrap">
          <!-- 工具栏 -->
          <div class="editor-toolbar">
            <div class="tool-group">
              <button type="button" class="tool-btn" title="撤销" @mousedown.prevent @click="exec('undo')">↶</button>
              <button type="button" class="tool-btn" title="重做" @mousedown.prevent @click="exec('redo')">↷</button>
            </div>
            <div class="tool-divider"></div>
            <div class="tool-group tool-group--relative">
              <button ref="paragraphBtnRef" type="button" class="tool-btn" title="段落格式" @mousedown.prevent @click="toggleParagraphMenu">≡</button>
              <div v-if="showParagraphMenu" ref="paragraphMenuRef" class="tool-menu">
                <div class="tool-menu-item" @mousedown.prevent @click="setParagraph('p')">正文</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setParagraph('h2')">标题 2</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setParagraph('h3')">标题 3</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setParagraph('blockquote')">引用</div>
              </div>
            </div>
            <div class="tool-group tool-group--relative">
              <button ref="fontBtnRef" type="button" class="tool-btn text-tool" title="字体" @mousedown.prevent @click="toggleFontMenu">
                <span class="tool-text">T</span>
                <span class="tool-sub">默认</span>
              </button>
              <div v-if="showFontMenu" ref="fontMenuRef" class="tool-menu">
                <div class="tool-menu-item" @mousedown.prevent @click="setFont('inherit')">默认</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setFont('Microsoft YaHei')">微软雅黑</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setFont('SimSun')">宋体</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setFont('SimHei')">黑体</div>
                <div class="tool-menu-item" @mousedown.prevent @click="setFont('KaiTi')">楷体</div>
              </div>
            </div>
            <div class="tool-divider"></div>
            <div class="tool-group">
              <button type="button" class="tool-btn" :class="{ active: fmtState.insertUnorderedList }" title="无序列表" @mousedown.prevent @click="exec('insertUnorderedList')">☰</button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.insertOrderedList }" title="有序列表" @mousedown.prevent @click="exec('insertOrderedList')">☷</button>
            </div>
            <div class="tool-divider"></div>
            <div class="tool-group">
              <button type="button" class="tool-btn" :class="{ active: fmtState.bold }" title="加粗" @mousedown.prevent @click="exec('bold')"><b>B</b></button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.italic }" title="斜体" @mousedown.prevent @click="exec('italic')"><i>I</i></button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.strikeThrough }" title="删除线" @mousedown.prevent @click="exec('strikeThrough')"><s>S</s></button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.underline }" title="下划线" @mousedown.prevent @click="exec('underline')"><u>U</u></button>
            </div>
            <div class="tool-divider"></div>
            <div class="tool-group">
              <button type="button" class="tool-btn" :class="{ active: fmtState.justifyLeft }" title="左对齐" @mousedown.prevent @click="exec('justifyLeft')">☰</button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.justifyCenter }" title="居中" @mousedown.prevent @click="exec('justifyCenter')">≡</button>
              <button type="button" class="tool-btn" :class="{ active: fmtState.justifyRight }" title="右对齐" @mousedown.prevent @click="exec('justifyRight')">☰</button>
            </div>
            <div class="tool-divider"></div>
            <div class="tool-group">
              <button type="button" class="tool-btn" title="插入链接" @mousedown.prevent @click="insertLink">🔗</button>
              <button type="button" class="tool-btn" title="插入图片" @mousedown.prevent @click="openImageModal">🖼️</button>
            </div>
          </div>
          <!-- 编辑区域（contentEditable） -->
          <div
            ref="editorRef"
            class="editor-content"
            contenteditable="true"
            data-placeholder="分享你的想法、经验或问题..."
            @input="onEditorInput"
            @paste="onPaste"
            @keyup="updateActive"
            @mouseup="updateActive"
            @focus="updateActive"
            @blur="updateActive"
          ></div>
          <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onFileSelected" />
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="form-actions">
        <button class="btn-cancel" @click="goBack">取消</button>
        <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>

    <!-- 插入图片弹窗（URL / 本地上传到 COS） -->
    <a-modal v-model:open="imageModalOpen" title="插入图片" :footer="null" :width="460" :destroyOnClose="true">
      <a-tabs>
        <a-tab-pane key="url" tab="图片地址">
          <a-input v-model:value="imageUrl" placeholder="https://..." @keyup.enter="confirmImageUrl" />
          <div style="margin-top:14px;text-align:right">
            <a-button @click="closeImageModal">取消</a-button>
            <a-button type="primary" style="margin-left:8px" @click="confirmImageUrl">插入</a-button>
          </div>
        </a-tab-pane>
        <a-tab-pane key="upload" tab="本地上传">
          <div class="upload-area" @click="triggerFilePicker" style="border:1px dashed #dcdde0;border-radius:4px;padding:32px;text-align:center;cursor:pointer;background:#fafbfc;">
            <div v-if="!imageUploading" style="color:#4e5969;">
              <div style="font-size:36px;margin-bottom:10px;">📁</div>
              <div style="font-size:14px;">点击选择本地图片</div>
              <div style="font-size:12px;color:#868c96;margin-top:6px;">支持 jpg / png / gif 等，上传到素材库 (COS)</div>
            </div>
            <div v-else style="color:#4056d8;font-size:14px;">上传中...</div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DOMPurify from 'dompurify'
import { addCommunityPost } from '@/api/communityPostController'
import { uploadMaterial, getMaterialVOById } from '@/api/materialController'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const submitting = ref(false)
const tagInput = ref('')
const form = ref({
  title: '',
  content: '',
  category: '',
  tags: [] as string[],
})

const categories = [
  { key: 'official', label: '官方公告' },
  { key: 'tips', label: '经验技巧' },
  { key: 'suggest', label: '产品建议' },
  { key: 'feedback', label: '问题反馈' },
  { key: 'other', label: '其他' },
]

const goBack = () => {
  router.back()
}

const addTag = () => {
  const tag = tagInput.value.trim()
  if (!tag) return
  if (form.value.tags.length >= 5) { message.warning('最多添加 5 个标签'); return }
  if (form.value.tags.includes(tag)) { message.warning('该标签已添加'); return }
  if (tag.length > 20) { message.warning('标签长度不能超过 20 字'); return }
  form.value.tags.push(tag)
  tagInput.value = ''
}

const removeTag = (index: number) => {
  form.value.tags.splice(index, 1)
}

const handleBackspace = () => {
  if (!tagInput.value && form.value.tags.length > 0) {
    form.value.tags.pop()
  }
}

/* ===== 富文本编辑器（contentEditable + execCommand + DOMPurify） ===== */
const editorRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const paragraphBtnRef = ref<HTMLElement | null>(null)
const fontBtnRef = ref<HTMLElement | null>(null)
const paragraphMenuRef = ref<HTMLElement | null>(null)
const fontMenuRef = ref<HTMLElement | null>(null)

/** 当前光标所在位置的格式状态（决定工具按钮是否高亮） */
const fmtState = ref({
  bold: false,
  italic: false,
  underline: false,
  strikeThrough: false,
  insertUnorderedList: false,
  insertOrderedList: false,
  justifyLeft: false,
  justifyCenter: false,
  justifyRight: false,
})
const showParagraphMenu = ref(false)
const showFontMenu = ref(false)

/** 执行一条富文本命令（先聚焦编辑器，再 execCommand，再刷新状态/同步内容） */
const exec = (cmd: string, value?: string) => {
  if (!editorRef.value) return
  editorRef.value.focus()
  document.execCommand(cmd, false, value)
  updateActive()
  onEditorInput()
}
const updateActive = () => {
  fmtState.value.bold = document.queryCommandState('bold')
  fmtState.value.italic = document.queryCommandState('italic')
  fmtState.value.underline = document.queryCommandState('underline')
  fmtState.value.strikeThrough = document.queryCommandState('strikeThrough')
  fmtState.value.insertUnorderedList = document.queryCommandState('insertUnorderedList')
  fmtState.value.insertOrderedList = document.queryCommandState('insertOrderedList')
  fmtState.value.justifyLeft = document.queryCommandState('justifyLeft')
  fmtState.value.justifyCenter = document.queryCommandState('justifyCenter')
  fmtState.value.justifyRight = document.queryCommandState('justifyRight')
}
/** 编辑区输入：补链接 target/rel，再用 DOMPurify 净化后写回 form.content */
const onEditorInput = () => {
  if (!editorRef.value) return
  editorRef.value.querySelectorAll('a').forEach(a => {
    a.target = '_blank'
    a.rel = 'noopener noreferrer'
  })
  form.value.content = DOMPurify.sanitize(editorRef.value.innerHTML)
}
/** 粘贴时净化（防止从外部页面带入脚本/样式） */
const onPaste = (e: ClipboardEvent) => {
  e.preventDefault()
  const cd = e.clipboardData
  if (!cd) return
  const data = cd.getData('text/html') || cd.getData('text/plain')
  document.execCommand('insertHTML', false, DOMPurify.sanitize(data))
}
const onSelectionChange = () => {
  if (document.activeElement === editorRef.value) updateActive()
}

const insertLink = () => {
  const sel = window.getSelection()?.toString() || ''
  const defaultUrl = sel.startsWith('http') ? sel : 'https://'
  const url = window.prompt('请输入链接地址（https://）', defaultUrl)
  if (!url) return
  exec('createLink', url)
}

/* 插入图片弹窗 */
const imageModalOpen = ref(false)
const imageUrl = ref('')
const imageUploading = ref(false)
const openImageModal = () => {
  imageUrl.value = ''
  imageUploading.value = false
  imageModalOpen.value = true
}
const closeImageModal = () => { imageModalOpen.value = false }
const confirmImageUrl = () => {
  const u = imageUrl.value.trim()
  if (!u) { message.warning('请输入图片地址'); return }
  exec('insertImage', u)
  closeImageModal()
}
const triggerFilePicker = () => { fileInputRef.value?.click() }
/** 上传本地图片到 COS（走 /material/upload + getMaterialVOById 拿 url），再插入编辑器 */
const onFileSelected = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  imageUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const upRes = await uploadMaterial(fd)
    if (upRes.data.code !== 0 || !upRes.data.data) {
      throw new Error(upRes.data.message || '上传失败')
    }
    const voRes = await getMaterialVOById({ id: upRes.data.data })
    if (voRes.data.code !== 0 || !voRes.data.data?.url) {
      throw new Error(voRes.data.message || '获取图片地址失败')
    }
    exec('insertImage', voRes.data.data.url)
    closeImageModal()
  } catch (err: any) {
    console.error(err)
    message.error(err?.message || '图片上传失败')
  } finally {
    imageUploading.value = false
    if (input) input.value = ''
  }
}

const setParagraph = (tag: string) => {
  exec('formatBlock', '<' + tag + '>')
  showParagraphMenu.value = false
}
const setFont = (family: string) => {
  exec('fontName', family)
  showFontMenu.value = false
}
const toggleParagraphMenu = () => {
  showParagraphMenu.value = !showParagraphMenu.value
  showFontMenu.value = false
}
const toggleFontMenu = () => {
  showFontMenu.value = !showFontMenu.value
  showParagraphMenu.value = false
}

/** 点外面关闭段落/字体下拉 */
const onDocMouseDown = (e: MouseEvent) => {
  const t = e.target as Node
  if (showParagraphMenu.value
      && !paragraphMenuRef.value?.contains(t)
      && !paragraphBtnRef.value?.contains(t)) {
    showParagraphMenu.value = false
  }
  if (showFontMenu.value
      && !fontMenuRef.value?.contains(t)
      && !fontBtnRef.value?.contains(t)) {
    showFontMenu.value = false
  }
}

onMounted(() => {
  if (editorRef.value && form.value.content) {
    editorRef.value.innerHTML = DOMPurify.sanitize(form.value.content)
  }
  document.addEventListener('selectionchange', onSelectionChange)
  document.addEventListener('mousedown', onDocMouseDown)
})
onBeforeUnmount(() => {
  document.removeEventListener('selectionchange', onSelectionChange)
  document.removeEventListener('mousedown', onDocMouseDown)
})

const handleSubmit = async () => {
  if (!form.value.title.trim()) { message.warning('请输入帖子标题'); return }
  // 去掉 HTML 标签判断文本是否为空（防止 <p></p> 这种"看似有内容"通过）
  const text = (form.value.content || '').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim()
  if (!text) { message.warning('请输入帖子内容'); return }

  submitting.value = true
  try {
    const res = await addCommunityPost({
      title: form.value.title.trim(),
      content: form.value.content,
      category: form.value.category || undefined,
      tags: form.value.tags.length > 0 ? form.value.tags : undefined,
    })

    if (res.data.code === 0) {
      message.success('发布成功')
      router.replace('/community')
    } else {
      message.error(res.data.message || '发布失败')
    }
  } catch (error) {
    console.error('发布帖子失败', error)
    message.error('发布失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.post-edit-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding: 24px;
}

/* 顶部导航 */
.nav-bar {
  max-width: 900px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #1f2329;
  cursor: pointer;
  transition: color 0.2s;
}

.back-link:hover {
  color: #4056d8;
}

.back-icon {
  font-size: 12px;
}

.nav-title {
  color: #1f2329;
  font-weight: 500;
}

/* 发布卡片 */
.post-card {
  max-width: 900px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 12px;
  padding: 24px 28px 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 24px 0;
}

/* 表单组 */
.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
  margin-bottom: 8px;
}

/* 标题输入 */
.title-input-wrap {
  position: relative;
}

.title-input {
  width: 100%;
  padding: 10px 60px 10px 12px;
  border: 1px solid #dcdde0;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.title-input:focus {
  border-color: #4056d8;
}

.char-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: #868c96;
}

/* 板块选择 */
.select-wrap {
  position: relative;
}

.category-select {
  width: 100%;
  padding: 10px 36px 10px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 14px;
  color: #1f2329;
  background: #fff;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
  box-sizing: border-box;
  appearance: none;
  -webkit-appearance: none;
}

.category-select:focus {
  border-color: #4056d8;
}

.category-select option {
  padding: 8px;
}

.select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #868c96;
  font-size: 12px;
  pointer-events: none;
}

/* 标签输入 */
.tags-input-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  min-height: 42px;
  transition: border-color 0.2s;
}

.tags-input-wrap:focus-within {
  border-color: #4056d8;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #eef0ff;
  color: #4056d8;
  border-radius: 4px;
  font-size: 13px;
}

.tag-remove {
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.tag-remove:hover {
  opacity: 1;
}

.tag-input {
  flex: 1;
  min-width: 120px;
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
}

/* 编辑器 */
.editor-wrap {
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow: visible;
  position: relative;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  background: #fafbfc;
  border-bottom: 1px solid #e5e6eb;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tool-group--relative {
  position: relative;
}

.tool-btn {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #4e5969;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.tool-btn:hover {
  background: #eef0ff;
  color: #4056d8;
}

/* 格式激活态：光标在对应格式中时高亮 */
.tool-btn.active {
  background: #eef0ff;
  color: #4056d8;
}

.tool-btn b {
  font-weight: 700;
}

.tool-btn i {
  font-style: italic;
}

.tool-btn s {
  text-decoration: line-through;
}

.tool-btn u {
  text-decoration: underline;
}

.text-tool {
  gap: 2px;
  padding: 0 8px;
}

.tool-text {
  font-size: 14px;
  font-weight: 500;
}

.tool-sub {
  font-size: 11px;
  color: #868c96;
}

.tool-divider {
  width: 1px;
  height: 20px;
  background: #e5e6eb;
  margin: 0 4px;
}

/* 段落/字体下拉菜单 */
.tool-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  z-index: 30;
  min-width: 120px;
  padding: 4px 0;
}

.tool-menu-item {
  padding: 6px 12px;
  font-size: 13px;
  color: #1f2329;
  cursor: pointer;
  white-space: nowrap;
  line-height: 1.5;
}

.tool-menu-item:hover {
  background: #f5f6f8;
}

.editor-content {
  width: 100%;
  min-height: 320px;
  padding: 12px;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  color: #1f2329;
  font-family: inherit;
  box-sizing: border-box;
  overflow-y: auto;
}

/* contentEditable 空内容时显示占位文字 */
.editor-content:empty::before {
  content: attr(data-placeholder);
  color: #a9aeb8;
  pointer-events: none;
}

/* 编辑器内富文本的渲染样式（输入时就能看到效果） */
.editor-content p { margin: 0 0 8px 0; }
.editor-content h1,
.editor-content h2,
.editor-content h3 { font-weight: 600; margin: 14px 0 8px 0; line-height: 1.4; }
.editor-content h1 { font-size: 20px; }
.editor-content h2 { font-size: 18px; }
.editor-content h3 { font-size: 16px; }
.editor-content ul,
.editor-content ol { margin: 0 0 8px 0; padding-left: 24px; }
.editor-content li { margin-bottom: 4px; }
.editor-content blockquote {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid #dcdde0;
  color: #4e5969;
  background: #fafbfc;
}
.editor-content img { max-width: 100%; height: auto; border-radius: 4px; margin: 6px 0; }
.editor-content a { color: #4056d8; text-decoration: underline; }

/* 底部按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.btn-cancel {
  padding: 8px 24px;
  border: 1px solid #dcdde0;
  background: #fff;
  color: #4e5969;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  border-color: #4056d8;
  color: #4056d8;
}

.btn-submit {
  padding: 8px 28px;
  border: none;
  background: #1f2329;
  color: #fff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-submit:hover:not(:disabled) {
  background: #333;
}

.btn-submit:disabled {
  background: #a9aeb8;
  cursor: not-allowed;
}
</style>
