/**
 * 图片压缩 —— 纯浏览器 canvas，零依赖。
 *
 * ## 为什么要压
 *
 * 视觉模型按**图片 token** 计费，分辨率越高越贵（实测 1079×624 的截图 ≈ 702 tokens）。
 * 手机拍的单据动辄 3~5MB、4000×3000，直接传会有三个问题：
 *   1. 请求体巨大（base64 还要再涨约 33%）
 *   2. 视觉 token 翻好几倍 → 贵
 *   3. 后端得兜一个"图片过大"的限制
 *
 * ## 为什么在浏览器压，而不是后端压
 *
 * 这台机器**没有图像处理工具**（Pillow 没装，ffmpeg / ImageMagick 也没有）。
 * 而浏览器天生有 canvas —— 缩放 + 转 JPEG 十几行代码，还省掉了把原图传上去的带宽。
 *
 * ## 参数是怎么定的
 *
 * - `maxEdge = 1600`：实测 1079×624 的截图识别得很好；
 *   A4 扫描件约 2480×3508，压到长边 1600 后小字仍能认。
 *   再往下（比如 1000）笔画细的字就开始糊了 —— **识别率比省那点 token 重要**。
 * - `quality = 0.85`：JPEG 在 0.85 上肉眼基本无损，体积约是原图的 1/10。
 *
 * ⚠️ **不要压成 WebP** —— 视觉模型对 WebP 的支持不如 JPEG 稳。
 * ⚠️ **HEIC/HEIF 直接拒绝** —— 浏览器普遍解不了（iPhone 默认格式就是它），
 *    早点说清楚，别让用户等半天看到"解析失败"。
 */

/** 压缩结果 */
export interface CompressResult {
  /** base64（**不含** `data:` 前缀，由后端拼） */
  base64: string
  mediaType: 'image/jpeg'
  width: number
  height: number
  /** 压缩后约多少字节（用 base64 长度反推） */
  bytes: number
}

export interface CompressOptions {
  /** 长边上限，默认 1600 */
  maxEdge?: number
  /** JPEG 质量 0~1，默认 0.85 */
  quality?: number
}

type Drawable = ImageBitmap | HTMLImageElement

async function loadImage(file: File): Promise<Drawable> {
  // 优先 createImageBitmap：不占 DOM、更快；Safari 老版本没有就退回 Image
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file)
    } catch {
      /* 落到下面的兜底 */
    }
  }
  return await new Promise<Drawable>((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片解码失败，可能是不支持的格式'))
    }
    img.src = url
  })
}

/**
 * 把用户选的图片压到适合视觉模型的大小。
 *
 * @throws 非图片 / HEIC / 解码失败 / 浏览器不支持 canvas
 */
export async function compressImage(
  file: File,
  opts: CompressOptions = {}
): Promise<CompressResult> {
  const maxEdge = opts.maxEdge ?? 1600
  const quality = opts.quality ?? 0.85

  if (!file.type.startsWith('image/')) {
    throw new Error('只能上传图片（JPG / PNG）')
  }
  if (/heic|heif/i.test(file.type) || /\.(heic|heif)$/i.test(file.name)) {
    throw new Error('不支持 HEIC/HEIF 格式，请先转成 JPG，或直接截图再上传')
  }

  const src = await loadImage(file)
  const sw = 'width' in src ? src.width : 0
  const sh = 'height' in src ? src.height : 0
  if (!sw || !sh) throw new Error('读不到图片尺寸')

  const scale = Math.min(1, maxEdge / Math.max(sw, sh))
  const w = Math.max(1, Math.round(sw * scale))
  const h = Math.max(1, Math.round(sh * scale))

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('浏览器不支持 canvas，无法压缩图片')

  // 铺白底：PNG 的透明区域转 JPEG 会变黑，单据一般是白纸，铺白更接近原样
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, w, h)
  ctx.drawImage(src, 0, 0, w, h)
  if ('close' in src && typeof src.close === 'function') src.close()

  const dataUrl = canvas.toDataURL('image/jpeg', quality)
  const base64 = dataUrl.split(',')[1] || ''
  if (!base64) throw new Error('图片压缩失败')

  return {
    base64,
    mediaType: 'image/jpeg',
    width: w,
    height: h,
    // base64 每 4 个字符对应 3 字节
    bytes: Math.round(base64.length * 0.75)
  }
}

/** 人类可读的体积，用于界面上告诉用户「压到了多大」 */
export function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
