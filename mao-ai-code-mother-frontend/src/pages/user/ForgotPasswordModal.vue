<template>
  <div v-if="visible" class="fp-mask" @click.self="close">
    <div class="fp-modal">
      <div class="fp-header">
        <span class="fp-title">找回密码</span>
        <span class="fp-close" @click="close">×</span>
      </div>

      <div class="fp-tab-wrap">
        <div class="fp-tab-item" :class="{ active: resetTab === 'phone' }" @click="resetTab = 'phone'">
          手机验证码找回
        </div>
        <div class="fp-tab-item" :class="{ active: resetTab === 'email' }" @click="resetTab = 'email'">
          邮箱验证码找回
        </div>
      </div>

      <!-- 人机校验：图形验证码（挡自动化轰炸） -->
      <CaptchaInput v-model="captchaInput" ref="captchaRef" placeholder="请输入图形验证码" />

      <!-- 手机验证码找回 -->
      <div v-show="resetTab === 'phone'" class="fp-form">
        <div class="fp-input-item">
          <input v-model="phone" placeholder="请输入注册时绑定的手机号" maxlength="11" />
        </div>
        <div class="fp-input-item fp-code-row">
          <input v-model="code" placeholder="请输入验证码" maxlength="6" class="fp-code-input" />
          <button class="fp-code-btn" :disabled="countdown > 0 || sending" @click="sendCode">
            {{ countdown > 0 ? countdown + 's 后重发' : sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <div class="fp-input-item">
          <input v-model="newPassword" type="password" placeholder="请输入新密码（至少 8 位）" />
        </div>
        <div class="fp-input-item">
          <input v-model="checkPassword" type="password" placeholder="请再次输入新密码" />
        </div>
        <button class="fp-submit" :disabled="submitting" @click="handleReset">
          {{ submitting ? '重置中…' : '重置密码' }}
        </button>
      </div>

      <!-- 邮箱验证码找回 -->
      <div v-show="resetTab === 'email'" class="fp-form">
        <div class="fp-input-item">
          <input v-model="email" placeholder="请输入注册时绑定的邮箱（如 123@qq.com）" />
        </div>
        <div class="fp-input-item fp-code-row">
          <input v-model="code" placeholder="请输入验证码" maxlength="6" class="fp-code-input" />
          <button class="fp-code-btn" :disabled="countdown > 0 || sending" @click="sendCode">
            {{ countdown > 0 ? countdown + 's 后重发' : sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <div class="fp-input-item">
          <input v-model="newPassword" type="password" placeholder="请输入新密码（至少 8 位）" />
        </div>
        <div class="fp-input-item">
          <input v-model="checkPassword" type="password" placeholder="请再次输入新密码" />
        </div>
        <button class="fp-submit" :disabled="submitting" @click="handleReset">
          {{ submitting ? '重置中…' : '重置密码' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount } from 'vue'
import {
  sendResetSmsCode,
  resetPasswordByPhone,
  sendResetEmailCode,
  resetPasswordByEmail,
} from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { isValidPhone, isValidEmail } from '@/utils/validators'
import CaptchaInput from '@/components/CaptchaInput.vue'

// 由父组件通过 v-model:visible 控制显隐
const visible = defineModel<boolean>('visible', { default: false })

const resetTab = ref<'phone' | 'email'>('phone')
const phone = ref('')
const email = ref('')
const code = ref('')
const newPassword = ref('')
const checkPassword = ref('')

const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码（人机校验）
const captchaRef = ref<InstanceType<typeof CaptchaInput> | null>(null)
const captchaInput = ref('')

const close = () => {
  visible.value = false
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

const sendCode = async () => {
  if (resetTab.value === 'phone') {
    const v = phone.value.trim()
    if (!isValidPhone(v)) {
      message.warning('请输入正确的手机号')
      return
    }
    sending.value = true
    try {
      const res = await sendResetSmsCode({ phone: v, captchaKey: captchaRef.value?.getKey(), captcha: captchaInput.value })
      if (res.data.code === 0) {
        message.success('验证码已发送，请查收短信')
        startCountdown(60)
      } else {
        message.error(res.data.message || '发送失败')
        captchaRef.value?.refresh()
      }
    } catch {
      message.error('发送失败，请稍后再试')
      captchaRef.value?.refresh()
    } finally {
      sending.value = false
    }
  } else {
    const v = email.value.trim()
    if (!isValidEmail(v)) {
      message.warning('请输入正确的邮箱')
      return
    }
    sending.value = true
    try {
      const res = await sendResetEmailCode({ email: v, captchaKey: captchaRef.value?.getKey(), captcha: captchaInput.value })
      if (res.data.code === 0) {
        message.success('验证码已发送，请查收邮件')
        startCountdown(60)
      } else {
        message.error(res.data.message || '发送失败')
        captchaRef.value?.refresh()
      }
    } catch {
      message.error('发送失败，请稍后再试')
      captchaRef.value?.refresh()
    } finally {
      sending.value = false
    }
  }
}

const handleReset = async () => {
  const pwd = newPassword.value
  const pwd2 = checkPassword.value
  if (pwd.length < 8) {
    message.warning('新密码至少 8 位')
    return
  }
  if (pwd !== pwd2) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (!code.value.trim()) {
    message.warning('请输入验证码')
    return
  }

  submitting.value = true
  try {
    if (resetTab.value === 'phone') {
      const res = await resetPasswordByPhone({
        phone: phone.value.trim(),
        code: code.value.trim(),
        newPassword: pwd,
        checkPassword: pwd2,
      })
      if (res.data.code === 0) {
        message.success('密码重置成功，请重新登录')
        close()
        resetFields()
      } else {
        message.error(res.data.message || '重置失败')
      }
    } else {
      const res = await resetPasswordByEmail({
        email: email.value.trim(),
        code: code.value.trim(),
        newPassword: pwd,
        checkPassword: pwd2,
      })
      if (res.data.code === 0) {
        message.success('密码重置成功，请重新登录')
        close()
        resetFields()
      } else {
        message.error(res.data.message || '重置失败')
      }
    }
  } catch {
    message.error('重置失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const resetFields = () => {
  phone.value = ''
  email.value = ''
  code.value = ''
  newPassword.value = ''
  checkPassword.value = ''
}

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.fp-mask {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.fp-modal {
  width: 420px;
  max-width: 92vw;
  background: #fff;
  border-radius: 14px;
  padding: 24px 26px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

.fp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.fp-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.fp-close {
  font-size: 22px;
  line-height: 1;
  color: #9ca3af;
  cursor: pointer;
}

.fp-close:hover {
  color: #4b5563;
}

.fp-tab-wrap {
  display: flex;
  margin-bottom: 18px;
}

.fp-tab-item {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  padding-bottom: 8px;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.fp-tab-item.active {
  color: #3b6ef7;
  border-bottom-color: #3b6ef7;
}

.fp-input-item {
  margin-bottom: 14px;
}

.fp-input-item input {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.fp-input-item input:focus {
  border-color: #3b6ef7;
}

.fp-code-row {
  display: flex;
  gap: 8px;
}

.fp-code-input {
  flex: 1;
}

.fp-code-btn {
  width: 116px;
  flex-shrink: 0;
  background: #3b6ef7;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.fp-code-btn:hover:not(:disabled) {
  background: #2f5ce0;
}

.fp-code-btn:disabled {
  background: #c3cbe8;
  cursor: not-allowed;
}

.fp-submit {
  width: 100%;
  background: #3b6ef7;
  color: #fff;
  border: none;
  padding: 11px;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  margin-top: 4px;
  transition: background 0.2s;
}

.fp-submit:hover:not(:disabled) {
  background: #2f5ce0;
}

.fp-submit:disabled {
  background: #c3cbe8;
  cursor: not-allowed;
}
</style>
