# CSS 预处理器与工程化样式

## CSS 预处理器的需求

原生 CSS 在大型项目中面临诸多挑战：没有变量导致主题色散落各处、缺乏嵌套使选择器冗长重复、无法复用样式片段造成代码膨胀。CSS 预处理器通过引入编程语言特性，让样式开发更高效、可维护。

预处理器解决的核心问题：

- **变量**：统一管理颜色、间距、字体等设计令牌
- **嵌套**：通过层级关系组织选择器，减少重复书写
- **混入（Mixin）**：封装可复用的样式片段，支持参数化
- **函数**：执行计算、颜色转换等逻辑操作

## Sass/SCSS

Sass 是最成熟的 CSS 预处理器，有两种语法格式：缩进语法（.sass）和 SCSS（.scss）。SCSS 与 CSS 完全兼容，是主流选择。

### 变量

使用 `$` 符号定义变量：

```scss
$primary-color: #1890ff;
$font-size-base: 14px;
$spacing-unit: 8px;
$border-radius: 4px;

.button {
  background-color: $primary-color;
  font-size: $font-size-base;
  padding: $spacing-unit $spacing-unit * 2;
  border-radius: $border-radius;
}
```

变量支持作用域，块级作用域内定义的变量会覆盖外部同名变量：

```scss
$color: red;

.container {
  $color: blue;
  color: $color; // blue
}

.footer {
  color: $color; // red
}
```

使用 `!default` 可设置默认值，仅在变量未定义时生效：

```scss
$theme-color: #333 !default;
```

### 嵌套

嵌套让选择器层级清晰可见：

```scss
.nav {
  background: #fff;
  
  &__list {
    display: flex;
    list-style: none;
  }
  
  &__item {
    margin-right: 16px;
    
    &:hover {
      color: $primary-color;
    }
    
    &--active {
      font-weight: bold;
    }
  }
  
  &__link {
    text-decoration: none;
    
    &::after {
      content: '';
      display: block;
    }
  }
}
```

编译后生成扁平的选择器：

```css
.nav { background: #fff; }
.nav__list { display: flex; list-style: none; }
.nav__item { margin-right: 16px; }
.nav__item:hover { color: #1890ff; }
.nav__item--active { font-weight: bold; }
.nav__link { text-decoration: none; }
.nav__link::after { content: ''; display: block; }
```

### @mixin 与 @include

`@mixin` 定义可复用的样式块，`@include` 引入：

```scss
@mixin flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

@mixin text-ellipsis($lines: 1) {
  overflow: hidden;
  @if $lines == 1 {
    text-overflow: ellipsis;
    white-space: nowrap;
  } @else {
    display: -webkit-box;
    -webkit-line-clamp: $lines;
    -webkit-box-orient: vertical;
  }
}

.card {
  @include flex-center;
  
  &__title {
    @include text-ellipsis;
  }
  
  &__desc {
    @include text-ellipsis(3);
  }
}
```

使用 `@content` 接收代码块：

```scss
@mixin respond-to($breakpoint) {
  @if $breakpoint == tablet {
    @media (max-width: 768px) { @content; }
  } @else if $breakpoint == mobile {
    @media (max-width: 480px) { @content; }
  }
}

.sidebar {
  width: 300px;
  
  @include respond-to(tablet) {
    width: 100%;
  }
}
```

### @extend

`@extend` 实现选择器继承，共享相同的声明：

```scss
%message-base {
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 14px;
}

.success {
  @extend %message-base;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.error {
  @extend %message-base;
  background: #fff2f0;
  border: 1px solid #ffccc7;
}
```

`%` 占位符选择器不会被编译输出，只有被 `@extend` 时才生成。

### @function

函数用于计算并返回值：

```scss
@function strip-unit($value) {
  @return $value / ($value * 0 + 1);
}

@function to-rem($size, $base: 16) {
  @return ($size / $base) * 1rem;
}

.title {
  font-size: to-rem(24);
  margin-bottom: to-rem(16);
}

@function color-opacity($color, $opacity) {
  @return rgba($color, $opacity);
}

.overlay {
  background: color-opacity(#000, 0.5);
}
```

### Partials 与 @use

使用 `_` 前缀命名局部文件，不会被单独编译：

```scss
// _variables.scss
$primary: #1890ff;
$success: #52c41a;

// _mixins.scss
@mixin clearfix {
  &::after {
    content: '';
    display: table;
    clear: both;
  }
}

// main.scss
@use 'variables' as vars;
@use 'mixins';

.container {
  color: vars.$primary;
  @include mixins.clearfix;
}
```

`@use` 替代了旧的 `@import`，提供了模块化和命名空间，避免全局污染。

## Less

Less 语法与 Sass 类似，但使用 `@` 定义变量：

```less
@primary-color: #1890ff;
@spacing: 8px;

// 嵌套
.header {
  background: @primary-color;
  
  &__title {
    font-size: 20px;
  }
}

// mixin（无需 @mixin 关键字）
.flex-center() {
  display: flex;
  justify-content: center;
  align-items: center;
}

.center-box {
  .flex-center();
}

// 函数
@base-width: 10%;
.container {
  width: @base-width * 2;  // 20%
  color: lighten(@primary-color, 20%);
}
```

Less 的 mixin 可以带参数、设置默认值：

```scss
.border(@width: 1px, @color: #ddd, @style: solid) {
  border: @width @style @color;
}

.box {
  .border();
}

.highlight {
  .border(2px, @primary-color);
}
```

## CSS Modules

CSS Modules 在构建时将类名转换为唯一哈希，实现真正的局部作用域。

### 基本用法

```css
/* styles.module.css */
.container {
  max-width: 1200px;
  margin: 0 auto;
}

.title {
  font-size: 24px;
  color: #333;
}
```

```js
import styles from './styles.module.css';

// styles.container => "styles_container__a1b2c"
// styles.title => "styles_title__d3e4f"
element.className = styles.container;
```

### Vue scoped 原理

Vue 的 `<style scoped>` 通过 PostCSS 转换实现类似效果：

```vue
<template>
  <div class="container">
    <h1 class="title">Hello</h1>
  </div>
</template>

<style scoped>
.container { padding: 20px; }
.title { color: blue; }
</style>
```

编译后：

```html
<div class="container" data-v-7ba5bd90>
  <h1 class="title" data-v-7ba5bd90>Hello</h1>
</div>
```

```css
.container[data-v-7ba5bd90] { padding: 20px; }
.title[data-v-7ba5bd90] { color: blue; }
```

Vue 为每个组件生成唯一的属性选择器，样式自动限定在组件内。深度选择器 `:deep()` 可穿透子组件：

```scss
.parent {
  :deep(.child-component) {
    color: red;
  }
}
```

## CSS-in-JS 简介

CSS-in-JS 将样式写在 JavaScript 中，利用 JS 的能力实现动态样式。

### styled-components

```jsx
import styled from 'styled-components';

const Button = styled.button`
  background: ${props => props.primary ? '#1890ff' : '#fff'};
  color: ${props => props.primary ? '#fff' : '#333'};
  border: 1px solid ${props => props.primary ? '#1890ff' : '#ddd'};
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  
  &:hover {
    opacity: 0.8;
  }
`;

// 继承样式
const LargeButton = styled(Button)`
  padding: 12px 24px;
  font-size: 16px;
`;

// 使用
<Button primary>提交</Button>
<LargeButton>取消</LargeButton>
```

### emotion

```jsx
/** @jsxImportSource @emotion/react */
import { css } from '@emotion/react';

const cardStyle = css`
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px;
`;

const titleStyle = (color) => css`
  color: ${color};
  font-size: 18px;
  margin-bottom: 8px;
`;

<div css={cardStyle}>
  <h2 css={titleStyle('#1890ff')}>标题</h2>
</div>
```

CSS-in-JS 的优势在于动态样式、作用域隔离和与 JS 的深度集成，但会增加运行时开销。

## PostCSS

PostCSS 是 CSS 的转换工具，通过插件生态实现各种功能。

### autoprefixer

自动添加浏览器前缀：

```css
/* 输入 */
.user-select {
  user-select: none;
  display: flex;
}

/* 输出 */
.user-select {
  -webkit-user-select: none;
  -moz-user-select: none;
  user-select: none;
  display: -webkit-flex;
  display: flex;
}
```

配置 `browserslist` 控制目标浏览器：

```json
{
  "browserslist": ["> 1%", "last 2 versions", "not dead"]
}
```

### postcss-preset-env

将现代 CSS 语法转换为兼容语法：

```css
/* 输入 */
:root {
  --main-color: #06c;
}

.card {
  color: var(--main-color);
  & .title {
    font-weight: bold;
  }
}

/* 输出（兼容旧浏览器） */
.card {
  color: #06c;
}
.card .title {
  font-weight: bold;
}
```

### CSS 压缩

使用 `cssnano` 压缩 CSS：

```js
// postcss.config.js
module.exports = {
  plugins: [
    require('autoprefixer'),
    require('cssnano')({
      preset: ['default', {
        discardComments: { removeAll: true },
        normalizeWhitespace: true
      }]
    })
  ]
};
```

## Tailwind CSS

Tailwind CSS 是原子化 CSS 框架，提供大量工具类直接在 HTML 中使用。

### 原子化 CSS

每个类只负责一个样式属性：

```html
<div class="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-md">
  <h2 class="text-2xl font-bold text-gray-800 mb-4">文章标题</h2>
  <p class="text-gray-600 leading-relaxed mb-6">
    这是一段描述文字，使用 Tailwind 的工具类控制样式。
  </p>
  <button class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors">
    阅读更多
  </button>
</div>
```

### @apply

在 CSS 中组合工具类：

```css
.btn {
  @apply px-4 py-2 rounded font-medium transition-colors;
}

.btn-primary {
  @apply bg-blue-500 text-white hover:bg-blue-600;
}

.btn-outline {
  @apply border border-gray-300 text-gray-700 hover:bg-gray-50;
}
```

### 配置与 JIT

`tailwind.config.js` 自定义设计系统：

```js
module.exports = {
  content: ['./src/**/*.{html,js,vue}'],
  theme: {
    extend: {
      colors: {
        primary: '#1890ff',
        success: '#52c41a',
      },
      spacing: {
        '128': '32rem',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
    require('@tailwindcss/forms'),
  ],
};
```

JIT（Just-In-Time）模式按需生成样式，支持任意值：

```html
<div class="top-[117px] grid-cols-[200px_1fr] bg-[#1da57a]">
  任意值
</div>
```

## BEM 命名规范

BEM（Block__Element--Modifier）提供清晰的类名结构：

```scss
// Block：独立的功能组件
.card {
  background: #fff;
  border-radius: 8px;
  
  // Element：Block 的组成部分
  &__header {
    padding: 16px;
    border-bottom: 1px solid #eee;
  }
  
  &__title {
    font-size: 18px;
    font-weight: bold;
  }
  
  &__body {
    padding: 16px;
  }
  
  &__footer {
    padding: 12px 16px;
    text-align: right;
  }
  
  // Modifier：变体状态
  &--primary {
    border-left: 4px solid #1890ff;
  }
  
  &--loading {
    opacity: 0.6;
    pointer-events: none;
  }
}
```

BEM 命名避免选择器嵌套，提升可读性：

```html
<div class="card card--primary">
  <div class="card__header">
    <h3 class="card__title">标题</h3>
  </div>
  <div class="card__body">内容</div>
  <div class="card__footer">
    <button class="card__button card__button--submit">提交</button>
  </div>
</div>
```

## CSS 变量（Custom Properties）

原生 CSS 变量无需预处理器，支持动态修改：

### :root 定义与 var() 使用

```css
:root {
  --color-primary: #1890ff;
  --color-success: #52c41a;
  --color-warning: #faad14;
  --color-error: #ff4d4f;
  
  --font-size-sm: 12px;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.button {
  background: var(--color-primary);
  font-size: var(--font-size-base);
  padding: var(--spacing-sm) var(--spacing-md);
  box-shadow: var(--shadow-sm);
}

.alert--success {
  background: var(--color-success);
}

.alert--error {
  background: var(--color-error);
}
```

### 动态主题

```css
/* 浅色主题 */
[data-theme="light"] {
  --bg-primary: #ffffff;
  --bg-secondary: #f5f5f5;
  --text-primary: #333333;
  --text-secondary: #666666;
  --border-color: #e8e8e8;
}

/* 深色主题 */
[data-theme="dark"] {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2d2d2d;
  --text-primary: #e0e0e0;
  --text-secondary: #a0a0a0;
  --border-color: #404040;
}

body {
  background: var(--bg-primary);
  color: var(--text-primary);
}

.card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}
```

JavaScript 切换主题：

```js
function toggleTheme() {
  const html = document.documentElement;
  const current = html.getAttribute('data-theme');
  html.setAttribute('data-theme', current === 'dark' ? 'light' : 'dark');
}
```

CSS 变量还支持计算：

```css
.grid {
  --columns: 3;
  --gap: 16px;
  
  display: grid;
  grid-template-columns: repeat(var(--columns), 1fr);
  gap: var(--gap);
}

.grid-item {
  /* 使用 calc 结合变量 */
  width: calc((100% - var(--gap) * (var(--columns) - 1)) / var(--columns));
}
```

## 样式方案选择指南

不同方案适用于不同场景，以下是对比分析：

| 方案 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| Sass/Less | 中大型项目、设计系统 | 成熟稳定、功能丰富、团队熟悉度高 | 需要构建步骤、可能过度嵌套 |
| CSS Modules | 组件化项目、React/Vue | 真正的作用域隔离、零运行时 | 类名可读性差、动态样式不便 |
| Tailwind CSS | 快速开发、原型设计 | 开发效率高、包体积小、设计一致 | HTML 较长、学习曲线 |
| CSS-in-JS | 高度动态样式、组件库 | 完整 JS 能力、类型安全 | 运行时开销、SSR 复杂 |
| CSS 变量 | 主题切换、设计令牌 | 原生支持、动态修改、无构建 | 语法不够灵活、无循环/条件 |

选择建议：

1. **新项目优先考虑 Tailwind CSS**：开发效率高，配合组件化框架效果显著
2. **需要复杂主题系统用 CSS 变量**：原生支持动态切换，性能最优
3. **组件库开发用 CSS-in-JS**：便于封装和分发，支持 TypeScript 类型推导
4. **遗留项目用 Sass/Less**：迁移成本低，团队上手快
5. **关注性能用 CSS Modules**：零运行时开销，构建时优化

实际项目中，多种方案常组合使用。例如 Tailwind + CSS 变量管理设计系统，Sass 处理复杂动画，CSS Modules 实现组件样式隔离。关键是根据团队技术栈、项目规模和性能要求做出合理选择。