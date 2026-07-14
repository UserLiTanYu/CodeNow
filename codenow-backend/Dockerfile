# 后端 Dockerfile
# 基于 JDK 21 轻量镜像
FROM eclipse-temurin:21-jre-alpine

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 创建工作目录
WORKDIR /app

# 复制 JAR 包
COPY target/codenow-backend.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 启动命令（通过 spring.profiles.active 激活生产配置）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
