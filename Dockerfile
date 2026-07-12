# ============================================
# Stage 1: 构建阶段 (Maven + JDK)
# ============================================
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app

# 复制源码
COPY pom.xml .
COPY src ./src

# 构建（跳过测试加速）
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: 运行阶段 (JRE only — 大幅减小镜像)
# ============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /app/target/ai-agent-0.0.1-SNAPSHOT.jar app.jar

# JVM 内存调优（适配 1GB 容器）
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxMetaspaceSize=128m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8123

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
