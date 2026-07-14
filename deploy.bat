@echo off
chcp 65001 >nul
echo ==========================================
echo   码上记（CodeNow）Docker 部署脚本
echo ==========================================

:: 检查 Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未安装 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)

:: 检查 .env 文件
if not exist .env (
    echo 未找到 .env 文件，正在从模板创建...
    copy .env.example .env
    echo 请编辑 .env 文件配置实际参数，然后重新运行此脚本
    pause
    exit /b 1
)

:: 构建后端
echo.
echo [1/4] 构建后端 JAR 包...
cd codenow-backend
call mvnw.cmd clean package -DskipTests -q
cd ..
echo 后端构建完成

:: 构建镜像
echo.
echo [2/4] 构建 Docker 镜像...
docker compose build

:: 启动服务
echo.
echo [3/4] 启动所有服务...
docker compose up -d

:: 等待启动
echo.
echo [4/4] 等待服务启动...
timeout /t 10 /nobreak >nul

:: 显示状态
echo.
echo ==========================================
echo   服务状态
echo ==========================================
docker compose ps

echo.
echo ==========================================
echo   部署完成！
echo ==========================================
echo   博客前台:  http://localhost/blog
echo   管理后台:  http://localhost
echo   后端 API:  http://localhost:8080
echo   默认账号:  admin / 123456
echo ==========================================
pause
