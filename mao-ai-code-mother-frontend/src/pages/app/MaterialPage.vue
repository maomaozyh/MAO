<template>
  <div class="page-wrap">
    <div class="page-header">
      <div class="header-left">
        <h1>素材库</h1>
        <div class="tab-group">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="tab-item"
            :class="{ active: activeTab === t.key }"
            @click="activeTab = t.key"
          >{{ t.label }}</button>
        </div>
      </div>
      <div class="header-right">
        <div class="storage-bar-wrap">
          <span>已用 {{ usedStorage }} / 共 100.0 MB</span>
          <div class="storage-progress">
            <div class="storage-progress-fill" :style="{ width: storagePercent + '%' }"></div>
          </div>
        </div>
        <div class="action-buttons">
          <button class="btn-normal" @click="openCreateFolder">+ 新建文件夹</button>
          <div class="dropdown-wrap" @click.stop>
            <button class="btn-primary" @click="toggleUploadMenu">⌃ 上传素材</button>
            <div v-if="uploadMenuOpen" class="dropdown-menu show upload-menu-right">
              <div class="dropdown-item" @click="onUploadClick(); uploadMenuOpen = false">
                <span>📄</span><span>上传文件</span>
              </div>
              <div class="dropdown-item" @click="folderInput?.click(); uploadMenuOpen = false">
                <span>📁</span><span>上传文件夹</span>
              </div>
            </div>
          </div>
        </div>
        <input v-model="searchKey" class="search-input" placeholder="搜索素材" />
      </div>
    </div>

    <div
      v-if="materialList.length === 0"
      class="upload-drop-area"
      :class="{ 'drag-over': isDragOver }"
      @dragover.prevent="onDragOver"
      @dragleave="onDragLeave"
      @drop.prevent="onDrop"
      @click="onUploadClick"
    >
      <div class="upload-icon">⇧</div>
      <div class="upload-text-main">点击或拖拽上传文件</div>
      <div class="upload-text-tip">支持图片、视频、音频、3D等文件（单文件 ≤ 200MB）</div>
    </div>
    <input ref="fileInput" type="file" multiple hidden @change="onFileChange" />
    <input ref="folderInput" type="file" webkitdirectory multiple hidden @change="onFolderChange" />

    <div v-if="loading" class="list-tip">加载中…</div>
    <div v-else-if="folders.length === 0 && materialList.length === 0" class="list-tip">还没有素材，上传试试吧</div>
    <div v-else class="material-grid">
      <!-- 文件夹卡片：与素材卡片同尺寸、一起平铺；点击筛选，再点已选中的取消筛选 -->
      <div
        v-for="f in folders"
        :key="'f_' + f.id"
        class="material-card folder-card"
        :class="{ active: activeFolderId === f.id }"
        @click="selectFolder(f.id)"
      >
        <div class="material-thumb folder-thumb">
          <span class="folder-icon">📁</span>
        </div>
        <div class="material-meta">
          <div class="material-name" :title="f.name">{{ f.name }}</div>
          <div class="material-sub">
            <span>文件夹</span>
            <a class="material-del" title="删除文件夹" @click.stop="deleteFolder(f)">删除</a>
          </div>
        </div>
      </div>
      <div v-for="mat in displayList" :key="mat.id" class="material-card">
        <div class="material-thumb">
          <img v-if="mat.type === 'image'" :src="mat.url" :alt="mat.name" />
          <span v-else class="material-file-icon">{{ mat.type === 'video' ? '🎬' : mat.type === 'audio' ? '🎵' : mat.type === '3d' ? '🧊' : '📎' }}</span>
        </div>
        <div class="material-meta">
          <div class="material-name" :title="mat.name">{{ mat.name }}</div>
          <div class="material-sub">
            <span>{{ formatSize(mat.size) }}</span>
            <a class="material-del" @click="removeMaterial(mat)">删除</a>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建文件夹卡片 -->
    <div v-if="showFolderModal" class="folder-modal-mask" @click.self="showFolderModal = false">
      <div class="folder-modal">
        <div class="folder-modal-top">
          <svg width="120" height="86" viewBox="0 0 128 96">
            <path fill="#90caf9" d="M52 8 C44 8 40 14 40 22 L40 28 L112 28 C120 28 124 34 124 42 L124 80 C124 88 118 94 110 94 L18 94 C10 94 4 88 4 80 L4 22 C4 14 10 8 18 8 Z" />
            <path fill="#42a5f5" d="M4 40 L124 40 L124 80 C124 88 118 94 110 94 L18 94 C10 94 4 88 4 80 Z" />
          </svg>
        </div>
        <div class="folder-modal-bottom">
          <input
            ref="folderNameInput"
            v-model="newFolderName"
            class="folder-name-input"
            placeholder="请输入文件夹名称"
            @keydown.enter="confirmCreateFolder"
          />
          <div class="folder-modal-actions">
            <button class="f-btn" @click="showFolderModal = false">取消</button>
            <button class="f-btn primary" @click="confirmCreateFolder">创建</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onActivated, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  uploadMaterial,
  listMyMaterialVoByPage,
  deleteMaterial,
  addMaterialFolder,
  listMaterialFolder,
  deleteMaterialFolder,
} from '@/api/materialController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

// 类型单选筛选
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'image', label: '图片' },
  { key: 'video', label: '视频' },
  { key: 'audio', label: '音频' },
  { key: '3d', label: '3D' },
  { key: 'other', label: '其他' },
]
const activeTab = ref('all')
const searchKey = ref('')
const materialList = ref<API.MaterialVO[]>([])
const folders = ref<API.MaterialFolderVO[]>([])
const activeFolderId = ref<number | null>(null)
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()
const folderInput = ref<HTMLInputElement>()
const uploadMenuOpen = ref(false)
const isDragOver = ref(false)

const showFolderModal = ref(false)
const newFolderName = ref('')
const folderNameInput = ref<HTMLInputElement>()

const totalSize = computed(() => materialList.value.reduce((sum, m) => sum + (m.size || 0), 0))
const usedStorage = computed(() => (totalSize.value / 1024 / 1024).toFixed(1) + ' MB')
const storagePercent = computed(() => Math.min(100, (totalSize.value / (100 * 1024 * 1024)) * 100))
const displayList = computed(() => {
  const kw = searchKey.value.trim().toLowerCase()
  return materialList.value.filter((m) => {
    if (activeTab.value !== 'all' && m.type !== activeTab.value) return false
    if (kw && !(m.name || '').toLowerCase().includes(kw)) return false
    return true
  })
})

const formatSize = (bytes?: number) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const loadMaterials = async () => {
  loading.value = true
  try {
    const params: API.MaterialQueryRequest = { pageNum: 1, pageSize: 50, sortOrder: 'desc' }
    if (activeFolderId.value !== null) {
      params.folderId = activeFolderId.value
    }
    const res = await listMyMaterialVoByPage(params)
    if (res.data.code === 0 && res.data.data) {
      materialList.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('加载素材失败', error)
  } finally {
    loading.value = false
  }
}

const loadFolders = async () => {
  try {
    const res = await listMaterialFolder()
    if (res.data.code === 0 && res.data.data) {
      folders.value = res.data.data
    }
  } catch (error) {
    console.error('加载文件夹失败', error)
  }
}

const selectFolder = (id: number | null) => {
  // 再点已选中的文件夹 → 取消筛选回「全部」
  activeFolderId.value = activeFolderId.value === id ? null : id
  loadMaterials()
}

const openCreateFolder = () => {
  newFolderName.value = ''
  showFolderModal.value = true
  nextTick(() => {
    setTimeout(() => folderNameInput.value?.focus(), 50)
  })
}

const confirmCreateFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) {
    message.warning('请输入文件夹名称')
    return
  }
  try {
    const res = await addMaterialFolder({ name })
    if (res.data.code === 0) {
      message.success('文件夹创建成功')
      showFolderModal.value = false
      await loadFolders()
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建文件夹失败', error)
    message.error('创建失败，请重试')
  }
}

const deleteFolder = async (f: API.MaterialFolderVO) => {
  if (!f.id) return
  try {
    const res = await deleteMaterialFolder({ id: f.id })
    if (res.data.code === 0) {
      message.success('文件夹已删除')
      if (activeFolderId.value === f.id) {
        activeFolderId.value = null
      }
      await loadFolders()
      await loadMaterials()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除文件夹失败', error)
  }
}

const doUpload = async (files: FileList) => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  uploading.value = true
  try {
    for (const file of Array.from(files)) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('name', file.name)
      if (activeFolderId.value !== null) {
        formData.append('folderId', String(activeFolderId.value))
      }
      const res = await uploadMaterial(formData)
      if (res.data.code !== 0) {
        message.error('上传失败：' + res.data.message)
      }
    }
    message.success('上传完成')
    await loadMaterials()
  } catch (error) {
    console.error('上传素材失败', error)
    message.error('上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const onUploadClick = () => {
  fileInput.value?.click()
}

const toggleUploadMenu = () => {
  uploadMenuOpen.value = !uploadMenuOpen.value
}

const onFolderChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    doUpload(input.files)
  }
  input.value = ''
}

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    doUpload(input.files)
  }
  input.value = ''
}

const onDrop = (e: DragEvent) => {
  isDragOver.value = false
  if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
    doUpload(e.dataTransfer.files)
  }
}

const onDragOver = () => {
  isDragOver.value = true
}

const onDragLeave = () => {
  isDragOver.value = false
}

const removeMaterial = async (mat: API.MaterialVO) => {
  if (!mat.id) return
  try {
    const res = await deleteMaterial({ id: mat.id })
    if (res.data.code === 0) {
      materialList.value = materialList.value.filter((m) => m.id !== mat.id)
      message.success('素材已删除')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除素材失败', error)
  }
}

onMounted(() => {
  loadMaterials()
  loadFolders()
  document.addEventListener('click', () => {
    uploadMenuOpen.value = false
  })
})

// keep-alive 恢复（Tab 切回）时刷新素材/文件夹；初次激活 onMounted 已加载，跳过
let keepAliveActivatedOnce = false
onActivated(() => {
  if (keepAliveActivatedOnce) {
    loadMaterials()
    loadFolders()
  }
  keepAliveActivatedOnce = true
})
</script>

<style scoped>
.page-wrap {
  padding: 24px 28px;
  background: #fff;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left h1 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1f2329;
}

.tab-group {
  display: flex;
  gap: 8px;
}

.tab-item {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  background: #f4f5f7;
  color: #1f2329;
  transition: all 0.2s;
}

.tab-item:hover {
  background: #e8e9ec;
}

.tab-item.active {
  background-color: #111111;
  color: #fff;
}

/* 「全部 + ▼」类型多选筛选 */
.tab-with-arrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tab-arrow {
  font-size: 10px;
  opacity: 0.75;
}
.filter-dropdown {
  position: absolute;
  top: 38px;
  left: 0;
  min-width: 140px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 8px 0;
  z-index: 100;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  font-size: 13px;
  color: #1f2329;
  cursor: pointer;
}
.filter-item:hover {
  background: #f7f8fa;
}
.filter-item input[type='checkbox'] {
  cursor: pointer;
}
.filter-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px 2px;
  border-top: 1px solid #f0f1f3;
  margin-top: 4px;
}
.filter-reset {
  background: transparent;
  border: none;
  color: #4f7cff;
  cursor: pointer;
  font-size: 12px;
}
.filter-tip {
  font-size: 11px;
  color: #a0a4ac;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.storage-bar-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #666;
}

.storage-progress {
  width: 180px;
  height: 8px;
  background: #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.storage-progress-fill {
  height: 100%;
  background: #b8c8ff;
  transition: width 0.3s;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn-normal {
  padding: 7px 14px;
  border: 1px solid #dcdde0;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #1f2329;
}

.btn-normal:hover {
  background: #f5f5f5;
}

.btn-primary {
  padding: 7px 14px;
  background: #000000;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary:hover {
  background: #222;
}

.search-input {
  width: 240px;
  padding: 7px 12px;
  border: 1px solid #dcdde0;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}

.search-input:focus {
  border-color: #4056d8;
}

.folder-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.folder-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #e3e5e9;
  background: #fafbfc;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
}

.folder-item:hover {
  background: #f0f4ff;
  border-color: #b8c8ff;
}

.folder-item.active {
  background: #111;
  color: #fff;
  border-color: #111;
}

.folder-del {
  margin-left: 2px;
  font-size: 16px;
  line-height: 1;
  opacity: 0.6;
  cursor: pointer;
}

.folder-del:hover {
  opacity: 1;
  color: #f04438;
}

.upload-drop-area {
  max-width: 520px;
  margin: 0 auto 24px;
  border: 1px dashed #dcdde0;
  border-radius: 8px;
  padding: 18px 12px;
  background: #f9fafb;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
  text-align: center;
}

.upload-drop-area:hover,
.upload-drop-area.drag-over {
  background: #f0f4ff;
  border-color: #b8c8ff;
}

.upload-icon {
  font-size: 20px;
  color: #999;
  margin-bottom: 4px;
}

.upload-text-main {
  font-size: 14px;
  color: #333;
}

.upload-text-tip {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.list-tip {
  text-align: center;
  color: #999;
  padding: 24px 0;
  font-size: 14px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.material-card {
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.material-thumb {
  height: 110px;
  background: #f5f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.material-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.material-file-icon {
  font-size: 36px;
}

.material-meta {
  padding: 8px 10px;
}

.material-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-sub {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.material-del {
  color: #f04438;
  cursor: pointer;
}

.material-del:hover {
  text-decoration: underline;
}

/* 文件夹卡片：与素材卡片同尺寸平铺，暖色背景 + 选中高亮 */
.folder-card {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.folder-card:hover {
  border-color: #4f7cff;
}
.folder-card.active {
  border-color: #4f7cff;
  box-shadow: 0 0 0 2px rgba(79, 124, 255, 0.18);
}
.folder-thumb {
  background: linear-gradient(135deg, #fff7e6 0%, #fff0c9 100%);
}
.folder-icon {
  font-size: 42px;
  line-height: 1;
}

.dropdown-wrap {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 44px;
  left: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
  border: 1px solid #eee;
  width: 160px;
  z-index: 99;
}

.dropdown-item {
  padding: 10px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f4f6ff;
}

.upload-menu-right {
  left: auto;
  right: 0;
}

.folder-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.folder-modal {
  width: 320px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.18);
}

.folder-modal-top {
  background: linear-gradient(135deg, #e8f0fe 0%, #f3eafb 100%);
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.folder-modal-bottom {
  padding: 18px 22px 22px;
}

.folder-name-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 15px;
  border-radius: 10px;
  background: #f7f8fa;
  border: none;
  outline: none;
}

.folder-modal-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.f-btn {
  padding: 7px 16px;
  border-radius: 8px;
  border: 1px solid #dcdde0;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  color: #333;
}

.f-btn.primary {
  background: #111;
  color: #fff;
  border-color: #111;
}

.f-btn:hover {
  opacity: 0.88;
}
</style>
