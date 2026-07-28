<template>
  <!-- 博客文章卡片组件：展示文章标题、摘要、作者、分类、标签、封面图等信息 -->
  <article class="article-card" :class="{ 'has-cover': article.coverImage }">
    <!-- 整个卡片的点击热区，点击跳转到文章详情页 -->
    <router-link
      :to="`/blog/article/${article.id}`"
      class="card-hit-area"
      :aria-label="`阅读文章：${article.title}`"
    />

    <!-- 卡片内容区域 -->
    <div class="card-content">
      <!-- 标题行：置顶标签 + 文章标题 -->
      <div class="title-row">
        <el-tag v-if="article.isTop" size="small" type="danger" effect="dark" class="top-tag">置顶</el-tag>
        <h2 class="card-title">{{ article.title }}</h2>
      </div>
      <!-- 文章摘要 -->
      <p class="card-summary">{{ article.summary || '暂无摘要' }}</p>

      <!-- 元信息区域：分类、发布时间、阅读量、标签 -->
      <div class="card-meta">
        <!-- 文章分类 -->
        <router-link
          v-if="showCategory && item.categoryName"
          :to="categoryTarget"
          class="meta-link category-link"
          @click.stop
        >
          <el-icon><Folder /></el-icon>
          {{ item.categoryName }}
        </router-link>
        <!-- 发布时间 -->
        <time v-if="article.createTime" class="meta-item" :datetime="article.createTime">
          <el-icon><Clock /></el-icon>
          {{ formatDate(article.createTime) }}
        </time>
        <!-- 阅读量 -->
        <span class="meta-item">
          <el-icon><View /></el-icon>
          {{ article.viewCount || 0 }} 阅读
        </span>
        <!-- 文章标签（最多显示两个） -->
        <router-link
          v-for="tag in visibleTags"
          :key="tag.id"
          :to="tagTarget(tag.id)"
          :class="['meta-link', 'tag-link', tagTone(tag.name)]"
          @click.stop
        >
          {{ tag.name }}
        </router-link>
        <!-- 超出两个标签时显示折叠计数 -->
        <span v-if="hiddenTagCount > 0" class="more-tags" :title="hiddenTagNames">+{{ hiddenTagCount }}</span>
      </div>
    </div>

    <!-- 封面图区域（有封面图时显示） -->
    <div v-if="article.coverImage" class="card-cover">
      <img :src="article.coverImage" :alt="`${article.title}封面`" loading="lazy" />
    </div>
  </article>
</template>

<script setup>
/**
 * 博客文章卡片组件
 * 在博客列表页展示文章摘要信息，包含标题、摘要、作者头像、分类、标签、发布时间、阅读量和封面图。
 * 标签最多展示两个，超出部分折叠为计数显示。
 */
import { computed } from 'vue'
import { Clock, Folder, View } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/format'

/** 组件属性：item 文章数据对象，showCategory 是否显示分类链接 */
const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  showCategory: {
    type: Boolean,
    default: true,
  },
  contextAuthorId: {
    type: [Number, String],
    default: null,
  },
  contextRootPath: {
    type: String,
    default: '',
  },
})

/** 文章对象 */
const article = computed(() => props.item.article)
/** 文章标签列表 */
const tags = computed(() => props.item.tags || [])
/** 分类链接；按调用页面指定的根路径或作者主页保持筛选上下文。 */
const categoryTarget = computed(() => {
  if (props.contextRootPath) {
    return { path: props.contextRootPath, query: { categoryId: article.value.categoryId } }
  }
  return props.contextAuthorId
    ? { path: `/blog/author/${props.contextAuthorId}`, query: { categoryId: article.value.categoryId } }
    : `/blog/category/${article.value.categoryId}`
})
/** 标签链接；按调用页面指定的根路径或作者主页保持筛选上下文。 */
function tagTarget(tagId) {
  if (props.contextRootPath) return { path: props.contextRootPath, query: { tagId } }
  return props.contextAuthorId
    ? { path: `/blog/author/${props.contextAuthorId}`, query: { tagId } }
    : `/blog/tag/${tagId}`
}
/** 可见标签（最多两个），卡片最多展示两个标签，剩余标签折叠成计数，防止元信息区域挤压标题和封面 */
const visibleTags = computed(() => tags.value.slice(0, 2))
/** 被隐藏的标签数量 */
const hiddenTagCount = computed(() => Math.max(tags.value.length - visibleTags.value.length, 0))
/** 被隐藏的标签名称列表（用于 hover 提示） */
const hiddenTagNames = computed(() => tags.value.slice(2).map((tag) => tag.name).join('、'))

/** 根据标签名称关键词返回对应的色调 CSS 类名，用于标签视觉分类 */
function tagTone(name = '') {
  const value = name.toLowerCase()
  if (value.includes('java')) return 'tag-java'
  if (value.includes('spring')) return 'tag-spring'
  if (value.includes('mysql') || value.includes('redis') || value.includes('mybatis') || value.includes('数据库')) return 'tag-database'
  if (value.includes('vue') || value.includes('javascript') || value.includes('前端')) return 'tag-frontend'
  if (value.includes('设计')) return 'tag-design'
  return 'tag-default'
}
</script>

<style scoped>
.article-card {
  position: relative;
  margin-bottom: 10px;
  padding: 14px 18px;
  display: flex;
  gap: 14px;
  overflow: hidden;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.article-card:hover {
  border-color: var(--blog-color-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--blog-shadow-hover);
}
.article-card:hover .card-title {
  color: var(--blog-color-primary);
}
.card-hit-area {
  position: absolute;
  inset: 0;
  z-index: 1;
  border-radius: inherit;
}
.card-hit-area:focus-visible {
  outline: 3px solid rgba(51, 126, 204, 0.28);
  outline-offset: -3px;
}
.card-content {
  min-width: 0;
  flex: 1;
}
.title-row {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: var(--blog-space-2);
}
.card-title {
  margin: 0;
  color: var(--blog-color-text);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  transition: color 0.18s ease;
}
.top-tag {
  flex-shrink: 0;
  border-radius: var(--blog-radius-tag);
}
.card-summary {
  margin: 0 0 7px;
  overflow: hidden;
  display: -webkit-box;
  color: #707986;
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}
.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px 12px;
  color: var(--blog-color-text-muted);
  font-size: 12px;
  line-height: 1.4;
}
.meta-item,
.meta-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-link {
  position: relative;
  z-index: 2;
  color: var(--blog-color-text-secondary);
  text-decoration: none;
  transition: color 0.16s ease, background-color 0.16s ease;
}
.meta-link:hover {
  color: var(--blog-color-primary);
}
.meta-link:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.3);
  outline-offset: 2px;
}
.tag-link,
.more-tags {
  padding: 2px 7px;
  border-radius: var(--blog-radius-tag);
  background: var(--blog-color-background);
  font-size: 11px;
}
.tag-link:hover {
  background: var(--blog-color-primary-soft);
}
.tag-java { color: #9a5b13; background: #fff4e5; }
.tag-spring { color: #3e7b43; background: #edf8ee; }
.tag-database { color: #7155a4; background: #f3effb; }
.tag-frontend { color: #28719c; background: #eaf6fb; }
.tag-design { color: #8a6a16; background: #fff8dc; }
.tag-default { color: var(--blog-color-text-secondary); background: var(--blog-color-background); }
.tag-java:hover { background: #ffe8c7; }
.tag-spring:hover { background: #dcf1de; }
.tag-database:hover { background: #e8dff7; }
.tag-frontend:hover { background: #d8eef8; }
.tag-design:hover { background: #f9edb8; }
.more-tags {
  color: var(--blog-color-text-muted);
}
.card-cover {
  width: 150px;
  height: 88px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-background);
}
.card-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: opacity 0.18s ease;
}
.article-card:hover .card-cover img {
  opacity: 0.94;
}

@media (max-width: 640px) {
  .article-card {
    padding: 12px 14px;
    gap: 12px;
  }
  .card-title {
    font-size: 16px;
  }
  .card-summary {
    font-size: 12px;
  }
  .card-cover {
    width: 112px;
    height: 76px;
  }
}

@media (max-width: 430px) {
  .article-card.has-cover {
    display: grid;
  }
  .has-cover .card-cover {
    width: 100%;
    height: 150px;
    grid-row: 1;
  }
}
</style>
