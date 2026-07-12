# ============================================
# Vue 前端 Dockerfile（多阶段构建）
# ============================================

# --- Stage 1: 编译 ---
FROM node:20-alpine AS builder

WORKDIR /build

# 利用 Docker 缓存加速依赖安装
COPY package.json package-lock.json* ./
RUN npm install

# 拷贝源码并构建
COPY . .
RUN npm run build

# --- Stage 2: Nginx 运行 ---
FROM nginx:alpine

# 删除默认配置
RUN rm -f /etc/nginx/conf.d/default.conf

# 拷贝自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 拷贝构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
