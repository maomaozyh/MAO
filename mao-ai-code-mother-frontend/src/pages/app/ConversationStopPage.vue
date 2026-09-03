<template>
  <div class="stop-page">
    <div class="top-bar">
      <div class="top-left">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2" class="back-svg" @click="goBack">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2"/>
        </svg>
        <span>1</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2">
          <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
        </svg>
      </div>
      <div class="top-right">
        <span class="tag-419">419 积分</span>
        <div class="btn-share" @click="share">分享</div>
      </div>
    </div>

    <div class="main-wrap">
      <div class="avatar-bubble">😐</div>
      <div class="text-tip">对话已停止</div>
      <div class="tip-sub">当前对话因模型不支持图片 / 视觉输入而停止，请改用文字重新描述</div>
      <div class="op-buttons">
        <svg class="op-btn" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2" @click="retry">
          <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v12h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2h-3z"/>
        </svg>
        <svg class="op-btn" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2" @click="goHome">
          <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
          <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
        </svg>
      </div>
    </div>

    <div class="float-down">⌄</div>

    <div class="bottom-input-area">
      <div class="input-box">
        <input class="input-text" v-model="inputVal" placeholder="请输入修改描述，@快捷调用技能" @keyup.enter="send"/>
        <div class="input-footer">
          <div class="left-group">
            <div class="select-btn">☑ 深度 ∨</div>
            <div class="add-btn" @click="add">+</div>
          </div>
          <div class="right-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2">
              <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>
            </svg>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#444" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
            <div class="send-btn" @click="send">↑</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const inputVal = ref('')

function goBack() {
  router.back()
}
function goHome() {
  router.push('/')
}
function retry() {
  router.push('/app/chat')
}
function share() {
  alert('已分享')
}
function add() {
  alert('新增')
}
function send() {
  if (!inputVal.value.trim()) return
  alert('发送消息：' + inputVal.value.trim())
  inputVal.value = ''
}
</script>

<style scoped>
.stop-page {
  width: 100%;
  background: #f8f9fc;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
}
.top-left {
  display: flex;
  gap: 12px;
  align-items: center;
}
.top-right {
  display: flex;
  gap: 12px;
  align-items: center;
}
.tag-419 {
  color: #4169f4;
  font-size: 14px;
}
.btn-share {
  background: #111;
  color: #fff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}
.back-svg {
  cursor: pointer;
}
.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80px;
}
.avatar-bubble {
  width: 48px;
  height: 48px;
  background: #f0f4fc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 16px;
}
.text-tip {
  font-size: 14px;
  color: #222;
  margin-bottom: 12px;
  font-weight: 600;
}
.tip-sub {
  font-size: 13px;
  color: #999;
  margin-bottom: 20px;
  text-align: center;
  max-width: 320px;
}
.op-buttons {
  display: flex;
  gap: 24px;
}
.op-btn {
  width: 20px;
  height: 20px;
  opacity: 0.6;
  cursor: pointer;
}
.float-down {
  position: fixed;
  right: 12px;
  bottom: 120px;
  width: 32px;
  height: 32px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 6px #00000015;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.bottom-input-area {
  padding: 16px 24px;
}
.input-box {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 14px 18px;
  box-shadow: 0 2px 12px #00000008;
}
.input-text {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 4px 0 12px;
}
.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.left-group {
  display: flex;
  gap: 8px;
}
.select-btn, .add-btn {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 13px;
  cursor: pointer;
}
.add-btn {
  background: #f8f9fc;
}
.right-group {
  display: flex;
  gap: 14px;
  align-items: center;
  opacity: 0.65;
}
.send-btn {
  width: 28px;
  height: 28px;
  background: #e8ebf2;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>