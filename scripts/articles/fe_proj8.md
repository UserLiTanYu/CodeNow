# 文件上传与实时通信实现

文件上传和实时通信是现代Web应用的重要功能。大文件上传需要处理分片、断点续传、进度回调等复杂场景；实时通信则需要选择合适的技术方案。本文将深入探讨这些功能的实现细节。

## 文件上传基础

### 基础上传接口

```java
@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ApiResponse<FileUploadResult> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "folder", defaultValue = "images") String folder
    ) {
        // 文件校验
        validateFile(file);
        
        // 存储文件
        FileUploadResult result = fileStorageService.store(file, folder);
        
        return ApiResponse.success(result);
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 文件大小校验（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过10MB");
        }
        
        // 文件类型校验
        String contentType = file.getContentType();
        Set<String> allowedTypes = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
        );
        if (!allowedTypes.contains(contentType)) {
            throw new BusinessException("不支持的文件类型: " + contentType);
        }
        
        // 文件扩展名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp");
            if (!allowedExtensions.contains(extension)) {
                throw new BusinessException("不支持的文件扩展名: " + extension);
            }
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
```

### 文件存储服务

```java
@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.upload.base-path}")
    private String basePath;
    
    @Value("${app.upload.url-prefix}")
    private String urlPrefix;
    
    public FileUploadResult store(MultipartFile file, String folder) {
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            
            // 创建目录
            Path directory = Paths.get(basePath, folder);
            Files.createDirectories(directory);
            
            // 存储文件
            Path targetPath = directory.resolve(storedFilename);
            file.transferTo(targetPath.toFile());
            
            // 返回结果
            return FileUploadResult.builder()
                .filename(originalFilename)
                .storedFilename(storedFilename)
                .url(urlPrefix + "/" + folder + "/" + storedFilename)
                .size(file.getSize())
                .contentType(file.getContentType())
                .build();
                
        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
}
```

## 分片上传

### 前端分片

```typescript
// utils/chunkUpload.ts
interface ChunkUploadOptions {
  file: File;
  chunkSize?: number;
  onProgress?: (progress: number) => void;
  onChunkComplete?: (chunkIndex: number, totalChunks: number) => void;
}

interface UploadChunk {
  index: number;
  start: number;
  end: number;
  blob: Blob;
}

export async function chunkUpload(options: ChunkUploadOptions): Promise<string> {
  const { file, chunkSize = 2 * 1024 * 1024, onProgress, onChunkComplete } = options;
  
  // 1. 计算文件MD5（用于断点续传）
  const fileMd5 = await calculateFileMd5(file, onProgress);
  
  // 2. 检查已上传的分片
  const { uploadedChunks, uploadId } = await checkUploadedChunks(fileMd5);
  
  // 3. 切分文件
  const chunks = splitFileIntoChunks(file, chunkSize);
  
  // 4. 上传未完成的分片
  let uploadedCount = uploadedChunks.length;
  
  for (const chunk of chunks) {
    if (uploadedChunks.includes(chunk.index)) {
      continue; // 跳过已上传的分片
    }
    
    await uploadChunk({
      uploadId,
      fileMd5,
      chunkIndex: chunk.index,
      totalChunks: chunks.length,
      blob: chunk.blob,
      filename: file.name
    });
    
    uploadedCount++;
    const progress = Math.round((uploadedCount / chunks.length) * 100);
    onProgress?.(progress);
    onChunkComplete?.(chunk.index, chunks.length);
  }
  
  // 5. 合并分片
  const result = await mergeChunks({
    uploadId,
    fileMd5,
    filename: file.name,
    totalChunks: chunks.length
  });
  
  return result.url;
}

function splitFileIntoChunks(file: File, chunkSize: number): UploadChunk[] {
  const chunks: UploadChunk[] = [];
  const totalChunks = Math.ceil(file.size / chunkSize);
  
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    
    chunks.push({
      index: i,
      start,
      end,
      blob: file.slice(start, end)
    });
  }
  
  return chunks;
}

async function calculateFileMd5(file: File, onProgress?: (p: number) => void): Promise<string> {
  // 使用SparkMD5计算文件MD5
  const spark = new SparkMD5.ArrayBuffer();
  const chunkSize = 2 * 1024 * 1024;
  const chunks = Math.ceil(file.size / chunkSize);
  
  for (let i = 0; i < chunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    const chunk = await file.slice(start, end).arrayBuffer();
    spark.append(chunk);
    
    // 可以在这里报告MD5计算进度
    onProgress?.(Math.round(((i + 1) / chunks) * 30)); // 前30%用于MD5计算
  }
  
  return spark.end();
}
```

### 后端分片处理

```java
@RestController
@RequestMapping("/api/upload")
public class ChunkUploadController {
    
    @Autowired
    private ChunkUploadService chunkUploadService;
    
    // 检查已上传的分片
    @GetMapping("/check")
    public ApiResponse<ChunkCheckResult> checkChunks(
        @RequestParam String fileMd5
    ) {
        ChunkCheckResult result = chunkUploadService.checkUploadedChunks(fileMd5);
        return ApiResponse.success(result);
    }
    
    // 上传单个分片
    @PostMapping("/chunk")
    public ApiResponse<Void> uploadChunk(
        @RequestParam String uploadId,
        @RequestParam String fileMd5,
        @RequestParam Integer chunkIndex,
        @RequestParam Integer totalChunks,
        @RequestParam MultipartFile chunk,
        @RequestParam String filename
    ) {
        chunkUploadService.uploadChunk(
            uploadId, fileMd5, chunkIndex, totalChunks, chunk, filename
        );
        return ApiResponse.success();
    }
    
    // 合并分片
    @PostMapping("/merge")
    public ApiResponse<FileUploadResult> mergeChunks(
        @RequestBody MergeChunksRequest request
    ) {
        FileUploadResult result = chunkUploadService.mergeChunks(request);
        return ApiResponse.success(result);
    }
}

@Service
@Slf4j
public class ChunkUploadService {
    
    @Value("${app.upload.chunk-path}")
    private String chunkPath;
    
    @Value("${app.upload.base-path}")
    private String basePath;
    
    public ChunkCheckResult checkUploadedChunks(String fileMd5) {
        Path chunkDir = Paths.get(chunkPath, fileMd5);
        
        if (!Files.exists(chunkDir)) {
            return new ChunkCheckResult(null, Collections.emptyList());
        }
        
        // 生成或获取uploadId
        String uploadId = getOrCreateUploadId(fileMd5);
        
        // 获取已上传的分片列表
        List<Integer> uploadedChunks = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(chunkDir)) {
            for (Path path : stream) {
                String filename = path.getFileName().toString();
                if (filename.endsWith(".chunk")) {
                    int index = Integer.parseInt(filename.replace(".chunk", ""));
                    uploadedChunks.add(index);
                }
            }
        } catch (IOException e) {
            log.error("读取已上传分片失败", e);
        }
        
        return new ChunkCheckResult(uploadId, uploadedChunks);
    }
    
    @Transactional
    public void uploadChunk(String uploadId, String fileMd5, Integer chunkIndex, 
                           Integer totalChunks, MultipartFile chunk, String filename) {
        try {
            // 创建分片目录
            Path chunkDir = Paths.get(chunkPath, fileMd5);
            Files.createDirectories(chunkDir);
            
            // 保存分片
            Path chunkFile = chunkDir.resolve(chunkIndex + ".chunk");
            chunk.transferTo(chunkFile.toFile());
            
            log.info("分片上传成功: {}-{}/{}", fileMd5, chunkIndex, totalChunks);
            
        } catch (IOException e) {
            log.error("分片上传失败", e);
            throw new BusinessException("分片上传失败");
        }
    }
    
    @Transactional
    public FileUploadResult mergeChunks(MergeChunksRequest request) {
        String fileMd5 = request.getFileMd5();
        String filename = request.getFilename();
        int totalChunks = request.getTotalChunks();
        
        Path chunkDir = Paths.get(chunkPath, fileMd5);
        Path targetDir = Paths.get(basePath, "uploads");
        
        try {
            Files.createDirectories(targetDir);
            
            // 生成目标文件名
            String extension = getFileExtension(filename);
            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetFile = targetDir.resolve(storedFilename);
            
            // 合并分片
            try (OutputStream out = Files.newOutputStream(targetFile)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkFile = chunkDir.resolve(i + ".chunk");
                    if (!Files.exists(chunkFile)) {
                        throw new BusinessException("分片 " + i + " 不存在");
                    }
                    Files.copy(chunkFile, out);
                }
            }
            
            // 清理分片文件
            deleteDirectory(chunkDir);
            
            return FileUploadResult.builder()
                .filename(filename)
                .storedFilename(storedFilename)
                .url("/uploads/" + storedFilename)
                .size(Files.size(targetFile))
                .build();
                
        } catch (IOException e) {
            log.error("合并分片失败", e);
            throw new BusinessException("文件合并失败");
        }
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
```

## OSS直传方案

### 前端直传OSS

```typescript
// utils/ossUpload.ts
import OSS from 'ali-oss';

interface OssConfig {
  region: string;
  accessKeyId: string;
  accessKeySecret: string;
  bucket: string;
  stsToken?: string;
}

export async function uploadToOss(
  file: File,
  config: OssConfig,
  onProgress?: (progress: number) => void
): Promise<string> {
  const client = new OSS({
    region: config.region,
    accessKeyId: config.accessKeyId,
    accessKeySecret: config.accessKeySecret,
    bucket: config.bucket,
    stsToken: config.stsToken
  });
  
  // 生成存储路径
  const date = new Date();
  const path = `uploads/${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`;
  const filename = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}.${getFileExtension(file.name)}`;
  const key = `${path}/${filename}`;
  
  // 上传文件
  const result = await client.multipartUpload(key, file, {
    progress: (p, checkpoint) => {
      const progress = Math.round(p * 100);
      onProgress?.(progress);
    },
    parallel: 4,
    partSize: 1024 * 1024
  });
  
  return result.res.requestUrls[0];
}

// 获取STS临时凭证
async function getStsToken(): Promise<OssConfig> {
  const response = await fetch('/api/oss/sts-token');
  const { data } = await response.json();
  return data;
}

// 使用示例
async function handleFileUpload(file: File) {
  const config = await getStsToken();
  const url = await uploadToOss(file, config, (progress) => {
    console.log(`上传进度: ${progress}%`);
  });
  console.log('文件URL:', url);
}
```

### 后端STS凭证服务

```java
@Service
public class OssStsService {
    
    @Value("${oss.access-key-id}")
    private String accessKeyId;
    
    @Value("${oss.access-key-secret}")
    private String accessKeySecret;
    
    @Value("${oss.role-arn}")
    private String roleArn;
    
    @Value("${oss.bucket}")
    private String bucket;
    
    @Value("${oss.region}")
    private String region;
    
    public StsTokenResponse generateStsToken() {
        try {
            // 创建STS客户端
            IAcsClient client = new DefaultAcsClient(
                Region.getRegion(region),
                accessKeyId,
                accessKeySecret
            );
            
            // 构建请求
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(roleArn);
            request.setRoleSessionName("upload-session-" + System.currentTimeMillis());
            request.setDurationSeconds(3600L); // 1小时有效期
            
            // 限制权限
            String policy = "{"
                + "\"Version\":\"1\","
                + "\"Statement\":[{"
                + "\"Effect\":\"Allow\","
                + "\"Action\":["
                + "\"oss:PutObject\","
                + "\"oss:PutObjectAcl\""
                + "],"
                + "\"Resource\":["
                + "\"acs:oss:*:*:" + bucket + "/uploads/*\""
                + "]"
                + "}]"
                + "}";
            request.setPolicy(policy);
            
            // 获取凭证
            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();
            
            return StsTokenResponse.builder()
                .region(region)
                .bucket(bucket)
                .accessKeyId(credentials.getAccessKeyId())
                .accessKeySecret(credentials.getAccessKeySecret())
                .stsToken(credentials.getSecurityToken())
                .expiration(credentials.getExpiration())
                .build();
                
        } catch (Exception e) {
            log.error("获取STS凭证失败", e);
            throw new BusinessException("获取上传凭证失败");
        }
    }
}
```

## WebSocket实时通知

### WebSocket配置

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}

// 认证拦截器
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (tokenProvider.validateToken(token)) {
                    UserDetails userDetails = tokenProvider.getUserDetails(token);
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(authentication);
                }
            }
        }
        
        return message;
    }
}
```

### 消息服务

```java
@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // 发送给指定用户
    public void sendToUser(Long userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }
    
    // 广播给所有用户
    public void broadcast(Notification notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
    
    // 发送给特定主题的订阅者
    public void sendToTopic(String topic, Object message) {
        messagingTemplate.convertAndSend("/topic/" + topic, message);
    }
    
    // 文章发布通知
    @EventListener
    public void handleArticlePublished(ArticlePublishedEvent event) {
        Notification notification = Notification.builder()
            .type(NotificationType.ARTICLE_PUBLISHED)
            .title("新文章发布")
            .content("您关注的作者发布了新文章: " + event.getTitle())
            .link("/articles/" + event.getArticleId())
            .createdAt(LocalDateTime.now())
            .build();
        
        // 通知所有关注者
        List<Long> followerIds = userService.getFollowerIds(event.getAuthorId());
        followerIds.forEach(userId -> sendToUser(userId, notification));
    }
    
    // 评论通知
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        Notification notification = Notification.builder()
            .type(NotificationType.NEW_COMMENT)
            .title("收到新评论")
            .content("用户 " + event.getCommenterName() + " 评论了您的文章: " 
                + event.getArticleTitle())
            .link("/articles/" + event.getArticleId() + "#comment-" + event.getCommentId())
            .createdAt(LocalDateTime.now())
            .build();
        
        sendToUser(event.getAuthorId(), notification);
    }
}
```

### 前端WebSocket客户端

```typescript
// utils/websocket.ts
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketClient {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  
  connect(token: string): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      onConnect: () => {
        console.log('WebSocket连接成功');
        this.reconnectAttempts = 0;
        this.resubscribeAll();
      },
      onDisconnect: () => {
        console.log('WebSocket断开连接');
      },
      onStompError: (frame) => {
        console.error('WebSocket错误:', frame.headers['message']);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });
    
    this.client.activate();
  }
  
  disconnect(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions.clear();
    this.client?.deactivate();
  }
  
  subscribe(destination: string, callback: (message: IMessage) => void): void {
    if (!this.client?.connected) {
      console.warn('WebSocket未连接，等待连接后订阅');
      return;
    }
    
    const subscription = this.client.subscribe(destination, callback);
    this.subscriptions.set(destination, subscription);
  }
  
  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }
  
  send(destination: string, body: any): void {
    if (!this.client?.connected) {
      console.error('WebSocket未连接');
      return;
    }
    
    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
  }
  
  private resubscribeAll(): void {
    // 重新订阅之前的主题
    this.subscriptions.forEach((_, destination) => {
      // 需要保存callback才能重新订阅
    });
  }
}

export const wsClient = new WebSocketClient();

// 使用示例
import { wsClient } from '@/utils/websocket';
import { useUserStore } from '@/stores/modules/user';

export function useNotifications() {
  const userStore = useUserStore();
  
  function connect() {
    wsClient.connect(userStore.token);
    
    // 订阅个人通知
    wsClient.subscribe('/user/queue/notifications', (message) => {
      const notification = JSON.parse(message.body);
      handleNotification(notification);
    });
    
    // 订阅全局通知
    wsClient.subscribe('/topic/notifications', (message) => {
      const notification = JSON.parse(message.body);
      handleNotification(notification);
    });
  }
  
  function handleNotification(notification: any) {
    // 显示通知
    ElNotification({
      title: notification.title,
      message: notification.content,
      type: 'info',
      onClick: () => {
        if (notification.link) {
          router.push(notification.link);
        }
      }
    });
    
    // 更新通知列表
    useNotificationStore().addNotification(notification);
  }
  
  return { connect };
}
```

## SSE服务端推送

### 后端SSE实现

```java
@RestController
@RequestMapping("/api/sse")
public class SseController {
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestHeader("Authorization") String token) {
        // 验证token获取用户ID
        Long userId = validateToken(token);
        String emitterId = userId + "_" + System.currentTimeMillis();
        
        SseEmitter emitter = new SseEmitter(3600000L); // 1小时超时
        
        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError(e -> emitters.remove(emitterId));
        
        emitters.put(emitterId, emitter);
        
        // 发送初始连接成功消息
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("连接成功"));
        } catch (IOException e) {
            emitters.remove(emitterId);
        }
        
        return emitter;
    }
    
    // 发送通知给指定用户
    public void sendToUser(Long userId, String eventName, Object data) {
        emitters.forEach((id, emitter) -> {
            if (id.startsWith(userId + "_")) {
                try {
                    emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                } catch (IOException e) {
                    emitters.remove(id);
                }
            }
        });
    }
    
    // 广播
    public void broadcast(String eventName, Object data) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            } catch (IOException e) {
                emitters.remove(id);
            }
        });
    }
}
```

### 前端SSE客户端

```typescript
// utils/sse.ts
export class SSEClient {
  private eventSource: EventSource | null = null;
  private listeners: Map<string, Set<Function>> = new Map();
  
  connect(url: string, token: string): void {
    this.eventSource = new EventSource(`${url}?token=${token}`);
    
    this.eventSource.onopen = () => {
      console.log('SSE连接成功');
    };
    
    this.eventSource.onerror = (event) => {
      console.error('SSE连接错误:', event);
      this.reconnect(url, token);
    };
    
    // 监听默认消息
    this.eventSource.onmessage = (event) => {
      this.emit('message', JSON.parse(event.data));
    };
  }
  
  on(eventName: string, callback: Function): void {
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, new Set());
    }
    this.listeners.get(eventName)!.add(callback);
    
    // 如果EventSource已连接，添加事件监听
    if (this.eventSource) {
      this.eventSource.addEventListener(eventName, (event: any) => {
        callback(JSON.parse(event.data));
      });
    }
  }
  
  off(eventName: string, callback: Function): void {
    this.listeners.get(eventName)?.delete(callback);
  }
  
  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
    this.listeners.clear();
  }
  
  private emit(eventName: string, data: any): void {
    this.listeners.get(eventName)?.forEach(callback => callback(data));
  }
  
  private reconnect(url: string, token: string): void {
    setTimeout(() => {
      console.log('尝试重新连接SSE...');
      this.connect(url, token);
    }, 5000);
  }
}

// 使用示例
const sse = new SSEClient();

sse.connect('/api/sse/subscribe', userStore.token);

sse.on('newArticle', (data) => {
  ElNotification({
    title: '新文章',
    message: `发布了新文章: ${data.title}`,
    type: 'info'
  });
});

sse.on('comment', (data) => {
  ElNotification({
    title: '新评论',
    message: `${data.commenter} 评论了您的文章`,
    type: 'info'
  });
});
```

## 长轮询降级

当WebSocket和SSE都不可用时，使用长轮询作为降级方案：

```typescript
// utils/longPolling.ts
export class LongPollingClient {
  private polling = false;
  private interval: number;
  private url: string;
  private callback: (data: any) => void;
  private lastTimestamp: number = 0;
  
  constructor(config: { url: string; interval?: number; callback: (data: any) => void }) {
    this.url = config.url;
    this.interval = config.interval || 5000;
    this.callback = config.callback;
  }
  
  start(): void {
    if (this.polling) return;
    
    this.polling = true;
    this.poll();
  }
  
  stop(): void {
    this.polling = false;
  }
  
  private async poll(): Promise<void> {
    if (!this.polling) return;
    
    try {
      const response = await fetch(`${this.url}?since=${this.lastTimestamp}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        if (data.items && data.items.length > 0) {
          data.items.forEach((item: any) => this.callback(item));
          this.lastTimestamp = data.timestamp;
        }
      }
    } catch (error) {
      console.error('长轮询错误:', error);
    }
    
    // 继续轮询
    if (this.polling) {
      setTimeout(() => this.poll(), this.interval);
    }
  }
}

// 自动降级
export function createRealtimeClient() {
  // 优先使用WebSocket
  if (typeof WebSocket !== 'undefined') {
    return new WebSocketClient();
  }
  
  // 其次使用SSE
  if (typeof EventSource !== 'undefined') {
    return new SSEClient();
  }
  
  // 降级到长轮询
  return new LongPollingClient({
    url: '/api/notifications/poll',
    interval: 5000,
    callback: (data) => {
      // 处理通知
    }
  });
}
```

文件上传和实时通信是现代Web应用的核心功能。选择合适的技术方案，处理好各种边界情况和异常场景，才能提供稳定可靠的用户体验。
