# ============================================
# Spring Boot 后端 Dockerfile（多阶段构建）
# ============================================

# --- Stage 1: 编译 ---
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
# 先拷贝 pom.xml 以利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B -q || true

# 拷贝源码并编译（跳过测试，加速构建）
COPY src ./src
RUN mvn package -DskipTests -B -q

# --- Stage 2: 运行 ---
FROM eclipse-temurin:21-jre-alpine AS runtime

# 安全：非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 创建临时文件目录
RUN mkdir -p /app/tmp && chown -R appuser:appgroup /app

# 复制 JAR
COPY --from=builder /build/target/*.jar app.jar

USER appuser

EXPOSE 8123

# JVM 参数：限制内存、启用容器感知
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Duser.dir=/app", \
    "-jar", "app.jar"]
