<template>
  <div class="settings-page">
    <a-row :gutter="20">
      <!-- 左侧菜单 -->
      <a-col :span="5">
        <a-card class="settings-menu-card">
          <div
            v-for="group in settingGroups"
            :key="group.title"
            class="setting-group"
          >
            <div class="group-title">{{ group.title }}</div>
            <div
              v-for="item in group.items"
              :key="item.key"
              class="setting-item"
              :class="{ active: activeMenu === item.key }"
              @click="activeMenu = item.key"
            >
              <span class="item-icon">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧内容 -->
      <a-col :span="19">
        <!-- 基础设置 -->
        <a-card v-if="activeMenu === 'basic'" title="基础设置" class="settings-content">
          <a-alert type="info" show-icon class="config-tip">
            <template #message>仅「站点名称」会保存至后端，其余字段（域名 / Logo / ICP / 描述）暂未接入配置中心，保存时不提交。</template>
          </a-alert>
          <a-form layout="vertical" :model="basicForm" style="margin-top: 12px">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="站点名称">
                  <a-input v-model:value="basicForm.siteName" placeholder="请输入站点名称" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="站点域名">
                  <a-input v-model:value="basicForm.siteDomain" placeholder="请输入站点域名" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="站点 Logo">
              <a-upload
                list-type="picture-card"
                :show-upload-list="false"
                :before-upload="beforeUploadLogo"
              >
                <div v-if="basicForm.siteLogo">
                  <img :src="basicForm.siteLogo" alt="logo" style="width: 100%; height: 100%; object-fit: cover;" />
                </div>
                <div v-else>
                  <PlusOutlined />
                  <div style="margin-top: 8px">上传 Logo</div>
                </div>
              </a-upload>
            </a-form-item>
            <a-form-item label="ICP 备案号">
              <a-input v-model:value="basicForm.icp" placeholder="请输入ICP备案号" />
            </a-form-item>
            <a-form-item label="站点描述">
              <a-textarea
                v-model:value="basicForm.description"
                :rows="4"
                placeholder="请输入站点描述"
              />
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 功能开关 -->
        <a-card v-if="activeMenu === 'features'" title="功能开关" class="settings-content">
          <a-alert type="info" show-icon class="config-tip">
            <template #message>「用户注册」「社区功能」已接入后端配置，保存即生效；「评论功能 / 邮件通知 / 维护模式」暂未接入，开关状态仅本地展示。</template>
          </a-alert>
          <a-list class="feature-list" style="margin-top: 12px">
            <a-list-item v-for="feature in features" :key="feature.key">
              <div class="feature-item">
                <div class="feature-icon" :style="{ background: feature.bgColor, color: feature.color }">
                  {{ feature.icon }}
                </div>
                <div class="feature-info">
                  <div class="feature-name">
                    {{ feature.name }}
                    <a-tag v-if="feature.backed" color="green" class="backed-tag">已接入</a-tag>
                    <a-tag v-else color="default" class="backed-tag">未接入</a-tag>
                  </div>
                  <div class="feature-desc">{{ feature.desc }}</div>
                </div>
                <a-switch v-model:checked="feature.enabled" />
              </div>
            </a-list-item>
          </a-list>
        </a-card>

        <!-- 系统参数 -->
        <a-card v-if="activeMenu === 'system'" title="系统参数" class="settings-content">
          <a-alert type="info" show-icon class="config-tip">
            <template #message>以下参数均保存至后端配置中心，立即生效。</template>
          </a-alert>
          <a-form layout="vertical" style="margin-top: 12px">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="新用户赠送积分">
                  <a-input-number v-model:value="sysForm.giftSeconds" :min="0" :max="100000" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="单文件上传上限 (MB)">
                  <a-input-number v-model:value="sysForm.maxSizeMb" :min="1" :max="200" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="AI 对话模型">
              <a-input v-model:value="sysForm.modelName" placeholder="如 deepseek-chat" />
            </a-form-item>
            <a-form-item label="说明">
              <a-alert type="warning" show-icon>
                <template #message>修改「AI 对话模型」需与后端模型配置一致，填写不存在的模型名可能导致对话失败。</template>
              </a-alert>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 安全设置 -->
        <a-card v-if="activeMenu === 'security'" title="安全设置" class="settings-content">
          <a-alert type="info" show-icon class="config-tip">
            <template #message>安全设置暂未接入后端配置中心，当前为本地预览，保存不生效。</template>
          </a-alert>
          <a-form layout="vertical" style="margin-top: 12px">
            <a-form-item label="管理员密码">
              <a-button type="primary" ghost @click="showPasswordModal = true">修改密码</a-button>
            </a-form-item>
            <a-divider />
            <a-form-item label="登录限制">
              <a-space direction="vertical" style="width: 100%">
                <div>
                  <span style="margin-right: 12px;">密码错误次数限制</span>
                  <a-input-number v-model:value="securityConfig.maxLoginAttempts" :min="1" :max="20" />
                  <span style="margin-left: 8px; color: #94a3b8;">次</span>
                </div>
                <div>
                  <span style="margin-right: 12px;">锁定时间</span>
                  <a-input-number v-model:value="securityConfig.lockMinutes" :min="1" :max="1440" />
                  <span style="margin-left: 8px; color: #94a3b8;">分钟</span>
                </div>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 联系方式 -->
        <a-card v-if="activeMenu === 'contact'" title="联系方式" class="settings-content">
          <a-alert type="info" show-icon class="config-tip">
            <template #message>联系方式暂未接入后端配置中心，当前为本地预览，保存不生效。</template>
          </a-alert>
          <a-form layout="vertical" :model="contactForm" style="margin-top: 12px">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="客服邮箱">
                  <a-input v-model:value="contactForm.email" placeholder="请输入客服邮箱" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="客服电话">
                  <a-input v-model:value="contactForm.phone" placeholder="请输入客服电话" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="工作时间">
              <a-input v-model:value="contactForm.workTime" placeholder="例如：周一至周五 9:00-18:00" />
            </a-form-item>
            <a-form-item label="公司地址">
              <a-input v-model:value="contactForm.address" placeholder="请输入公司地址" />
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 底部操作按钮 -->
        <div class="settings-actions">
          <a-button :loading="configLoading" @click="resetSettings">重置</a-button>
          <a-button type="primary" :loading="saving" @click="saveSettings">保存设置</a-button>
        </div>
      </a-col>
    </a-row>

    <!-- 修改密码弹窗 -->
    <a-modal v-model:open="showPasswordModal" title="修改管理员密码" width="480">
      <a-form layout="vertical">
        <a-form-item label="当前密码">
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认新密码">
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="showPasswordModal = false">取消</a-button>
        <a-button type="primary" @click="changePassword">确认修改</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { listSysConfig, updateSysConfig } from '@/api/adminController'

const activeMenu = ref('basic')
const showPasswordModal = ref(false)
const configLoading = ref(false)
const saving = ref(false)

// 后端 sys_config 表里真实存在的配置项，只有这些能持久化
const BACKED_KEYS = [
  'site.name',
  'site.registerEnabled',
  'community.enabled',
  'user.giftSeconds',
  'ai.modelName',
  'upload.maxSizeMb',
]

const configList = ref<API.SysConfig[]>([])
const configMap = computed(() => {
  const map: Record<string, string> = {}
  configList.value.forEach((c) => {
    if (c.configKey) map[c.configKey] = c.configValue ?? ''
  })
  return map
})

// 从后端配置填充表单
function applyConfigToForm() {
  const map = configMap.value
  if (map['site.name']) basicForm.siteName = map['site.name']
  const reg = features.find((f) => f.key === 'register')
  if (reg) reg.enabled = map['site.registerEnabled'] !== 'false'
  const com = features.find((f) => f.key === 'community')
  if (com) com.enabled = map['community.enabled'] !== 'false'
  if (map['user.giftSeconds']) sysForm.giftSeconds = Number(map['user.giftSeconds'])
  if (map['ai.modelName']) sysForm.modelName = map['ai.modelName']
  if (map['upload.maxSizeMb']) sysForm.maxSizeMb = Number(map['upload.maxSizeMb'])
}

async function loadConfigs() {
  configLoading.value = true
  try {
    const res = await listSysConfig()
    if (res.data.code === 0 && res.data.data) {
      configList.value = res.data.data
      applyConfigToForm()
    } else {
      message.error('加载系统配置失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('加载系统配置失败', error)
    message.error('加载系统配置失败，请确认已登录管理员账号')
  } finally {
    configLoading.value = false
  }
}

const settingGroups = [
  {
    title: '基础设置',
    items: [
      { key: 'basic', label: '站点信息', icon: '🏠' },
      { key: 'features', label: '功能开关', icon: '🔧' },
    ],
  },
  {
    title: '系统管理',
    items: [
      { key: 'system', label: '系统参数', icon: '⚙️' },
      { key: 'security', label: '安全设置', icon: '🔒' },
      { key: 'contact', label: '联系方式', icon: '📞' },
    ],
  },
]

const basicForm = reactive({
  // siteName 已接入后端（site.name）；其余字段后端暂无配置项，保存时不提交
  siteName: 'AI 代码生成平台',
  siteDomain: 'https://example.com',
  siteLogo: '',
  icp: '粤ICP备xxxxxxxx号',
  description: '一个强大的 AI 代码生成平台，帮助开发者快速构建企业级应用。',
})

const features = reactive([
  // register / community 已接入后端（site.registerEnabled / community.enabled）
  { key: 'register', name: '用户注册', desc: '是否允许新用户注册账号', icon: '📝', bgColor: '#eef2ff', color: '#6366f1', enabled: true, backed: true },
  { key: 'community', name: '社区功能', desc: '是否开放社区帖子发布', icon: '👥', bgColor: '#ecfdf5', color: '#10b981', enabled: true, backed: true },
  // 以下三项后端暂无配置，保存时提示未接入
  { key: 'comment', name: '评论功能', desc: '是否允许用户发表评论', icon: '💬', bgColor: '#fffbeb', color: '#f59e0b', enabled: true, backed: false },
  { key: 'email', name: '邮件通知', desc: '是否发送邮件通知', icon: '📧', bgColor: '#fdf2f8', color: '#ec4899', enabled: false, backed: false },
  { key: 'maintenance', name: '维护模式', desc: '开启后用户无法正常访问', icon: '⚠️', bgColor: '#fef2f2', color: '#ef4444', enabled: false, backed: false },
])

// 系统参数分组，全部接入后端
const sysForm = reactive({
  giftSeconds: 100,
  modelName: 'deepseek-chat',
  maxSizeMb: 20,
})

const securityConfig = reactive({
  maxLoginAttempts: 5,
  lockMinutes: 30,
})

const contactForm = reactive({
  email: 'support@example.com',
  phone: '400-xxx-xxxx',
  workTime: '周一至周五 9:00-18:00',
  address: '广东省深圳市南山区xx大厦',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function beforeUploadLogo(file: File) {
  const reader = new FileReader()
  reader.onload = (e) => {
    basicForm.siteLogo = e.target?.result as string
  }
  reader.readAsDataURL(file)
  return false
}

// 把当前表单值映射成 {configKey: configValue}，只含后端真实配置项
function buildConfigPayload(): Record<string, string> {
  const payload: Record<string, string> = {}
  payload['site.name'] = basicForm.siteName
  const reg = features.find((f) => f.key === 'register')
  if (reg) payload['site.registerEnabled'] = String(reg.enabled)
  const com = features.find((f) => f.key === 'community')
  if (com) payload['community.enabled'] = String(com.enabled)
  payload['user.giftSeconds'] = String(sysForm.giftSeconds)
  payload['ai.modelName'] = sysForm.modelName
  payload['upload.maxSizeMb'] = String(sysForm.maxSizeMb)
  return payload
}

async function saveSettings() {
  const payload = buildConfigPayload()
  // 客服电话格式校验（允许 手机/座机/400，含 + - ( ) 与空格）
  if (contactForm.phone && contactForm.phone.trim() && !/^[+\d\-()\s]{7,20}$/.test(contactForm.phone.trim())) {
    message.warning('客服电话格式不正确')
    return
  }
  // 只提交与后端当前值不同的项
  const dirty: { key: string; value: string }[] = []
  for (const key of BACKED_KEYS) {
    const next = payload[key]
    const prev = configMap.value[key]
    if (next !== undefined && next !== prev) {
      dirty.push({ key, value: next })
    }
  }
  if (dirty.length === 0) {
    message.info('配置无变化')
    return
  }
  saving.value = true
  try {
    let allOk = true
    for (const item of dirty) {
      const res = await updateSysConfig({ configKey: item.key, configValue: item.value })
      if (!(res.data.code === 0 && res.data.data)) {
        allOk = false
        message.error(`保存 ${item.key} 失败：${res.data.message || '未知错误'}`)
      }
    }
    if (allOk) {
      message.success(`已保存 ${dirty.length} 项配置`)
      // 未接入后端的项（Logo/ICP/域名/描述/安全/联系方式）不参与保存
    }
    await loadConfigs()
  } catch (error) {
    console.error('保存系统配置失败', error)
    message.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function resetSettings() {
  loadConfigs()
  message.info('已从服务端重新加载配置')
}

function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    message.error('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  message.success('密码修改成功')
  showPasswordModal.value = false
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.settings-page {
  width: 100%;
}

.settings-menu-card {
  border-radius: 10px;
  padding: 8px 0;
}

.setting-group {
  margin-bottom: 8px;
}

.group-title {
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 16px 4px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
  transition: all 0.2s;
  border-radius: 6px;
  margin: 2px 8px;
}

.setting-item:hover {
  background: #f8fafc;
}

.setting-item.active {
  background: #eef2ff;
  color: #6366f1;
  font-weight: 500;
}

.item-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.settings-content {
  border-radius: 10px;
  margin-bottom: 16px;
}

.config-tip {
  margin-bottom: 4px;
}

.backed-tag {
  margin-left: 6px;
  font-size: 11px;
  line-height: 18px;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0;
}

.feature-list {
  padding: 0 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.feature-info {
  flex: 1;
}

.feature-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 2px;
}

.feature-desc {
  font-size: 12px;
  color: #94a3b8;
}
</style>
