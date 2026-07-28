<template>
  <!-- 作者文章编辑页面，复用通用文章编辑器组件，注入作者级别的 API -->
  <ArticleEditor
    :article-api="articleApi"
    :load-categories="getAuthorCategories"
    :load-tags="getAuthorTags"
    redirect-path="/author-console/articles"
    image-tools
    :upload-image-request="uploadAuthorImage"
  />
</template>

<script setup>
/** 作者文章编辑页面 - 封装文章编辑器，注入作者级别的 API 和图片上传功能 */
import ArticleEditor from '@/components/article/ArticleEditor.vue'
import { createAuthorArticle, getAuthorArticle, getAuthorCategories, getAuthorTags, updateAuthorArticle, uploadAuthorImage } from '@/api/authorConsole'

/** 作者文章 CRUD 接口映射，供 ArticleEditor 组件调用 */
const articleApi = {
  get: getAuthorArticle,
  create: createAuthorArticle,
  update: updateAuthorArticle,
}
</script>
