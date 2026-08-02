# Linux 网络排查与性能监控

## netstat/ss 查看网络连接

### netstat 命令

netstat 是传统的网络统计工具，用于显示网络连接、路由表、接口统计等信息。

```bash
# 查看所有 TCP 连接
netstat -ant

# 查看所有监听端口
netstat -tlnp

# 查看所有 UDP 连接
netstat -anu

# 查看指定端口的连接
netstat -ant | grep :8080

# 查看连接状态统计
netstat -ant | awk '{print $6}' | sort | uniq -c | sort -rn

# 查看进程对应的连接
netstat -tlnp | grep nginx
```

### 网络连接状态说明

| 状态 | 说明 |
|------|------|
| LISTEN | 监听中 |
| ESTABLISHED | 已建立连接 |
| SYN_SENT | 已发送连接请求 |
| SYN_RECV | 已接收连接请求 |
| FIN_WAIT1 | 等待远程连接中断请求 |
| FIN_WAIT2 | 等待远程连接中断请求确认 |
| TIME_WAIT | 等待足够时间确保远程收到确认 |
| CLOSE_WAIT | 等待本地用户发起中断请求 |
| CLOSED | 连接已关闭 |

### ss 命令（推荐）

ss 是 netstat 的替代工具，速度更快，信息更详细。

```bash
# 查看所有 TCP 连接
ss -t

# 查看所有监听端口
ss -tl

# 查看所有 TCP 连接（包括进程信息）
ss -tlnp

# 查看所有 UDP 连接
ss -u

# 查看所有连接
ss -a

# 查看连接状态统计
ss -s

# 查看指定端口
ss -tl | grep :8080

# 查看与指定 IP 的连接
ss -tn dst 192.168.1.100

# 查看指定状态的连接
ss -t state established
ss -t state time-wait

# 查看连接详细信息（包括计时器）
ss -t -i

# 查看 socket 内存使用情况
ss -t -m
```

### ss 高级过滤

```bash
# 按状态过滤
ss -t state established
ss -t state time-wait
ss -t state close-wait

# 按源/目标过滤
ss -tn src :8080
ss -tn dst :3306
ss -tn src 192.168.1.0/24

# 组合过滤
ss -tn state established src :8080 dst 192.168.1.0/24

# 按进程过滤
ss -tlnp | grep nginx
ss -tlnp | grep java
```

## tcpdump 抓包

tcpdump 是强大的网络抓包工具，用于分析网络流量和排查网络问题。

### 基本用法

```bash
# 抓取所有流量
tcpdump -i eth0

# 抓取指定接口的流量
tcpdump -i lo

# 抓取指定数量的包
tcpdump -i eth0 -c 100

# 保存到文件
tcpdump -i eth0 -w capture.pcap

# 读取文件
tcpdump -r capture.pcap

# 不解析主机名和端口名
tcpdump -i eth0 -nn
```

### 过滤表达式

```bash
# 按主机过滤
tcpdump -i eth0 host 192.168.1.100
tcpdump -i eth0 src host 192.168.1.100
tcpdump -i eth0 dst host 192.168.1.100

# 按端口过滤
tcpdump -i eth0 port 80
tcpdump -i eth0 src port 8080
tcpdump -i eth0 dst port 3306
tcpdump -i eth0 portrange 8000-9000

# 按协议过滤
tcpdump -i eth0 tcp
tcpdump -i eth0 udp
tcpdump -i eth0 icmp

# 按网络过滤
tcpdump -i eth0 net 192.168.1.0/24

# 组合过滤
tcpdump -i eth0 host 192.168.1.100 and port 80
tcpdump -i eth0 src 192.168.1.100 and dst port 3306
tcpdump -i eth0 host 192.168.1.100 or host 192.168.1.101
tcpdump -i eth0 not port 22
```

### 高级选项

```bash
# 显示详细信息
tcpdump -i eth0 -v

# 显示更详细信息
tcpdump -i eth0 -vv

# 显示包内容（十六进制和 ASCII）
tcpdump -i eth0 -X

# 显示包内容（ASCII）
tcpdump -i eth0 -A

# 显示绝对序列号
tcpdump -i eth0 -S

# 显示完整包头
tcpdump -i eth0 -e

# 限制捕获长度
tcpdump -i eth0 -s 0  # 完整包

# 按时间格式显示
tcpdump -i eth0 -tttt
```

### 实用示例

```bash
# 抓取 HTTP 请求
tcpdump -i eth0 -A -s 0 'tcp port 80 and (((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)'

# 抓取 DNS 查询
tcpdump -i eth0 -nn 'udp port 53'

# 抓取 TCP SYN 包（新连接）
tcpdump -i eth0 'tcp[tcpflags] & tcp-syn != 0'

# 抓取 TCP FIN 包（关闭连接）
tcpdump -i eth0 'tcp[tcpflags] & tcp-fin != 0'

# 抓取 MySQL 查询
tcpdump -i eth0 -A -s 0 'dst port 3306' | grep -i 'select\|insert\|update\|delete'

# 分析连接问题
tcpdump -i eth0 -nn 'tcp[tcpflags] & (tcp-syn|tcp-rst) != 0'
```

## iptables/nftables 防火墙

### iptables 基本概念

iptables 使用表（table）和链（chain）来组织规则：

- **filter 表**：过滤数据包（默认表）
- **nat 表**：网络地址转换
- **mangle 表**：修改数据包
- **raw 表**：连接跟踪

### iptables 基本命令

```bash
# 查看规则
iptables -L
iptables -L -n  # 不解析域名
iptables -L -v  # 显示详细信息
iptables -L --line-numbers  # 显示行号

# 查看指定表的规则
iptables -t nat -L

# 清空规则
iptables -F  # 清空 filter 表
iptables -t nat -F  # 清空 nat 表

# 清空计数器
iptables -Z
```

### iptables 规则配置

```bash
# 允许已建立的连接
iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT

# 允许回环接口
iptables -A INPUT -i lo -j ACCEPT

# 允许 SSH
iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# 允许 HTTP/HTTPS
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 允许 ICMP（ping）
iptables -A INPUT -p icmp -j ACCEPT

# 拒绝其他入站流量
iptables -A INPUT -j DROP

# 允许所有出站流量
iptables -A OUTPUT -j ACCEPT
```

### iptables NAT 配置

```bash
# 开启 IP 转发
echo 1 > /proc/sys/net/ipv4/ip_forward

# SNAT（源地址转换）
iptables -t nat -A POSTROUTING -s 192.168.1.0/24 -o eth0 -j MASQUERADE

# DNAT（目的地址转换）
iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to-destination 192.168.1.100:8080
```

### iptables 持久化

```bash
# Ubuntu/Debian
apt-get install iptables-persistent
netfilter-persistent save

# CentOS/RHEL
service iptables save
```

### nftables（iptables 替代）

```bash
# 查看规则
nft list ruleset

# 创建表和链
nft add table inet filter
nft add chain inet filter input { type filter hook input priority 0 \; policy drop \; }

# 添加规则
nft add rule inet filter input ct state established,related accept
nft add rule inet filter input iif lo accept
nft add rule inet filter input tcp dport 22 accept
nft add rule inet filter input tcp dport 80 accept
nft add rule inet filter input tcp dport 443 accept

# 保存规则
nft list ruleset > /etc/nftables.conf

# 加载规则
nft -f /etc/nftables.conf
```

## 性能监控工具

### top 命令

```bash
# 基本使用
top

# top 交互命令：
# P - 按 CPU 使用率排序
# M - 按内存使用率排序
# T - 按时间排序
# k - 终止进程
# r - 调整优先级
# c - 显示完整命令行
# 1 - 显示每个 CPU 核心的使用率
# h - 显示帮助
```

### top 输出解读

```
top - 14:30:00 up 10 days,  3:45,  2 users,  load average: 0.50, 0.75, 1.00
Tasks: 200 total,   2 running, 198 sleeping,   0 stopped,   0 zombie
%Cpu(s):  5.0 us,  2.0 sy,  0.0 ni, 92.0 id,  0.5 wa,  0.0 hi,  0.5 si,  0.0 st
MiB Mem :  16000.0 total,   8000.0 free,   4000.0 used,   4000.0 buff/cache
MiB Swap:   4096.0 total,   4096.0 free,      0.0 used.  11000.0 avail Mem

# 字段说明：
# us - 用户空间 CPU 使用率
# sy - 内核空间 CPU 使用率
# ni - 改变过优先级的进程 CPU 使用率
# id - 空闲 CPU 使用率
# wa - 等待 I/O 的 CPU 使用率
# hi - 硬中断 CPU 使用率
# si - 软中断 CPU 使用率
# st - 虚拟机偷取的 CPU 时间
```

### htop 命令

```bash
# 安装 htop
apt-get install htop  # Ubuntu/Debian
yum install htop      # CentOS/RHEL

# 使用 htop
htop

# htop 快捷键：
# F1 - 帮助
# F2 - 设置
# F3 - 搜索进程
# F4 - 过滤进程
# F5 - 树状显示
# F6 - 排序选择
# F9 - 终止进程
# F10 - 退出
# 空格 - 标记进程
# u - 按用户过滤
# t - 树状/列表切换
```

### vmstat 命令

```bash
# 基本使用
vmstat

# 每秒采样一次，共 5 次
vmstat 1 5

# 显示活跃和非活跃内存
vmstat -a

# 以 MB 为单位显示
vmstat -S M

# 显示磁盘统计
vmstat -d

# 显示扩展统计
vmstat -w
```

### vmstat 输出解读

```
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
 r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
 1  0      0 800000 200000 400000    0    0    10    20  100  200  5  2 92  1  0

# 字段说明：
# r - 运行队列中的进程数
# b - 等待 I/O 的进程数
# swpd - 使用的虚拟内存量
# free - 空闲内存量
# buff - 用作缓冲区的内存量
# cache - 用作缓存的内存量
# si - 从磁盘交换到内存的量
# so - 从内存交换到磁盘的量
# bi - 块设备接收的量
# bo - 块设备发送的量
# in - 每秒中断数
# cs - 每秒上下文切换数
# us - 用户 CPU 时间
# sy - 系统 CPU 时间
# id - 空闲 CPU 时间
# wa - 等待 I/O 的 CPU 时间
# st - 虚拟机偷取的时间
```

### iostat 命令

```bash
# 基本使用
iostat

# 显示 CPU 和设备统计
iostat -c -d

# 每秒采样一次，共 5 次
iostat -d 1 5

# 显示扩展统计
iostat -x

# 以 MB 为单位显示
iostat -m

# 显示指定设备
iostat -d sda sdb
```

### iostat 输出解读

```
Device            r/s     w/s     rkB/s     wkB/s   rrqm/s   wrqm/s  %rrqm  %wrqm  r_await  w_await  aqu-sz  rareq-sz  wareq-sz  svctm  %util
sda              5.00   10.00    100.00    200.00     0.50     1.00   9.09   9.09    5.00    10.00    0.15     20.00     20.00   2.00   3.00

# 字段说明：
# r/s - 每秒读请求数
# w/s - 每秒写请求数
# rkB/s - 每秒读取的 KB 数
# wkB/s - 每秒写入的 KB 数
# rrqm/s - 每秒合并的读请求数
# wrqm/s - 每秒合并的写请求数
# %rrqm - 读请求合并百分比
# %wrqm - 写请求合并百分比
# r_await - 平均读等待时间（ms）
# w_await - 平均写等待时间（ms）
# aqu-sz - 平均队列长度
# rareq-sz - 平均读请求大小
# wareq-sz - 平均写请求大小
# svctm - 平均服务时间（ms）
# %util - 设备使用百分比
```

## lsof 文件句柄

lsof（List Open Files）用于查看进程打开的文件和网络连接。

```bash
# 查看所有打开的文件
lsof

# 查看指定进程打开的文件
lsof -p 1234

# 查看指定用户打开的文件
lsof -u username

# 查看指定文件被哪些进程使用
lsof /var/log/syslog

# 查看网络连接
lsof -i

# 查看指定端口的连接
lsof -i :8080
lsof -i tcp:8080
lsof -i udp:53

# 查看指定协议的连接
lsof -i tcp
lsof -i udp

# 查看指定状态的连接
lsof -i -sTCP:ESTABLISHED

# 查看指定目录下的文件
lsof +D /var/log

# 查看指定类型的文件
lsof -t /var/log/*.log

# 组合查询
lsof -i :80 -u nginx -a

# 统计打开的文件数
lsof | wc -l

# 查看文件描述符详情
ls -l /proc/1234/fd
```

### lsof 实用示例

```bash
# 查看哪个进程占用了端口
lsof -i :8080
# 输出：
# COMMAND   PID   USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
# java      1234  root   50u  IPv4 123456      0t0  TCP *:8080 (LISTEN)

# 查看进程打开的所有文件
lsof -p 1234 | head -20

# 查看进程打开的网络连接
lsof -p 1234 -i

# 查看被删除但仍被进程占用的文件
lsof | grep deleted

# 查看特定文件系统的打开文件
lsof /home
```

## strace 系统调用追踪

strace 用于追踪程序的系统调用，是排查程序问题的强大工具。

### 基本用法

```bash
# 追踪程序的所有系统调用
strace ls -l

# 追踪已运行的进程
strace -p 1234

# 保存到文件
strace -o trace.log ls -l

# 追踪子进程
strace -f ls -l

# 显示时间戳
strace -t ls -l
strace -tt ls -l  # 微秒精度
strace -T ls -l   # 显示每个调用的耗时
```

### 过滤系统调用

```bash
# 只追踪文件相关调用
strace -e trace=file ls -l

# 只追踪网络相关调用
strace -e trace=network curl http://example.com

# 只追踪进程相关调用
strace -e trace=process ls -l

# 只追踪信号
strace -e trace=signal ls -l

# 只追踪内存相关调用
strace -e trace=memory ls -l

# 排除特定调用
strace -e trace=!write ls -l

# 追踪特定系统调用
strace -e open,read,write ls -l
```

### 高级选项

```bash
# 显示调用的参数和返回值
strace -v ls -l

# 显示字符串的最大长度
strace -s 1024 ls -l

# 显示进程环境
strace -e verbose=all ls -l

# 统计系统调用
strace -c ls -l

# 追踪并统计
strace -C ls -l
```

### 实用示例

```bash
# 排查程序启动慢
strace -T -e trace=file ./slow-program

# 查看程序访问了哪些文件
strace -e trace=open,openat,access,stat,statx ./program 2>&1 | grep -v ENOENT

# 排查网络连接问题
strace -e trace=connect,sendto,recvfrom,sendmsg,recvmsg ./network-app

# 查看程序的内存分配
strace -e trace=mmap,brk,mprotect ./program

# 排查权限问题
strace -e trace=open,openat,access,stat 2>&1 | grep -i "permission denied"

# 分析程序性能
strace -c -p 1234
# 等待一段时间后 Ctrl+C 查看统计
```

## 综合排查流程

### 网络问题排查

```bash
# 1. 检查网络接口状态
ip addr show
ifconfig

# 2. 检查路由表
ip route show
route -n

# 3. 检查 DNS 解析
nslookup example.com
dig example.com

# 4. 检查端口监听
ss -tlnp | grep :8080
netstat -tlnp | grep :8080

# 5. 测试连通性
ping 192.168.1.100
telnet 192.168.1.100 8080
curl -v http://192.168.1.100:8080

# 6. 抓包分析
tcpdump -i eth0 host 192.168.1.100 and port 8080 -w debug.pcap

# 7. 检查防火墙规则
iptables -L -n
```

### 性能问题排查

```bash
# 1. 检查系统负载
uptime
top

# 2. 检查 CPU 使用率
mpstat -P ALL 1 5

# 3. 检查内存使用
free -h
vmstat 1 5

# 4. 检查磁盘 I/O
iostat -x 1 5
iotop

# 5. 检查网络 I/O
iftop
nload

# 6. 检查进程状态
ps aux | head -20
pstree -p

# 7. 检查系统日志
dmesg | tail -50
journalctl -xe
```

### 服务问题排查

```bash
# 1. 检查服务状态
systemctl status nginx

# 2. 查看服务日志
journalctl -u nginx -f
tail -f /var/log/nginx/error.log

# 3. 检查进程
ps aux | grep nginx

# 4. 检查端口
ss -tlnp | grep nginx

# 5. 检查文件句柄
lsof -p $(pgrep nginx)

# 6. 检查系统资源限制
ulimit -a

# 7. 检查配置文件
nginx -t
```
