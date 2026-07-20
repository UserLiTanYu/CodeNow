<template>
  <!-- 管理员文章编辑页面，复用通用文章编辑器组件 -->
  <ArticleEditor
    :article-api="articleApi"
    :load-categories="getCategories"
    :load-tags="getTags"
    redirect-path="/articles"
    admin-tools
  />
</template>

<script setup>
/** 管理员文章编辑页面 - 封装文章编辑器，注入管理员级别的 API 和功能 */
import ArticleEditor from '@/components/article/ArticleEditor.vue'
import { createArticle, getArticle, updateArticle } from '@/api/article'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'

/** 文章 CRUD 接口映射，供 ArticleEditor 组件调用 */
const articleApi = {
  get: getArticle,
  create: createArticle,
  update: updateArticle,
}
</script>
