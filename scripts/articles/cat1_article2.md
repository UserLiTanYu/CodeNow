# 开发环境搭建与 JDK 安装

## JDK 的选择：Oracle JDK vs OpenJDK

在安装 JDK 之前，首先需要了解两种主要的 JDK 发行版：

| 对比项 | Oracle JDK | OpenJDK |
|-------|-----------|---------|
| 维护方 | Oracle 公司 | Oracle + 社区（OpenJDK 项目） |
| 许可证 | 商业许可（Java 17+ 免费用于开发和测试，生产环境需付费） | GPLv2 + Classpath Exception（免费商用） |
| 更新节奏 | 与 OpenJDK 同步 | 每 6 个月一个新版本 |
| LTS 支持 | Oracle 提供付费支持 | 各发行版厂商提供免费/付费支持 |

**对于个人学习和大多数项目，推荐使用 OpenJDK 的发行版。** 常见的 OpenJDK 发行版包括：

- **Eclipse Temurin**（原 AdoptOpenJDK）：Eclipse 基金会维护，社区最广泛，推荐首选
- **Azul Zulu**：Azul Systems 维护，提供免费的 LTS 版本
- **Amazon Corretto**：Amazon 维护，免费，广泛用于 AWS 生态
- **Microsoft OpenJDK**：微软维护，与 Azure 生态集成良好

它们的核心功能完全一致（都基于 OpenJDK 源码构建），差异主要在于商业支持和附加工具。初学者选择 **Eclipse Temurin** 即可。

本文以 **JDK 21**（当前 LTS 版本）为例进行安装讲解。

## Windows 下安装 JDK 21

### 方式一：手动安装（推荐）

1. 访问 [Adoptium 官网](https://adoptium.net/)，下载 Windows x64 的 JDK 21 安装包（`.msi` 格式）

2. 双击运行安装程序，安装过程中勾选以下选项：
   - ✅ Add to PATH（自动配置 PATH 环境变量）
   - ✅ Set JAVA_HOME variable（自动设置 JAVA_HOME）
   - ✅ JavaSoft (Oracle) registry keys（注册表项，方便工具识别）

3. 安装完成后，**重新打开**一个新的命令行窗口（PowerShell 或 CMD），验证安装：

```powershell
java -version
javac -version
```

正常输出类似以下内容即表示安装成功：

```
openjdk version "21.0.3" 2024-04-16 LTS
OpenJDK Runtime Environment Temurin-21.0.3+7 (build 21.0.3+7-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.3+7 (build 21.0.3+7-LTS, mixed mode, sharing)
```

### 方式二：使用包管理器

```powershell
# 使用 winget（Windows 10/11 自带）
winget install EclipseAdoptium.Temurin.21.JDK

# 或使用 scoop
scoop install temurin21-jdk
```

## macOS 下安装 JDK 21

### 使用 Homebrew（推荐）

```bash
# 安装 Homebrew（如果尚未安装）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 JDK 21
brew install --cask temurin@21

# 如果需要切换版本，可以安装 jenv
brew install jenv
jenv add /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
```

### 手动安装

1. 从 [Adoptium 官网](https://adoptium.net/) 下载 macOS 的 `.pkg` 安装包
2. 双击运行安装程序，按提示完成安装
3. 验证安装：

```bash
java -version
javac -version
```

## Linux 下安装 JDK 21

### Ubuntu / Debian

```bash
# 添加 Adoptium 仓库
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
sudo add-apt-repository --yes https://packages.adoptium.net/artifactory/deb/
sudo apt update

# 安装 JDK 21
sudo apt install temurin-21-jdk

# 验证
java -version
```

### CentOS / RHEL / Fedora

```bash
# 添加 Adoptium 仓库
sudo rpm --import https://packages.adoptium.net/artifactory/api/gpg/key/public
sudo dnf install -y https://packages.adoptium.net/artifactory/rpm/centos/$(rpm -E %rhel)/$(uname -m)/temurin-21-jdk-21.0.3.6-1.x86_64.rpm

# 或使用 yum
sudo yum install temurin-21-jdk
```

### 使用 SDKMAN!（推荐管理多版本）

```bash
# 安装 SDKMAN!
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 列出可用的 JDK 21 版本
sdk list java | grep "21\."

# 安装 Temurin JDK 21
sdk install java 21.0.3-tem

# 切换版本
sdk use java 21.0.3-tem
sdk default java 21.0.3-tem
```

## 环境变量配置

安装程序通常会自动配置环境变量，但了解它们的作用对于排查问题至关重要。

### JAVA_HOME

指向 JDK 的安装根目录。许多工具（Maven、Gradle、IDE、应用服务器）依赖此变量来定位 JDK。

```powershell
# Windows（PowerShell 中设置永久环境变量）
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.7-hotspot", "Machine")

# Linux / macOS（写入 ~/.bashrc 或 ~/.zshrc）
export JAVA_HOME=/usr/lib/jvm/temurin-21
```

### PATH

将 JDK 的 `bin` 目录加入系统 `PATH`，使 `java`、`javac` 等命令可在任意目录下直接执行。

```powershell
# Windows（通常安装程序已自动配置）
# 手动添加：系统属性 → 环境变量 → Path → 新建 → 添加 %JAVA_HOME%\bin

# Linux / macOS
export PATH=$JAVA_HOME/bin:$PATH
```

### CLASSPATH

指定 JVM 搜索类文件的路径。在现代 Java 开发中，**通常不需要手动设置 CLASSPATH**——Maven、Gradle 等构建工具会自动管理依赖路径。

```powershell
# 一般情况下无需设置。如果确实需要：
# Windows
set CLASSPATH=.;%JAVA_HOME%\lib

# Linux / macOS
export CLASSPATH=.:$JAVA_HOME/lib
```

> **提示**：`.` 表示当前目录。`CLASSPATH` 中的路径分隔符在 Windows 上是 `;`（分号），在 Linux/macOS 上是 `:`（冒号）。

### 验证环境变量

```powershell
# Windows PowerShell
echo $env:JAVA_HOME
echo $env:PATH

# Linux / macOS
echo $JAVA_HOME
echo $PATH
which java
```

## IDE 选择与安装

好的 IDE 能大幅提升开发效率。以下是三种主流 Java IDE 的对比：

| IDE | 优势 | 劣势 | 适用场景 |
|-----|------|------|---------|
| **IntelliJ IDEA** | 智能代码补全、重构能力强大、开箱即用 | 内存占用较大（建议 8GB+ 内存） | 推荐首选，社区版免费且功能足够 |
| **Eclipse** | 开源免费、插件生态丰富、轻量级 | UI 略显老旧、配置较繁琐 | 学习 Java 底层原理、需要特定插件时 |
| **VS Code + Extension Pack** | 轻量快速、前端开发友好、扩展生态强大 | Java 支持不如专用 IDE 深入 | 前后端混合项目、轻量级 Java 开发 |

### IntelliJ IDEA 安装

1. 访问 [JetBrains 官网](https://www.jetbrains.com/idea/)，下载 **Community Edition**（社区版，免费）
2. 安装时勾选：
   - ✅ Create Desktop Shortcut（64-bit）
   - ✅ Add "Open Folder as Project"
   - ✅ `.java` 文件关联
3. 首次启动时，选择 UI 主题、安装推荐插件、确认 JDK 路径

### VS Code + Extension Pack

1. 安装 [VS Code](https://code.visualstudio.com/)
2. 在扩展市场搜索并安装 **Extension Pack for Java**（由 Microsoft 发布），它包含以下扩展：
   - Language Support for Java（Red Hat）
   - Debugger for Java
   - Test Runner for Java
   - Maven for Java
   - Project Manager for Java
   - IntelliCode

## Maven / Gradle 构建工具简介

在实际开发中，我们不会手动编译和运行几十上百个源文件。构建工具负责项目的依赖管理、编译、测试、打包等任务。

### Maven

Apache Maven 是 Java 生态中最成熟的构建工具，采用 **POM（Project Object Model）** 配置文件管理项目。

```xml
<!-- pom.xml 示例 -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

常用命令：

```bash
mvn clean compile    # 清理并编译
mvn clean test       # 清理并运行测试
mvn clean package    # 清理、测试并打包（生成 JAR/WAR）
```

Maven 的目录结构有严格约定：

```
my-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/        # 源代码
│   │   └── resources/   # 配置文件
│   └── test/
│       ├── java/        # 测试代码
│       └── resources/   # 测试配置
└── target/              # 构建输出
```

### Gradle

Gradle 是新一代构建工具，使用 Groovy 或 Kotlin DSL 代替 XML，配置更简洁，构建速度更快（增量构建和构建缓存）。

```groovy
// build.gradle 示例
plugins {
    id 'java'
}
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}
```

**建议初学者先学习 Maven**，它在企业项目中使用更广泛，且约定大于配置的理念有助于理解 Java 项目的标准结构。

## JDK 目录结构

安装完 JDK 后，了解其目录结构有助于理解 Java 工具链的组成：

```
jdk-21/
├── bin/           # 可执行工具
│   ├── java       # JVM 启动器（运行字节码）
│   ├── javac      # Java 编译器（源码 → 字节码）
│   ├── javadoc    # API 文档生成器
│   ├── jar        # JAR 包打包工具
│   ├── jdb        # 调试器
│   ├── jshell     # 交互式编程工具（REPL）
│   ├── jmod       # 模块打包工具
│   ├── jlink      # 自定义运行时镜像工具
│   └── jdeps      # 依赖分析工具
├── conf/          # 配置文件
│   ├── security/  # 安全策略配置
│   ├── management/# JMX 管理配置
│   └── logging.properties  # 日志配置
├── lib/           # 运行时库文件
│   ├── modules    # JDK 模块的打包文件
│   └── src.zip    # JDK 源码压缩包（学习源码的好资源）
├── include/       # C/C++ 头文件（JNI 开发时使用）
├── jmods/         # JDK 模块文件（.jmod 格式，Java 9+ 模块化系统）
└── legal/         # 许可证文件
```

**重点工具说明：**

- **`bin/java`**：最常用的命令，启动 JVM 执行字节码。`java -jar app.jar` 运行打包后的应用。
- **`bin/javac`**：Java 编译器，将 `.java` 源文件编译为 `.class` 字节码文件。
- **`bin/jshell`**：Java 9 引入的 REPL（Read-Eval-Print Loop）工具，可以交互式地执行 Java 代码片段，非常适合学习和验证小段代码。
- **`lib/src.zip`**：JDK 的源码压缩包。阅读 JDK 源码是提升 Java 内功的重要途径，建议在 IDE 中关联此文件。

```powershell
# 使用 jshell 快速验证代码
jshell
# 进入 jshell 后可以直接输入 Java 语句
jshell> int a = 10, b = 20;
jshell> System.out.println(a + b);
30
jshell> /exit
```

搭建好开发环境后，你已经具备了编写和运行 Java 程序的一切条件。接下来，我们将编写第一个 Java 程序，正式开启 Java 编程之旅。
