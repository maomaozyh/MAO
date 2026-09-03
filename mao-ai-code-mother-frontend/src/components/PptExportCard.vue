<template>
  <div class="ppt-export-card">
    <div class="pec-head">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="pec-icon">
        <path d="M4 4h11l5 5v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z" />
        <path d="M14 4v5h5" />
      </svg>
      <div class="pec-meta">
        <div class="pec-title">{{ parsed?.title || 'PPT 演示文稿' }}</div>
        <div class="pec-sub">{{ slideCount }} 页 · 可导出为 .pptx</div>
      </div>
    </div>
    <button class="pec-btn" :disabled="exporting || !parsed" @click="exportPptx">
      <svg v-if="!exporting" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="7 10 12 15 17 10" />
        <line x1="12" y1="15" x2="12" y2="3" />
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="pec-spin">
        <path d="M21 12a9 9 0 1 1-6.219-8.56" />
      </svg>
      <span>{{ exporting ? '生成中…' : '导出为 PPTX' }}</span>
    </button>
    <div v-if="error" class="pec-error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  slidesJson: string
}>()

const exporting = ref(false)
const error = ref('')

interface Slide {
  title?: string
  bullets?: string[]
  notes?: string
}
interface SlidesData {
  title?: string
  slides?: Slide[]
}

const parsed = computed<SlidesData | null>(() => {
  try {
    const data = JSON.parse(props.slidesJson)
    if (data && Array.isArray(data.slides)) {
      return data as SlidesData
    }
    return null
  } catch {
    return null
  }
})

const slideCount = computed(() => parsed.value?.slides?.length ?? 0)

async function exportPptx() {
  if (!parsed.value) {
    error.value = 'PPT 大纲数据无效，无法导出'
    return
  }
  exporting.value = true
  error.value = ''
  try {
    const pptxgen = (await import('pptxgenjs')).default
    const pres = new pptxgen()
    pres.defineLayout({ name: 'WIDE', width: 13.33, height: 7.5 })
    pres.layout = 'WIDE'
    pres.author = '秒哒'
    pres.title = parsed.value.title || '演示文稿'

    // 封面页
    const cover = pres.addSlide()
    cover.background = { color: 'F5F3FF' }
    cover.addText(parsed.value.title || '演示文稿', {
      x: 0.8,
      y: 3.0,
      w: 11.7,
      h: 1.6,
      fontSize: 40,
      bold: true,
      color: '2D2A5A',
      align: 'center',
    })

    for (const s of parsed.value.slides ?? []) {
      const slide = pres.addSlide()
      slide.addText(s.title || '', {
        x: 0.6,
        y: 0.5,
        w: 12.1,
        h: 1.0,
        fontSize: 28,
        bold: true,
        color: '1F1F3D',
      })
      slide.addText(
        (s.bullets || []).map((b) => ({ text: b, options: { bullet: { indent: 20 } } })),
        {
          x: 0.8,
          y: 1.7,
          w: 11.7,
          h: 5.0,
          fontSize: 18,
          color: '333333',
          lineSpacingMultiple: 1.3,
        },
      )
      if (s.notes) {
        slide.addNotes(s.notes)
      }
    }

    const fileName = (parsed.value.title || 'presentation').replace(/[\\/:*?"<>|]/g, '_') + '.pptx'
    await pres.writeFile({ fileName })
  } catch (e) {
    error.value = '导出失败：' + (e instanceof Error ? e.message : String(e))
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.ppt-export-card {
  border: 1px solid #e9e6ff;
  background: linear-gradient(180deg, #faf8ff 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 16px;
  margin: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
}
.pec-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pec-icon {
  width: 34px;
  height: 34px;
  color: #6c3ce0;
  flex-shrink: 0;
}
.pec-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f3d;
}
.pec-sub {
  font-size: 12px;
  color: #8a8f99;
  margin-top: 2px;
}
.pec-btn {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #6c3ce0;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease, transform 0.1s ease;
}
.pec-btn:hover:not(:disabled) {
  background: #5a32c4;
}
.pec-btn:active:not(:disabled) {
  transform: scale(0.97);
}
.pec-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.pec-btn svg {
  width: 16px;
  height: 16px;
}
.pec-spin {
  animation: pec-rotate 1s linear infinite;
}
@keyframes pec-rotate {
  to {
    transform: rotate(360deg);
  }
}
.pec-error {
  font-size: 12px;
  color: #e5484d;
}
</style>
