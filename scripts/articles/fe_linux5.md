# Shell 脚本编程与自动化

## 变量

### 变量定义与使用

```bash
#!/bin/bash

# 变量定义（等号两边不能有空格）
name="John Doe"
age=30
readonly PI=3.14159  # 只读变量

# 变量使用
echo "Name: $name"
echo "Age: ${age}"  # 使用花括号明确边界
echo "Full: ${name}_backup"

# 删除变量
unset name

# 字符串操作
str="Hello World"
echo ${#str}          # 长度：11
echo ${str:0:5}       # 子串：Hello
echo ${str/World/Bash} # 替换：Hello Bash
```

### 特殊变量

```bash
#!/bin/bash

# 特殊变量
echo "脚本名称: $0"
echo "参数个数: $#"
echo "所有参数: $*"
echo "所有参数: $@"
echo "进程ID: $$"
echo "上个命令退出状态: $?"

# 参数使用
echo "第一个参数: $1"
echo "第二个参数: $2"
echo "第十个参数: ${10}"

# 参数遍历
for arg in "$@"; do
    echo "参数: $arg"
done
```

### 环境变量

```bash
# 查看所有环境变量
env
printenv

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-21
export PATH=$PATH:/usr/local/bin

# 永久设置（添加到 ~/.bashrc 或 ~/.profile）
echo 'export JAVA_HOME=/usr/lib/jvm/java-21' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc
source ~/.bashrc
```

## 条件判断

### if 语句

```bash
#!/bin/bash

# 基本 if
if [ "$1" = "hello" ]; then
    echo "Hello!"
fi

# if-else
if [ -f "$1" ]; then
    echo "文件存在: $1"
else
    echo "文件不存在: $1"
fi

# if-elif-else
if [ "$1" -gt 90 ]; then
    echo "优秀"
elif [ "$1" -gt 80 ]; then
    echo "良好"
elif [ "$1" -gt 60 ]; then
    echo "及格"
else
    echo "不及格"
fi
```

### 条件测试

```bash
# 文件测试
[ -f file ]     # 文件存在且是普通文件
[ -d dir ]      # 目录存在
[ -e path ]     # 路径存在
[ -r file ]     # 文件可读
[ -w file ]     # 文件可写
[ -x file ]     # 文件可执行
[ -s file ]     # 文件大小大于0
[ -L file ]     # 文件是符号链接

# 字符串测试
[ -z "$str" ]   # 字符串为空
[ -n "$str" ]   # 字符串非空
[ "$a" = "$b" ]  # 字符串相等
[ "$a" != "$b" ] # 字符串不等
[ "$a" \< "$b" ] # 字符串小于（需要转义）

# 数值测试
[ $a -eq $b ]   # 等于
[ $a -ne $b ]   # 不等于
[ $a -gt $b ]   # 大于
[ $a -ge $b ]   # 大于等于
[ $a -lt $b ]   # 小于
[ $a -le $b ]   # 小于等于

# 逻辑运算
[ "$a" = "1" -a "$b" = "2" ]  # AND
[ "$a" = "1" -o "$b" = "2" ]  # OR
[ ! "$a" = "1" ]               # NOT

# 双括号（支持更丰富的语法）
[[ $a -gt 10 && $b -lt 20 ]]
[[ $a =~ ^[0-9]+$ ]]  # 正则匹配
```

### case 语句

```bash
#!/bin/bash

case "$1" in
    start)
        echo "启动服务"
        ;;
    stop)
        echo "停止服务"
        ;;
    restart)
        echo "重启服务"
        ;;
    status)
        echo "查看状态"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

## 循环

### for 循环

```bash
#!/bin/bash

# 列表遍历
for fruit in apple banana orange; do
    echo "水果: $fruit"
done

# 范围遍历
for i in {1..10}; do
    echo "数字: $i"
done

# 步长遍历
for i in {0..100..5}; do
    echo "数字: $i"
done

# C 风格 for
for ((i=0; i<10; i++)); do
    echo "索引: $i"
done

# 遍历文件
for file in *.txt; do
    echo "文件: $file"
done

# 遍历命令输出
for user in $(cat /etc/passwd | cut -d: -f1); do
    echo "用户: $user"
done
```

### while 循环

```bash
#!/bin/bash

# 基本 while
count=0
while [ $count -lt 5 ]; do
    echo "计数: $count"
    count=$((count + 1))
done

# 读取文件
while IFS= read -r line; do
    echo "行: $line"
done < /etc/passwd

# 读取管道
cat /etc/passwd | while IFS=: read -r user _ uid gid _ home shell; do
    echo "$user (UID=$UID): $home"
done

# 无限循环
while true; do
    echo "运行中..."
    sleep 1
done
```

### until 循环

```bash
#!/bin/bash

# until 循环（条件为假时执行）
count=0
until [ $count -ge 5 ]; do
    echo "计数: $count"
    count=$((count + 1))
done
```

### 循环控制

```bash
#!/bin/bash

# break - 跳出循环
for i in {1..10}; do
    if [ $i -eq 5 ]; then
        break
    fi
    echo "数字: $i"
done

# continue - 跳过当前迭代
for i in {1..10}; do
    if [ $i -eq 5 ]; then
        continue
    fi
    echo "数字: $i"
done
```

## 函数定义

### 基本函数

```bash
#!/bin/bash

# 函数定义
greet() {
    echo "Hello, $1!"
}

# 调用函数
greet "World"
greet "Bash"

# 带返回值的函数
add() {
    local result=$(( $1 + $2 ))
    echo $result
    return 0
}

# 获取返回值
sum=$(add 5 3)
echo "Sum: $sum"
```

### 局部变量

```bash
#!/bin/bash

# 使用 local 声明局部变量
my_function() {
    local name="$1"
    local age="$2"
    echo "Name: $name, Age: $age"
}

my_function "Alice" 30
# name 和 age 在函数外部不可用
```

### 函数参数

```bash
#!/bin/bash

# 函数参数
process_args() {
    echo "函数名: $FUNCNAME"
    echo "参数个数: $#"
    echo "所有参数: $@"
    
    for arg in "$@"; do
        echo "参数: $arg"
    done
}

process_args "arg1" "arg2" "arg3"
```

### 递归函数

```bash
#!/bin/bash

# 阶乘
factorial() {
    if [ $1 -le 1 ]; then
        echo 1
    else
        local prev=$(factorial $(($1 - 1)))
        echo $(($1 * prev))
    fi
}

result=$(factorial 5)
echo "5! = $result"
```

## 管道与重定向

### 管道

```bash
# 基本管道
ls -l | grep ".txt"
cat /var/log/syslog | tail -100
ps aux | grep nginx | grep -v grep

# 多级管道
cat /etc/passwd | cut -d: -f1 | sort | uniq

# 管道与 xargs
find . -name "*.txt" | xargs rm
find . -name "*.txt" -print0 | xargs -0 rm  # 处理带空格的文件名
```

### 输入重定向

```bash
# 从文件读取输入
wc -l < /etc/passwd

# Here Document
cat << EOF
Hello
World
EOF

# Here String
grep "error" <<< "This is an error message"
```

### 输出重定向

```bash
# 标准输出重定向（覆盖）
echo "Hello" > output.txt

# 标准输出重定向（追加）
echo "World" >> output.txt

# 标准错误重定向
command 2> error.log

# 标准输出和标准错误都重定向
command > output.txt 2>&1
command &> output.txt

# 分别重定向
command > output.txt 2> error.log

# 丢弃输出
command > /dev/null 2>&1
```

### 文件描述符

```bash
#!/bin/bash

# 打开文件描述符
exec 3> output.txt
echo "写入到 FD 3" >&3
exec 3>&-  # 关闭 FD 3

# 读取文件描述符
exec 4< /etc/passwd
while read -r line <&4; do
    echo "$line"
done
exec 4<&-  # 关闭 FD 4
```

## 正则与 grep/sed/awk

### grep 命令

```bash
# 基本搜索
grep "pattern" file

# 忽略大小写
grep -i "pattern" file

# 递归搜索
grep -r "pattern" /path

# 显示行号
grep -n "pattern" file

# 只显示匹配的文件
grep -l "pattern" *.txt

# 显示匹配的上下文
grep -B 3 -A 3 "pattern" file  # 前3后3行

# 使用正则表达式
grep -E "^[0-9]+" file          # 扩展正则
grep -P "\d{3}-\d{4}" file      # Perl 正则

# 统计匹配行数
grep -c "pattern" file

# 反向匹配
grep -v "pattern" file
```

### sed 命令

```bash
# 替换
sed 's/old/new/' file           # 替换第一个匹配
sed 's/old/new/g' file          # 替换所有匹配
sed 's/old/new/gi' file         # 忽略大小写替换

# 删除行
sed '3d' file                   # 删除第3行
sed '3,5d' file                 # 删除3-5行
sed '/pattern/d' file           # 删除匹配行

# 插入和追加
sed '3i\新行' file              # 在第3行前插入
sed '3a\新行' file              # 在第3行后追加

# 打印特定行
sed -n '3p' file                # 打印第3行
sed -n '3,5p' file              # 打印3-5行

# 就地编辑
sed -i 's/old/new/g' file       # 直接修改文件
sed -i.bak 's/old/new/g' file   # 备份原文件

# 多个操作
sed -e 's/foo/bar/g' -e 's/baz/qux/g' file
```

### awk 命令

```bash
# 基本使用
awk '{print $1}' file           # 打印第一列
awk '{print $1, $3}' file       # 打印第一列和第三列
awk -F: '{print $1}' /etc/passwd # 指定分隔符

# 条件过滤
awk '$3 > 100' file             # 第三列大于100
awk '/pattern/' file            # 匹配行

# 内置变量
awk '{print NR, $0}' file       # 行号和行内容
awk '{print NF, $NF}' file      # 字段数和最后一个字段

# 数学运算
awk '{sum += $1} END {print sum}' file  # 求和

# 格式化输出
awk '{printf "%-20s %10d\n", $1, $2}' file

# 多个操作
awk '{
    if ($3 > 100)
        print $1, "HIGH"
    else
        print $1, "LOW"
}' file
```

## crontab 定时任务

### crontab 基本操作

```bash
# 编辑定时任务
crontab -e

# 查看定时任务
crontab -l

# 删除所有定时任务
crontab -r

# 指定用户
crontab -u username -e
```

### cron 表达式

```
# 格式：分 时 日 月 星期 命令
# * * * * * command

# 字段说明：
# 分钟：0-59
# 小时：0-23
# 日期：1-31
# 月份：1-12
# 星期：0-7（0和7都是周日）

# 示例：
0 2 * * * /backup.sh           # 每天凌晨2点
*/5 * * * * /check.sh          # 每5分钟
0 0 * * 0 /cleanup.sh          # 每周日午夜
0 9-18 * * 1-5 /work.sh        # 工作日9-18点每小时
0 0 1 * * /monthly.sh          # 每月1日午夜
```

### crontab 示例

```bash
# 每天备份数据库
0 2 * * * /usr/local/bin/backup-db.sh >> /var/log/backup.log 2>&1

# 每小时检查服务状态
0 * * * * /usr/local/bin/check-services.sh

# 每5分钟同步文件
*/5 * * * * /usr/local/bin/sync-files.sh

# 每周一清理日志
0 0 * * 1 find /var/log -name "*.log" -mtime +7 -delete

# 每月1日生成报告
0 9 1 * * /usr/local/bin/generate-report.sh

# 使用环境变量
SHELL=/bin/bash
PATH=/usr/local/bin:/usr/bin:/bin
0 2 * * * backup.sh
```

## 脚本调试

### set 命令

```bash
#!/bin/bash

# 开启调试模式
set -x  # 显示执行的命令
set -e  # 命令失败时退出
set -u  # 使用未定义变量时报错
set -o pipefail  # 管道命令失败时退出

# 组合使用
set -euo pipefail

# 关闭调试
set +x
```

### 调试技巧

```bash
#!/bin/bash

# 方法1：使用 set -x
set -x
# ... 脚本内容 ...
set +x

# 方法2：使用 bash -x 执行
# bash -x script.sh

# 方法3：使用 trap 调试
trap 'echo "行 $LINENO: $BASH_COMMAND"' ERR

# 方法4：使用 shellcheck 静态分析
# shellcheck script.sh

# 方法5：使用 bashdb 调试器
# bashdb script.sh
```

### 错误处理

```bash
#!/bin/bash

# 错误处理函数
error_handler() {
    echo "错误发生在行 $1"
    exit 1
}

# 设置错误捕获
trap 'error_handler $LINENO' ERR

# 使用 set -e
set -e

# 临时禁用 set -e
set +e
command_that_might_fail
if [ $? -ne 0 ]; then
    echo "命令失败"
fi
set -e
```

## 常用运维脚本示例

### 服务管理脚本

```bash
#!/bin/bash

# 服务管理脚本
SERVICE_NAME="myapp"
PID_FILE="/var/run/${SERVICE_NAME}.pid"
LOG_FILE="/var/log/${SERVICE_NAME}.log"

start() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$SERVICE_NAME 已经在运行"
        return 1
    fi
    
    echo "启动 $SERVICE_NAME..."
    nohup /usr/local/bin/$SERVICE_NAME >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "$SERVICE_NAME 启动成功 (PID: $!)"
}

stop() {
    if [ ! -f "$PID_FILE" ] || ! kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$SERVICE_NAME 未运行"
        return 1
    fi
    
    echo "停止 $SERVICE_NAME..."
    kill $(cat "$PID_FILE")
    rm -f "$PID_FILE"
    echo "$SERVICE_NAME 已停止"
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$SERVICE_NAME 正在运行 (PID: $(cat $PID_FILE))"
    else
        echo "$SERVICE_NAME 未运行"
    fi
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    status)  status ;;
    *)       echo "用法: $0 {start|stop|restart|status}" ;;
esac
```

### 日志清理脚本

```bash
#!/bin/bash

# 日志清理脚本
LOG_DIR="/var/log/myapp"
RETENTION_DAYS=30

# 清理过期日志
find "$LOG_DIR" -name "*.log" -type f -mtime +$RETENTION_DAYS -delete

# 压缩超过7天的日志
find "$LOG_DIR" -name "*.log" -type f -mtime +7 -exec gzip {} \;

# 删除超过90天的压缩日志
find "$LOG_DIR" -name "*.gz" -type f -mtime +90 -delete

echo "日志清理完成"
```

### 磁盘监控脚本

```bash
#!/bin/bash

# 磁盘监控脚本
THRESHOLD=80

df -H | grep -vE '^Filesystem|tmpfs|cdrom' | awk '{ print $5 " " $1 }' | while read output; do
    usage=$(echo $output | awk '{ print $1}' | cut -d'%' -f1)
    partition=$(echo $output | awk '{ print $2 }')
    
    if [ $usage -ge $THRESHOLD ]; then
        echo "警告: $partition 使用率 ${usage}%"
        # 发送告警邮件
        # echo "磁盘告警: $partition 使用率 ${usage}%" | mail -s "磁盘告警" admin@example.com
    fi
done
```

### 系统监控脚本

```bash
#!/bin/bash

# 系统监控脚本
echo "=== 系统监控报告 ==="
echo "时间: $(date)"
echo ""

echo "--- CPU 使用率 ---"
top -bn1 | head -5
echo ""

echo "--- 内存使用 ---"
free -h
echo ""

echo "--- 磁盘使用 ---"
df -h
echo ""

echo "--- 网络连接 ---"
ss -t | wc -l
echo "个 TCP 连接"
echo ""

echo "--- 负载信息 ---"
uptime
```

### 备份脚本

```bash
#!/bin/bash

# 备份脚本
BACKUP_DIR="/backup"
SOURCE_DIR="/data"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/backup_${DATE}.tar.gz"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 执行备份
tar -czf "$BACKUP_FILE" "$SOURCE_DIR"

# 检查备份是否成功
if [ $? -eq 0 ]; then
    echo "备份成功: $BACKUP_FILE"
    echo "文件大小: $(du -h $BACKUP_FILE | cut -f1)"
else
    echo "备份失败"
    exit 1
fi

# 删除30天前的备份
find "$BACKUP_DIR" -name "backup_*.tar.gz" -mtime +30 -delete
```

### 批量操作脚本

```bash
#!/bin/bash

# 批量创建用户
USERS=("alice" "bob" "charlie")

for user in "${USERS[@]}"; do
    if id "$user" &>/dev/null; then
        echo "用户 $user 已存在"
    else
        useradd -m -s /bin/bash "$user"
        echo "用户 $user 创建成功"
    fi
done

# 批量服务器操作
SERVERS=("server1" "server2" "server3")

for server in "${SERVERS[@]}"; do
    echo "在 $server 上执行命令..."
    ssh "$server" "uptime"
done
```
