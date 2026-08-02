# 后端缓存策略与性能优化

缓存是提升系统性能最有效的手段之一。合理的缓存策略可以将响应时间从几百毫秒降低到几毫秒，同时大幅减轻数据库压力。本文将介绍多级缓存架构的设计、Spring Boot缓存注解的使用、缓存一致性方案以及热点数据处理策略。

## 多级缓存架构

### 缓存层次设计

```
┌─────────────────────────────────────────────────────────────┐
│                        请求流程                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│  │  客户端   │───▶│ 本地缓存  │───▶│ Redis   │───▶│  数据库   │ │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│                      │              │              │        │
│                    命中率60%      命中率30%      命中率10%    │
│                   <1ms响应       <5ms响应      <50ms响应     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Caffeine本地缓存

Caffeine是Java中性能最好的本地缓存库，适用于热点数据和变更频率低的数据：

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()
        );
        return cacheManager;
    }
    
    @Bean
    public CaffeineCacheManager articleCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("articles");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .expireAfterAccess(Duration.ofMinutes(2))
        );
        return cacheManager;
    }
}

// 缓存统计端点
@RestController
@RequestMapping("/api/admin/cache")
public class CacheStatsController {
    
    @Autowired
    private CacheManager cacheManager;
    
    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache instanceof CaffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    ((CaffeineCache) cache).getNativeCache();
                stats.put(name, Map.of(
                    "size", nativeCache.estimatedSize(),
                    "hitRate", nativeCache.stats().hitRate(),
                    "missRate", nativeCache.stats().missRate()
                ));
            }
        });
        
        return stats;
    }
}
```

### Redis分布式缓存

Redis适用于需要跨实例共享的缓存数据：

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // 使用Jackson序列化
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        
        return template;
    }
    
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("articles", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("categories", config.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("tags", config.entryTtl(Duration.ofHours(4)));
        cacheConfigs.put("hotArticles", config.entryTtl(Duration.ofMinutes(10)));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigs)
            .transactionAware()
            .build();
    }
}
```

## Spring Boot缓存注解

### @Cacheable

用于查询方法，结果会自动缓存：

```java
@Service
public class ArticleService {
    
    // 基本用法
    @Cacheable(value = "articles", key = "#id")
    public ArticleDTO findById(Long id) {
        return articleRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
    }
    
    // 复杂key
    @Cacheable(value = "articles", key = "#authorId + ':' + #page")
    public PageResult<ArticleDTO> findByAuthor(Long authorId, int page) {
        // 查询逻辑
    }
    
    // 条件缓存
    @Cacheable(value = "articles", key = "#id", unless = "#result == null")
    public ArticleDTO findByIdOrNull(Long id) {
        return articleRepository.findById(id).map(this::toDTO).orElse(null);
    }
    
    // 同步缓存（防止缓存击穿）
    @Cacheable(value = "articles", key = "#id", sync = true)
    public ArticleDTO findByIdSync(Long id) {
        return articleRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
    }
}
```

### @CacheEvict

用于删除缓存：

```java
@Service
public class ArticleService {
    
    // 删除单条缓存
    @CacheEvict(value = "articles", key = "#id")
    public void deleteById(Long id) {
        articleRepository.deleteById(id);
    }
    
    // 删除多条缓存
    @Caching(evict = {
        @CacheEvict(value = "articles", key = "#article.id"),
        @CacheEvict(value = "articleLists", allEntries = true)
    })
    public ArticleDTO update(ArticleDTO article) {
        // 更新逻辑
    }
    
    // 清空整个缓存
    @CacheEvict(value = "articles", allEntries = true)
    public void clearArticleCache() {
        // 清空缓存
    }
}
```

### @CachePut

用于更新缓存（同时更新数据库和缓存）：

```java
@Service
public class ArticleService {
    
    @CachePut(value = "articles", key = "#result.id")
    public ArticleDTO save(ArticleDTO article) {
        Article entity = toEntity(article);
        Article saved = articleRepository.save(entity);
        return toDTO(saved);
    }
}
```

### @Caching

组合多个缓存操作：

```java
@Service
public class ArticleService {
    
    @Caching(
        cacheable = {
            @Cacheable(value = "articles", key = "#id", unless = "#result == null")
        },
        put = {
            @CachePut(value = "recentArticles", key = "#id", unless = "#result == null")
        }
    )
    public ArticleDTO findById(Long id) {
        return articleRepository.findById(id).map(this::toDTO).orElse(null);
    }
}
```

## 缓存一致性方案

### Cache Aside Pattern

最常见的缓存模式，应用层负责维护缓存一致性：

```java
@Service
@Transactional
public class ArticleService {
    
    // 读：先读缓存，未命中则读数据库并写入缓存
    @Override
    public ArticleDTO findById(Long id) {
        // 1. 先读缓存
        String cacheKey = "articles:" + id;
        ArticleDTO cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 2. 缓存未命中，读数据库
        ArticleDTO article = articleRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        
        // 3. 写入缓存，设置过期时间
        redisTemplate.opsForValue().set(cacheKey, article, Duration.ofMinutes(30));
        
        return article;
    }
    
    // 写：先更新数据库，再删除缓存
    @Override
    public ArticleDTO update(Long id, ArticleUpdateRequest request) {
        // 1. 更新数据库
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        Article saved = articleRepository.save(article);
        
        // 2. 删除缓存（而不是更新缓存）
        String cacheKey = "articles:" + id;
        redisTemplate.delete(cacheKey);
        
        // 3. 延迟双删（防止并发问题）
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                redisTemplate.delete(cacheKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        return toDTO(saved);
    }
}
```

### Read Through / Write Through

缓存层代理数据库操作，对应用层透明：

```java
@Component
public class ArticleCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    // Read Through：缓存层自动加载
    public ArticleDTO findById(Long id) {
        String key = "articles:" + id;
        
        // 使用Redis的原子操作
        return (ArticleDTO) redisTemplate.execute(new SessionCallback<ArticleDTO>() {
            @Override
            public ArticleDTO execute(RedisOperations operations) {
                // 1. 读缓存
                ArticleDTO cached = (ArticleDTO) operations.opsForValue().get(key);
                if (cached != null) {
                    return cached;
                }
                
                // 2. 加分布式锁，防止缓存击穿
                String lockKey = "lock:articles:" + id;
                Boolean locked = operations.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
                
                if (Boolean.TRUE.equals(locked)) {
                    try {
                        // 3. 双重检查
                        cached = (ArticleDTO) operations.opsForValue().get(key);
                        if (cached != null) {
                            return cached;
                        }
                        
                        // 4. 从数据库加载
                        ArticleDTO article = articleRepository.findById(id)
                            .map(this::toDTO)
                            .orElse(null);
                        
                        // 5. 写入缓存
                        if (article != null) {
                            operations.opsForValue().set(key, article, Duration.ofMinutes(30));
                        }
                        
                        return article;
                    } finally {
                        operations.delete(lockKey);
                    }
                }
                
                // 6. 未获取锁，等待后重试
                try {
                    Thread.sleep(100);
                    return findById(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        });
    }
}
```

### Write Behind（异步写入）

适用于写多读少的场景，写入先进入缓存，异步批量写入数据库：

```java
@Component
@Slf4j
public class ArticleWriteBehindService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    private final BlockingQueue<Article> writeQueue = new LinkedBlockingQueue<>(1000);
    
    @PostConstruct
    public void init() {
        // 启动异步写入线程
        Thread writeThread = new Thread(this::processWriteQueue);
        writeThread.setDaemon(true);
        writeThread.start();
    }
    
    // 写入操作先进入队列
    public void saveAsync(Article article) {
        String cacheKey = "articles:" + article.getId();
        redisTemplate.opsForValue().set(cacheKey, toDTO(article), Duration.ofHours(1));
        
        if (!writeQueue.offer(article)) {
            log.warn("写入队列已满，同步写入数据库");
            articleRepository.save(article);
        }
    }
    
    // 异步批量写入
    private void processWriteQueue() {
        while (true) {
            try {
                List<Article> batch = new ArrayList<>();
                // 等待1秒或收集到100条
                Article first = writeQueue.poll(1, TimeUnit.SECONDS);
                if (first != null) {
                    batch.add(first);
                    writeQueue.drainTo(batch, 99);
                    
                    articleRepository.saveAll(batch);
                    log.info("批量写入{}条文章", batch.size());
                }
            } catch (Exception e) {
                log.error("异步写入异常", e);
            }
        }
    }
}
```

## 热点数据处理

### 缓存击穿防护

热点key过期时，大量请求同时穿透到数据库：

```java
@Service
public class HotArticleService {
    
    private final LoadingCache<Long, ArticleDTO> localCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build(id -> loadFromRedis(id));
    
    // 使用本地缓存+分布式锁双重防护
    public ArticleDTO getHotArticle(Long id) {
        try {
            return localCache.get(id);
        } catch (Exception e) {
            log.error("获取热门文章异常", e);
            return loadFromDatabase(id);
        }
    }
    
    private ArticleDTO loadFromRedis(Long id) {
        String key = "hot_articles:" + id;
        String lockKey = "lock:" + key;
        
        // 尝试获取分布式锁
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                ArticleDTO cached = (ArticleDTO) redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return cached;
                }
                
                // 从数据库加载
                ArticleDTO article = loadFromDatabase(id);
                
                // 写入Redis
                redisTemplate.opsForValue().set(key, article, Duration.ofMinutes(10));
                
                return article;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }
        
        // 未获取锁，短暂等待后重试
        try {
            Thread.sleep(50);
            ArticleDTO cached = (ArticleDTO) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return loadFromDatabase(id);
    }
}
```

### 缓存雪崩防护

大量key同时过期导致请求全部打到数据库：

```java
@Component
public class CacheRefreshScheduler {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ArticleRepository articleRepository;
    
    // 定时刷新热点数据缓存
    @Scheduled(fixedRate = 300000) // 5分钟
    public void refreshHotArticleCache() {
        log.info("开始刷新热门文章缓存");
        
        // 获取热门文章ID列表
        List<Long> hotArticleIds = getHotArticleIds();
        
        hotArticleIds.parallelStream().forEach(id -> {
            try {
                String key = "articles:" + id;
                ArticleDTO article = articleRepository.findById(id)
                    .map(this::toDTO)
                    .orElse(null);
                
                if (article != null) {
                    // 随机过期时间，防止同时过期
                    int randomMinutes = ThreadLocalRandom.current().nextInt(25, 35);
                    redisTemplate.opsForValue().set(key, article, 
                        Duration.ofMinutes(randomMinutes));
                }
            } catch (Exception e) {
                log.error("刷新文章缓存失败: {}", id, e);
            }
        });
        
        log.info("热门文章缓存刷新完成，共{}篇", hotArticleIds.size());
    }
}
```

### 缓存穿透防护

查询不存在的数据，缓存永远无法命中：

```java
@Service
public class ArticleServiceWithPenetrationProtection {
    
    // 方案1：缓存空值
    public ArticleDTO findById(Long id) {
        String key = "articles:" + id;
        String cached = redisTemplate.opsForValue().get(key);
        
        if ("NULL".equals(cached)) {
            return null;
        }
        
        if (cached != null) {
            return JSON.parseObject(cached, ArticleDTO.class);
        }
        
        ArticleDTO article = articleRepository.findById(id)
            .map(this::toDTO)
            .orElse(null);
        
        if (article != null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(article), 
                Duration.ofMinutes(30));
        } else {
            // 缓存空值，设置较短过期时间
            redisTemplate.opsForValue().set(key, "NULL", Duration.ofMinutes(5));
        }
        
        return article;
    }
    
    // 方案2：布隆过滤器
    @Autowired
    private BloomFilter<Long> articleBloomFilter;
    
    public ArticleDTO findByIdWithBloomFilter(Long id) {
        // 先检查布隆过滤器
        if (!articleBloomFilter.mightContain(id)) {
            return null; // 一定不存在
        }
        
        // 布隆过滤器说可能存在，再查缓存和数据库
        return findById(id);
    }
}
```

### 缓存监控

```java
@RestController
@RequestMapping("/api/admin/cache")
public class CacheMonitorController {
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    
    @GetMapping("/info")
    public Map<String, Object> getCacheInfo() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        Properties info = connection.info();
        
        Map<String, Object> result = new HashMap<>();
        result.put("usedMemory", info.getProperty("used_memory_human"));
        result.put("connectedClients", info.getProperty("connected_clients"));
        result.put("keyspaceHits", info.getProperty("keyspace_hits"));
        result.put("keyspaceMisses", info.getProperty("keyspace_misses"));
        
        // 计算命中率
        long hits = Long.parseLong(info.getProperty("keyspace_hits"));
        long misses = Long.parseLong(info.getProperty("keyspace_misses"));
        double hitRate = hits * 100.0 / (hits + misses);
        result.put("hitRate", String.format("%.2f%%", hitRate));
        
        return result;
    }
    
    @GetMapping("/keys")
    public Map<String, Object> getKeyInfo(@RequestParam String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", keys.size());
        result.put("keys", keys.stream().limit(100).collect(Collectors.toList()));
        
        return result;
    }
    
    @DeleteMapping("/keys")
    public void deleteKeys(@RequestParam String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

缓存设计需要根据业务场景做出权衡。没有万能的缓存策略，只有最适合当前场景的方案。关键是要理解每种方案的优缺点，在一致性、性能和复杂度之间找到平衡点。
