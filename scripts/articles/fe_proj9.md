# 测试策略与代码质量保障

软件测试是保障代码质量的重要手段，但测试不是越多越好，而是要找到投入产出比最优的策略。本文将介绍测试金字塔理论、各层测试的实践方法、代码评审流程以及静态分析工具的使用。

## 测试金字塔

### 理论模型

```
         ╱╲
        ╱E2E╲        少量，验证关键业务流程
       ╱──────╲
      ╱ 集成测试 ╲     适量，验证模块间协作
     ╱────────────╲
    ╱   单元测试    ╲    大量，验证单个函数/类
   ╱────────────────╲
```

### 测试策略对照表

| 测试类型 | 数量 | 速度 | 成本 | 覆盖范围 | 工具 |
|----------|------|------|------|----------|------|
| 单元测试 | 多 | 快 | 低 | 函数/类 | JUnit, Vitest |
| 集成测试 | 中 | 中 | 中 | 模块间 | Spring Test, TestContainers |
| E2E测试 | 少 | 慢 | 高 | 完整流程 | Playwright, Cypress |

## 后端单元测试

### JUnit 5 + Mockito

```java
// ArticleServiceTest.java
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    
    @Mock
    private ArticleRepository articleRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private TagRepository tagRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private ArticleService articleService;
    
    @Test
    @DisplayName("创建文章 - 正常情况")
    void createArticle_Success() {
        // Given
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("测试文章");
        request.setContent("这是一篇测试文章的内容...");
        request.setCategoryId(1L);
        request.setTags(Set.of("Java", "Spring"));
        
        User author = User.builder().id(1L).username("testuser").build();
        
        Category category = Category.builder().id(1L).name("后端").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        
        when(tagRepository.findByName("Java")).thenReturn(Optional.of(new Tag(1L, "Java")));
        when(tagRepository.findByName("Spring")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(2L);
            return tag;
        });
        
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(1L);
            return article;
        });
        
        // When
        ArticleDTO result = articleService.createArticle(request, author);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("测试文章");
        assertThat(result.getAuthor().getId()).isEqualTo(1L);
        
        verify(categoryRepository).findById(1L);
        verify(tagRepository).findByName("Java");
        verify(tagRepository).findByName("Spring");
        verify(tagRepository).save(any(Tag.class));
        verify(articleRepository).save(any(Article.class));
    }
    
    @Test
    @DisplayName("创建文章 - 分类不存在")
    void createArticle_CategoryNotFound() {
        // Given
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("测试文章");
        request.setContent("内容...");
        request.setCategoryId(999L);
        
        User author = User.builder().id(1L).build();
        
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> articleService.createArticle(request, author))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("分类不存在");
        
        verify(articleRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("创建文章 - 无权使用他人分类")
    void createArticle_AccessDenied() {
        // Given
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("测试文章");
        request.setContent("内容...");
        request.setCategoryId(1L);
        
        User author = User.builder().id(1L).build();
        Category category = Category.builder()
            .id(1L)
            .author(User.builder().id(2L).build()) // 属于其他用户
            .build();
        
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        
        // When & Then
        assertThatThrownBy(() -> articleService.createArticle(request, author))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("无权使用该分类");
    }
    
    @Test
    @DisplayName("发布文章 - 状态变更")
    void publishArticle_Success() {
        // Given
        Article article = Article.builder()
            .id(1L)
            .title("测试文章")
            .status(ArticleStatus.DRAFT)
            .build();
        
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        
        // When
        ArticleDTO result = articleService.publishArticle(1L);
        
        // Then
        assertThat(result.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        verify(articleRepository).save(article);
    }
    
    @Test
    @DisplayName("发布文章 - 非草稿状态不能发布")
    void publishArticle_InvalidStatus() {
        // Given
        Article article = Article.builder()
            .id(1L)
            .status(ArticleStatus.PUBLISHED)
            .build();
        
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        
        // When & Then
        assertThatThrownBy(() -> articleService.publishArticle(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("只有草稿状态的文章才能发布");
    }
    
    @Test
    @DisplayName("分页查询文章")
    void listArticles_Pagination() {
        // Given
        ArticleQuery query = new ArticleQuery();
        query.setPage(1);
        query.setSize(10);
        
        List<Article> articles = IntStream.range(0, 10)
            .mapToObj(i -> Article.builder().id((long) i).title("文章" + i).build())
            .collect(Collectors.toList());
        
        Page<Article> page = new PageImpl<>(articles, PageRequest.of(0, 10), 100);
        when(articleRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);
        
        // When
        PageResult<ArticleDTO> result = articleService.listArticles(query);
        
        // Then
        assertThat(result.getItems()).hasSize(10);
        assertThat(result.getTotal()).isEqualTo(100);
        assertThat(result.getPage()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("文章缓存测试")
    void findById_CacheHit() {
        // Given
        Long articleId = 1L;
        ArticleDTO cachedArticle = new ArticleDTO();
        cachedArticle.setId(articleId);
        cachedArticle.setTitle("缓存中的文章");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("articles:1")).thenReturn(cachedArticle);
        
        // When
        ArticleDTO result = articleService.findById(articleId);
        
        // Then
        assertThat(result.getTitle()).isEqualTo("缓存中的文章");
        verify(articleRepository, never()).findById(any());
    }
}
```

### 测试工具类

```java
// TestUtils.java
public class TestUtils {
    
    public static User createTestUser() {
        return User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .role(Role.AUTHOR)
            .build();
    }
    
    public static Article createTestArticle(User author) {
        return Article.builder()
            .id(1L)
            .title("测试文章")
            .content("测试内容")
            .slug("test-article")
            .status(ArticleStatus.DRAFT)
            .author(author)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    public static CreateArticleRequest createArticleRequest() {
        CreateArticleRequest request = new CreateArticleRequest();
        request.setTitle("测试文章");
        request.setContent("这是一篇测试文章的内容...");
        request.setCategoryId(1L);
        request.setTags(Set.of("Java", "Spring"));
        return request;
    }
}
```

## 前端单元测试

### Vitest配置

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{js,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.test.ts',
        'src/**/*.spec.ts',
        'src/test/**'
      ]
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  }
});
```

```typescript
// src/test/setup.ts
import { config } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import ElementPlus from 'element-plus';

// 全局配置
config.global.plugins = [ElementPlus];

// 每个测试前重置Pinia
beforeEach(() => {
  setActivePinia(createPinia());
});
```

### Vue组件测试

```typescript
// src/components/ArticleCard.test.ts
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ArticleCard from './ArticleCard.vue';
import type { Article } from '@/types';

describe('ArticleCard', () => {
  const mockArticle: Article = {
    id: 1,
    title: 'Spring Boot入门指南',
    summary: '这是一篇关于Spring Boot的入门文章...',
    author: {
      id: 1,
      name: '张三',
      avatar: 'https://example.com/avatar.jpg'
    },
    category: { id: 1, name: '后端' },
    tags: [
      { id: 1, name: 'Java' },
      { id: 2, name: 'Spring' }
    ],
    views: 1234,
    createdAt: '2024-01-15T10:30:00Z',
    status: 'PUBLISHED'
  };
  
  it('正确渲染文章标题', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    expect(wrapper.text()).toContain('Spring Boot入门指南');
  });
  
  it('正确渲染文章摘要', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    expect(wrapper.text()).toContain('这是一篇关于Spring Boot的入门文章...');
  });
  
  it('正确渲染作者信息', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    expect(wrapper.text()).toContain('张三');
    expect(wrapper.find('img[alt="avatar"]').attributes('src')).toBe(
      'https://example.com/avatar.jpg'
    );
  });
  
  it('正确渲染标签', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    const tags = wrapper.findAll('.tag');
    expect(tags).toHaveLength(2);
    expect(tags[0].text()).toBe('Java');
    expect(tags[1].text()).toBe('Spring');
  });
  
  it('正确格式化阅读量', () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    expect(wrapper.text()).toContain('1.2k'); // 1234 -> 1.2k
  });
  
  it('点击卡片触发navigate事件', async () => {
    const wrapper = mount(ArticleCard, {
      props: { article: mockArticle }
    });
    
    await wrapper.find('.article-card').trigger('click');
    
    expect(wrapper.emitted('navigate')).toBeTruthy();
    expect(wrapper.emitted('navigate')![0]).toEqual([1]);
  });
  
  it('无标签时不渲染标签区域', () => {
    const articleWithoutTags = { ...mockArticle, tags: [] };
    const wrapper = mount(ArticleCard, {
      props: { article: articleWithoutTags }
    });
    
    expect(wrapper.find('.tags').exists()).toBe(false);
  });
});
```

### Store测试

```typescript
// src/stores/modules/user.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useUserStore } from './user';
import * as authApi from '@/api/modules/auth';

vi.mock('@/api/modules/auth');

describe('UserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });
  
  describe('loginAction', () => {
    it('登录成功后更新token和用户信息', async () => {
      const mockToken = 'mock-token-123';
      const mockUserInfo = {
        id: 1,
        username: 'testuser',
        roles: ['AUTHOR'],
        permissions: ['article:create']
      };
      
      vi.mocked(authApi.login).mockResolvedValue({
        data: { token: mockToken, refreshToken: 'refresh-123' }
      });
      vi.mocked(authApi.getUserInfo).mockResolvedValue({
        data: mockUserInfo
      });
      
      const store = useUserStore();
      
      await store.loginAction({
        username: 'testuser',
        password: '123456',
        captcha: '1234',
        captchaKey: 'key123'
      });
      
      expect(store.token).toBe(mockToken);
      expect(store.isLoggedIn).toBe(true);
      expect(store.username).toBe('testuser');
      expect(store.roles).toEqual(['AUTHOR']);
      expect(localStorage.getItem('token')).toBe(mockToken);
    });
    
    it('登录失败抛出异常', async () => {
      vi.mocked(authApi.login).mockRejectedValue(new Error('用户名或密码错误'));
      
      const store = useUserStore();
      
      await expect(store.loginAction({
        username: 'testuser',
        password: 'wrong',
        captcha: '1234',
        captchaKey: 'key123'
      })).rejects.toThrow('用户名或密码错误');
      
      expect(store.isLoggedIn).toBe(false);
    });
  });
  
  describe('hasPermission', () => {
    it('管理员拥有所有权限', () => {
      const store = useUserStore();
      store.roles = ['ADMIN'];
      store.permissions = [];
      
      expect(store.hasPermission('any:permission')).toBe(true);
    });
    
    it('普通用户根据permissions判断', () => {
      const store = useUserStore();
      store.roles = ['AUTHOR'];
      store.permissions = ['article:create', 'article:update'];
      
      expect(store.hasPermission('article:create')).toBe(true);
      expect(store.hasPermission('article:delete')).toBe(false);
    });
  });
  
  describe('logoutAction', () => {
    it('登出后清除状态', async () => {
      vi.mocked(authApi.logout).mockResolvedValue(undefined);
      
      const store = useUserStore();
      store.token = 'some-token';
      store.userInfo = { id: 1, username: 'test' } as any;
      store.roles = ['ADMIN'];
      
      await store.logoutAction();
      
      expect(store.token).toBe('');
      expect(store.userInfo).toBeNull();
      expect(store.roles).toEqual([]);
      expect(localStorage.getItem('token')).toBeNull();
    });
  });
});
```

### Composable测试

```typescript
// src/composables/usePagination.test.ts
import { describe, it, expect } from 'vitest';
import { usePagination } from './usePagination';

describe('usePagination', () => {
  it('初始化默认值', () => {
    const { currentPage, pageSize, total } = usePagination();
    
    expect(currentPage.value).toBe(1);
    expect(pageSize.value).toBe(20);
    expect(total.value).toBe(0);
  });
  
  it('计算总页数', () => {
    const { totalPages, total, pageSize } = usePagination();
    
    total.value = 100;
    pageSize.value = 20;
    expect(totalPages.value).toBe(5);
    
    total.value = 101;
    expect(totalPages.value).toBe(6);
  });
  
  it('切换页码', () => {
    const { currentPage, goToPage, nextPage, prevPage, total, pageSize } = usePagination();
    
    total.value = 100;
    pageSize.value = 20;
    
    goToPage(3);
    expect(currentPage.value).toBe(3);
    
    nextPage();
    expect(currentPage.value).toBe(4);
    
    prevPage();
    expect(currentPage.value).toBe(3);
  });
  
  it('页码边界检查', () => {
    const { currentPage, goToPage, nextPage, prevPage, total, pageSize } = usePagination();
    
    total.value = 100;
    pageSize.value = 20;
    
    goToPage(0);
    expect(currentPage.value).toBe(1);
    
    goToPage(10);
    expect(currentPage.value).toBe(5);
    
    currentPage.value = 1;
    prevPage();
    expect(currentPage.value).toBe(1);
    
    currentPage.value = 5;
    nextPage();
    expect(currentPage.value).toBe(5);
  });
});
```

## E2E测试

### Playwright配置

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] }
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] }
    }
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI
  }
});
```

### E2E测试用例

```typescript
// e2e/article.spec.ts
import { test, expect } from '@playwright/test';

test.describe('文章管理', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('[data-testid="username"]', 'admin');
    await page.fill('[data-testid="password"]', '123456');
    await page.fill('[data-testid="captcha"]', '1234');
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');
  });
  
  test('创建并发布文章', async ({ page }) => {
    // 进入文章创建页面
    await page.goto('/article/create');
    
    // 填写文章信息
    await page.fill('[data-testid="title"]', 'Playwright E2E测试文章');
    await page.fill('[data-testid="content"]', '这是一篇通过E2E测试创建的文章...');
    
    // 选择分类
    await page.click('[data-testid="category-select"]');
    await page.click('text=后端');
    
    // 添加标签
    await page.fill('[data-testid="tag-input"]', 'E2E');
    await page.press('[data-testid="tag-input"]', 'Enter');
    
    // 点击发布
    await page.click('[data-testid="publish-button"]');
    
    // 验证跳转到文章详情页
    await expect(page).toHaveURL(/\/article\/\d+/);
    await expect(page.locator('h1')).toContainText('Playwright E2E测试文章');
    
    // 验证文章状态
    await expect(page.locator('[data-testid="status"]')).toContainText('已发布');
  });
  
  test('文章列表分页', async ({ page }) => {
    await page.goto('/article/list');
    
    // 等待文章列表加载
    await page.waitForSelector('.article-item');
    
    // 获取第一篇文章标题
    const firstTitle = await page.locator('.article-item:first-child .title').textContent();
    
    // 点击下一页
    await page.click('[data-testid="next-page"]');
    
    // 验证页面变化
    await expect(page.locator('.article-item:first-child .title')).not.toHaveText(firstTitle!);
    
    // 验证URL变化
    expect(page.url()).toContain('page=2');
  });
  
  test('文章搜索', async ({ page }) => {
    await page.goto('/article/list');
    
    // 输入搜索关键词
    await page.fill('[data-testid="search-input"]', 'Spring');
    await page.click('[data-testid="search-button"]');
    
    // 等待搜索结果
    await page.waitForSelector('.article-item');
    
    // 验证所有结果都包含关键词
    const titles = await page.locator('.article-item .title').allTextContents();
    titles.forEach(title => {
      expect(title.toLowerCase()).toContain('spring');
    });
  });
});

// e2e/auth.spec.ts
test.describe('认证流程', () => {
  test('登录成功跳转', async ({ page }) => {
    await page.goto('/login');
    
    await page.fill('[data-testid="username"]', 'admin');
    await page.fill('[data-testid="password"]', '123456');
    await page.fill('[data-testid="captcha"]', '1234');
    await page.click('[data-testid="login-button"]');
    
    await expect(page).toHaveURL('/dashboard');
    await expect(page.locator('[data-testid="username"]')).toContainText('admin');
  });
  
  test('登录失败提示', async ({ page }) => {
    await page.goto('/login');
    
    await page.fill('[data-testid="username"]', 'admin');
    await page.fill('[data-testid="password"]', 'wrong');
    await page.fill('[data-testid="captcha"]', '1234');
    await page.click('[data-testid="login-button"]');
    
    await expect(page.locator('.el-message--error')).toBeVisible();
    await expect(page).toHaveURL('/login');
  });
  
  test('未登录访问受保护页面跳转登录', async ({ page }) => {
    await page.goto('/article/create');
    
    await expect(page).toHaveURL(/\/login\?redirect=.*/);
  });
});
```

## 代码评审清单

```markdown
## 代码评审清单

### 功能完整性
- [ ] 是否实现了需求中的所有功能点？
- [ ] 是否处理了边界条件和异常情况？
- [ ] 是否有遗漏的场景？

### 代码质量
- [ ] 命名是否清晰有意义？
- [ ] 函数是否职责单一？
- [ ] 是否有重复代码可以提取？
- [ ] 注释是否必要且准确？

### 性能考虑
- [ ] 是否有N+1查询问题？
- [ ] 是否有不必要的循环或递归？
- [ ] 缓存使用是否合理？
- [ ] 是否有内存泄漏风险？

### 安全性
- [ ] 输入是否做了校验？
- [ ] SQL注入/XSS防护是否到位？
- [ ] 敏感信息是否脱敏？
- [ ] 权限检查是否完整？

### 可测试性
- [ ] 是否编写了单元测试？
- [ ] 测试覆盖了关键路径吗？
- [ ] 测试用例是否清晰易懂？

### 前端特有
- [ ] 组件是否有不必要的重渲染？
- [ ] 事件监听是否正确清理？
- [ ] 异步操作是否处理了竞态条件？
- [ ] 是否有内存泄漏（未清理的定时器、订阅等）？
```

## 静态分析

### ESLint配置

```json
// .eslintrc.cjs
module.exports = {
  root: true,
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    '@vue/eslint-config-typescript',
    '@vue/eslint-config-prettier'
  ],
  rules: {
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'warn',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'no-console': ['warn', { allow: ['warn', 'error'] }]
  }
};
```

### SonarQube集成

```yaml
# sonar-project.properties
sonar.projectKey=codenow
sonar.projectName=CodeNow
sonar.projectVersion=1.0

sonar.sources=src
sonar.tests=src
sonar.test.inclusions=**/*.test.ts,**/*.spec.ts

sonar.typescript.lcov.reportPaths=coverage/lcov.info

sonar.qualitygate.wait=true
```

测试策略要根据项目实际情况制定，没有放之四海而皆准的方案。关键是找到适合团队和项目的平衡点，让测试真正成为质量保障的手段，而不是负担。
