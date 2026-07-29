<template>
  <article class="about-page">
    <section class="about-hero">
      <p class="about-eyebrow">ABOUT {{ siteName.toUpperCase() }}</p>
      <h1>关于{{ siteName }}</h1>
      <p class="about-slogan">{{ slogan }}</p>
      <p class="about-lead">{{ description }}</p>
    </section>

    <section class="stats-grid" aria-label="站点数据">
      <div v-for="item in statItems" :key="item.label" class="stat-card">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section class="about-section">
      <div class="section-heading">
        <p class="section-index">01</p>
        <div>
          <h2>本站简介</h2>
          <p>记录实践，也帮助每一位开发者更高效地找到知识。</p>
        </div>
      </div>
      <p v-if="loadingProfile" class="profile-status">正在加载站点简介...</p>
      <div v-else class="about-content markdown-body" v-html="renderedAboutContent"></div>
    </section>

    <section class="about-section">
      <div class="section-heading">
        <p class="section-index">02</p>
        <div>
          <h2>你可以在这里</h2>
          <p>从阅读到创作，让技术经验形成可以持续积累的内容。</p>
        </div>
      </div>
      <div class="feature-grid">
        <div v-for="feature in features" :key="feature.title" class="feature-card">
          <span class="feature-number">{{ feature.number }}</span>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </div>
      </div>
    </section>

    <section class="about-section">
      <div class="section-heading">
        <p class="section-index">03</p>
        <div>
          <h2>内容原则</h2>
          <p>我们希望每一篇内容都经得起阅读、实践与时间的检验。</p>
        </div>
      </div>
      <ul class="principle-list">
        <li v-for="principle in principles" :key="principle">{{ principle }}</li>
      </ul>
    </section>

    <section v-if="hasPublicContact" class="about-section contact-section">
      <div class="section-heading">
        <p class="section-index">04</p>
        <div>
          <h2>联系本站</h2>
          <p v-if="profile.foundedAt">自 {{ foundedYear }} 年开始，持续记录与分享。</p>
          <p v-else>欢迎通过以下公开方式与我们联系。</p>
        </div>
      </div>
      <div class="contact-links">
        <a v-if="profile.contactEmail" :href="`mailto:${profile.contactEmail}`">{{ profile.contactEmail }}</a>
        <a v-if="profile.githubUrl" :href="profile.githubUrl" target="_blank" rel="noopener noreferrer">访问 GitHub</a>
      </div>
    </section>

    <section class="about-footer">
      <div>
        <h2>开始探索</h2>
        <p>阅读文章、发现作者，或者分享你的技术经验。</p>
      </div>
      <div class="about-actions">
        <router-link to="/blog" class="primary-action">浏览文章</router-link>
        <router-link to="/blog/authors" class="secondary-action">发现作者</router-link>
        <router-link to="/blog/author-application" class="secondary-action">申请成为作者</router-link>
      </div>
      <div class="policy-links">
        <router-link to="/blog/terms">用户协议</router-link>
        <span aria-hidden="true">·</span>
        <router-link to="/blog/privacy">隐私政策</router-link>
      </div>
    </section>
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { getSiteProfile, getSiteStats } from '@/api/blog'

const loadingProfile = ref(true)
const profile = reactive({
  siteName: '',
  slogan: '',
  description: '',
  bio: '',
  aboutContent: '',
  contactEmail: '',
  githubUrl: '',
  foundedAt: '',
})
const stats = reactive({
  articleCount: 0,
  authorCount: 0,
  categoryCount: 0,
  totalViews: 0,
})

const defaults = {
  siteName: '码上记',
  slogan: '记录实践，分享知识',
  description: '码上记是一个面向开发者的技术学习与知识分享平台，专注于系统化教程、开发实践和项目经验沉淀。',
  aboutContent: '在这里，我们整理可靠的技术知识、真实的开发经验和可复用的解决方案，帮助学习过程更清晰、更连贯。',
}

const siteName = computed(() => profile.siteName || defaults.siteName)
const slogan = computed(() => profile.slogan || defaults.slogan)
const description = computed(() => profile.description || defaults.description)
const renderedAboutContent = computed(() => {
  const source = profile.aboutContent || profile.bio || defaults.aboutContent
  return DOMPurify.sanitize(marked(source), { ADD_ATTR: ['class'] })
})
const foundedYear = computed(() => profile.foundedAt?.slice(0, 4) || '')
const hasPublicContact = computed(() => Boolean(profile.contactEmail || profile.githubUrl || profile.foundedAt))
const statItems = computed(() => [
  { label: '公开文章', value: formatNumber(stats.articleCount) },
  { label: '入驻作者', value: formatNumber(stats.authorCount) },
  { label: '技术分类', value: formatNumber(stats.categoryCount) },
  { label: '累计阅读', value: formatNumber(stats.totalViews) },
])

const features = [
  { number: '01', title: '系统学习', description: '按照分类和学习顺序阅读系列文章，建立完整的知识脉络。' },
  { number: '02', title: '发现作者', description: '认识不同技术方向的创作者，了解他们的实践经验与思考。' },
  { number: '03', title: '参与交流', description: '通过评论和收藏记录有价值的内容，与作者持续交流。' },
  { number: '04', title: '分享经验', description: '申请成为作者，把解决问题的过程沉淀成能帮助他人的文章。' },
]

const principles = [
  '注重原创与真实实践，明确区分经验、观点和事实。',
  '示例尽量完整可运行，让读者能够验证并继续探索。',
  '持续维护和修正已有内容，减少过期信息带来的困扰。',
  '尊重版权与引用来源，共同维护友善、专业的交流环境。',
]

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function loadSiteProfile() {
  try {
    const response = await getSiteProfile()
    Object.assign(profile, response.data || {})
  } catch {
    Object.assign(profile, {
      siteName: '',
      slogan: '',
      description: '',
      bio: '',
      aboutContent: '',
      contactEmail: '',
      githubUrl: '',
      foundedAt: '',
    })
  } finally {
    loadingProfile.value = false
  }
}

async function loadSiteStats() {
  try {
    const response = await getSiteStats()
    Object.assign(stats, response.data || {})
  } catch {
    Object.assign(stats, { articleCount: 0, authorCount: 0, categoryCount: 0, totalViews: 0 })
  }
}

onMounted(() => {
  loadSiteProfile()
  loadSiteStats()
})
</script>

<style scoped>
.about-page { display: flex; flex-direction: column; gap: 20px; color: var(--blog-color-text-secondary); }
.about-hero,
.about-section,
.about-footer { padding: 38px 42px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface); }
.about-hero {
  position: relative;
  overflow: hidden;
  background: radial-gradient(circle at 88% 20%, rgba(51, 126, 204, 0.14), transparent 28%), linear-gradient(135deg, #ffffff 0%, #f6faff 100%);
}
.about-hero::after {
  position: absolute; right: 42px; bottom: -52px; width: 160px; height: 160px;
  border: 24px solid rgba(51, 126, 204, 0.06); border-radius: 50%; content: '';
}
.about-eyebrow,
.section-index { margin: 0; color: var(--blog-color-primary); font-size: 12px; font-weight: 700; letter-spacing: 0.12em; }
.about-hero h1 { margin: 10px 0 8px; color: var(--blog-color-text); font-size: clamp(30px, 4vw, 46px); line-height: 1.2; }
.about-slogan { margin: 0 0 15px; color: var(--blog-color-primary); font-size: 18px; font-weight: 650; }
.about-lead { position: relative; z-index: 1; max-width: 720px; margin: 0; font-size: 16px; line-height: 1.9; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.stat-card { padding: 24px 18px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface); text-align: center; }
.stat-card strong { display: block; color: var(--blog-color-primary); font-size: 26px; font-variant-numeric: tabular-nums; }
.stat-card span { display: block; margin-top: 5px; color: var(--blog-color-text-muted); font-size: 13px; }
.section-heading { margin-bottom: 26px; display: grid; grid-template-columns: 46px minmax(0, 1fr); gap: 10px; }
.section-heading h2,
.about-footer h2 { margin: 0 0 7px; color: var(--blog-color-text); font-size: 22px; }
.section-heading p:not(.section-index),
.about-footer p { margin: 0; color: var(--blog-color-text-muted); line-height: 1.7; }
.about-content { padding: 22px 24px; border-left: 4px solid var(--blog-color-primary); border-radius: 0 var(--blog-radius-button) var(--blog-radius-button) 0; background: var(--blog-color-primary-soft); line-height: 1.9; }
.about-content :deep(> :first-child) { margin-top: 0; }
.about-content :deep(> :last-child) { margin-bottom: 0; }
.about-content :deep(a) { color: var(--blog-color-primary); }
.about-content :deep(img) { max-width: 100%; border-radius: var(--blog-radius-button); }
.profile-status { margin: 0; color: var(--blog-color-text-muted); }
.feature-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.feature-card { padding: 22px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-button); background: var(--blog-color-background); }
.feature-number { color: var(--blog-color-primary); font-size: 12px; font-weight: 700; }
.feature-card h3 { margin: 12px 0 8px; color: var(--blog-color-text); font-size: 17px; }
.feature-card p { margin: 0; color: var(--blog-color-text-muted); font-size: 14px; line-height: 1.75; }
.principle-list { margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 28px; list-style: none; }
.principle-list li { position: relative; padding: 12px 12px 12px 28px; line-height: 1.7; }
.principle-list li::before {
  position: absolute; top: 21px; left: 8px; width: 7px; height: 7px; border-radius: 50%;
  background: var(--blog-color-primary); box-shadow: 0 0 0 5px var(--blog-color-primary-soft); content: '';
}
.contact-links { display: flex; flex-wrap: wrap; gap: 10px; }
.contact-links a {
  padding: 9px 14px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-button);
  color: var(--blog-color-primary); background: var(--blog-color-primary-soft); text-decoration: none;
}
.about-footer { text-align: center; }
.about-actions { margin-top: 24px; display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; }
.primary-action,
.secondary-action {
  min-height: 40px; padding: 0 18px; display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-button); color: var(--blog-color-text-secondary);
  background: var(--blog-color-surface); font-weight: 600; text-decoration: none;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}
.primary-action { border-color: var(--blog-color-primary); color: #fff; background: var(--blog-color-primary); }
.primary-action:hover { background: #2d70b8; }
.secondary-action:hover { border-color: var(--blog-color-border-hover); color: var(--blog-color-primary); background: var(--blog-color-primary-soft); }
.policy-links { margin-top: 22px; display: flex; justify-content: center; gap: 8px; color: var(--blog-color-text-muted); font-size: 13px; }
.policy-links a { color: inherit; text-decoration: none; }
.policy-links a:hover { color: var(--blog-color-primary); }
@media (max-width: 768px) {
  .about-hero,
  .about-section,
  .about-footer { padding: 28px 24px; }
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .about-lead { font-size: 15px; }
  .feature-grid,
  .principle-list { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .about-page { gap: 14px; }
  .about-hero,
  .about-section,
  .about-footer { padding: 24px 18px; }
  .section-heading { grid-template-columns: 1fr; }
  .primary-action,
  .secondary-action { width: 100%; }
}
</style>
