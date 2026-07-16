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
          class="meta-link tag-link"
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
</script>

<style scoped>
.article-card {
  position: relative;
  margin-bottom: 16px;
  padding: 24px 26px;
  display: flex;
  gap: 24px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.article-card:hover {
  border-color: #c6e2ff;
  transform: translateY(-2px);
  box-shadow: 0 7px 18px rgba(31, 45, 61, 0.08);
}
.article-card:hover .card-title {
  color: #337ecc;
}
.card-hit-area {
  position: absolute;
  inset: 0;
  z-index: 1;
  border-radius: inherit;
}
.card-hit-area:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.35);
  outline-offset: -3px;
}
.card-content {
  min-width: 0;
  flex: 1;
}
.title-row {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 9px;
}
.card-title {
  margin: 0;
  color: #252b33;
  font-size: 21px;
  font-weight: 600;
  line-height: 1.45;
  transition: color 0.18s ease;
}
.top-tag {
  flex-shrink: 0;
}
.card-summary {
  margin: 0 0 15px;
  overflow: hidden;
  display: -webkit-box;
  color: #606873;
  font-size: 15px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px 14px;
  color: #9098a3;
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
  color: #7c8795;
  text-decoration: none;
  transition: color 0.16s ease, background-color 0.16s ease;
}
.meta-link:hover {
  color: #337ecc;
}
.meta-link:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.3);
  outline-offset: 2px;
}
.tag-link,
.more-tags {
  padding: 3px 8px;
  border-radius: 5px;
  background: #f2f4f7;
  font-size: 12px;
}
.tag-link:hover {
  background: #eaf4ff;
}
.more-tags {
  color: #9098a3;
}
.card-cover {
  width: 180px;
  height: 110px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 8px;
  background: #f5f7fa;
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
    padding: 19px;
    gap: 14px;
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
