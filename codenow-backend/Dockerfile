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
RUN ./mvnw clean package -DskipTests -B

# ===== 运行阶段 =====
FROM eclipse-temurin:21-jre-alpine

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

WORKDIR /app

# 从构建阶段复制 JAR 包
COPY --from=builder /build/target/codenow-backend.jar app.jar

EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 使用 exec 形式确保正确的信号处理（PID 1）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
