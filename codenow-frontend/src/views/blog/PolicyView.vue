<template>
  <article class="policy-card">
    <h1>{{ isPrivacy ? '隐私政策' : '用户协议' }}</h1>
    <p class="updated">版本：2026-01</p>
    <template v-if="isPrivacy">
      <h2>我们收集的信息</h2><p>注册及使用社区功能时，我们会收集用户名、邮箱、昵称、登录与操作 IP 等提供服务所必需的信息。</p>
      <h2>信息用途</h2><p>相关信息仅用于账号认证、评论与收藏、账号安全、滥用防护及必要的服务通知。邮箱不会在公开评论区展示。</p>
      <h2>信息管理</h2><p>你可以在个人中心修改公开昵称。如需查询、更正或删除账号数据，请联系网站管理员。</p>
    </template>
    <template v-else>
      <h2>账号使用</h2><p>用户应妥善保管账号，不得利用本站发布违法、有害、侵权或恶意内容。</p>
      <h2>社区内容</h2><p>评论可能经过审核。管理员有权删除不当内容，并对滥用功能的账号进行限制或禁用。</p>
      <h2>服务变更</h2><p>本站可能根据功能发展更新协议。重要变更会通过页面提示等合理方式通知用户。</p>
    </template>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const isPrivacy = computed(() => route.name === 'privacy')
</script>

<style scoped>
.policy-card { padding: 32px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; color: var(--blog-color-text-secondary); line-height: 1.8; }
h1 { margin-top: 0; color: var(--blog-color-text); } h2 { margin: 28px 0 8px; color: var(--blog-color-text); font-size: 18px; } .updated { color: var(--blog-color-text-muted); }
</style>
