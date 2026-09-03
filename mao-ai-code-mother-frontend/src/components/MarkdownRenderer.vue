<template>
  <div class="markdown-content" v-html="renderedMarkdown" @click="onContentClick"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
// 瘦身：lib/common 只带 ~40 种常用语言（js/ts/python/java/go/json/bash/html/css/sql/yaml...），
// 全量 import 'highlight.js' 会打包 190+ 种语言（900KB+）
import hljs from 'highlight.js/lib/common'

// 引入代码高亮样式
import 'highlight.js/styles/github.css'

interface Props {
  content: string
}

const props = defineProps<Props>()

// 配置 markdown-it 实例
const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str: string, lang: string): string {
    const language = (lang || 'text').toLowerCase()
    let codeHtml: string

    if (lang && hljs.getLanguage(lang)) {
      try {
        codeHtml = hljs.highlight(str, { language: lang, ignoreIllegals: true }).value
      } catch {
        codeHtml = md.utils.escapeHtml(str)
      }
    } else {
      codeHtml = md.utils.escapeHtml(str)
    }

    // 代码块带语言标签 + 复制按钮的头部，提升 AI 回答可读性
    return (
      '<div class="code-block">' +
      '<div class="code-head">' +
      '<span class="code-lang">' +
      language.toUpperCase() +
      '</span>' +
      '<button class="code-copy" type="button">复制</button>' +
      '</div>' +
      '<pre class="hljs"><code>' +
      codeHtml +
      '</code></pre>' +
      '</div>'
    )
  },
})

// 链接新窗口打开，避免跳出应用
md.renderer.rules.link_open = function (tokens, idx, options, env, self) {
  const token = tokens[idx]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

// 计算渲染后的 Markdown
const renderedMarkdown = computed(() => {
  return md.render(props.content)
})

// 代码块复制：事件委托（v-html 内按钮无法用 Vue 事件）
const onContentClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const btn = target.closest('.code-copy') as HTMLButtonElement | null
  if (!btn) return
  const block = btn.closest('.code-block')
  const code = block?.querySelector('code')?.textContent || ''
  if (!code) return
  const restore = () => {
    btn.textContent = '复制'
  }
  try {
    navigator.clipboard?.writeText(code).then(
      () => {
        btn.textContent = '已复制'
        setTimeout(restore, 1500)
      },
      restore,
    )
  } catch {
    restore()
  }
}
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
}

/* 全局样式，影响 v-html 内容 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.5em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
}

.markdown-content :deep(p) {
  margin: 0.8em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-content :deep(code:not(pre code)) {
  background-color: #f1f1f1;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background-color: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9em;
  line-height: 1.4;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 0.5em 0.8em;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

.markdown-content :deep(a) {
  color: #1890ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5em 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 1.5em 0;
}

/* 代码高亮样式优化 */
.markdown-content :deep(.hljs) {
  background-color: #f8f8f8 !important;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  line-height: 1.4;
}

.markdown-content :deep(.hljs-keyword) {
  color: #d73a49;
  font-weight: 600;
}

.markdown-content :deep(.hljs-string) {
  color: #032f62;
}

.markdown-content :deep(.hljs-comment) {
  color: #6a737d;
  font-style: italic;
}

.markdown-content :deep(.hljs-number) {
  color: #005cc5;
}

.markdown-content :deep(.hljs-function) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-tag) {
  color: #22863a;
}

.markdown-content :deep(.hljs-attr) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-title) {
  color: #6f42c1;
  font-weight: 600;
}

/* ===== 代码块（带语言标签 + 复制按钮） ===== */
.markdown-content :deep(.code-block) {
  margin: 1em 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #f8f8f8;
}

.markdown-content :deep(.code-head) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #ffffff;
  border-bottom: 1px solid #eef0f4;
}

.markdown-content :deep(.code-lang) {
  font-size: 12px;
  color: #8a8f99;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.markdown-content :deep(.code-copy) {
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 6px;
  transition: background 0.15s ease, color 0.15s ease;
}

.markdown-content :deep(.code-copy:hover) {
  background: #f2f4f8;
  color: #1f2329;
}

.markdown-content :deep(.code-block pre.hljs) {
  margin: 0;
  border: none;
  border-radius: 0;
  background-color: transparent !important;
}
</style>
