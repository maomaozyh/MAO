<template>
  <div class="captcha-row">
    <input
      v-model="code"
      :placeholder="placeholder || '请输入图形验证码'"
      maxlength="4"
      class="captcha-input"
      autocomplete="off"
    />
    <img v-if="img" :src="img" class="captcha-img" @click="refresh" title="点击刷新" alt="图形验证码" />
    <span class="captcha-refresh" @click="refresh">看不清</span>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { getCaptcha } from '@/api/userController.ts'

const code = defineModel<string>({ default: '' })
const captchaKey = ref('')
const img = ref('')
const loading = ref(false)

const refresh = async () => {
  code.value = ''
  if (loading.value) return
  loading.value = true
  try {
    const res = await getCaptcha()
    if (res.data?.code === 0 && res.data.data) {
      captchaKey.value = res.data.data.captchaKey || ''
      img.value = res.data.data.captchaImg || ''
    }
  } finally {
    loading.value = false
  }
}

onMounted(refresh)

// 父组件通过 ref 取当前验证码标识；发送失败后调用 refresh() 换一张
defineExpose({ refresh, getKey: () => captchaKey.value })
</script>

<style scoped>
.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.captcha-input {
  flex: 1;
  padding: 11px 13px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.captcha-input:focus {
  border-color: #3b6ef7;
}

.captcha-img {
  width: 100px;
  height: 38px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-refresh {
  font-size: 12px;
  color: #3b6ef7;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
</style>
