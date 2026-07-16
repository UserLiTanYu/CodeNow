<template>
  <article class="article-card" :class="{ 'has-cover': article.coverImage }">
    <router-link
      :to="`/blog/article/${article.id}`"
      class="card-hit-area"
      :aria-label="`阅读文章：${article.title}`"
    />

    <div class="card-content">
      <div class="title-row">
        <el-tag v-if="article.isTop" size="small" type="danger" effect="dark" class="top-tag">置顶</el-tag>
        <h2 class="card-title">{{ article.title }}</h2>
      </div>
      <p class="card-summary">{{ article.summary || '暂无摘要' }}</p>

      <div class="card-meta">
        <router-link
          v-if="showCategory && item.categoryName"
          :to="`/blog/category/${article.categoryId}`"
          class="meta-link category-link"
          @click.stop
        >
          <el-icon><Folder /></el-icon>
          {{ item.categoryName }}
        </router-link>
        <time v-if="article.createTime" class="meta-item" :datetime="article.createTime">
          <el-icon><Clock /></el-icon>
          {{ formatDateOnly(article.createTime) }}
        </time>
        <span class="meta-item">
          <el-icon><View /></el-icon>
          {{ article.viewCount || 0 }} 阅读
        </span>
        <router-link
          v-for="tag in visibleTags"
          :key="tag.id"
          :to="`/blog/tag/${tag.id}`"
          :class="['meta-link', 'tag-link', tagTone(tag.name)]"
          @click.stop
        >
          {{ tag.name }}
        </router-link>
        <span v-if="hiddenTagCount > 0" class="more-tags" :title="hiddenTagNames">+{{ hiddenTagCount }}</span>
      </div>
    </div>

    <div v-if="article.coverImage" class="card-cover">
      <img :src="article.coverImage" :alt="`${article.title}封面`" loading="lazy" />
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { Clock, Folder, View } from '@element-plus/icons-vue'
import { formatDateOnly } from '@/utils/format'

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  showCategory: {
    type: Boolean,
    default: true,
  },
})

const article = computed(() => props.item.article)
const tags = computed(() => props.item.tags || [])
const visibleTags = computed(() => tags.value.slice(0, 2))
const hiddenTagCount = computed(() => Math.max(tags.value.length - visibleTags.value.length, 0))
const hiddenTagNames = computed(() => tags.value.slice(2).map((tag) => tag.name).join('、'))

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
  margin-bottom: var(--blog-space-4);
  padding: 20px 24px;
  display: flex;
  gap: var(--blog-space-5);
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
  margin-bottom: 7px;
  display: flex;
  align-items: center;
  gap: var(--blog-space-2);
}
.card-title {
  margin: 0;
  color: var(--blog-color-text);
  font-size: 21px;
  font-weight: 600;
  line-height: 1.45;
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
  margin: 0 0 12px;
  overflow: hidden;
  display: -webkit-box;
  color: #707986;
  font-size: 15px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}
.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--blog-space-2) var(--blog-space-4);
  color: var(--blog-color-text-muted);
  font-size: 13px;
  line-height: 1.5;
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
  padding: var(--blog-space-1) var(--blog-space-2);
  border-radius: var(--blog-radius-tag);
  background: var(--blog-color-background);
  font-size: 12px;
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
  width: 180px;
  height: 110px;
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
    padding: var(--blog-space-4);
    gap: var(--blog-space-4);
  }
  .card-title {
    font-size: 18px;
  }
  .card-summary {
    font-size: 14px;
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
