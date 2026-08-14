import { ref } from 'vue'

/**
 * 当前文章详情页的文章作者ID。
 * 由 BlogArticle.vue 在详情加载完成后写入，BlogLayout.vue 读取以将侧边栏
 * （分类/热门文章）作用域化到文章作者。使用模块级共享 ref 而非 provide/inject，
 * 避免父组件 setup 先于子组件 provide 执行导致的注入时序问题。
 */
export const currentArticleAuthorId = ref(null)
