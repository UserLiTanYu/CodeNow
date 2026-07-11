<template>
  <div class="blog-layout">
    <!-- 顶栏 -->
    <header class="blog-header">
      <div class="header-inner">
        <router-link to="/blog" class="logo">码上记</router-link>
        <nav class="nav-categories">
          <router-link to="/blog" class="nav-item">首页</router-link>
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/blog/category/${cat.id}`"
            class="nav-item"
          >
            {{ cat.name }}
          </router-link>
        </nav>
        <router-link to="/login" class="admin-link">管理后台</router-link>
      </div>
    </header>

    <!-- 主体 -->
    <div class="blog-body">
      <main class="blog-main">
        <router-view />
      </main>
      <aside class="blog-sidebar">
        <div class="sidebar-section">
          <h3 class="sidebar-title">热门文章</h3>
          <div v-if="hotArticles.length > 0" class="hot-list">
            <router-link
              v-for="item in hotArticles"
              :key="item.article.id"
              :to="`/blog/article/${item.article.id}`"
              class="hot-item"
            >
              <span class="hot-title">{{ item.article.title }}</span>
              <span class="hot-views">{{ item.article.viewCount }} 阅读</span>
            </router-link>
          </div>
          <p v-else class="empty-text">暂无热门文章</p>
        </div>
        <div class="sidebar-section">
          <h3 class="sidebar-title">标签</h3>
          <div class="tag-cloud">
            <router-link
              v-for="tag in tags"
              :key="tag.id"
              :to="`/blog/tag/${tag.id}`"
              class="tag-item"
            >
              {{ tag.name }}
            </router-link>
          </div>
        </div>
        <div class="sidebar-section">
          <h3 class="sidebar-title">关于</h3>
          <p class="about-text">一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。</p>
        </div>
      </aside>
    </div>

    <!-- 底栏 -->
    <footer class="blog-footer">
      <p>&copy; {{ new Date().getFullYear() }} 码上记 CodeNow. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const categories = ref([])
const tags = ref([])
const hotArticles = ref([])

onMounted(async () => {
  try {
    const [catRes, tagRes, hotRes] = await Promise.all([
      request.get('/blog/categories'),
      request.get('/blog/tags'),
      request.get('/blog/articles/hot', { params: { topN: 10 } }),
    ])
    categories.value = catRes.data
    tags.value = tagRes.data
    hotArticles.value = hotRes.data
  } catch {
    // 静默失败
  }
})
</script>

<style scoped>
.blog-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

/* 顶栏 */
.blog-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.logo {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  text-decoration: none;
  white-space: nowrap;
}
.nav-categories {
  display: flex;
  gap: 8px;
  flex: 1;
  overflow-x: auto;
}
.nav-item {
  padding: 6px 14px;
  border-radius: 4px;
  text-decoration: none;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.2s;
}
.nav-item:hover,
.nav-item.router-link-exact-active {
  color: #409eff;
  background: #ecf5ff;
}
.admin-link {
  font-size: 13px;
  color: #909399;
  text-decoration: none;
  white-space: nowrap;
}
.admin-link:hover {
  color: #409eff;
}

/* 主体 */
.blog-body {
  flex: 1;
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
  display: flex;
  gap: 24px;
  width: 100%;
  box-sizing: border-box;
}
.blog-main {
  flex: 1;
  min-width: 0;
}
.blog-sidebar {
  width: 280px;
  flex-shrink: 0;
}
.sidebar-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-item {
  padding: 4px 12px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  text-decoration: none;
  transition: all 0.2s;
}
.tag-item:hover {
  background: #409eff;
  color: #fff;
}
.about-text {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
  margin: 0;
}
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hot-item {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s;
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-item:hover .hot-title {
  color: #409eff;
}
.hot-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.hot-views {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.empty-text {
  font-size: 13px;
  color: #c0c4cc;
  margin: 0;
}

/* 底栏 */
.blog-footer {
  background: #fff;
  border-top: 1px solid #eee;
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 13px;
}
.blog-footer p {
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .blog-sidebar {
    display: none;
  }
  .header-inner {
    gap: 16px;
  }
}
</style>
