# Maven 多模块项目与 Profile 管理

## 父子模块结构

Maven 多模块项目允许将大型项目拆分为多个模块，每个模块独立构建，同时通过父 POM 统一管理依赖和插件。

### 父 POM 结构

```xml
<!-- 父 pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>My Project</name>
    <description>Parent POM for multi-module project</description>

    <!-- 子模块声明 -->
    <modules>
        <module>my-project-common</module>
        <module>my-project-dao</module>
        <module>my-project-service</module>
        <module>my-project-web</module>
    </modules>

    <!-- 统一版本管理 -->
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        
        <!-- 依赖版本 -->
        <spring-boot.version>3.4.7</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <hutool.version>5.8.25</hutool.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <!-- 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- 项目内部模块 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-project-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-project-dao</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>my-project-service</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- 第三方依赖 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件管理 -->
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.12.1</version>
                    <configuration>
                        <source>${maven.compiler.source}</source>
                        <target>${maven.compiler.target}</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 子模块 POM 结构

**my-project-common (公共模块):**

```xml
<!-- my-project-common/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承父 POM -->
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-project-common</artifactId>
    <packaging>jar</packaging>
    <name>My Project Common</name>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>
</project>
```

**my-project-dao (数据访问层):**

```xml
<!-- my-project-dao/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-project-dao</artifactId>
    <packaging>jar</packaging>
    <name>My Project DAO</name>

    <dependencies>
        <!-- 内部模块依赖 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-project-common</artifactId>
        </dependency>

        <!-- 数据库相关 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

**my-project-service (业务逻辑层):**

```xml
<!-- my-project-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-project-service</artifactId>
    <packaging>jar</packaging>
    <name>My Project Service</name>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-project-dao</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

**my-project-web (Web 应用层):**

```xml
<!-- my-project-web/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-project</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>my-project-web</artifactId>
    <packaging>jar</packaging>
    <name>My Project Web</name>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-project-service</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## 依赖管理详解

### dependencyManagement vs dependencies

```xml
<!-- 父 POM 中的 dependencyManagement：只声明版本，不实际引入依赖 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子 POM 中引入依赖时不需要指定版本 -->
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <!-- 版本由父 POM 的 dependencyManagement 管理 -->
    </dependency>
</dependencies>
```

### scope 作用域

| Scope | 编译 | 测试 | 运行 | 打包 | 说明 |
|-------|------|------|------|------|------|
| compile | ✓ | ✓ | ✓ | ✓ | 默认值 |
| provided | ✓ | ✓ | ✗ | ✗ | 如 servlet-api |
| runtime | ✗ | ✓ | ✓ | ✓ | 如 JDBC 驱动 |
| test | ✗ | ✓ | ✗ | ✗ | 如 JUnit |
| system | ✓ | ✓ | ✗ | ✗ | 本地系统路径 |
| import | - | - | - | - | 仅用于 pom 类型 |

### 依赖排除

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 可选依赖

```xml
<!-- 在提供方标记为 optional -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <optional>true</optional>
</dependency>

<!-- 使用方需要显式引入 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

## 插件管理

### pluginManagement 配置

```xml
<build>
    <pluginManagement>
        <plugins>
            <!-- 编译器插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.12.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- 测试插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                        <include>**/*Tests.java</include>
                    </includes>
                </configuration>
            </plugin>

            <!-- 资源插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>

    <!-- 子模块实际使用的插件 -->
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <!-- 版本和配置继承自 pluginManagement -->
        </plugin>
    </plugins>
</build>
```

### 常用插件配置

**Spring Boot 插件:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.example.Application</mainClass>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**代码覆盖率插件:**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Profile 定义与激活

### Profile 定义

```xml
<profiles>
    <!-- 开发环境 -->
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <env>dev</env>
            <db.url>jdbc:mysql://localhost:3306/mydb_dev</db.url>
            <db.username>root</db.username>
            <db.password>123456</db.password>
        </properties>
    </profile>

    <!-- 测试环境 -->
    <profile>
        <id>test</id>
        <properties>
            <env>test</env>
            <db.url>jdbc:mysql://test-server:3306/mydb_test</db.url>
            <db.username>test_user</db.username>
            <db.password>${TEST_DB_PASSWORD}</db.password>
        </properties>
    </profile>

    <!-- 生产环境 -->
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
            <db.url>jdbc:mysql://prod-server:3306/mydb_prod</db.url>
            <db.username>${PROD_DB_USERNAME}</db.username>
            <db.password>${PROD_DB_PASSWORD}</db.password>
        </properties>
    </profile>

    <!-- 功能开关 -->
    <profile>
        <id>feature-new-ui</id>
        <properties>
            <feature.new-ui.enabled>true</feature.new-ui.enabled>
        </properties>
    </profile>
</profiles>
```

### Profile 激活方式

**命令行激活:**

```bash
# 激活单个 Profile
mvn clean install -P dev

# 激活多个 Profile
mvn clean install -P dev,feature-new-ui

# 禁用 Profile
mvn clean install -P !prod
```

**环境变量激活:**

```xml
<profile>
    <id>prod</id>
    <activation>
        <property>
            <name>env</name>
            <value>prod</value>
        </property>
    </activation>
    <!-- 配置 -->
</profile>
```

```bash
# 通过 -D 参数激活
mvn clean install -Denv=prod
```

**操作系统激活:**

```xml
<profile>
    <id>windows</id>
    <activation>
        <os>
            <family>windows</family>
        </os>
    </activation>
    <properties>
        <script.extension>.bat</script.extension>
    </properties>
</profile>

<profile>
    <id>unix</id>
    <activation>
        <os>
            <family>unix</family>
        </os>
    </activation>
    <properties>
        <script.extension>.sh</script.extension>
    </properties>
</profile>
```

**文件存在激活:**

```xml
<profile>
    <id>docker</id>
    <activation>
        <file>
            <exists>Dockerfile</exists>
        </file>
    </activation>
    <properties>
        <docker.build.enabled>true</docker.build.enabled>
    </properties>
</profile>
```

### Profile 资源过滤

```xml
<profiles>
    <profile>
        <id>dev</id>
        <build>
            <resources>
                <resource>
                    <directory>src/main/resources</directory>
                    <filtering>true</filtering>
                </resource>
                <resource>
                    <directory>src/main/resources-dev</directory>
                </resource>
            </resources>
        </build>
    </profile>
</profiles>
```

```yaml
# application.yml（使用占位符）
spring:
  datasource:
    url: @db.url@
    username: @db.username@
    password: @db.password@
```

## Assembly 打包

### Assembly 插件配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <descriptors>
            <descriptor>src/assembly/bin.xml</descriptor>
        </descriptors>
        <finalName>${project.artifactId}-${project.version}</finalName>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Assembly 描述符

```xml
<!-- src/assembly/bin.xml -->
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0 http://maven.apache.org/xsd/assembly-2.2.0.xsd">
    <id>bin</id>
    <formats>
        <format>tar.gz</format>
        <format>zip</format>
    </formats>

    <includeBaseDirectory>true</includeBaseDirectory>

    <fileSets>
        <!-- 配置文件 -->
        <fileSet>
            <directory>src/main/resources</directory>
            <outputDirectory>conf</outputDirectory>
        </fileSet>

        <!-- 启动脚本 -->
        <fileSet>
            <directory>src/main/scripts</directory>
            <outputDirectory>bin</outputDirectory>
            <fileMode>0755</fileMode>
        </fileSet>

        <!-- 文档 -->
        <fileSet>
            <directory>doc</directory>
            <outputDirectory>doc</outputDirectory>
        </fileSet>
    </fileSets>

    <dependencySets>
        <dependencySet>
            <outputDirectory>lib</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
        </dependencySet>
    </dependencySets>
</assembly>
```

## 仓库配置

### 私有 Nexus 仓库

```xml
<!-- settings.xml -->
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>admin</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>admin</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>

    <mirrors>
        <mirror>
            <id>nexus</id>
            <mirrorOf>central</mirrorOf>
            <url>http://nexus.example.com/repository/maven-public/</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>nexus</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>http://nexus.example.com/repository/maven-public/</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>central</id>
                    <url>http://nexus.example.com/repository/maven-public/</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>nexus</activeProfile>
    </activeProfiles>
</settings>
```

### 发布到私有仓库

```xml
<!-- pom.xml -->
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://nexus.example.com/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>http://nexus.example.com/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

```bash
# 发布到仓库
mvn clean deploy
```

## 版本管理

### versions-maven-plugin

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.16.2</version>
</plugin>
```

### 版本管理命令

```bash
# 查看当前版本
mvn versions:display-property-updates
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

# 更新版本号
mvn versions:set -DnewVersion=2.0.0

# 移除 SNAPSHOT 后缀
mvn versions:set -DremoveSnapshot

# 添加 SNAPSHOT 后缀
mvn versions:set -DnewVersion=2.0.0-SNAPSHOT

# 回滚版本修改
mvn versions:revert

# 提交版本修改
mvn versions:commit
```

### 语义化版本

```xml
<!-- 版本格式：主版本.次版本.修订号-预发布版本+构建元数据 -->
<version>1.0.0</version>              <!-- 正式版本 -->
<version>1.0.0-SNAPSHOT</version>      <!-- 开发版本 -->
<version>1.0.0-RC1</version>          <!-- 候选版本 -->
<version>1.0.0-beta.1</version>       <!-- 测试版本 -->
<version>1.0.0-alpha.1</version>      <!-- 内部版本 -->
```

## Maven Wrapper

### 安装 Maven Wrapper

```bash
# 在项目根目录执行
mvn wrapper:wrapper

# 指定 Maven 版本
mvn wrapper:wrapper -Dmaven=3.9.6
```

### 使用 Maven Wrapper

```bash
# Linux/macOS
./mvnw clean install

# Windows
mvnw.cmd clean install

# 指定 Maven 版本
./mvnw -Dmaven.version=3.9.6 clean install
```

### .mvn/wrapper 配置

```properties
# .mvn/wrapper/maven-wrapper.properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

## 多模块构建命令

```bash
# 构建整个项目
mvn clean install

# 只构建特定模块
mvn clean install -pl my-project-web

# 构建模块及其依赖
mvn clean install -pl my-project-web -am

# 构建模块及其依赖模块
mvn clean install -pl my-project-web -amd

# 跳过测试
mvn clean install -DskipTests

# 跳过测试编译
mvn clean install -Dmaven.test.skip=true

# 并行构建
mvn clean install -T 4

# 并行构建（CPU核心数）
mvn clean install -T 1C
```
