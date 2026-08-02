# 延迟队列、死信队列与消息幂等性

在分布式系统中，消息队列承担着异步解耦、流量削峰的重要职责。然而，实际业务场景远比简单的生产-消费模型复杂。延迟队列满足定时任务需求，死信队列处理异常消息，消息幂等性保障消费安全。本文将深入探讨这三个核心概念及其实现方案。

## 延迟队列的需求场景

### 典型业务场景

| 场景 | 延迟时间 | 业务逻辑 |
|------|---------|---------|
| 订单超时取消 | 30 分钟 | 未支付订单自动关闭 |
| 定时推送通知 | 可配置 | 预约提醒、活动开始通知 |
| 延迟重试 | 递增延迟 | 失败任务指数退避重试 |
| 会议提醒 | 15 分钟 | 会议开始前通知参会人 |
| 退款审核超时 | 24 小时 | 自动审核通过 |

### 延迟队列 vs 定时任务

```
┌─────────────────────────────────────────────────────────┐
│                    方案对比                              │
├─────────────────┬──────────────────┬────────────────────┤
│     维度        │    定时任务       │     延迟队列        │
├─────────────────┼──────────────────┼────────────────────┤
│  实时性         │  依赖扫描间隔     │  精准触发           │
│  资源消耗       │  全表扫描开销大   │  按需消费           │
│  扩展性         │  单机/分布式锁    │  天然支持集群       │
│  复杂度         │  简单             │  需要 MQ 支持       │
│  适用场景       │  批量处理         │  单条精准触发       │
└─────────────────┴──────────────────┴────────────────────┘
```

## RabbitMQ 延迟队列实现

### 方案一：TTL + 死信队列

通过消息 TTL 过期后进入死信队列实现延迟效果：

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Producer   │───▶│  延迟队列     │───▶│  死信队列     │
│              │    │  (TTL=30min) │    │  (消费端)     │
│  发送订单消息 │    │  无消费者     │    │  处理超时订单 │
└──────────────┘    └──────────────┘    └──────────────┘
                          │
                          │ TTL 过期后
                          ▼
                    路由到死信交换机
```

**RabbitMQ 配置类**

```java
@Configuration
public class DelayQueueConfig {

    // 延迟交换机
    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    // 延迟队列
    public static final String DELAY_QUEUE = "order.delay.queue";
    // 死信交换机
    public static final String DEAD_EXCHANGE = "order.dead.exchange";
    // 死信队列
    public static final String DEAD_QUEUE = "order.dead.queue";
    // 路由键
    public static final String DELAY_ROUTING_KEY = "order.delay";
    public static final String DEAD_ROUTING_KEY = "order.dead";

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadQueue())
            .to(deadExchange())
            .with(DEAD_ROUTING_KEY);
    }

    /**
     * 延迟队列（带 TTL 和死信配置）
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
            // 设置消息 TTL（30分钟）
            .withArgument("x-message-ttl", 30 * 60 * 1000)
            // 设置死信交换机
            .withArgument("x-dead-letter-exchange", DEAD_EXCHANGE)
            // 设置死信路由键
            .withArgument("x-dead-letter-routing-key", DEAD_ROUTING_KEY)
            .build();
    }

    /**
     * 延迟交换机
     */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /**
     * 绑定延迟队列到延迟交换机
     */
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
            .to(delayExchange())
            .with(DELAY_ROUTING_KEY);
    }
}
```

### 方案二：延迟插件

使用 `rabbitmq_delayed_message_exchange` 插件实现更灵活的延迟：

```java
@Configuration
public class DelayedPluginConfig {

    public static final String DELAYED_EXCHANGE = "order.delayed.exchange";
    public static final String DELAYED_QUEUE = "order.delayed.queue";
    public static final String DELAYED_ROUTING_KEY = "order.delayed";

    /**
     * 延迟交换机（使用插件）
     */
    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(DELAYED_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue delayedQueue() {
        return QueueBuilder.durable(DELAYED_QUEUE).build();
    }

    @Bean
    public Binding delayedBinding() {
        return BindingBuilder.bind(delayedQueue())
            .to(delayedExchange())
            .with(DELAYED_ROUTING_KEY)
            .noargs();
    }
}
```

## Spring Boot 延迟消息

### 生产者：发送延迟消息

```java
@Service
@Slf4j
public class OrderDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送延迟订单消息
     */
    public void sendDelayOrder(OrderDTO order, long delayMillis) {
        rabbitTemplate.convertAndSend(
            DelayedPluginConfig.DELAYED_EXCHANGE,
            DelayedPluginConfig.DELAYED_ROUTING_KEY,
            order,
            message -> {
                // 设置消息延迟时间
                message.getMessageProperties().setDelay((int) delayMillis);
                // 设置消息持久化
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                // 设置消息 ID
                message.getMessageProperties().setMessageId(order.getOrderId());
                return message;
            }
        );
        log.info("发送延迟订单消息: orderId={}, delay={}ms", order.getOrderId(), delayMillis);
    }

    /**
     * 发送订单超时检查消息
     */
    public void sendOrderTimeoutCheck(String orderId) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        order.setEventType("TIMEOUT_CHECK");

        // 30分钟后检查
        sendDelayOrder(order, 30 * 60 * 1000);
    }
}
```

### 消费者：处理延迟消息

```java
@Component
@Slf4j
public class OrderDelayConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = DelayedPluginConfig.DELAYED_QUEUE)
    public void handleDelayedOrder(OrderDTO order, Message message, Channel channel) throws IOException {
        log.info("收到延迟订单消息: orderId={}, eventType={}",
                 order.getOrderId(), order.getEventType());

        try {
            if ("TIMEOUT_CHECK".equals(order.getEventType())) {
                // 检查订单是否已支付
                OrderStatus status = orderService.getOrderStatus(order.getOrderId());
                if (status == OrderStatus.PENDING_PAYMENT) {
                    // 超时未支付，取消订单
                    orderService.cancelOrder(order.getOrderId(), "超时未支付自动取消");
                    log.info("订单超时已取消: orderId={}", order.getOrderId());
                } else {
                    log.info("订单已处理，跳过: orderId={}, status={}",
                             order.getOrderId(), status);
                }
            }
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理延迟订单消息失败: orderId={}", order.getOrderId(), e);
            // 拒绝并重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
```

### 动态延迟时间实现

```java
@Service
public class DynamicDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送动态延迟消息
     * 延迟时间由消息头指定
     */
    public void sendDynamicDelay(Object payload, String exchange, String routingKey,
                                  long delayMillis, Map<String, Object> headers) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            // 设置延迟时间
            message.getMessageProperties().setDelay((int) delayMillis);

            // 设置自定义头信息
            if (headers != null) {
                headers.forEach((key, value) ->
                    message.getMessageProperties().getHeaders().put(key, value)
                );
            }

            return message;
        });
    }

    /**
     * 发送递增延迟重试消息
     */
    public void sendRetryWithBackoff(Object payload, String exchange, String routingKey,
                                      int retryCount, String originalMessageId) {
        // 指数退避：1s, 2s, 4s, 8s, 16s...
        long delayMillis = (long) Math.pow(2, retryCount) * 1000;
        // 最大延迟 5 分钟
        delayMillis = Math.min(delayMillis, 5 * 60 * 1000);

        Map<String, Object> headers = Map.of(
            "x-retry-count", retryCount,
            "x-original-message-id", originalMessageId,
            "x-max-retries", 3
        );

        sendDynamicDelay(payload, exchange, routingKey, delayMillis, headers);
        log.info("发送重试消息: retryCount={}, delay={}ms", retryCount, delayMillis);
    }
}
```

## 死信队列（DLX）

### 死信产生的条件

消息成为死信的三种情况：

1. **消息被拒绝**：`basic.reject` 或 `basic.nack`，且 `requeue=false`
2. **消息过期**：消息 TTL 到期
3. **队列满**：队列达到最大长度

### 死信队列完整配置

```java
@Configuration
public class DeadLetterConfig {

    /**
     * 死信队列配置示例
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
            // 队列最大长度
            .withArgument("x-max-length", 10000)
            // 超出长度的消息进入死信
            .withArgument("x-overflow", "reject-publish")
            // 死信交换机
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            // 死信路由键
            .withArgument("x-dead-letter-routing-key", "dlx.order")
            // 队列 TTL（消息在队列中的最大存活时间）
            .withArgument("x-message-ttl", 24 * 60 * 60 * 1000)
            .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlx.order.queue").build();
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("dlx.order");
    }
}
```

### 死信消息处理器

```java
@Component
@Slf4j
public class DeadLetterHandler {

    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    @RabbitListener(queues = "dlx.order.queue")
    public void handleDeadLetter(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        String originalQueue = message.getMessageProperties().getHeaders()
            .get("x-first-death-queue").toString();
        String reason = message.getMessageProperties().getHeaders()
            .get("x-first-death-reason").toString();

        log.warn("收到死信消息: messageId={}, originalQueue={}, reason={}",
                 messageId, originalQueue, reason);

        try {
            // 解析原始消息
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            Object originalMessage = objectMapper.readValue(body, Object.class);

            // 根据死因处理
            switch (reason) {
                case "rejected":
                    log.error("消息被拒绝: {}", body);
                    alertService.sendAlert("消息处理失败", body);
                    break;
                case "expired":
                    log.warn("消息已过期: {}", body);
                    // 可以选择重新投递或记录
                    break;
                case "maxlen":
                    log.warn("队列已满，消息被丢弃: {}", body);
                    alertService.sendAlert("队列溢出", "队列 " + originalQueue + " 已满");
                    break;
                default:
                    log.warn("未知死因: reason={}, body={}", reason, body);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理死信消息失败", e);
            // 死信消息处理失败，可以写入数据库或文件
            saveFailedMessage(message, reason);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    private void saveFailedMessage(Message message, String reason) {
        // 保存到数据库，人工处理
        log.error("死信消息已保存到数据库，等待人工处理");
    }
}
```

## 消息幂等性

### 为什么消息可能重复消费

消息重复的常见原因：

```
┌─────────────────────────────────────────────────────────┐
│                  消息重复原因                             │
├─────────────────────────────────────────────────────────┤
│  1. 生产者重试：网络抖动导致发送重试                      │
│  2. MQ 重投递：消息处理超时，MQ 重新投递                  │
│  3. 消费者重试：处理失败后重新入队                        │
│  4. 网络分区：消费者确认丢失                              │
│  5. 集群切换：主从切换导致消息重复                        │
└─────────────────────────────────────────────────────────┘
```

### 幂等方案一：唯一消息 ID + 去重表

```java
@Service
@Slf4j
public class IdempotentMessageProcessor {

    private final MessageRecordRepository messageRecordRepository;

    /**
     * 幂等处理消息
     */
    @Transactional
    public boolean processIdempotent(String messageId, Runnable processor) {
        // 1. 检查消息是否已处理
        if (messageRecordRepository.existsByMessageId(messageId)) {
            log.info("消息已处理，跳过: messageId={}", messageId);
            return false;
        }

        try {
            // 2. 插入消息记录（利用唯一约束防止并发）
            MessageRecord record = new MessageRecord();
            record.setMessageId(messageId);
            record.setStatus(MessageStatus.PROCESSING);
            record.setCreateTime(LocalDateTime.now());
            messageRecordRepository.save(record);

            // 3. 执行业务逻辑
            processor.run();

            // 4. 更新状态为已完成
            record.setStatus(MessageStatus.COMPLETED);
            record.setCompleteTime(LocalDateTime.now());
            messageRecordRepository.save(record);

            return true;
        } catch (DuplicateKeyException e) {
            // 唯一约束冲突，说明有并发处理
            log.warn("消息并发处理冲突: messageId={}", messageId);
            return false;
        } catch (Exception e) {
            // 处理失败，更新状态
            log.error("消息处理失败: messageId={}", messageId, e);
            messageRecordRepository.updateStatus(messageId, MessageStatus.FAILED);
            throw e;
        }
    }
}
```

**消息记录实体**

```java
@Entity
@Table(name = "t_message_record", indexes = {
    @Index(name = "idx_message_id", columnList = "messageId", unique = true),
    @Index(name = "idx_create_time", columnList = "createTime")
})
@Data
public class MessageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(length = 500)
    private String errorMessage;

    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}

public enum MessageStatus {
    PROCESSING, COMPLETED, FAILED
}
```

**数据库表结构**

```sql
CREATE TABLE t_message_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    create_time DATETIME NOT NULL,
    complete_time DATETIME,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 幂等方案二：乐观锁

利用版本号实现乐观锁幂等：

```java
@Service
@Slf4j
public class OptimisticLockProcessor {

    private final OrderRepository orderRepository;

    /**
     * 使用乐观锁处理订单状态更新
     */
    @Transactional
    public boolean updateOrderStatus(String orderId, OrderStatus newStatus, int expectedVersion) {
        // 使用版本号进行更新
        int affected = orderRepository.updateStatusWithVersion(
            orderId, newStatus, expectedVersion, expectedVersion + 1
        );

        if (affected == 0) {
            log.warn("乐观锁冲突或订单不存在: orderId={}, expectedVersion={}",
                     orderId, expectedVersion);
            return false;
        }

        log.info("订单状态更新成功: orderId={}, newStatus={}", orderId, newStatus);
        return true;
    }
}

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.version = :newVersion " +
           "WHERE o.orderId = :orderId AND o.version = :expectedVersion")
    int updateStatusWithVersion(@Param("orderId") String orderId,
                                @Param("status") OrderStatus status,
                                @Param("expectedVersion") int expectedVersion,
                                @Param("newVersion") int newVersion);
}
```

**订单实体**

```java
@Entity
@Table(name = "t_order")
@Data
public class Order {

    @Id
    @Column(length = 32)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Version
    private Integer version;

    private BigDecimal amount;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
}

public enum OrderStatus {
    CREATED,        // 已创建
    PENDING_PAYMENT,// 待支付
    PAID,           // 已支付
    SHIPPING,       // 发货中
    COMPLETED,      // 已完成
    CANCELLED       // 已取消
}
```

### 幂等方案三：状态机

基于订单状态机实现幂等的状态流转：

```java
@Service
@Slf4j
public class OrderStateMachineProcessor {

    private final OrderRepository orderRepository;

    // 定义合法的状态转换
    private static final Map<OrderStatus, Set<OrderStatus>> STATE_TRANSITIONS = Map.of(
        OrderStatus.CREATED, Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED),
        OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
        OrderStatus.PAID, Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
        OrderStatus.SHIPPING, Set.of(OrderStatus.COMPLETED),
        OrderStatus.COMPLETED, Set.of(),
        OrderStatus.CANCELLED, Set.of()
    );

    /**
     * 使用状态机进行状态转换
     */
    @Transactional
    public boolean transition(String orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus currentStatus = order.getStatus();

        // 检查目标状态是否已是当前状态（幂等）
        if (currentStatus == targetStatus) {
            log.info("订单已是目标状态，跳过: orderId={}, status={}", orderId, targetStatus);
            return true;
        }

        // 检查状态转换是否合法
        Set<OrderStatus> allowedTransitions = STATE_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null || !allowedTransitions.contains(targetStatus)) {
            log.warn("非法状态转换: orderId={}, {} -> {}", orderId, currentStatus, targetStatus);
            throw new IllegalStateException(
                String.format("订单 %s 无法从 %s 转换到 %s", orderId, currentStatus, targetStatus)
            );
        }

        // 执行状态转换
        order.setStatus(targetStatus);
        orderRepository.save(order);
        log.info("订单状态转换成功: orderId={}, {} -> {}", orderId, currentStatus, targetStatus);

        return true;
    }
}
```

**状态机可视化**

```
┌─────────────────────────────────────────────────────────┐
│                    订单状态机                             │
│                                                         │
│  ┌─────────┐    支付     ┌─────────────┐                │
│  │ CREATED │───────────▶│PENDING_     │                │
│  └─────────┘            │PAYMENT      │                │
│       │                 └──────┬──────┘                │
│       │ 取消                   │                        │
│       ▼                       │ 支付成功                 │
│  ┌──────────┐                 ▼                        │
│  │CANCELLED │◀────────┌─────────┐     发货    ┌─────────┐│
│  └──────────┘   取消   │  PAID  │──────────▶│SHIPPING ││
│       ▲               └─────────┘           └────┬────┘│
│       │                                         │     │
│       │ 取消                                     │完成  │
│       │                                         ▼     │
│       │                                    ┌──────────┐│
│       └────────────────────────────────────│COMPLETED ││
│                                            └──────────┘│
└─────────────────────────────────────────────────────────┘
```

## 消息可靠性总结

### 消息全链路可靠性保障

```
┌─────────────────────────────────────────────────────────────────┐
│                      消息可靠性全链路                             │
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ 生产者   │───▶│   MQ     │───▶│ 消费者   │───▶│  业务库  │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│       │              │               │               │         │
│       ▼              ▼               ▼               ▼         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   │
│  │发送确认  │   │持久化存储│   │消费确认  │   │幂等消费  │   │
│  │本地事务  │   │镜像队列 │   │手动ACK   │   │去重表    │   │
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 生产者确认配置

```java
@Configuration
public class RabbitProducerConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 启用发布确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: id={}", correlationData.getId());
            } else {
                log.error("消息发送失败: id={}, cause={}", correlationData.getId(), cause);
                // 重试或记录失败消息
            }
        });

        // 启用返回确认
        template.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, replyText={}",
                      returned.getExchange(), returned.getRoutingKey(),
                      returned.getReplyText());
        });

        // 设置为强制返回
        template.setMandatory(true);

        return template;
    }
}
```

### 消费者可靠消费

```java
@Component
@Slf4j
public class ReliableConsumer {

    @RabbitListener(queues = "order.queue")
    public void handleMessage(OrderDTO order, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 1. 业务处理
            processOrder(order);

            // 2. 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("消息消费成功: orderId={}", order.getOrderId());

        } catch (BusinessException e) {
            // 业务异常，拒绝并不重试
            log.error("业务处理失败: orderId={}", order.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // 系统异常，可以重试
            log.error("系统异常，稍后重试: orderId={}", order.getOrderId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
```

### 消息可靠性检查清单

| 环节 | 配置项 | 作用 |
|------|--------|------|
| 生产端 | `publisher-confirm-type: correlated` | 确认消息到达交换机 |
| 生产端 | `publisher-returns: true` | 确认消息到达队列 |
| MQ 端 | `durable: true` | 消息持久化 |
| MQ 端 | `x-ha-policy: all` | 镜像队列（集群） |
| 消费端 | `acknowledge-mode: manual` | 手动确认 |
| 消费端 | 消息去重表 | 幂等消费 |

通过以上方案，我们构建了完整的消息可靠性保障体系：延迟队列满足定时业务需求，死信队列处理异常消息，幂等机制确保消息消费安全。
