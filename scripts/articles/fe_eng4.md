# Monorepo、包管理与构建优化

## Monorepo 概念

Monorepo（单仓库多包）是一种将多个项目或模块放在同一个 Git 仓库中进行管理的策略。与之对应的是 Polyrepo，即每个项目独立一个仓库。

Monorepo 的核心优势：

- **代码共享**：公共模块无需发布到 npm，直接通过路径引用，修改即生效
- **版本一致性**：所有包共享同一份 `node_modules`，避免同一依赖出现多个版本
- **原子提交**：跨多个包的改动可以在一次 commit 中完成，保证仓库状态的一致性
- **统一工具链**：lint、test、build 配置统一管理，减少重复维护成本

典型的 Monorepo 目录结构：

```
packages/
  shared/          # 公共工具库
    package.json
    src/
  web-app/         # 前端应用
    package.json
    src/
  admin-panel/     # 管理后台
    package.json
    src/
package.json       # 根 package.json
pnpm-workspace.yaml
```

Monorepo 并非银弹，也存在一些挑战：仓库体积增长快、CI 构建时间变长、权限管理粒度粗。对于团队协作频繁、模块间耦合度高的项目，Monorepo 是更优的选择。

## pnpm workspace

pnpm 是目前最流行的 Monorepo 包管理器，其 workspace 功能天然支持多包管理。

创建 `pnpm-workspace.yaml` 定义工作区：

```yaml
packages:
  - "packages/*"
  - "apps/*"
  - "!**/test/**"
```

根目录的 `package.json` 中声明公共依赖：

```json
{
  "name": "my-monorepo",
  "private": true,
  "devDependencies": {
    "typescript": "^5.4.0",
    "vitest": "^2.0.0"
  }
}
```

包之间的相互引用使用 `workspace:` 协议：

```json
{
  "name": "@my-project/web-app",
  "dependencies": {
    "@my-project/shared": "workspace:*"
  }
}
```

### 依赖提升与幽灵依赖

pnpm 默认将依赖提升到根目录的 `node_modules`，但采用严格隔离策略——只有在 `package.json` 中显式声明的依赖才能被导入。

幽灵依赖（Phantom Dependencies）是指未在 `package.json` 中声明、却因为依赖提升而能直接使用的包。例如项目依赖了 `lodash`，而 `lodash` 依赖了 `lodash-es`，在 npm/yarn 中你可以直接 `import "lodash-es"`，但这是不安全的——一旦 `lodash` 不再依赖 `lodash-es`，代码就会报错。

pnpm 通过 `node_modules/.pnpm` 目录结构天然解决了幽灵依赖问题。如果确实需要提升某些依赖，可以在 `.npmrc` 中配置：

```ini
public-hoist-pattern[]=*eslint*
public-hoist-pattern[]=*prettier*
```

常用 pnpm 命令：

```bash
# 安装所有依赖
pnpm install

# 在指定包中运行命令
pnpm --filter @my-project/web-app dev

# 运行所有包的 build 脚本（拓扑排序）
pnpm -r run build

# 只运行有改动的包的测试
pnpm -r --changed-since=origin/main run test
```

## Turborepo

Turborepo 是 Vercel 推出的 Monorepo 构建编排工具，核心能力是任务编排、缓存和增量构建。

`turbo.json` 配置示例：

```json
{
  "$schema": "https://turbo.build/schema.json",
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "test": {
      "dependsOn": ["build"],
      "outputs": []
    },
    "lint": {
      "outputs": []
    },
    "dev": {
      "cache": false,
      "persistent": true
    }
  }
}
```

`dependsOn` 中的 `^build` 表示先构建当前包的所有依赖包（拓扑排序），确保依赖包是最新产物。

### 远程缓存

Turborepo 的杀手级特性是远程缓存。本地构建一次后，CI 环境可以直接复用缓存，无需重复构建：

```bash
# 登录 Turborepo 远程缓存
npx turbo login

# 链接远程缓存
npx turbo link

# 正常构建（自动上传/下载缓存）
pnpm turbo run build
```

缓存命中时，构建时间可以从分钟级降到秒级。配合 GitHub Actions 使用效果显著：

```yaml
- name: Build
  run: pnpm turbo run build
  env:
    TURBO_TOKEN: ${{ secrets.TURBO_TOKEN }}
    TURBO_TEAM: ${{ secrets.TURBO_TEAM }}
```

## Lerna / Nx 简介与对比

### Lerna

Lerna 是最早的 Monorepo 管理工具，核心功能是版本管理和包发布。Lerna v7+ 已被 Nx 团队接管，底层可以使用 Nx 做任务编排。

```json
{
  "packages": ["packages/*"],
  "version": "independent",
  "npmClient": "pnpm",
  "command": {
    "publish": {
      "conventionalCommits": true
    }
  }
}
```

### Nx

Nx 是功能最全面的 Monorepo 工具，提供了任务编排、依赖图可视化、增量构建、代码生成等能力。适合大型团队和复杂项目。

```bash
# 查看依赖图
npx nx graph

# 只运行受影响的项目测试
npx nx affected --target=test
```

### 对比

| 特性 | Turborepo | Nx | Lerna |
|------|-----------|-----|-------|
| 任务编排 | 优秀 | 优秀 | 基础（依赖 Nx） |
| 远程缓存 | 内置 | 内置 | 需配置 |
| 依赖图 | 基础 | 可视化 | 基础 |
| 代码生成 | 不支持 | 支持 | 不支持 |
| 学习成本 | 低 | 中高 | 低 |
| 包发布 | 不支持 | 支持 | 核心功能 |

对于中小型项目，pnpm workspace + Turborepo 是最轻量的组合。大型企业级项目可以考虑 Nx。

## 包管理器对比

| 特性 | npm | yarn v1 | yarn Berry | pnpm |
|------|-----|---------|------------|------|
| 安装速度 | 慢 | 中 | 快 | 最快 |
| 磁盘占用 | 高 | 高 | 中 | 低（硬链接） |
| 依赖隔离 | 弱 | 弱 | 强 | 强 |
| 幽灵依赖 | 存在 | 存在 | 不存在 | 不存在 |
| Monorepo 支持 | workspaces | workspaces | workspaces | workspace |
| Lock 文件 | package-lock.json | yarn.lock | yarn.lock | pnpm-lock.yaml |
| 离线模式 | 有限 | 支持 | 支持 | 支持 |

pnpm 通过硬链接和内容寻址存储，在多个项目间共享同一份依赖文件，磁盘占用可降低 50%-70%。

## Vite 构建优化

### 代码分割

Vite 基于 Rollup 构建，天然支持代码分割。通过动态导入实现按需加载：

```js
// 路由级别代码分割
const routes = [
  {
    path: "/dashboard",
    component: () => import("./views/Dashboard.vue"),
  },
  {
    path: "/settings",
    component: () => import("./views/Settings.vue"),
  },
];
```

手动配置分包策略：

```js
// vite.config.js
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          "vue-vendor": ["vue", "vue-router", "pinia"],
          "element-plus": ["element-plus"],
          echarts: ["echarts"],
        },
      },
    },
  },
});
```

### Tree Shaking

Vite 在生产构建时自动启用 Tree Shaking。确保使用 ES Module 格式的依赖，并在 `package.json` 中正确设置 `sideEffects`：

```json
{
  "sideEffects": ["*.css", "*.vue"]
}
```

避免以下写法，它们会阻止 Tree Shaking：

```js
// 错误：修改原型会标记为有副作用
Array.prototype.myMethod = function () {};

// 错误：导入后未使用但有副作用
import "./polyfill.js";
```

### 动态导入与预加载

使用魔法注释为关键路由添加预加载：

```js
const Dashboard = () =>
  import(/* webpackPreload: true */ "./views/Dashboard.vue");
const HeavyChart = () =>
  import(/* webpackPrefetch: true */ "./views/HeavyChart.vue");
```

## Webpack 迁移 Vite

从 Webpack 迁移到 Vite 常见问题及解决方案：

**1. `require` 语法不兼容**

Vite 基于 ESM，不支持 CommonJS 的 `require`。需要替换为 `import`：

```js
// 迁移前
const config = require("./config");

// 迁移后
import config from "./config";
```

对于第三方库中的 `require`，可以使用 `vite-plugin-commonjs` 插件过渡。

**2. 环境变量差异**

Webpack 使用 `process.env`，Vite 使用 `import.meta.env`：

```js
// Webpack
const apiUrl = process.env.VUE_APP_API_URL;

// Vite
const apiUrl = import.meta.env.VITE_API_URL;
```

**3. 别名配置**

```js
// vite.config.js
import { resolve } from "path";

export default defineConfig({
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
    },
  },
});
```

**4. CSS 预处理器**

Vite 内置支持 Sass/Less/Stylus，只需安装对应依赖：

```bash
pnpm add -D sass
```

无需额外配置 loader。

## 资源优化

### 图片压缩

使用 `vite-plugin-imagemin` 在构建时自动压缩图片：

```js
import viteImagemin from "vite-plugin-imagemin";

export default defineConfig({
  plugins: [
    viteImagemin({
      gifsicle: { optimizationLevel: 7 },
      optipng: { optimizationLevel: 7 },
      mozjpeg: { quality: 80 },
      pngquant: { quality: [0.8, 0.9] },
      svgo: {
        plugins: [{ name: "removeViewBox" }, { name: "removeEmptyAttrs" }],
      },
    }),
  ],
});
```

使用 Sharp 进行 Node.js 端的图片处理：

```js
import sharp from "sharp";

await sharp("input.png")
  .resize(800, 600, { fit: "inside" })
  .webp({ quality: 80 })
  .toFile("output.webp");
```

### 字体子集化

中文动辄几 MB 的字体文件严重影响首屏加载。使用 `font-spider` 或 `cn-font-split` 进行子集化：

```bash
npx cn-font-split --input ./src/assets/fonts/SourceHanSansCN.ttf
```

### SVG 图标方案

推荐使用 `vite-plugin-svg-icons` 实现 SVG Sprite：

```js
import { createSvgIconsPlugin } from "vite-plugin-svg-icons";

export default defineConfig({
  plugins: [
    createSvgIconsPlugin({
      iconDirs: [resolve(__dirname, "src/icons/svg")],
      symbolId: "icon-[name]",
    }),
  ],
});
```

页面中使用：

```html
<svg>
  <use xlink:href="#icon-home" />
</svg>
```

## Bundle 分析

### rollup-plugin-visualizer

```js
import { visualizer } from "rollup-plugin-visualizer";

export default defineConfig({
  plugins: [
    visualizer({
      open: true,
      filename: "stats.html",
      gzipSize: true,
    }),
  ],
});
```

构建后会生成交互式的依赖体积分析页面，可以直观看到各模块占比。

### source-map-explorer

```bash
npx source-map-explorer dist/assets/*.js --no-border-checks
```

重点关注：

- 是否有重复依赖被打入 bundle
- 是否有未使用的大型库
- node_modules 中的包是否使用了正确的 ES Module 版本

## CDN 加速

将大型第三方库通过 CDN 引入，减少构建产物体积：

```js
// vite.config.js
export default defineConfig({
  build: {
    rollupOptions: {
      external: ["vue", "vue-router", "axios", "echarts"],
      output: {
        globals: {
          vue: "Vue",
          "vue-router": "VueRouter",
          axios: "axios",
          echarts: "echarts",
        },
      },
    },
  },
});
```

在 `index.html` 中通过 CDN 引入：

```html
<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>
<script src="https://unpkg.com/vue-router@4/dist/vue-router.global.prod.js"></script>
<script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>
```

使用 `vite-plugin-cdn-import` 可以自动化这个过程：

```js
import { cdn } from "vite-plugin-cdn-import";

export default defineConfig({
  plugins: [
    cdn({
      modules: [
        { name: "vue", var: "Vue", path: "https://unpkg.com/vue@3/dist/vue.global.prod.js" },
      ],
    }),
  ],
});
```

## 构建缓存

### 本地持久化缓存

Vite 默认在 `node_modules/.vite` 目录下缓存预构建的依赖。可以通过 `cacheDir` 自定义缓存路径：

```js
export default defineConfig({
  cacheDir: ".vite-cache",
});
```

### CI 中的缓存策略

GitHub Actions 中缓存 node_modules 和构建产物：

```yaml
- name: Cache dependencies
  uses: actions/cache@v4
  with:
    path: |
      node_modules
      ~/.pnpm-store
    key: ${{ runner.os }}-pnpm-${{ hashFiles('**/pnpm-lock.yaml') }}
    restore-keys: |
      ${{ runner.os }}-pnpm-

- name: Cache build
  uses: actions/cache@v4
  with:
    path: |
      dist
      **/.vite
    key: ${{ runner.os }}-build-${{ github.sha }}
```

Turborepo 的缓存配合 CI 使用可以实现跨构建的增量编译，未改动的包直接命中缓存跳过构建，显著缩短 CI 时间。
