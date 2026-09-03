<template>
  <div class="login-page">
    <div class="login-wrap">
      <!-- 左侧品牌介绍 -->
      <div class="login-left">
        <div class="logo-title">元知<span class="blue-text">AI</span><br />无代码应用搭建平台</div>
<div class="desc-item">Ⓞ 零代码无需编程，一句话快速生成完整业务应用</div>
<div class="desc-item">ⓘ 内置后端服务与插件生态，复杂业务系统一键部署上线</div>
<div class="desc-item">Ⓢ 覆盖多元业务场景落地，助力企业创造高额商业价值</div>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="login-card">
        <div class="card-body">
          <div class="form-area">
            <div class="tab-wrap">
              <div class="tab-item" :class="{ active: activeTab === 'sms' }" @click="activeTab = 'sms'">短信登录</div>
              <div class="tab-item" :class="{ active: activeTab === 'account' }" @click="activeTab = 'account'">账号登录</div>
            </div>

            <!-- 账号登录 -->
            <div v-show="activeTab === 'account'">
              <div class="input-item">
                <input v-model="formState.userAccount" placeholder="手机号/用户名/邮箱" />
              </div>
              <div class="input-item">
                <input v-model="formState.userPassword" type="password" placeholder="密码" />
              </div>
              <!-- [已关闭] 登录强制绑定手机号：账号登录下的手机号 + 验证码输入，按需重新开启
              <div class="input-item">
                <input v-model="formState.phone" placeholder="手机号（登录需短信验证码）" maxlength="11" />
              </div>
              <div class="input-item sms-code-row">
                <input v-model="formState.code" placeholder="请输入验证码" maxlength="6" class="sms-code-input" />
                <button class="sms-code-btn" :disabled="countdown > 0 || smsSending" @click="sendCode(formState.phone)">
                  {{ countdown > 0 ? countdown + 's 后重发' : smsSending ? '发送中…' : '获取验证码' }}
                </button>
              </div>
              -->
            </div>

            <!-- 短信登录 -->
            <div v-show="activeTab === 'sms'">
              <div class="input-item">
                <input v-model="smsPhone" placeholder="请输入手机号" maxlength="11" />
              </div>
              <CaptchaInput v-model="captchaInput" ref="captchaRef" />
              <div class="input-item sms-code-row">
                <input v-model="smsCode" placeholder="请输入验证码" maxlength="6" class="sms-code-input" />
                <button class="sms-code-btn" :disabled="countdown > 0 || smsSending" @click="sendCode(smsPhone)">
                  {{ countdown > 0 ? countdown + 's 后重发' : smsSending ? '发送中…' : '获取验证码' }}
                </button>
              </div>
            </div>

            <div class="checkbox-row">
              <input type="checkbox" id="agree" v-model="agreed" />
              <label for="agree">我已阅读并同意 <a href="#">秒哒用户协议</a>、<a href="#">秒哒隐私政策</a> 和 <a href="#">百度用户协议、隐私政策</a></label>
            </div>

            <button class="login-btn" @click="handleSubmit">登录</button>

            <div class="link-row">
              <a href="javascript:void(0)" @click="forgotVisible = true">忘记密码</a> | <RouterLink to="/user/register">注册</RouterLink>
            </div>
          </div>

          <div class="qrcode-area">
            <div>微信扫码登录</div>
            <div class="qrcode-box wechat-qr" @click="goWechatLogin" title="微信扫码登录">
              <svg viewBox="0 0 24 24" width="40" height="40" fill="#07c160">
                <path d="M9.3 4.2C5.4 4.2 2.3 6.8 2.3 10c0 1.8 1 3.4 2.6 4.5l-.6 2 2.3-1.2c.6.2 1.3.3 2 .3.4 0 .7 0 1.1-.1-.1-.5-.1-1-.1-1.5 0-3.3 2.8-5.9 6.3-5.9.3 0 .6 0 .9.1-.5-2.9-3.5-5-7.5-5z"/>
                <path d="M21.6 14.4c0-2.6-2.4-4.7-5.4-4.7s-5.4 2.1-5.4 4.7 2.4 4.7 5.4 4.7c.6 0 1.2-.1 1.7-.3l1.9 1-.5-1.7c1.4-.9 2.3-2.2 2.3-3.7z"/>
              </svg>
              <div class="wechat-qr-text">微信扫码</div>
            </div>
            <div class="qrcode-tip">打开微信扫一扫<br />快速登录</div>
          </div>
        </div>

        <div class="other-login-title">使用其他账号登录</div>
        <div class="third-icon-group">
          <div class="third-icon wechat-third" @click="goWechatLogin" title="微信登录">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="#fff">
              <path d="M9.3 4.2C5.4 4.2 2.3 6.8 2.3 10c0 1.8 1 3.4 2.6 4.5l-.6 2 2.3-1.2c.6.2 1.3.3 2 .3.4 0 .7 0 1.1-.1-.1-.5-.1-1-.1-1.5 0-3.3 2.8-5.9 6.3-5.9.3 0 .6 0 .9.1-.5-2.9-3.5-5-7.5-5z"/>
              <path d="M21.6 14.4c0-2.6-2.4-4.7-5.4-4.7s-5.4 2.1-5.4 4.7 2.4 4.7 5.4 4.7c.6 0 1.2-.1 1.7-.3l1.9 1-.5-1.7c1.4-.9 2.3-2.2 2.3-3.7z"/>
            </svg>
          </div>
          <div class="third-icon" title="QQ 登录" @click="goQQLogin">🐧</div>
          <div class="third-icon" title="更多登录方式（即将上线）">＋</div>
        </div>
      </div>
    </div>

    <div class="page-footer">©2026 Baidu 使用百度前必读 增值电信业务经营许可证：B1.B2‑20100266号ICP证030173号 隐私政策</div>

    <!-- 找回密码弹窗（手机 / 邮箱两种方式） -->
    <ForgotPasswordModal v-model:visible="forgotVisible" />
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { userLogin, sendSmsCode, userLoginBySms } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { isValidPhone } from '@/utils/validators'
import ForgotPasswordModal from '@/pages/user/ForgotPasswordModal.vue'
import CaptchaInput from '@/components/CaptchaInput.vue'

// 找回密码弹窗显隐
const forgotVisible = ref(false)

const activeTab = ref('account')
const agreed = ref(false)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
  phone: '',
  code: '',
})

// 短信登录
const smsPhone = ref('')
const smsCode = ref('')
const smsSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 图形验证码（人机校验）
const captchaRef = ref<InstanceType<typeof CaptchaInput> | null>(null)
const captchaInput = ref('')

// 手机号校验统一走 @/utils/validators 的 isValidPhone（与后端 PHONE_REGEX 一致）
// 账号登录 / 短信登录两个 tab 共用：传入对应手机号即可
const sendCode = async (phoneValue: string) => {
  const phone = (phoneValue || '').trim()
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

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const afterLoginSuccess = async () => {
  await loginUserStore.fetchLoginUser()
  message.success('登录成功')
  // 优先跳转到 redirect 指定地址
  const redirect = route.query.redirect as string
  if (redirect) {
    router.push({ path: redirect, replace: true })
    return
  }
  // 管理员跳转到后台
  if (loginUserStore.loginUser?.userRole === 'admin') {
    router.push({ path: '/admin/dashboard', replace: true })
    return
  }
  // 普通用户跳首页
  router.push({ path: '/', replace: true })
}

const handleSubmit = async () => {
  if (!agreed.value) {
    message.warning('请先阅读并同意用户协议')
    return
  }
  if (activeTab.value === 'sms') {
    if (!smsPhone.value.trim() || !smsCode.value.trim()) {
      message.warning('请输入手机号和验证码')
      return
    }
    try {
      const res = await userLoginBySms({ phone: smsPhone.value.trim(), code: smsCode.value.trim() })
      if (res.data.code === 0 && res.data.data) {
        await afterLoginSuccess()
      } else {
        message.error('登录失败，' + res.data.message)
      }
    } catch {
      message.error('登录失败，请重试')
    }
    return
  }
  if (!formState.userAccount || !formState.userPassword) {
    message.warning('请输入账号和密码')
    return
  }
  // [已关闭] 登录强制绑定手机号校验，按需重新开启
  // if (!formState.phone || !formState.code) {
  //   message.warning('请输入手机号并填写验证码')
  //   return
  // }
  const res = await userLogin(formState)
  if (res.data.code === 0 && res.data.data) {
    await afterLoginSuccess()
  } else {
    message.error('登录失败，' + res.data.message)
  }
}

// 微信扫码登录：跳转后端授权入口（vite proxy 转发到后端）
const goWechatLogin = () => {
  window.location.href = '/api/user/login/wechat'
}

// QQ 互联扫码登录：跳转后端授权入口（vite proxy 转发到后端）
const goQQLogin = () => {
  window.location.href = '/api/user/login/qq'
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

.login-page {
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

.login-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.2);
  z-index: 0;
}

.login-wrap {
  width: 100%;
  max-width: 1400px;
  display: flex;
  align-items: center;
  gap: 60px;
  position: relative;
  z-index: 1;
}

/* 左侧品牌 */
.login-left {
  flex: 1;
  color: #1f2937;
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

.tip-bubble {
  margin-top: 24px;
  background: #ffffffcc;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 14px;
  color: #374151;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* 右侧登录卡片 */
.login-card {
  width: 480px;
  background: #ffffff;
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: 0 4px 20px #00000012;
  flex-shrink: 0;
}

.card-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 28px;
}

.card-body {
  display: flex;
  gap: 24px;
}

.form-area {
  flex: 1;
}

.tab-wrap {
  display: flex;
  margin-bottom: 20px;
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 15px;
  color: #6b7280;
  padding-bottom: 8px;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.tab-item.active {
  color: #3b6ef7;
  border-bottom-color: #3b6ef7;
}

.input-item {
  margin-bottom: 14px;
}

.input-item input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
}

.input-item input:focus {
  border-color: #3b6ef7;
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

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 18px;
}

.checkbox-row a {
  color: #3b6ef7;
  text-decoration: none;
}

.login-btn {
  width: 100%;
  background: #b4bcf8;
  color: #fff;
  border: none;
  padding: 12px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.login-btn:hover {
  background: #9aa5f5;
}

.link-row {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
}

.link-row a {
  color: #6b7280;
  text-decoration: none;
  margin: 0 6px;
}

/* 二维码 */
.qrcode-area {
  width: 140px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

.qrcode-box {
  width: 120px;
  height: 120px;
  border: 1px solid #ddd;
  margin: 10px auto;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #999;
}

/* 微信扫码登录框 */
.wechat-qr {
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.wechat-qr:hover {
  border-color: #07c160;
  box-shadow: 0 4px 12px rgba(7, 193, 96, 0.15);
}

.wechat-qr-text {
  font-size: 12px;
  color: #07c160;
  font-weight: 500;
}

.qrcode-tip {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

/* 第三方登录 */
.other-login-title {
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
  margin: 28px 0 14px;
  position: relative;
}

.other-login-title::before,
.other-login-title::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 42%;
  height: 1px;
  background: #e5e7eb;
}

.other-login-title::before {
  left: 0;
}

.other-login-title::after {
  right: 0;
}

.third-icon-group {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.third-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.third-icon:hover {
  background: #f5f5f5;
}

.wechat-third {
  background: #07c160;
  border-color: #07c160;
}

.wechat-third:hover {
  background: #06ad56;
  border-color: #06ad56;
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
