# ===== 构建阶段 =====
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 先复制 Maven 配置和依赖（利用 Docker 缓存层）
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN ./mvnw clean package -B

# ===== 运行阶段 =====
FROM eclipse-temurin:21-jre-alpine

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制 JAR 包
COPY --from=builder /build/target/codenow-backend.jar app.jar

# 设置文件权限
RUN chown appuser:appgroup app.jar

EXPOSE 8080

# 以非 root 用户运行
USER appuser

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 使用 exec 形式确保正确的信号处理（PID 1）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
