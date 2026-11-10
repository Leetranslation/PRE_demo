import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // AI 知识库问答服务（Python FastAPI），独立进程，默认 8000 端口。
      // ⚠️ 不能挂在 /api 下 —— /api 已经被上面这条整个给 Java 后端了。
      //
      // 🔴 这里**必须带斜杠**（`^/ai/`），不能只写 `'/ai'`。
      //    代理是按**路径前缀**匹配的：写成 `'/ai'` 时，前端路由 `/ai-brain`
      //    （AI 工业大脑）也会命中，被转给 8000 —— 而 AI 服务上并没有 `/ai-brain`，
      //    于是直接打开 / 刷新 `http://localhost:5173/ai-brain` 会返回
      //    `{"detail":"Not Found"}`（FastAPI 的 404）。
      //    从菜单点进去是好的（客户端路由不过服务器），所以这个问题极难察觉。
      //    → 以 `^` 开头 = 按正则匹配，`^/ai/` 只吃 `/ai/xxx`，放过 `/ai-brain`。
      //    ⚠️ 生产环境 Nginx 有同一个坑：要写 `location /ai/` 而不是 `location /ai`。
      '^/ai/': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true
      }
    }
  }
})
