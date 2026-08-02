# 组件封装、主题定制与按需加载

Element Plus 开箱即用的功能已经非常丰富，但在实际项目中，直接使用原生组件往往会遇到代码重复、样式不统一、打包体积过大等问题。本文将从组件封装、主题定制、按需加载三个层面，介绍如何在 Vue 3 项目中高效地使用 Element Plus。

## 组件封装原则

### 单一职责

每个封装组件只解决一个具体问题。不要把表格、搜索、分页、导出全部塞进一个组件，而是拆分为独立的可组合单元：

```
ProTable
├── ProSearch    （搜索表单）
├── ProToolbar   （工具栏）
├── el-table     （表格本体）
└── ProPagination（分页）
```

### Props / Events / 插槽模式

封装 Element Plus 组件时，遵循 Vue 3 的标准通信模式：

| 通信方式 | 用途 | 示例 |
|---------|------|------|
| Props | 向子组件传递配置 | `size`、`disabled`、`placeholder` |
| Events | 子组件通知父组件 | `@change`、`@submit`、`@close` |
| 插槽 | 父组件自定义渲染 | `#prefix`、`#default`、`#footer` |

封装组件应该透传原生组件的属性和事件，使用 `v-bind="$attrs"` 和 `defineOptions({ inheritAttrs: false })`：

```vue
<script setup>
defineOptions({ inheritAttrs: false })
</script>

<template>
  <el-input v-bind="$attrs">
    <template v-for="(_, name) in $slots" #[name]="slotData">
      <slot :name="name" v-bind="slotData ?? {}" />
    </template>
  </el-input>
</template>
```

## 二次封装 Element Plus 组件

### 统一 API 封装

以按钮为例，封装一个统一的业务按钮组件：

```vue
<!-- ProButton.vue -->
<template>
  <el-tooltip v-if="tooltip" :content="tooltip" placement="top">
    <el-button
      :type="type"
      :size="size"
      :loading="loading"
      :disabled="disabled"
      @click="handleClick"
    >
      <el-icon v-if="icon"><component :is="icon" /></el-icon>
      <slot />
    </el-button>
  </el-tooltip>
  <el-button
    v-else
    :type="type"
    :size="size"
    :loading="loading"
    :disabled="disabled"
    @click="handleClick"
  >
    <el-icon v-if="icon"><component :is="icon" /></el-icon>
    <slot />
  </el-button>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  type: { type: String, default: 'primary' },
  size: { type: String, default: 'default' },
  icon: { type: [String, Object], default: null },
  tooltip: { type: String, default: '' },
  confirm: { type: Boolean, default: false },
  confirmText: { type: String, default: '确定执行此操作？' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

async function handleClick(e) {
  if (props.confirm) {
    try {
      await ElMessageBox.confirm(props.confirmText, '提示', {
        type: 'warning',
      })
    } catch {
      return
    }
  }
  emit('click', e)
}
</script>
```

使用时可以非常简洁：

```vue
<ProButton icon="Delete" confirm @click="handleDelete">删除</ProButton>
<ProButton type="success" tooltip="点击导出数据">导出</ProButton>
```

## 通用弹窗封装

### useDialog 组合式函数

将弹窗的打开、关闭、确认、取消逻辑封装为组合式函数：

```js
// composables/useDialog.js
export function useDialog(options = {}) {
  const visible = ref(false)
  const loading = ref(false)
  const formData = ref({})

  function open(data = {}) {
    formData.value = { ...options.defaultValues, ...data }
    visible.value = true
  }

  function close() {
    visible.value = false
    loading.value = false
    formData.value = {}
  }

  async function onConfirm(callback) {
    loading.value = true
    try {
      await callback(formData.value)
      close()
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  return {
    visible,
    loading,
    formData,
    open,
    close,
    onConfirm,
  }
}
```

配合弹窗组件使用：

```vue
<script setup>
const dialog = useDialog({
  defaultValues: { name: '', email: '', role: 'user' },
})

async function handleSubmit(data) {
  await createUserApi(data)
  ElMessage.success('创建成功')
  refreshList()
}
</script>

<template>
  <el-button @click="dialog.open()">新增用户</el-button>

  <el-dialog v-model="dialog.visible.value" title="新增用户" width="500px">
    <el-form :model="dialog.formData.value">
      <el-form-item label="姓名">
        <el-input v-model="dialog.formData.value.name" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="dialog.formData.value.email" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog.close()">取消</el-button>
      <el-button
        type="primary"
        :loading="dialog.loading.value"
        @click="dialog.onConfirm(handleSubmit)"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>
```

## 通用搜索表单封装

### 配置驱动表单生成

通过配置数组驱动表单渲染，避免重复编写模板代码：

```vue
<!-- SearchForm.vue -->
<template>
  <el-form :model="form" inline @submit.prevent="handleSearch">
    <el-form-item
      v-for="field in fields"
      :key="field.prop"
      :label="field.label"
    >
      <el-input
        v-if="field.type === 'input'"
        v-model="form[field.prop]"
        :placeholder="field.placeholder"
        clearable
      />
      <el-select
        v-else-if="field.type === 'select'"
        v-model="form[field.prop]"
        :placeholder="field.placeholder"
        clearable
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-date-picker
        v-else-if="field.type === 'daterange'"
        v-model="form[field.prop]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button
        v-if="collapsible && fields.length > defaultShow"
        link
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : '展开' }}
        <el-icon><ArrowDown v-if="!expanded" /><ArrowUp v-else /></el-icon>
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
const props = defineProps({
  fields: { type: Array, required: true },
  defaultShow: { type: Number, default: 3 },
  collapsible: { type: Boolean, default: true },
})

const emit = defineEmits(['search', 'reset'])
const expanded = ref(false)

const visibleFields = computed(() => {
  if (!props.collapsible || expanded.value) return props.fields
  return props.fields.slice(0, props.defaultShow)
})

const form = ref({})

function handleSearch() {
  emit('search', { ...form.value })
}

function handleReset() {
  form.value = {}
  emit('reset')
}
</script>
```

使用时只需定义字段配置：

```js
const searchFields = [
  { prop: 'name', label: '名称', type: 'input', placeholder: '请输入名称' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 },
    ],
  },
  { prop: 'dateRange', label: '日期', type: 'daterange' },
]
```

## Element Plus 主题定制

### CSS 变量覆盖

Element Plus 从 2.x 开始全面使用 CSS 变量，最简单的定制方式是覆盖这些变量：

```css
:root {
  --el-color-primary: #409eff;
  --el-color-primary-light-3: #79bbff;
  --el-color-primary-light-5: #a0cfff;
  --el-color-primary-light-7: #c6e2ff;
  --el-color-primary-light-9: #ecf5ff;
  --el-color-primary-dark-2: #337ecc;

  --el-border-radius-base: 4px;
  --el-border-radius-small: 2px;
  --el-border-radius-round: 20px;

  --el-font-size-base: 14px;
  --el-font-size-small: 12px;

  --el-component-size: 40px;
  --el-component-size-small: 32px;
  --el-component-size-large: 48px;
}
```

### SCSS 变量覆盖

如果需要更精细的控制，可以在项目中覆盖 SCSS 变量。创建 `element-variables.scss`：

```scss
// 覆盖主题色
@forward 'element-plus/theme-chalk/src/common/var.scss' with (
  $colors: (
    'primary': (
      'base': #5b8ff9,
    ),
    'success': (
      'base': #61ddaa,
    ),
  ),
  $border-radius: (
    'base': 6px,
  ),
);
```

然后在 `vite.config.js` 中注入：

```js
import { defineConfig } from 'vite'

export default defineConfig({
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/element-variables.scss" as *;`,
      },
    },
  },
})
```

## 暗色模式

### CSS 变量方案

Element Plus 内置了暗色模式支持，通过 `class` 切换即可：

```js
// composables/useTheme.js
export function useTheme() {
  const isDark = ref(false)

  function toggleTheme() {
    isDark.value = !isDark.value
    document.documentElement.classList.toggle('dark', isDark.value)
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  // 初始化
  onMounted(() => {
    const saved = localStorage.getItem('theme')
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    isDark.value = saved ? saved === 'dark' : prefersDark
    document.documentElement.classList.toggle('dark', isDark.value)
  })

  return { isDark, toggleTheme }
}
```

配合自定义 CSS 变量实现全局暗色适配：

```css
:root {
  --bg-color: #ffffff;
  --text-color: #303133;
  --border-color: #dcdfe6;
}

html.dark {
  --bg-color: #141414;
  --text-color: #e5eaf3;
  --border-color: #4c4d4f;
}

body {
  background-color: var(--bg-color);
  color: var(--text-color);
  transition: background-color 0.3s, color 0.3s;
}
```

## 按需导入与自动导入

### unplugin-vue-components

`unplugin-vue-components` 可以自动注册 Element Plus 组件，无需手动 import：

```bash
npm install unplugin-vue-components -D
```

```js
// vite.config.js
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: 'css', // 或 'sass' 以支持主题定制
        }),
      ],
    }),
  ],
})
```

配置后可以直接在模板中使用 `el-` 开头的组件，无需注册：

```vue
<template>
  <el-button type="primary">自动注册的按钮</el-button>
  <el-table :data="tableData">
    <el-table-column prop="name" label="名称" />
  </el-table>
</template>

<script setup>
// 不需要 import { ElButton, ElTable, ElTableColumn } from 'element-plus'
</script>
```

### unplugin-auto-import

`unplugin-auto-import` 自动导入 Vue、Vue Router、Element Plus 的 API：

```bash
npm install unplugin-auto-import -D
```

```js
// vite.config.js
import AutoImport from 'unplugin-auto-import/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    AutoImport({
      imports: ['vue', 'vue-router'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
  ],
})
```

配置后可以直接使用 `ElMessage`、`ElMessageBox` 等函数式组件：

```vue
<script setup>
// 不需要 import { ElMessage } from 'element-plus'
async function handleSubmit() {
  await submitApi()
  ElMessage.success('提交成功')
}
</script>
```

## 国际化

Element Plus 默认语言是英文，切换为中文：

```js
// main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
```

按需导入时，通过 `ConfigProvider` 设置：

```vue
<template>
  <el-config-provider :locale="zhCn">
    <App />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
```

自定义语言包覆盖：

```js
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const customLocale = {
  ...zhCn,
  el: {
    ...zhCn.el,
    table: {
      ...zhCn.el.table,
      emptyText: '暂无数据，请先添加',
    },
    pagination: {
      ...zhCn.el.pagination,
      total: '共 {total} 条记录',
    },
  },
}
```

## 图标使用

### @element-plus/icons-vue

```bash
npm install @element-plus/icons-vue
```

全局注册：

```js
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
```

模板中使用：

```vue
<el-icon><Edit /></el-icon>
<el-button :icon="Search">搜索</el-button>
```

按需导入（推荐，配合 tree-shaking）：

```vue
<script setup>
import { Edit, Delete, Search } from '@element-plus/icons-vue'
</script>
```

动态图标：

```vue
<script setup>
import * as icons from '@element-plus/icons-vue'

const iconName = ref('Edit')
const currentIcon = computed(() => icons[iconName.value])
</script>

<template>
  <el-icon><component :is="currentIcon" /></el-icon>
</template>
```

## 组件文档：VitePress 构建组件库文档

当封装了较多业务组件后，需要一个文档站来管理。VitePress 是 Vue 团队维护的文档工具，天然适合 Vue 组件文档。

安装和初始化：

```bash
npx vitepress init
```

在 `.vitepress/config.js` 中配置导航和侧边栏：

```js
export default {
  title: '业务组件库',
  themeConfig: {
    nav: [
      { text: '指南', link: '/guide/' },
      { text: '组件', link: '/components/' },
    ],
    sidebar: {
      '/components/': [
        {
          text: '通用组件',
          items: [
            { text: 'ProTable 表格', link: '/components/pro-table' },
            { text: 'ProDialog 弹窗', link: '/components/pro-dialog' },
            { text: 'ProSearch 搜索', link: '/components/pro-search' },
          ],
        },
      ],
    },
  },
}
```

在文档中嵌入组件示例，使用 `vitepress-plugin-demo` 插件：

```markdown
## 基础用法

:::demo 一个简单的表格示例

```vue
<template>
  <ProTable :request="fetchData">
    <el-table-column prop="name" label="名称" />
    <el-table-column prop="value" label="值" />
  </ProTable>
</template>
```

:::
```

如果需要更丰富的交互式文档，可以考虑 Storybook：

```bash
npx storybook@latest init
```

Storybook 支持 Controls（动态修改 props）、Actions（查看事件回调）、Docs（自动生成文档）等功能，适合需要深度调试组件行为的场景。

选择建议：如果组件以展示和使用说明为主，选 VitePress；如果需要交互式调试和视觉测试，选 Storybook。
