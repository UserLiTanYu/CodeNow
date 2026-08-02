# Gradle 构建与 Groovy/Kotlin DSL

## Gradle vs Maven 对比

| 特性 | Gradle | Maven |
|------|--------|-------|
| 构建脚本 | Groovy/Kotlin DSL | XML |
| 灵活性 | 高 | 中 |
| 性能 | 增量构建、构建缓存 | 无增量构建 |
| 学习曲线 | 陡峭 | 平缓 |
| 插件生态 | 丰富 | 丰富 |
| 依赖管理 | 强大 | 标准 |
| 多项目构建 | 优秀 | 良好 |
| IDE 支持 | 良好 | 优秀 |

## build.gradle 结构

### Groovy DSL

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.7'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    runtimeOnly 'com.mysql:mysql-connector-j'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Kotlin DSL

```kotlin
// build.gradle.kts (Kotlin DSL)
plugins {
    java
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    runtimeOnly("com.mysql:mysql-connector-j")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## 依赖配置

### 依赖类型

```groovy
dependencies {
    // 编译时依赖
    implementation 'com.google.guava:guava:33.0.0-jre'
    
    // 编译时依赖（传递给消费者）
    api 'com.fasterxml.jackson.core:jackson-databind:2.16.1'
    
    // 仅编译时
    compileOnly 'org.projectlombok:lombok:1.18.30'
    
    // 运行时依赖
    runtimeOnly 'com.mysql:mysql-connector-j:8.2.0'
    
    // 注解处理器
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
    
    // 测试依赖
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

### 依赖版本管理

```groovy
// 使用平台（BOM）
dependencies {
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.4.7')
    implementation 'org.springframework.boot:spring-boot-starter-web'  // 不需要版本
}

// 使用 version catalog
// gradle/libs.versions.toml
[versions]
spring-boot = "3.4.7"
guava = "33.0.0-jre"

[libraries]
spring-boot = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }

// build.gradle.kts
dependencies {
    implementation(libs.spring.boot)
    implementation(libs.guava)
}
```

### 依赖排除

```groovy
dependencies {
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
    
    // 排除所有传递依赖
    implementation('com.example:library:1.0') {
        transitive = false
    }
}
```

### 依赖冲突解决

```groovy
configurations.all {
    // 强制使用特定版本
    resolutionStrategy {
        force 'com.google.guava:guava:33.0.0-jre'
    }
    
    // 优先使用最新版本
    resolutionStrategy {
        preferProjectModules()
        cacheDynamicVersionsFor(10, 'minutes')
        cacheChangingModulesFor(0, 'seconds')
    }
}
```

## Task 自定义

### 基本 Task

```groovy
// 定义简单任务
tasks.register('hello') {
    group = 'custom'
    description = 'Prints hello message'
    
    doLast {
        println 'Hello from Gradle!'
    }
}

// 任务依赖
tasks.register('greet') {
    dependsOn 'hello'
    
    doLast {
        println 'How are you?'
    }
}
```

### 自定义 Task 类

```groovy
// 定义 Task 类
class GreetingTask extends DefaultTask {
    @Input
    String greeting = 'Hello'
    
    @Input
    String name = 'World'
    
    @TaskAction
    void greet() {
        println "${greeting}, ${name}!"
    }
}

// 注册任务
tasks.register('greet', GreetingTask) {
    greeting = 'Hi'
    name = 'Gradle'
}
```

### Task 类型

```groovy
// 复制任务
tasks.register('copyDocs', Copy) {
    from 'src/docs'
    into 'build/docs'
    
    include '**/*.md'
    exclude '**/draft/**'
}

// 删除任务
tasks.register('cleanDocs', Delete) {
    delete 'build/docs'
}

// 执行命令
tasks.register('runApp', Exec) {
    commandLine 'java', '-jar', 'build/libs/app.jar'
    
    workingDir 'build/libs'
}

// 创建归档
tasks.register('createTar', Tar) {
    from 'dist'
    destinationDirectory = buildDir
    archiveName = 'app.tar.gz'
    compression = Compression.GZIP
}
```

### 任务规则

```groovy
// 动态创建任务
tasks.addRule("Pattern: run<Name>") { String taskName ->
    if (taskName.startsWith('run')) {
        tasks.register(taskName) {
            doLast {
                println "Running ${taskName - 'run'}"
            }
        }
    }
}

// 使用: gradle runTests
```

## 增量构建

### 声明输入输出

```groovy
tasks.register('processTemplate') {
    // 声明输入
    inputs.file('src/template.txt')
    inputs.property('version', project.version)
    
    // 声明输出
    outputs.file('build/output.txt')
    
    doLast {
        def template = file('src/template.txt').text
        def output = template.replace('{{version}}', project.version)
        file('build/output.txt').text = output
    }
}
```

### 增量任务

```groovy
abstract class ProcessFilesTask extends DefaultTask {
    @InputFiles
    abstract ConfigurableFileCollection getSourceFiles()
    
    @OutputDirectory
    abstract DirectoryProperty getOutputDir()
    
    @TaskAction
    void process() {
        // 只处理变更的文件
        sourceFiles.files.each { file ->
            def outputFile = outputDir.file(file.name).get().asFile
            // 处理文件...
        }
    }
}
```

## 构建缓存

### 本地缓存

```properties
# gradle.properties
org.gradle.caching=true
org.gradle.cache.dir=/path/to/cache
```

### 远程缓存

```kotlin
// settings.gradle.kts
buildCache {
    local {
        enabled = true
    }
    remote<HttpBuildCache> {
        url = uri("https://cache.example.com/")
        push = true
        credentials {
            username = "cache-user"
            password = findProperty("cachePassword") as String? ?: ""
        }
    }
}
```

### 缓存配置

```groovy
// build.gradle
buildCache {
    local {
        enabled = true
        removeUnusedEntriesAfterDays = 30
    }
}

// 标记任务为可缓存
tasks.register('generateReport', GenerateReportTask) {
    outputs.cacheIf { true }
}
```

## Gradle Wrapper

### 生成 Wrapper

```bash
# 生成 Wrapper
gradle wrapper

# 指定版本
gradle wrapper --gradle-version 8.5

# 更新 Wrapper
./gradlew wrapper --gradle-version 8.5
```

### Wrapper 配置

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 使用 Wrapper

```bash
# Linux/macOS
./gradlew build

# Windows
gradlew.bat build

# 常用命令
./gradlew tasks          # 查看任务
./gradlew dependencies   # 查看依赖
./gradlew build          # 构建项目
./gradlew test           # 运行测试
./gradlew clean          # 清理
./gradlew assemble       # 打包
./gradlew bootRun        # 运行 Spring Boot
```

## 多项目构建

### 项目结构

```
my-project/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/
├── core/
│   ├── build.gradle.kts
│   └── src/
└── lib/
    ├── build.gradle.kts
    └── src/
```

### settings.gradle.kts

```kotlin
// settings.gradle.kts
rootProject.name = "my-project"

include("app", "core", "lib")

// 子项目配置
project(":app").projectDir = file("app")
project(":core").projectDir = file("core")
project(":lib").projectDir = file("lib")
```

### 根 build.gradle.kts

```kotlin
// build.gradle.kts (根项目)
plugins {
    id("org.springframework.boot") version "3.4.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    apply(plugin = "java")
    
    group = "com.example"
    version = "1.0.0-SNAPSHOT"
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    repositories {
        mavenCentral()
    }
    
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

### 子项目 build.gradle.kts

```kotlin
// app/build.gradle.kts
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":lib"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
}

// core/build.gradle.kts
dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("org.slf4j:slf4j-api:2.0.11")
}

// lib/build.gradle.kts
dependencies {
    implementation(project(":core"))
    implementation("com.google.guava:guava:33.0.0-jre")
}
```

### 项目间依赖

```kotlin
// app 依赖 core 和 lib
dependencies {
    implementation(project(":core"))
    implementation(project(":lib"))
}

// core 使用 api 暴露依赖
dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
}
```

## 高级配置

### 自定义插件

```groovy
// buildSrc/src/main/groovy/com/example/GreetingPlugin.groovy
package com.example

import org.gradle.api.Plugin
import org.gradle.api.Project

class GreetingPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create('greeting', GreetingExtension)
        
        project.tasks.register('greet') {
            doLast {
                println project.extensions.greeting.message
            }
        }
    }
}

class GreetingExtension {
    String message = 'Hello from GreetingPlugin!'
}
```

```groovy
// build.gradle
plugins {
    id 'com.example.greeting'
}

greeting {
    message = 'Hello, Gradle!'
}
```

### 性能优化

```properties
# gradle.properties
# 并行执行
org.gradle.parallel=true

# 构建缓存
org.gradle.caching=true

# 配置缓存
org.gradle.configuration-cache=true

# JVM 参数
org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError

# 守护进程
org.gradle.daemon=true
org.gradle.daemon.idletimeout=10000

# 日志级别
org.gradle.logging.level=warn
```

### 发布配置

```kotlin
// build.gradle.kts
plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("My Library")
                description.set("A sample library")
                url.set("https://github.com/example/my-library")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("developer")
                        name.set("Developer Name")
                        email.set("developer@example.com")
                    }
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhUsername") as String? ?: ""
                password = findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
```

## 常用命令

```bash
# 构建
./gradlew build

# 清理
./gradlew clean

# 运行测试
./gradlew test

# 打包
./gradlew assemble

# 运行 Spring Boot
./gradlew bootRun

# 查看依赖
./gradlew dependencies

# 查看任务
./gradlew tasks

# 生成依赖报告
./gradlew htmlDependencyReport

# 构建扫描
./gradlew build --scan

# 刷新依赖
./gradlew build --refresh-dependencies
```
