# ============================================
# 线上部署：直接使用预编译 JAR（跳过远端 Maven 编译）
# ============================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 复制预编译 jar
COPY target/ai-agent-0.0.1-SNAPSHOT.jar app.jar

# JVM 内存调优
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxMetaspaceSize=128m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8123

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
