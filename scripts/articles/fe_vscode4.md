# VS Code 远程开发与 Dev Container

## Remote Development 扩展包

VS Code 的 Remote Development 扩展包允许开发者在远程服务器、容器或 WSL（Windows Subsystem for Linux）中进行开发，本地 VS Code 只作为前端界面。

### 安装扩展

```
# 扩展包名称
Remote Development

# 包含的扩展：
- Remote - SSH
- Remote - WSL
- Remote - Containers
- Dev Containers
```

## Remote SSH

Remote SSH 允许通过 SSH 连接到远程服务器进行开发。

### 配置 SSH 连接

```bash
# SSH 配置文件 ~/.ssh/config
Host dev-server
    HostName 192.168.1.100
    User developer
    Port 22
    IdentityFile ~/.ssh/id_rsa

Host prod-server
    HostName prod.example.com
    User admin
    Port 2222
    IdentityFile ~/.ssh/prod_key
```

### 连接到远程服务器

1. 按 `Ctrl+Shift+P` 打开命令面板
2. 输入 `Remote-SSH: Connect to Host`
3. 选择配置好的主机
4. 输入密码或使用密钥认证

### SSH 连接配置

```json
// .vscode/settings.json（远程服务器上）
{
    "remote.SSH.defaultForwardedPorts": [],
    "remote.SSH.enableDynamicForwarding": true,
    "remote.SSH.enableX11Forwarding": true
}
```

### 端口转发

```bash
# 自动端口转发
# VS Code 会自动检测并转发应用使用的端口

# 手动端口转发
# 1. 打开命令面板
# 2. 输入 "Forward a Port"
# 3. 输入端口号

# 或者在 launch.json 中配置
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "node",
            "request": "launch",
            "name": "Debug on Remote",
            "program": "${workspaceFolder}/app.js",
            "remoteRoot": "/home/user/project",
            "localRoot": "${workspaceFolder}"
        }
    ]
}
```

### SSH 隧道配置

```bash
# 通过跳板机连接内网服务器
# ~/.ssh/config
Host jump-server
    HostName jump.example.com
    User jumper
    IdentityFile ~/.ssh/jump_key

Host internal-server
    HostName 10.0.0.100
    User developer
    ProxyJump jump-server
```

## Remote WSL

Remote WSL 允许在 Windows 上使用 WSL 作为开发环境。

### 启用 WSL

```powershell
# 以管理员身份运行 PowerShell

# 启用 WSL 功能
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart

# 启用虚拟机平台
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# 下载并安装 WSL2 Linux 内核更新包
wsl --update

# 设置默认版本为 WSL2
wsl --set-default-version 2

# 安装 Ubuntu
wsl --install -d Ubuntu
```

### WSL 开发环境配置

```bash
# 在 WSL 中安装开发工具
sudo apt update
sudo apt install -y build-essential git curl wget

# 安装 Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 安装 Python
sudo apt install -y python3 python3-pip

# 安装 Java
sudo apt install -y openjdk-21-jdk
```

### VS Code WSL 集成

```bash
# 在 WSL 终端中打开 VS Code
code .

# 这会自动安装 VS Code Server 并连接到 WSL
```

```json
// WSL 中的 VS Code 设置
// ~/.vscode-server/data/Machine/settings.json
{
    "terminal.integrated.defaultProfile.linux": "bash",
    "editor.fontFamily": "'Cascadia Code', 'Courier New', monospace",
    "editor.fontSize": 14
}
```

## Remote Containers

Remote Containers 允许在 Docker 容器中进行开发，确保开发环境的一致性。

### 基本 Dev Container 配置

```json
// .devcontainer/devcontainer.json
{
    "name": "My Project Dev",
    "build": {
        "dockerfile": "Dockerfile",
        "context": "..",
        "args": {
            "NODE_VERSION": "20"
        }
    },
    "forwardPorts": [3000, 5432],
    "postCreateCommand": "npm install",
    "customizations": {
        "vscode": {
            "extensions": [
                "dbaeumer.vscode-eslint",
                "esbenp.prettier-vscode",
                "ms-azuretools.vscode-docker"
            ],
            "settings": {
                "editor.formatOnSave": true,
                "editor.defaultFormatter": "esbenp.prettier-vscode"
            }
        }
    }
}
```

### Dev Container Dockerfile

```dockerfile
# .devcontainer/Dockerfile
FROM mcr.microsoft.com/devcontainers/javascript-node:20

# 安装额外的系统工具
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends \
        vim \
        git-lfs \
        jq \
    && apt-get autoremove -y && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/*

# 安装全局 npm 包
RUN npm install -g \
    typescript \
    ts-node \
    nodemon \
    pm2

# 安装 Python 工具
RUN pip3 install --no-cache-dir \
    black \
    flake8 \
    pytest
```

### Docker Compose Dev Container

```json
// .devcontainer/devcontainer.json
{
    "name": "Full Stack Dev",
    "dockerComposeFile": "docker-compose.yml",
    "service": "app",
    "workspaceFolder": "/workspace",
    "shutdownAction": "stopCompose",
    "postCreateCommand": "npm install && npm run build",
    "forwardPorts": [3000, 5432, 6379],
    "customizations": {
        "vscode": {
            "extensions": [
                "dbaeumer.vscode-eslint",
                "ms-vscode.vscode-typescript-next",
                "cweijan.vscode-postgresql-client2"
            ]
        }
    }
}
```

```yaml
# .devcontainer/docker-compose.yml
version: "3.8"

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    volumes:
      - ..:/workspace:cached
    command: sleep infinity
    networks:
      - dev-network

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: devdb
      POSTGRES_USER: devuser
      POSTGRES_PASSWORD: devpass
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - dev-network

  redis:
    image: redis:alpine
    networks:
      - dev-network

volumes:
  postgres-data:

networks:
  dev-network:
```

## Dev Containers 扩展详解

### devcontainer.json 完整配置

```json
{
    // 基本信息
    "name": "My Dev Container",
    "forwardPorts": [3000, 5432],
    "portsAttributes": {
        "3000": {
            "label": "Application",
            "onAutoForward": "notify"
        },
        "5432": {
            "label": "Database",
            "onAutoForward": "silent"
        }
    },

    // 构建配置
    "build": {
        "dockerfile": "Dockerfile",
        "context": ".",
        "args": {
            "VARIANT": "20",
            "INSTALL_NODE": "true"
        }
    },

    // 运行时配置
    "runArgs": [
        "--cap-add=SYS_PTRACE",
        "--security-opt",
        "seccomp=unconfined"
    ],

    // 环境变量
    "containerEnv": {
        "NODE_ENV": "development",
        "DATABASE_URL": "postgresql://devuser:devpass@postgres:5432/devdb"
    },

    // 挂载卷
    "mounts": [
        "source=${localWorkspaceFolder},target=/workspace,type=bind,consistency=cached",
        "source=${localEnv:HOME}/.ssh,target=/root/.ssh,type=bind,readonly"
    ],

    // 生命周期命令
    "onCreateCommand": "npm install",
    "postCreateCommand": "npm run setup",
    "postStartCommand": "echo 'Container started'",
    "postAttachCommand": "echo 'Attached to container'",

    // VS Code 自定义
    "customizations": {
        "vscode": {
            "settings": {
                "terminal.integrated.defaultProfile.linux": "bash",
                "editor.formatOnSave": true,
                "editor.codeActionsOnSave": {
                    "source.fixAll.eslint": true
                }
            },
            "extensions": [
                "dbaeumer.vscode-eslint",
                "esbenp.prettier-vscode",
                "ms-vscode.vscode-typescript-next",
                "bradlc.vscode-tailwindcss"
            ]
        }
    },

    // 用户配置
    "remoteUser": "node",
    "containerUser": "node"
}
```

### 特定语言的配置

**Python Dev Container:**

```json
{
    "name": "Python Dev",
    "image": "mcr.microsoft.com/devcontainers/python:3.11",
    "postCreateCommand": "pip install -r requirements.txt",
    "customizations": {
        "vscode": {
            "settings": {
                "python.defaultInterpreterPath": "/usr/local/bin/python",
                "python.linting.enabled": true,
                "python.linting.pylintEnabled": true
            },
            "extensions": [
                "ms-python.python",
                "ms-python.vscode-pylance",
                "ms-python.black-formatter"
            ]
        }
    }
}
```

**Java Dev Container:**

```json
{
    "name": "Java Dev",
    "image": "mcr.microsoft.com/devcontainers/java:21",
    "features": {
        "ghcr.io/devcontainers/features/java:1": {
            "version": "21",
            "installMaven": true,
            "installGradle": true
        }
    },
    "customizations": {
        "vscode": {
            "settings": {
                "java.jdt.ls.java.home": "/usr/local/sdkman/candidates/java/current"
            },
            "extensions": [
                "vscjava.vscode-java-pack",
                "vmware.vscode-spring-boot",
                "pivotal.vscode-spring-boot"
            ]
        }
    }
}
```

## 端口转发

### 自动端口转发

```json
// devcontainer.json
{
    "forwardPorts": [3000, 8080, 5432],
    "portsAttributes": {
        "3000": {
            "label": "Frontend",
            "onAutoForward": "openBrowser"
        },
        "8080": {
            "label": "Backend",
            "onAutoForward": "notify"
        },
        "5432": {
            "label": "Database",
            "onAutoForward": "ignore"
        }
    }
}
```

### 手动端口转发

```bash
# 命令面板：Forward a Port
# 或者使用命令行
# 在终端中查看转发的端口
lsof -i :3000

# 测试端口连通性
curl http://localhost:3000
```

## GitHub Codespaces

### 创建 Codespace

```bash
# 使用 GitHub CLI
gh codespace create --repo owner/repo --branch main

# 列出所有 Codespace
gh codespace list

# 连接到现有 Codespace
gh codespace code

# 停止 Codespace
gh codespace stop
```

### Codespace 配置

```json
// .devcontainer/devcontainer.json (Codespace 专用)
{
    "name": "Codespace Configuration",
    "image": "mcr.microsoft.com/devcontainers/universal:linux",
    "features": {
        "ghcr.io/devcontainers/features/node:1": {
            "version": "20"
        },
        "ghcr.io/devcontainers/features/python:1": {
            "version": "3.11"
        }
    },
    "postCreateCommand": ".devcontainer/setup.sh",
    "customizations": {
        "codespaces": {
            "openFiles": [
                "README.md",
                "src/index.ts"
            ]
        },
        "vscode": {
            "settings": {},
            "extensions": []
        }
    },
    "portsAttributes": {
        "3000": {
            "label": "Application",
            "visibility": "public"
        }
    }
}
```

### Codespace 设置脚本

```bash
#!/bin/bash
# .devcontainer/setup.sh

set -e

echo "Installing dependencies..."
npm install

echo "Setting up database..."
npm run db:setup

echo "Seeding data..."
npm run db:seed

echo "Setup complete!"
```

## Settings Sync

### 启用 Settings Sync

1. 打开命令面板：`Ctrl+Shift+P`
2. 输入 `Settings Sync: Turn On`
3. 选择要同步的内容：
   - Settings
   - Keybindings
   - Extensions
   - UI State
   - Profiles

### 同步配置

```json
// settings.json 中的同步相关配置
{
    "settingsSync.keybindingsPerPlatform": true,
    "settingsSync.ignoredExtensions": [
        "ms-vscode-remote.remote-containers",
        "ms-vscode-remote.remote-ssh"
    ],
    "settingsSync.ignoredSettings": [
        "terminal.integrated.defaultProfile.*"
    ]
}
```

### 多设备同步策略

```
# 最佳实践
1. 使用 Profiles 区分不同项目类型
2. 忽略平台特定的设置
3. 忽略远程特定的扩展
4. 定期检查同步状态
```

## 多根工作区

### 创建多根工作区

```json
// workspace.code-workspace
{
    "folders": [
        {
            "name": "Frontend",
            "path": "./frontend"
        },
        {
            "name": "Backend",
            "path": "./backend"
        },
        {
            "name": "Shared",
            "path": "./shared"
        },
        {
            "name": "Documentation",
            "path": "./docs"
        }
    ],
    "settings": {
        "editor.formatOnSave": true,
        "typescript.tsdk": "./frontend/node_modules/typescript/lib"
    },
    "launch": {
        "version": "0.2.0",
        "configurations": [
            {
                "type": "node",
                "request": "launch",
                "name": "Launch Backend",
                "program": "${workspaceFolder:Backend}/src/index.js"
            }
        ]
    }
}
```

### 多根工作区设置

```json
// 在多根工作区中，设置可以针对每个文件夹
{
    // 全局设置
    "editor.formatOnSave": true,
    
    // 针对特定文件夹的设置
    "[typescript]": {
        "editor.defaultFormatter": "esbenp.prettier-vscode"
    },
    
    // 文件关联
    "files.associations": {
        "*.component.ts": "typescript",
        "*.service.ts": "typescript"
    },
    
    // 排除文件
    "files.watcherExclude": {
        "**/node_modules/**": true,
        "**/dist/**": true
    },
    
    // 搜索排除
    "search.exclude": {
        "**/node_modules": true,
        "**/dist": true,
        "**/build": true
    }
}
```

### 多根工作区调试

```json
// .vscode/launch.json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Debug Frontend",
            "type": "chrome",
            "request": "launch",
            "url": "http://localhost:5173",
            "webRoot": "${workspaceFolder:Frontend}/src"
        },
        {
            "name": "Debug Backend",
            "type": "node",
            "request": "launch",
            "program": "${workspaceFolder:Backend}/src/index.js",
            "restart": true,
            "console": "integratedTerminal"
        }
    ],
    "compounds": [
        {
            "name": "Debug Full Stack",
            "configurations": [
                "Debug Frontend",
                "Debug Backend"
            ]
        }
    ]
}
```

## 远程开发最佳实践

### 网络优化

```json
// settings.json
{
    "remote.SSH.enableDynamicForwarding": true,
    "remote.SSH.useLocalServer": false,
    "remote.SSH.remotePlatform": {
        "dev-server": "linux"
    },
    "remote.SSH.maxReconnectionAttempts": 5
}
```

### 性能优化

```json
// settings.json
{
    "files.watcherExclude": {
        "**/node_modules/**": true,
        "**/dist/**": true,
        "**/.git/objects/**": true
    },
    "search.exclude": {
        "**/node_modules": true,
        "**/dist": true,
        "**/build": true
    },
    "files.exclude": {
        "**/node_modules": true,
        "**/.git": true
    }
}
```

### 安全配置

```json
// devcontainer.json
{
    "runArgs": [
        "--security-opt=no-new-privileges:true"
    ],
    "containerEnv": {
        "GIT_CONFIG_NOSYSTEM": "1"
    },
    "mounts": [
        "source=${localEnv:SSH_AUTH_SOCK},target=/ssh-agent,type=bind"
    ]
}
```

## 常见问题解决

### SSH 连接超时

```bash
# 增加 SSH 超时时间
# ~/.ssh/config
Host *
    ServerAliveInterval 60
    ServerAliveCountMax 3
    TCPKeepAlive yes
```

### WSL 性能问题

```json
// settings.json
{
    "terminal.integrated.enablePersistentSessions": false,
    "terminal.integrated.gpuAcceleration": "off"
}
```

### Dev Container 构建失败

```bash
# 清理 Docker 缓存
docker system prune -a

# 重新构建容器
# 命令面板：Dev Containers: Rebuild Container
```
