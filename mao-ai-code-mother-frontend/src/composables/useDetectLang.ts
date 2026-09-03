/**
 * 轻量语言识别（纯前端、无第三方依赖）
 * 用于首页输入框实时检测用户正在输入的语言，给出一个人读标签。
 *
 * 思路：
 *  1. 先按 Unicode 区块硬匹配 —— 韩文(Hangul) / 日文(假名) / 中文(CJK) /
 *     西里尔 / 泰文 各自独立成区，几乎不会误判。
 *  2. 拉丁文字按高频停用词命中数投票，区分 en/fr/es/de/pt。
 *  3. 含拉丁字母但无明显特征时，默认回退 English（代码生成类 prompt 多为英文）。
 *
 * 这是「提示性」识别，目的是给用户一个语言徽标，不追求 100% 精确。
 */

export type LangLabel =
  | '中文'
  | '日本語'
  | '한국어'
  | 'English'
  | 'Français'
  | 'Español'
  | 'Deutsch'
  | 'Português'
  | 'Русский'
  | 'ไทย'
  | '其他语言'

const STOPWORDS: Record<string, string[]> = {
  English: ['the', 'and', 'you', 'for', 'with', 'this', 'that', 'have', 'are', 'from'],
  Français: ['le', 'la', 'les', 'une', 'est', 'pas', 'vous', 'pour', 'avec', 'je'],
  Español: ['el', 'la', 'los', 'una', 'es', 'no', 'para', 'con', 'que', 'yo'],
  Deutsch: ['der', 'die', 'das', 'und', 'ist', 'nicht', 'ein', 'mit', 'ich', 'zu'],
  Português: ['com', 'para', 'que', 'uma', 'você', 'não', 'dos', 'das', 'ao', 'é'],
}

// 拉丁语言投票：返回命中数最多的语言；达到阈值才采纳
function voteLatin(text: string): LangLabel | null {
  const lower = text.toLowerCase()
  let best: LangLabel | null = null
  let bestHits = 0
  for (const [lang, words] of Object.entries(STOPWORDS)) {
    let hits = 0
    for (const w of words) {
      // \b 在 JS 中对中文标点不敏感，但拉丁词足够；用前后非字母判定
      const re = new RegExp('(^|[^a-z])' + w + '([^a-z]|$)', 'i')
      if (re.test(lower)) hits++
    }
    if (hits > bestHits) {
      bestHits = hits
      best = lang as LangLabel
    }
  }
  return bestHits >= 2 ? best : null
}

export function detectLang(text: string): LangLabel {
  const t = (text || '').trim()
  if (!t) return '其他语言'

  // 1) 按 Unicode 区块硬匹配（优先级最高）
  if (/[가-힣]/.test(t)) return '한국어' // Hangul
  if (/[ぁ-ゔァ-ヶ]/.test(t)) return '日本語' // Hiragana / Katakana
  if (/[㐀-鿿]/.test(t)) return '中文' // CJK 表意文字（无假名则判中文）
  if (/[Ѐ-ӿ]/.test(t)) return 'Русский' // Cyrillic
  if (/[ก-๛]/.test(t)) return 'ไทย' // Thai

  // 2) 拉丁文字：停用词投票
  if (/[a-zA-Z]/.test(t)) {
    const voted = voteLatin(t)
    if (voted) return voted
    return 'English' // 含拉丁但无明显特征，默认英文
  }

  return '其他语言'
}
