# CSS 动画、过渡与性能优化

## CSS 过渡（transition）

CSS 过渡让属性变化产生平滑的动画效果，是实现交互反馈的基础。

### 基本语法

```css
transition: property duration timing-function delay;
```

四个子属性：

```css
.button {
  background-color: #1890ff;
  color: #fff;
  
  /* 逐个指定 */
  transition-property: background-color, color, box-shadow;
  transition-duration: 0.3s;
  transition-timing-function: ease;
  transition-delay: 0s;
  
  /* 简写 */
  transition: background-color 0.3s ease, 
              color 0.3s ease, 
              box-shadow 0.3s ease;
  
  /* 通配所有属性 */
  transition: all 0.3s ease;
}

.button:hover {
  background-color: #40a9ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
}
```

### 缓动函数

`timing-function` 控制动画速度曲线：

```css
.linear { transition-timing-function: linear; }
.ease { transition-timing-function: ease; }
.ease-in { transition-timing-function: ease-in; }
.ease-out { transition-timing-function: ease-out; }
.ease-in-out { transition-timing-function: ease-in-out; }

/* 贝塞尔曲线自定义 */
.custom {
  transition-timing-function: cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* 阶梯函数 */
.steps {
  transition-timing-function: steps(4, end);
}
```

常用贝塞尔曲线值：

| 效果 | cubic-bezier 值 |
|------|-----------------|
| 缓出 | cubic-bezier(0, 0, 0.2, 1) |
| 缓入 | cubic-bezier(0.4, 0, 1, 1) |
| 缓入缓出 | cubic-bezier(0.4, 0, 0.2, 1) |
| 弹性 | cubic-bezier(0.68, -0.55, 0.265, 1.55) |

### 触发方式

```css
/* :hover 悬停 */
.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* :focus 聚焦 */
.input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* :active 按下 */
.button:active {
  transform: scale(0.98);
}

/* class 切换 */
.sidebar.collapsed {
  width: 64px;
}
```

## CSS 动画（@keyframes + animation）

### @keyframes 定义

```css
/* from/to 写法 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 百分比写法 */
@keyframes slideIn {
  0% {
    transform: translateX(-100%);
    opacity: 0;
  }
  60% {
    transform: translateX(10px);
    opacity: 0.8;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}
```

### animation 属性

```css
.element {
  animation-name: fadeIn;
  animation-duration: 0.5s;
  animation-timing-function: ease-out;
  animation-delay: 0.2s;
  animation-iteration-count: 1;
  animation-direction: normal;
  animation-fill-mode: forwards;
  animation-play-state: running;
  
  /* 简写 */
  animation: fadeIn 0.5s ease-out 0.2s 1 normal forwards;
}
```

### animation 子属性详解

**iteration-count** - 播放次数：

```css
.play-once { animation-iteration-count: 1; }
.play-twice { animation-iteration-count: 2; }
.play-infinite { animation-iteration-count: infinite; }
```

**direction** - 播放方向：

```css
.normal { animation-direction: normal; }        /* 正向播放 */
.reverse { animation-direction: reverse; }      /* 反向播放 */
.alternate { animation-direction: alternate; }  /* 奇数正向，偶数反向 */
.alt-reverse { animation-direction: alternate-reverse; }
```

**fill-mode** - 动画前后状态：

```css
.none { animation-fill-mode: none; }         /* 不改变默认状态 */
.forwards { animation-fill-mode: forwards; } /* 保持结束状态 */
.backwards { animation-fill-mode: backwards; } /* 应用开始状态（延迟期间） */
.both { animation-fill-mode: both; }         /* forwards + backwards */
```

## 常用动画效果

### 淡入

```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-in { animation: fadeIn 0.3s ease-out; }
.fade-in-up { animation: fadeInUp 0.4s ease-out; }
.fade-in-down { animation: fadeInDown 0.4s ease-out; }
```

### 滑入

```css
@keyframes slideInLeft {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}

@keyframes slideInRight {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

@keyframes slideInUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

/* 抽屉组件使用 */
.drawer {
  position: fixed;
  right: 0;
  top: 0;
  width: 300px;
  height: 100%;
  animation: slideInRight 0.3s ease-out;
}

.drawer-mask {
  animation: fadeIn 0.3s ease-out;
}
```

### 缩放

```css
@keyframes zoomIn {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 模态框弹出 */
.modal {
  animation: zoomIn 0.2s ease-out;
}

/* 呼吸效果 */
.notification-dot {
  animation: pulse 2s ease-in-out infinite;
}
```

### 旋转

```css
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes spinSlow {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 加载图标 */
.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #f3f3f3;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
```

### 弹跳

```css
@keyframes bounce {
  0%, 20%, 53%, 100% {
    animation-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
    transform: translateY(0);
  }
  40%, 43% {
    animation-timing-function: cubic-bezier(0.755, 0.05, 0.855, 0.06);
    transform: translateY(-30px);
  }
  70% {
    animation-timing-function: cubic-bezier(0.755, 0.05, 0.855, 0.06);
    transform: translateY(-15px);
  }
  80% {
    transition-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1);
    transform: translateY(0);
  }
  90% {
    transform: translateY(-4px);
  }
}

@keyframes bounceIn {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    transform: scale(1);
  }
}

.bounce { animation: bounce 1s; }
.bounce-in { animation: bounceIn 0.6s; }
```

## transform 变换

transform 提供 2D 和 3D 变换能力：

### 2D 变换

```css
/* 平移 */
.translate { transform: translate(50px, 100px); }
.translateX { transform: translateX(50px); }
.translateY { transform: translateY(-20px); }

/* 缩放 */
.scale { transform: scale(1.5); }
.scaleXY { transform: scale(1.2, 0.8); }

/* 旋转 */
.rotate { transform: rotate(45deg); }

/* 倾斜 */
.skew { transform: skew(10deg, 20deg); }

/* 组合变换（从右到左执行） */
.combined {
  transform: translateX(50px) rotate(45deg) scale(1.2);
}

/* 变换原点 */
.custom-origin {
  transform-origin: left top;
  transform: rotate(45deg);
}
```

### 3D 变换

```css
/* 透视 */
.container {
  perspective: 1000px;
}

/* 3D 旋转 */
.card-flip {
  transform-style: preserve-3d;
  transition: transform 0.6s;
}

.card-flip:hover {
  transform: rotateY(180deg);
}

/* 翻转卡片 */
.flip-card {
  width: 200px;
  height: 300px;
  perspective: 1000px;
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.flip-card:hover .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-card-front,
.flip-card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
}

.flip-card-back {
  transform: rotateY(180deg);
}

/* 3D 平移 */
.translate3d {
  transform: translate3d(50px, 100px, 50px);
}
```

## GPU 加速

### 硬件加速触发条件

某些 CSS 属性会触发 GPU 加速，创建独立的合成层：

```css
/* 会触发 GPU 加速的属性 */
.accelerated {
  transform: translateZ(0);
  /* 或 */
  will-change: transform;
  /* 或 */
  transform: translate3d(0, 0, 0);
}
```

触发 GPU 加速的属性：
- `transform`（3D 变换）
- `opacity`
- `filter`
- `will-change`

### will-change

`will-change` 提示浏览器元素将要变化的属性：

```css
/* 正确用法：提前告知 */
.sidebar {
  will-change: transform;
}

/* 动画结束后移除 */
.sidebar.animating {
  transform: translateX(100%);
}

/* 错误用法：不要过度使用 */
/* will-change: transform, opacity, left, top, filter; */  /* 过多 */
```

最佳实践：

```css
/* 仅在需要时添加 */
.animated-element {
  will-change: transform;
}

/* 动画结束后移除 */
.animated-element.done {
  will-change: auto;
}
```

```js
element.addEventListener('animationstart', () => {
  element.style.willChange = 'transform';
});

element.addEventListener('animationend', () => {
  element.style.willChange = 'auto';
});
```

### transform: translateZ(0) 技巧

```css
/* 强制创建合成层 */
.promote-layer {
  transform: translateZ(0);
}

/* 避免闪烁 */
.smooth-animation {
  -webkit-transform: translateZ(0);
  transform: translateZ(0);
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}
```

## 回流（Reflow）与重绘（Repaint）

### 回流

回流是元素布局的重新计算，开销最大：

触发回流的操作：
- 改变窗口大小
- 修改元素尺寸（width、height、padding、margin、border）
- 改变元素位置（position、float、display）
- 添加/删除可见 DOM 元素
- 读取布局属性（offsetWidth、offsetHeight、clientWidth、clientHeight、scrollTop、scrollLeft）

```js
// 触发回流
element.style.width = '200px';  // 写入
const width = element.offsetWidth;  // 读取（强制回流）
```

### 重绘

重绘是元素外观的重新绘制，不涉及布局：

触发重绘的操作：
- 改变颜色（color、background-color）
- 改变可见性（visibility）
- 改变轮廓（outline、box-shadow）
- 改变透明度（opacity）

```css
/* 重绘但不回流 */
.element {
  color: red;
  background: blue;
  visibility: hidden;
}
```

### 优化策略

```css
/* 使用 transform 代替 top/left */
.animated {
  /* 差：触发回流 */
  /* left: 100px; */
  
  /* 好：仅触发合成 */
  transform: translateX(100px);
}

/* 使用 opacity 代替 visibility */
.fade-out {
  /* 差：仍占据空间 */
  /* visibility: hidden; */
  
  /* 好：GPU 加速 */
  opacity: 0;
}
```

批量修改 DOM：

```js
// 差：多次回流
for (let i = 0; i < 100; i++) {
  const div = document.createElement('div');
  container.appendChild(div);
}

// 好：使用 DocumentFragment
const fragment = document.createDocumentFragment();
for (let i = 0; i < 100; i++) {
  const div = document.createElement('div');
  fragment.appendChild(div);
}
container.appendChild(fragment);

// 好：使用离线 DOM
const clone = container.cloneNode(true);
for (let i = 0; i < 100; i++) {
  const div = document.createElement('div');
  clone.appendChild(div);
}
container.parentNode.replaceChild(clone, container);
```

## CSS 性能优化

### 减少选择器嵌套

```css
/* 差：深层嵌套 */
.nav ul li a span { }

/* 好：扁平选择器 */
.nav-link-text { }

/* 差：通配选择器 */
.container * { }

/* 好：明确选择器 */
.container > .item { }
```

### 避免昂贵属性

```css
/* 差：昂贵的属性 */
.expensive {
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5),
              inset 0 0 20px rgba(0, 0, 0, 0.3);
  filter: blur(5px) grayscale(50%);
  border-radius: 50%;
}

/* 好：使用伪元素或图片替代 */
.alternative {
  position: relative;
}

.alternative::after {
  content: '';
  position: absolute;
  /* 使用图片替代复杂阴影 */
  background: url('shadow.png') no-repeat;
  pointer-events: none;
}
```

### 减少重绘区域

```css
/* 差：大面积重绘 */
.full-page-overlay {
  background: rgba(0, 0, 0, 0.5);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

/* 好：使用 isolated 属性 */
.optimized-overlay {
  background: rgba(0, 0, 0, 0.5);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  isolation: isolate; /* 创建新的层叠上下文 */
}
```

## content-visibility

`content-visibility` 允许浏览器跳过视口外元素的渲染：

```css
/* 自动判断是否渲染 */
.item {
  content-visibility: auto;
  contain-intrinsic-size: 0 500px; /* 预估高度 */
}

/* 强制跳过渲染 */
.offscreen {
  content-visibility: hidden;
}

/* 长列表优化 */
.list-item {
  content-visibility: auto;
  contain-intrinsic-size: 200px;
}
```

实际应用示例：

```html
<div class="article-list">
  <!-- 只有视口内的文章才会渲染 -->
  <article class="article-item">
    <h2>文章标题</h2>
    <p>文章内容...</p>
  </article>
  <!-- 更多文章 -->
</div>
```

```css
.article-item {
  content-visibility: auto;
  contain-intrinsic-size: 0 300px;
  contain: layout style paint;
}
```

## 选择器性能

### 匹配效率

浏览器从右到左匹配选择器，效率从高到低：

| 选择器类型 | 示例 | 匹配效率 |
|-----------|------|----------|
| ID 选择器 | #header | 最高 |
| 类选择器 | .nav-item | 高 |
| 标签选择器 | div | 中 |
| 相邻兄弟 | h2 + p | 中 |
| 子选择器 | ul > li | 中 |
| 后代选择器 | ul li | 低 |
| 通配选择器 | * | 最低 |
| 属性选择器 | [type="text"] | 低 |
| 伪类/伪元素 | :nth-child() | 低 |

```css
/* 差：从右向左匹配开销大 */
div.container ul li a { }

/* 好：直接匹配 */
.nav-link { }

/* 差：通配开销大 */
.content * { }

/* 好：明确选择器 */
.content > p { }
```

### 选择器优化原则

```css
/* 1. 避免过度限定 */
/* 差 */
div.header .nav ul li a.link { }
/* 好 */
.nav-link { }

/* 2. 避免链式选择器 */
/* 差 */
.btn.primary.large { }
/* 好 */
.btn-primary-lg { }

/* 3. 尽量使用类选择器 */
/* 差 */
#main > .content > .article > p { }
/* 好 */
.article-body { }
```

## 动画性能最佳实践

### 优先使用 transform 和 opacity

```css
/* 差：触发 layout */
.bad-animation {
  transition: width 0.3s, height 0.3s, left 0.3s, top 0.3s;
}

/* 好：仅触发 composite */
.good-animation {
  transition: transform 0.3s, opacity 0.3s;
}
```

性能影响对比：

| 属性 | Layout | Paint | Composite |
|------|--------|-------|-----------|
| left/top/width/height | ✓ | ✓ | ✓ |
| transform | ✗ | ✗ | ✓ |
| opacity | ✗ | ✗ | ✓ |
| color/background | ✗ | ✓ | ✓ |
| box-shadow | ✗ | ✓ | ✓ |

### 避免 Layout Thrashing

```js
// 差：读写交替导致强制同步布局
function badLayout() {
  for (let i = 0; i < elements.length; i++) {
    // 读取触发回流
    const width = elements[i].offsetWidth;
    // 写入再次触发回流
    elements[i].style.width = width * 2 + 'px';
  }
}

// 好：批量读取，批量写入
function goodLayout() {
  // 先批量读取
  const widths = elements.map(el => el.offsetWidth);
  
  // 再批量写入
  elements.forEach((el, i) => {
    el.style.width = widths[i] * 2 + 'px';
  });
}

// 使用 requestAnimationFrame
function animate() {
  requestAnimationFrame(() => {
    element.style.transform = `translateX(${position}px)`;
    position += 1;
    if (position < 500) {
      requestAnimationFrame(animate);
    }
  });
}
```

### 使用 CSS contain 属性

```css
/* 限制元素的影响范围 */
.isolated-component {
  contain: layout style paint;
}

/* 仅限制布局 */
.layout-isolated {
  contain: layout;
}

/* 完全独立 */
.fully-isolated {
  contain: strict; /* 等同于 size layout paint style */
}
```

### 动画帧率监控

```js
// 监控动画帧率
let lastTime = performance.now();
let frames = 0;
let fps = 0;

function measureFPS() {
  frames++;
  const currentTime = performance.now();
  
  if (currentTime >= lastTime + 1000) {
    fps = Math.round(frames * 1000 / (currentTime - lastTime));
    frames = 0;
    lastTime = currentTime;
    console.log(`FPS: ${fps}`);
  }
  
  requestAnimationFrame(measureFPS);
}

measureFPS();
```

### 离屏动画优化

```css
/* 使用 will-change 提示浏览器 */
.pre-animate {
  will-change: transform, opacity;
}

/* 动画完成后移除 */
.post-animate {
  will-change: auto;
}

/* 使用 contain 限制重绘范围 */
.animated-container {
  contain: layout style;
}
```

```js
// 使用 Web Animations API
element.animate([
  { transform: 'translateX(0)', opacity: 1 },
  { transform: 'translateX(100px)', opacity: 0 }
], {
  duration: 300,
  easing: 'ease-out',
  fill: 'forwards'
});
```

性能优化的核心原则：减少 Layout 触发、限制 Paint 范围、利用 Composite 加速。通过合理使用 transform、opacity、will-change、contain 等属性，可以实现流畅的 60fps 动画效果。