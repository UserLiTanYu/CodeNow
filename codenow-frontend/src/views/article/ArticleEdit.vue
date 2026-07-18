<template>
  <ArticleEditor
    :article-api="articleApi"
    :load-categories="getCategories"
    :load-tags="getTags"
    redirect-path="/articles"
    admin-tools
  />
</template>

<script setup>
import ArticleEditor from '@/components/article/ArticleEditor.vue'
import { createArticle, getArticle, updateArticle } from '@/api/article'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'

const articleApi = {
  get: getArticle,
  create: createArticle,
  update: updateArticle,
}
</script>
