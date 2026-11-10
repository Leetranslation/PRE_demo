<script setup lang="ts">
/**
 * TK Ai 品牌图标（矢量，无外部图片依赖）。
 *
 * 一个锁定的字形：上排「TK」，下排「Ai」——
 * 难点在 i 要能认出来。做法是把 i 写成小写形态：**点与竖之间留空隙**，
 * 连接线接在 i 的**中段**（与 A 的横杠同高），读起来就是「一条线把 A 和 i 串起来」。
 * 那个悬空的点是整组字形里唯一不连的笔画，反而成了视觉落点，一看就知道是 Ai。
 */
withDefaults(
  defineProps<{
    /** badge/mark 的边长；brand 的文字字号 */
    size?: number
    /** badge = 方形图标；brand = 图标 + 「TK Ai」文字；mark = 只有连写的 Ai */
    variant?: 'badge' | 'brand' | 'mark'
    /** 强调色（图标底色 / Ai 的颜色） */
    accent?: string
    /** 文字色（brand 下的「TK」） */
    color?: string
  }>(),
  { size: 24, variant: 'badge', accent: '#d32f2f', color: '#1f2430' }
)
</script>

<template>
  <svg
    v-if="variant === 'badge'"
    :width="size"
    :height="size"
    viewBox="0 0 72 72"
    class="tkai-icon"
    role="img"
    aria-label="TK Ai"
  >
    <rect width="72" height="72" rx="18" :fill="accent" />
    <text
      x="36"
      y="19"
      text-anchor="middle"
      dominant-baseline="central"
      font-size="17"
      font-weight="500"
      letter-spacing="0.5"
      fill="#ffffff"
    >
      TK
    </text>
    <g fill="none" stroke="#ffffff" stroke-width="3.6" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="21,63 30,40 39,63" />
      <line x1="24" y1="55" x2="51" y2="55" />
      <line x1="51" y1="41" x2="51" y2="63" />
    </g>
    <circle cx="51" cy="33" r="2.8" fill="#ffffff" />
  </svg>

  <span v-else-if="variant === 'mark'" class="tkai-mark">
    <svg
      :width="size * (30 / 22)"
      :height="size"
      viewBox="0 0 30 22"
      role="img"
      aria-label="Ai"
    >
      <g
        fill="none"
        :stroke="accent"
        :stroke-width="size * 0.16"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="8,4 0,22 16,22" />
        <line x1="4" y1="13" x2="27" y2="13" />
        <line x1="27" y1="7" x2="27" y2="22" />
      </g>
      <circle cx="27" cy="2.4" :r="size * 0.09" :fill="accent" />
    </svg>
  </span>

  <span v-else class="tkai-brand">
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 72 72"
      class="tkai-icon"
      role="img"
      aria-label="TK Ai"
    >
      <rect width="72" height="72" rx="18" :fill="accent" />
      <text
        x="36"
        y="19"
        text-anchor="middle"
        dominant-baseline="central"
        font-size="17"
        font-weight="500"
        letter-spacing="0.5"
        fill="#ffffff"
      >
        TK
      </text>
      <g fill="none" stroke="#ffffff" stroke-width="3.6" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="21,63 30,40 39,63" />
        <line x1="24" y1="55" x2="51" y2="55" />
        <line x1="51" y1="41" x2="51" y2="63" />
      </g>
      <circle cx="51" cy="33" r="2.8" fill="#ffffff" />
    </svg>
    <span class="tkai-brand__text" :style="{ fontSize: size * 0.68 + 'px', color }">
      <span class="tkai-brand__tk">TK</span>
      <svg
        class="tkai-brand__mark"
        :width="size * 0.93 * (30 / 22)"
        :height="size * 0.93"
        viewBox="0 0 30 22"
        role="img"
        aria-label="Ai"
      >
        <g
          fill="none"
          :stroke="accent"
          stroke-width="3.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="8,4 0,22 16,22" />
          <line x1="4" y1="13" x2="27" y2="13" />
          <line x1="27" y1="7" x2="27" y2="22" />
        </g>
        <circle cx="27" cy="2.4" r="1.9" :fill="accent" />
      </svg>
    </span>
  </span>
</template>

<style scoped>
.tkai-icon,
.tkai-mark {
  display: block;
  flex: none;
}
.tkai-brand {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.tkai-brand__text {
  display: inline-flex;
  align-items: baseline;
  font-weight: 500;
  letter-spacing: 0.4px;
  line-height: 1;
}
.tkai-brand__mark {
  display: inline-block;
  vertical-align: baseline;
  margin-left: 3px;
}
</style>
