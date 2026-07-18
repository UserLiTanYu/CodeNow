<template>
  <ArticleEditor
    :article-api="articleApi"
    :load-categories="getBlogCategories"
    :load-tags="getAuthorTags"
    redirect-path="/author-console/articles"
    image-tools
    :upload-image-request="uploadAuthorImage"
  />
</template>

<script setup>
import ArticleEditor from '@/components/article/ArticleEditor.vue'
import { createAuthorArticle, getAuthorArticle, getAuthorTags, updateAuthorArticle, uploadAuthorImage } from '@/api/authorConsole'
import { getBlogCategories } from '@/api/blog'

const articleApi = {
  get: getAuthorArticle,
  create: createAuthorArticle,
  update: updateAuthorArticle,
}
</script>
