#!/bin/bash
# ============================================
# 码上记（CodeNow）一键部署脚本
# 用法: bash deploy.sh
# ============================================

set -e

echo "=========================================="
echo "  码上记（CodeNow）Docker 部署脚本"
echo "=========================================="

# 1. 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: 未安装 Docker，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: 未安装 Docker Compose，请先安装"
    exit 1
fi

# 2. 检查 .env 文件
if [ ! -f .env ]; then
    echo "未找到 .env 文件，正在从模板创建..."
    cp .env.example .env
    echo "请编辑 .env 文件配置实际参数，然后重新运行此脚本"
    exit 1
fi

# 3. 构建后端 JAR 包
echo ""
echo "[1/4] 构建后端 JAR 包..."
cd codenow-backend
./mvnw clean package -DskipTests -q
cd ..
echo "后端构建完成"

# 4. 构建并启动所有服务
echo ""
echo "[2/4] 构建 Docker 镜像..."
docker compose build

echo ""
echo "[3/4] 启动所有服务..."
docker compose up -d

echo ""
echo "[4/4] 等待服务启动..."
sleep 10

# 5. 检查服务状态
echo ""
echo "=========================================="
echo "  服务状态"
echo "=========================================="
docker compose ps

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo "  博客前台:  http://localhost:${FRONTEND_PORT:-80}/blog"
echo "  管理后台:  http://localhost:${FRONTEND_PORT:-80}"
echo "  后端 API:  http://localhost:${BACKEND_PORT:-8080}"
echo "  默认账号:  admin / 123456"
echo "=========================================="
