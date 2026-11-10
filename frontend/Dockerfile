# Token ERP 前端 —— 构建静态文件后用 Nginx 托管
#
# 为什么不用容器里的 vite dev：
#   dev server 只适合开发（热更新、慢、不压缩）。生产形态是「构建出 dist + Nginx」，
#   顺便把 /api 和 /ai 的反向代理也放在这里，一个入口搞定。

# ---------- 阶段 1：构建 ----------
FROM node:22-alpine AS build
WORKDIR /app

# 用 npm ci 而不是 npm install：严格按 package-lock.json 装，可复现
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 阶段 2：托管 ----------
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
