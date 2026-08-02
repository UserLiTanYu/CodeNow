# WebSocket 与异步请求处理

## 同步阻塞的局限

传统的 Servlet 模型是"一个请求对应一个线程"——客户端发起请求后，线程被阻塞，直到响应返回才释放。对于短耗时请求这没问题，但在以下场景中会成为瓶颈：

- **长轮询**：客户端等待某个事件发生，线程长时间挂起
- **服务端推送**：需要主动向客户端推送数据（如实时通知）
- **高并发连接**：每个连接占一个线程，线程池很快耗尽

Spring 提供了多种异步方案来解决这些问题：Servlet 异步处理、SSE（Server-Sent Events）和 WebSocket。

## Servlet 异步处理

### DeferredResult

`DeferredResult` 允许将响应的生成推迟到另一个线程，释放 Servlet 容器线程。

```java
@RestController
public class AsyncController {

    private final Map<String, DeferredResult<String>> pendingRequests = new ConcurrentHashMap<>();

    @GetMapping("/order/{id}/status")
    public DeferredResult<String> getOrderStatus(@PathVariable String id) {
        DeferredResult<String> result = new DeferredResult<>(30000L); // 超时 30 秒
        pendingRequests.put(id, result);

        result.onCompletion(() -> pendingRequests.remove(id));
        result.onTimeout(() -> {
            result.setErrorResult("请求超时");
            pendingRequests.remove(id);
        });

        return result;
    }

    // 由其他服务或事件触发
    public void notifyOrderComplete(String orderId) {
        DeferredResult<String> result = pendingRequests.remove(orderId);
        if (result != null) {
            result.setResult("订单 " + orderId + " 已完成");
        }
    }
}
```

`DeferredResult` 的核心优势在于：**结果可以在任何线程中设置**。例如，订单状态变更可能来自消息队列消费者，而不是处理 HTTP 请求的线程。

### Callable

`Callable` 更简单——将业务逻辑提交到异步线程执行，返回值作为响应体。

```java
@PostMapping("/import")
public Callable<ImportResult> importData(@RequestParam MultipartFile file) {
    return () -> {
        // 在 Spring MVC 的异步线程中执行
        return csvParser.parse(file.getInputStream());
    };
}
```

与 `DeferredResult` 的区别：`Callable` 的执行由 Spring MVC 管理（使用 `AsyncTaskExecutor`），而 `DeferredResult` 由开发者自行控制结果的设置时机。

### WebAsyncTask

`WebAsyncTask` 是 `Callable` 的增强版，支持超时和回调。

```java
@GetMapping("/report")
public WebAsyncTask<String> generateReport() {
    Callable<String> callable = () -> {
        return reportService.generate();
    };

    WebAsyncTask<String> task = new WebAsyncTask<>(60000L, callable);

    task.onTimeout(() -> "报告生成超时，请稍后重试");
    task.onCompletion(() -> log.info("报告生成完成"));

    return task;
}
```

### 异步方案对比

| 特性 | Callable | WebAsyncTask | DeferredResult |
|------|----------|--------------|----------------|
| 结果设置 | 自动（返回值） | 自动（返回值） | 手动（setResult） |
| 超时控制 | 需配置 | 支持 | 支持 |
| 回调 | 不支持 | onTimeout/onCompletion | onTimeout/onCompletion |
| 结果来源 | 当前线程 | 当前线程 | 任意线程 |
| 适用场景 | 简单异步 | 简单异步+超时 | 跨线程/事件驱动 |

## SSE（Server-Sent Events）

SSE 是一种轻量级的服务端推送方案，基于 HTTP 协议，客户端通过 `EventSource` API 接收数据。与 WebSocket 不同，SSE 是**单向**的——只有服务端能向客户端推送。

### SseEmitter 实现

```java
@RestController
public class NotificationController {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails user) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 分钟超时
        emitters.put(user.getId(), emitter);

        emitter.onCompletion(() -> emitters.remove(user.getId()));
        emitter.onTimeout(() -> emitters.remove(user.getId()));
        emitter.onError(e -> emitters.remove(user.getId()));

        // 发送初始连接成功事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("连接成功"));
        } catch (IOException e) {
            emitters.remove(user.getId());
        }

        return emitter;
    }

    public void sendNotification(Long userId, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
```

前端使用 `EventSource` 接收：

```javascript
const eventSource = new EventSource('/notifications/subscribe');

eventSource.addEventListener('notification', (event) => {
    console.log('收到通知:', event.data);
});

eventSource.onerror = () => {
    // EventSource 会自动重连
    console.log('连接断开，正在重连...');
};
```

### SSE 的限制

- **单向通信**：只能服务端推送，客户端发送数据需要另外的 HTTP 请求
- **文本格式**：只支持文本，不支持二进制数据
- **连接数限制**：浏览器对同一域名的 SSE 连接数有限制（HTTP/1.1 下通常为 6 个）
- **无跨域问题**：需要正确配置 CORS

## WebSocket 概述

WebSocket 提供**全双工**通信——客户端和服务端可以随时互相发送数据，不需要轮询。

### 握手过程

WebSocket 连接通过 HTTP 升级建立：

```
客户端请求：
GET /chat HTTP/1.1
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13

服务端响应：
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

握手成功后，底层连接从 HTTP 切换到 WebSocket 协议，双方可以通过帧（Frame）交换数据。

### WebSocket vs SSE

| 特性 | WebSocket | SSE |
|------|-----------|-----|
| 通信方向 | 双向 | 单向（服务端→客户端） |
| 协议 | ws:// / wss:// | HTTP |
| 数据格式 | 文本 + 二进制 | 仅文本 |
| 自动重连 | 需手动实现 | 内置 |
| 复杂度 | 较高 | 低 |
| 适用场景 | 聊天、协作编辑、游戏 | 通知、数据流、进度推送 |

## Spring WebSocket 支持

### 基本配置

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler(), "/ws/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
}
```

### WebSocketHandler 实现

```java
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("用户 {} 已连接，当前在线: {}", userId, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        // 解析消息并广播
        sessions.values().forEach(s -> {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("发送消息失败", e);
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("用户 {} 已断开，当前在线: {}", userId, sessions.size());
    }

    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
}
```

## STOMP 协议

原生 WebSocket 只提供底层帧传输，没有消息语义。STOMP（Simple Text Oriented Messaging Protocol）在 WebSocket 之上提供了**消息帧格式**，支持发布/订阅模式。

### 启用 STOMP

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅地址前缀
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送地址前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 用户目标前缀（点对点）
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS(); // 降级支持
    }
}
```

### @MessageMapping

```java
@Controller
public class ChatMessageController {

    @MessageMapping("/chat.send")  // 客户端发送到 /app/chat.send
    @SendTo("/topic/messages")     // 广播到 /topic/messages
    public ChatMessage sendMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    @MessageMapping("/chat.private")
    @SendToUser("/queue/messages")  // 发送到用户私有队列
    public ChatMessage sendPrivate(ChatMessage message, Principal principal) {
        message.setSender(principal.getName());
        return message;
    }
}
```

### STOMP 消息帧格式

```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hello","sender":"Alice"}
\u0000
```

STOMP 帧由命令（SEND/SUBSCRIBE/MESSAGE 等）、头部和正文组成，以空字符结尾。

## 消息代理：Simple vs External

### SimpleBroker

`enableSimpleBroker()` 使用内存中的消息代理，适合单机部署：

```java
registry.enableSimpleBroker("/topic", "/queue");
```

**局限性**：不支持集群——消息只存在于单个 JVM 内存中，多实例部署时消息无法跨节点传递。

### External Broker（RabbitMQ）

生产环境建议使用外部消息代理：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSocketMessageBroker
public class RabbitStompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("rabbitmq-host")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

使用外部代理后，多个 Spring Boot 实例共享同一个 RabbitMQ，消息天然支持集群分发。

## 前端 WebSocket 连接

### SockJS + STOMP.js

SockJS 提供降级机制——当浏览器不支持 WebSocket 时，自动切换到长轮询或 SSE。

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const socket = new SockJS('/ws');
const stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: () => {
        console.log('STOMP 连接成功');

        // 订阅广播消息
        stompClient.subscribe('/topic/messages', (message) => {
            const msg = JSON.parse(message.body);
            appendMessage(msg);
        });

        // 订阅私有消息
        stompClient.subscribe('/user/queue/messages', (message) => {
            const msg = JSON.parse(message.body);
            appendPrivateMessage(msg);
        });
    },
    onStompError: (frame) => {
        console.error('STOMP 错误:', frame.headers['message']);
    }
});

stompClient.activate();

// 发送消息
function sendMessage(content) {
    stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({ content, sender: currentUser })
    });
}
```

## WebSocket 认证

### HandshakeInterceptor

在握手阶段提取认证信息：

```java
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token != null && jwtUtil.validate(token)) {
            String userId = jwtUtil.extractUserId(token);
            attributes.put("userId", userId);
            return true;
        }
        return false; // 拒绝握手
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
```

### ChannelInterceptor（STOMP）

STOMP 模式下，使用 `ChannelInterceptor` 拦截消息：

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor
                    .getAccessor(message, StompHeaderAccessor.class);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = accessor.getFirstNativeHeader("Authorization");
                if (token != null && jwtUtil.validate(token)) {
                    UserDetails user = userDetailsService.loadByToken(token);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                }
            }
            return message;
        }
    });
}
```

前端在 CONNECT 帧中携带认证头：

```javascript
const stompClient = new Client({
    connectHeaders: {
        Authorization: 'Bearer ' + token
    },
    // ...
});
```

## 实际应用场景

### 实时通知系统

```java
@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyUser(Long userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    public void broadcastSystemMessage(String message) {
        messagingTemplate.convertAndSend("/topic/system",
                new SystemMessage(message, LocalDateTime.now()));
    }
}
```

### 在线用户列表

```java
@Component
public class OnlineUserTracker {

    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        String userId = getUserId(event);
        onlineUsers.add(userId);
        broadcastOnlineUsers();
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String userId = getUserId(event);
        onlineUsers.remove(userId);
        broadcastOnlineUsers();
    }

    private void broadcastOnlineUsers() {
        messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
    }
}
```

前端订阅在线用户列表：

```javascript
stompClient.subscribe('/topic/online-users', (message) => {
    const users = JSON.parse(message.body);
    updateOnlineUserList(users);
});
```

### 简易聊天室

```java
@Controller
public class ChatRoomController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/send")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendToRoom(@DestinationVariable String roomId, ChatMessage message) {
        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, Principal principal) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/system",
                principal.getName() + " 加入了聊天室");
    }
}
```

### 点对点私聊

```java
@MessageMapping("/chat.private.{targetUser}")
public void sendPrivateMessage(@DestinationVariable String targetUser,
                               ChatMessage message, Principal principal) {
    message.setSender(principal.getName());
    message.setTimestamp(LocalDateTime.now());

    // 发送给目标用户
    messagingTemplate.convertAndSendToUser(
            targetUser, "/queue/private", message);

    // 同时发送给自己（确认消息已发送）
    messagingTemplate.convertAndSendToUser(
            principal.getName(), "/queue/private", message);
}
```

### 连接状态管理

```java
@Configuration
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000}); // 心跳间隔
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

心跳机制确保连接存活，客户端和服务端定期交换心跳帧。如果一段时间未收到心跳，连接会被关闭。

前端处理断线重连：

```javascript
const stompClient = new Client({
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onDisconnect: () => {
        showReconnecting();
    },
    onConnect: () => {
        hideReconnecting();
        resubscribe();
    }
});
```