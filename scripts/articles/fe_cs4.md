# TCP/IP 协议栈与 HTTP 深入

网络编程是后端开发的必备技能。理解 TCP/IP 协议栈的工作原理，能帮助你写出更高效的网络应用，排查复杂的网络问题。

## TCP 协议

### TCP 三次握手

建立 TCP 连接时，客户端和服务器需要进行三次握手：

```
客户端                    服务器
  |                         |
  |---- SYN (seq=x) ------->|    第1次握手：客户端发起连接
  |                         |
  |<--- SYN+ACK (seq=y,    |    第2次握手：服务器确认并发起连接
  |     ack=x+1) ----------|
  |                         |
  |---- ACK (ack=y+1) ----->|    第3次握手：客户端确认
  |                         |
  |      连接建立            |
```

**为什么是三次握手？**

如果只有两次握手，服务器无法确认客户端收到了自己的 SYN+ACK。三次握手确保双方都确认了对方的接收能力。

```java
// Java 中建立 TCP 连接
Socket socket = new Socket();
socket.connect(new InetSocketAddress("example.com", 80));

// 三次握手发生在 connect() 调用时
// 这一步完成后，连接就建立了
```

### TCP 四次挥手

断开 TCP 连接时，需要四次挥手：

```
客户端                    服务器
  |                         |
  |---- FIN (seq=u) ------->|    第1次挥手：客户端请求关闭
  |                         |
  |<--- ACK (ack=u+1) ------|    第2次挥手：服务器确认收到
  |                         |
  |     (服务器可能还有数据要发送)
  |                         |
  |<--- FIN (seq=w) --------|    第3次挥手：服务器请求关闭
  |                         |
  |---- ACK (ack=w+1) ----->|    第4次挥手：客户端确认
  |                         |
  |  TIME_WAIT (2MSL)       |
  |      连接关闭            |
```

**为什么是四次挥手？**

因为 TCP 是全双工的，关闭连接需要双方各自关闭。服务器收到 FIN 后可能还有数据要发送，所以 ACK 和 FIN 分开发送。

**TIME_WAIT 状态**：

客户端发送最后的 ACK 后，进入 TIME_WAIT 状态，等待 2MSL（Maximum Segment Lifetime）。这是为了确保：
1. 最后的 ACK 能到达服务器
2. 让旧连接的数据包在网络中消失

### TCP 流量控制

流量控制通过滑动窗口机制实现，防止发送方发送过快导致接收方缓冲区溢出：

```
发送方                      接收方
  |                           |
  |--- 发送窗口大小=10 ------->|
  |                           |
  |--- 发送 1-10 号数据 ------>|
  |                           |
  |<-- ACK=11, 窗口大小=8 ----|  确认收到，但缓冲区只能再接收8个
  |                           |
  |--- 发送 11-18 号数据 ----->|
  |                           |
  |<-- ACK=19, 窗口大小=0 ----|  缓冲区满了，停止发送
  |                           |
  |     (等待一段时间)         |
  |                           |
  |<-- ACK=19, 窗口大小=5 ----|  缓冲区有空间了
  |                           |
```

### TCP 拥塞控制

拥塞控制防止网络拥塞，主要有四个算法：

**1. 慢启动（Slow Start）**

```
拥塞窗口从 1 开始
每收到一个 ACK，窗口 +1
每个 RTT 后，窗口翻倍
直到达到慢启动阈值（ssthresh）
```

**2. 拥塞避免（Congestion Avoidance）**

```
窗口达到 ssthresh 后
每个 RTT，窗口 +1（线性增长）
直到检测到丢包
```

**3. 快重传（Fast Retransmit）**

```
收到 3 个重复的 ACK
立即重传丢失的报文段
不必等待超时
```

**4. 快恢复（Fast Recovery）**

```
ssthresh = cwnd / 2
cwnd = ssthresh + 3
进入拥塞避免阶段
```

```
窗口大小
  ^
  |              * * *
  |           *         *
  |        *               *
  |     *    慢启动    拥塞避免
  |   *
  | *
  |*
  +-----------------------------> 时间
     ssthresh
```

### TCP vs UDP

| 特性 | TCP | UDP |
|------|-----|-----|
| 连接 | 面向连接 | 无连接 |
| 可靠性 | 可靠传输 | 不可靠 |
| 顺序 | 保证顺序 | 不保证 |
| 流量控制 | 有 | 无 |
| 拥塞控制 | 有 | 无 |
| 首部开销 | 20 字节 | 8 字节 |
| 适用场景 | 文件传输、网页浏览 | 视频直播、DNS、游戏 |

## HTTP 协议

### HTTP/1.1 vs HTTP/2 vs HTTP/3

| 特性 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|----------|--------|--------|
| 传输层 | TCP | TCP | QUIC (UDP) |
| 多路复用 | 不支持 | 支持 | 支持 |
| 头部压缩 | 无 | HPACK | QPACK |
| 服务器推送 | 不支持 | 支持 | 支持 |
| 队头阻塞 | 有 | 有（TCP层） | 无 |

**HTTP/1.1 的问题**：

```
每个请求都需要一个 TCP 连接（虽然有 Keep-Alive，但本质是串行的）

请求1 → 响应1 → 请求2 → 响应2 → 请求3 → 响应3

浏览器限制：同一域名最多 6 个并发连接
```

**HTTP/2 的改进**：

```
多路复用：一个 TCP 连接上可以并行多个请求/响应

请求1 ─┐
请求2 ─┼── 一个 TCP 连接 ──┤─ 响应1
请求3 ─┘                   ├─ 响应2
                           └─ 响应3

问题：TCP 层的队头阻塞（一个包丢失会阻塞所有流）
```

**HTTP/3 的 QUIC**：

```
基于 UDP，每个流独立可靠传输
一个流的丢包不影响其他流
0-RTT 连接建立
内置 TLS 1.3
```

### HTTPS 握手流程

HTTPS = HTTP + TLS/SSL，加密通信过程：

```
客户端                              服务器
  |                                   |
  |--- ClientHello ------------------>|  支持的TLS版本、加密套件、随机数
  |                                   |
  |<-- ServerHello -------------------|  选择的TLS版本、加密套件、随机数
  |<-- Certificate -------------------|  服务器证书
  |<-- ServerKeyExchange -------------|  密钥交换参数
  |<-- ServerHelloDone ---------------|
  |                                   |
  | (验证证书)                         |
  | (生成预主密钥)                     |
  | (用服务器公钥加密预主密钥)          |
  |                                   |
  |--- ClientKeyExchange ------------>|  加密的预主密钥
  |--- ChangeCipherSpec ------------->|  切换到加密通信
  |--- Finished --------------------->|  验证握手信息
  |                                   |
  | (计算会话密钥)                     |
  |                                   |
  |<-- ChangeCipherSpec --------------|  切换到加密通信
  |<-- Finished ----------------------|  验证握手信息
  |                                   |
  | ====== 加密通信开始 ======         |
```

```java
// Java 中创建 HTTPS 连接
URL url = new URL("https://api.example.com/data");
HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
conn.setSSLSocketFactory(SSLContext.getDefault().getSocketFactory());

// 信任自定义证书
TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
KeyStore ks = KeyStore.getInstance("JKS");
ks.load(new FileInputStream("truststore.jks"), "password".toCharArray());
tmf.init(ks);
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, tmf.getTrustManagers(), null);
conn.setSSLSocketFactory(sslContext.getSocketFactory());
```

### TLS 1.3 的改进

```
1. 握手更快：1-RTT 甚至 0-RTT
2. 更安全：移除了不安全的加密算法
3. 加密更多：握手过程也加密了
```

## DNS 解析

DNS 将域名解析为 IP 地址：

```
用户输入 www.example.com
       ↓
浏览器缓存 → 系统缓存 → hosts 文件
       ↓ (未命中)
本地 DNS 服务器
       ↓ (未命中)
根 DNS 服务器 → .com 权威服务器 → example.com 权威服务器
       ↓
返回 IP 地址
```

```java
// Java 中进行 DNS 解析
InetAddress address = InetAddress.getByName("www.example.com");
System.out.println(address.getHostAddress());

// 自定义 DNS 解析器
System.setProperty("dns.server", "8.8.8.8");
```

### DNS 记录类型

| 类型 | 说明 | 示例 |
|------|------|------|
| A | 域名到 IPv4 | example.com → 93.184.216.34 |
| AAAA | 域名到 IPv6 | example.com → 2606:2800:220:1:... |
| CNAME | 域名别名 | www.example.com → example.com |
| MX | 邮件服务器 | example.com → mail.example.com |
| NS | DNS 服务器 | example.com → ns1.example.com |
| TXT | 文本记录 | SPF、DKIM 验证 |

## CDN 原理

CDN（Content Delivery Network）通过在全球部署节点服务器，将内容缓存到离用户最近的节点。

### CDN 工作流程

```
用户访问 cdn.example.com/image.jpg
       ↓
Local DNS 解析 → CDN DNS 服务器
       ↓
CDN DNS 根据用户 IP 返回最优节点 IP
       ↓
用户请求最优节点
       ↓
节点有缓存 → 直接返回
节点无缓存 → 回源站获取 → 缓存后返回
```

### CDN 的 DNS 调度

```
1. 用户发起 DNS 查询
2. Local DNS 递归查询到 CDN 的 DNS
3. CDN DNS 根据以下信息选择最优节点：
   - 用户 IP 地理位置
   - 节点负载情况
   - 网络链路质量
   - 运营商匹配
4. 返回最优节点的 IP
```

### CDN 缓存策略

```nginx
# Nginx CDN 缓存配置
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 30d;  # 缓存30天
    add_header Cache-Control "public, no-transform";
    add_header X-Cache-Status $upstream_cache_status;
}

# 缓存键
proxy_cache_key "$scheme$host$request_uri";

# 缓存清除
# 通过 API 或清除缓存的 URL 规则
```

### CDN 回源策略

```
1. 缓存过期 → 回源
2. 缓存未命中 → 回源
3. 客户端请求头要求新鲜内容 → 回源

回源方式：
- Pull：CDN 节点主动拉取
- Push：源站主动推送到 CDN
```

## 网络编程实践

### Java NIO 网络编程

```java
// Selector 多路复用
Selector selector = Selector.open();

ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.bind(new InetSocketAddress(8080));
serverChannel.configureBlocking(false);
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();
    Set<SelectionKey> keys = selector.selectedKeys();

    for (SelectionKey key : keys) {
        if (key.isAcceptable()) {
            SocketChannel client = serverChannel.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            client.read(buffer);
            // 处理请求...
        }
    }
    keys.clear();
}
```

### 连接池优化

```java
// HTTP 连接池配置
PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
cm.setMaxTotal(200);  // 最大连接数
cm.setDefaultMaxPerRoute(20);  // 每个路由最大连接数

CloseableHttpClient httpClient = HttpClients.custom()
    .setConnectionManager(cm)
    .build();

// 数据库连接池配置
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

理解 TCP/IP 协议栈和 HTTP 的工作原理，能帮助你优化网络性能、排查连接问题、设计更好的网络架构。这些知识是后端开发者的核心竞争力。
