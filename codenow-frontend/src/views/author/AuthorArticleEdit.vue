<template>
  <ArticleEditor
    :article-api="articleApi"
    :load-categories="getBlogCategories"
    :load-tags="getBlogTags"
    redirect-path="/author-console/articles"
  />
</template>

<script setup>
import ArticleEditor from '@/components/article/ArticleEditor.vue'
import { createAuthorArticle, getAuthorArticle, updateAuthorArticle } from '@/api/authorConsole'
import { getBlogCategories, getBlogTags } from '@/api/blog'

const articleApi = {
  get: getAuthorArticle,
  create: createAuthorArticle,
  update: updateAuthorArticle,
}
</script>
