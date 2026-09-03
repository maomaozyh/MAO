<template>
  <div class="register-page">
    <div class="register-wrap">
      <!-- 左侧品牌介绍 -->
      <div class="register-left">
        <div class="logo-title">元知<span class="blue-text">AI</span><br />无代码应用搭建平台</div>
        <div class="desc-item">Ⓞ 零代码无需编程，一句话快速生成完整业务应用</div>
        <div class="desc-item">ⓘ 内置后端服务与插件生态，复杂业务系统一键部署上线</div>
        <div class="desc-item">Ⓢ 覆盖多元业务场景落地，助力企业创造高额商业价值</div>
      </div>

      <!-- 右侧注册卡片 -->
      <div class="register-card">
        <div class="card-title">注册元知 AI 账号</div>
        <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="请输入账号（4 位以上）" />
          </a-form-item>
          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码不能小于 8 位' },
            ]"
          >
            <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码（至少 8 位）" />
          </a-form-item>
          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请确认密码' },
              { min: 8, message: '密码不能小于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
          </a-form-item>
          <a-form-item
            name="phone"
            :rules="[{ required: true, message: '请输入手机号' }, { validator: validatePhone }]"
          >
            <a-input v-model:value="formState.phone" placeholder="请输入手机号" maxlength="11" />
          </a-form-item>
          <CaptchaInput v-model="captchaInput" ref="captchaRef" />
          <a-form-item name="code" :rules="[{ required: true, message: '请输入验证码' }]">
            <div class="sms-code-row">
              <a-input
                v-model:value="formState.code"
                placeholder="请输入验证码"
                maxlength="6"
                class="sms-code-input"
              />
              <button
                type="button"
                class="sms-code-btn"
                :disabled="countdown > 0 || smsSending"
                @click="sendCode"
              >
                {{ countdown > 0 ? countdown + 's 后重发' : smsSending ? '发送中…' : '获取验证码' }}
              </button>
            </div>
          </a-form-item>
          <button type="submit" class="register-btn">注册</button>
        </a-form>
        <div class="link-row">
          已有账号？<RouterLink to="/user/login">去登录</RouterLink>
        </div>
      </div>
    </div>

    <div class="page-footer">©2026 Baidu 使用百度前必读 增值电信业务经营许可证：B1.B2‑20100266号ICP证030173号 隐私政策</div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { reactive, ref, onBeforeUnmount } from 'vue'
import { userRegister, sendSmsCode } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { isValidPhone } from '@/utils/validators'
import CaptchaInput from '@/components/CaptchaInput.vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
  phone: '',
  code: '',
})

// 短信验证码倒计时（与登录页共用同一套频控 + 校验）
const smsSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码（人机校验）
const captchaRef = ref<InstanceType<typeof CaptchaInput> | null>(null)
const captchaInput = ref('')

const sendCode = async () => {
  const phone = (formState.phone || '').trim()
  if (!phone) {
    message.warning('请输入手机号')
    return
  }
  if (!isValidPhone(phone)) {
    message.warning('手机号格式不正确')
    return
  }
  smsSending.value = true
  try {
    const res = await sendSmsCode({ phone, captchaKey: captchaRef.value?.getKey(), captcha: captchaInput.value })
    if (res.data.code === 0) {
      message.success('验证码已发送，请查收')
      startCountdown(60)
    } else {
      message.error(res.data.message || '发送失败')
      captchaRef.value?.refresh()
    }
  } catch {
    message.error('发送失败，请稍后再试')
    captchaRef.value?.refresh()
  } finally {
    smsSending.value = false
  }
}

const startCountdown = (seconds: number) => {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

/**
 * 验证确认密码
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 校验手机号格式
 */
const validatePhone = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && !isValidPhone(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.register-page {
  min-height: 100vh;
  background-image: url("https://images.unsplash.com/photo-1519681393784-d120267933ba?q=80&w=1920");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.register-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.2);
  z-index: 0;
}

.register-wrap {
  width: 100%;
  max-width: 1400px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  position: relative;
  z-index: 1;
}

/* 左侧品牌 */
.register-left {
  flex: 1;
  color: #1f2937;
  min-width: 0;
}

.logo-title {
  font-size: 42px;
  font-weight: bold;
  margin-bottom: 12px;
  line-height: 1.3;
}

.blue-text {
  color: #3b6ef7;
}

.desc-item {
  margin: 8px 0;
  font-size: 15px;
  color: #4b5563;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 右侧注册卡片 */
.register-card {
  width: 460px;
  background: #ffffff;
  border-radius: 16px;
  padding: 32px 32px 28px;
  box-shadow: 0 4px 20px #00000012;
  flex-shrink: 0;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
  text-align: center;
}

/* 表单间距控制 */
.register-card :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.register-card :deep(.ant-input-affix-wrapper),
.register-card :deep(.ant-input) {
  border-radius: 8px;
  padding: 6px 11px;
  font-size: 14px;
}

.register-card :deep(.ant-form-item-explain-error) {
  font-size: 12px;
  margin-top: 2px;
}

/* 短信验证码行 */
.sms-code-row {
  display: flex;
  gap: 8px;
}

.sms-code-input {
  flex: 1;
}

.sms-code-btn {
  width: 118px;
  flex-shrink: 0;
  background: #3b6ef7;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}

.sms-code-btn:hover:not(:disabled) {
  background: #2f5ce0;
}

.sms-code-btn:disabled {
  background: #c3cbe8;
  cursor: not-allowed;
}

.register-btn {
  width: 100%;
  background: #3b6ef7;
  color: #fff;
  border: none;
  padding: 12px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 4px;
  transition: background 0.2s;
}

.register-btn:hover {
  background: #2f5ce0;
}

.link-row {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #6b7280;
}

.link-row a {
  color: #3b6ef7;
  text-decoration: none;
  margin-left: 4px;
}

/* 底部 */
.page-footer {
  position: fixed;
  bottom: 12px;
  left: 0;
  width: 100%;
  text-align: center;
  font-size: 12px;
  color: #788293;
  z-index: 1;
}
</style>
