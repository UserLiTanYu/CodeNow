# 前端监控、错误追踪与性能度量

## 前端监控体系

前端监控是保障线上应用质量的核心基础设施，通常包含三大支柱：

- **性能监控**：页面加载速度、接口响应时间、渲染帧率等运行时性能指标
- **错误监控**：JS 运行时错误、资源加载失败、接口异常、白屏检测
- **行为监控**：用户操作路径、页面访问量（PV/UV）、功能使用率、点击热力图

一个完整的监控体系包含数据采集、上报、存储、分析、告警五个环节。前端主要负责采集和上报，后端负责存储和分析。

## Web Vitals 核心指标

Google 提出的 Web Vitals 是衡量用户体验的标准化指标体系。其中 Core Web Vitals 包含以下指标：

### LCP（Largest Contentful Paint）— 最大内容绘制

衡量页面主要内容的可见时间。目标值 ≤ 2.5 秒。

```js
import { onLCP } from "web-vitals";

onLCP((metric) => {
  console.log("LCP:", metric.value);
  // 上报到监控平台
  reportMetric("LCP", metric.value);
});
```

LCP 优化手段：服务端渲染（SSR）、预加载关键资源、优化图片加载、减少服务端响应时间（TTFB）。

### FID（First Input Delay）— 首次输入延迟

衡量用户首次交互到浏览器响应的时间。目标值 ≤ 100 毫秒。

```js
import { onFID } from "web-vitals";

onFID((metric) => {
  console.log("FID:", metric.value);
  reportMetric("FID", metric.value);
});
```

FID 优化：减少主线程阻塞、拆分长任务（Long Task）、使用 Web Worker。

### CLS（Cumulative Layout Shift）— 累积布局偏移

衡量页面元素意外移动的程度。目标值 ≤ 0.1。

```js
import { onCLS } from "web-vitals";

onCLS((metric) => {
  console.log("CLS:", metric.value);
  reportMetric("CLS", metric.value);
});
```

CLS 优化：为图片和视频指定尺寸、避免动态注入内容导致布局跳动、使用 CSS `contain` 属性。

### INP（Interaction to Next Paint）— 交互到下一帧绘制

INP 已于 2024 年 3 月正式取代 FID 成为 Core Web Vitals 指标。它衡量的是页面整个生命周期中所有交互的响应延迟，而非仅首次交互。目标值 ≤ 200 毫秒。

```js
import { onINP } from "web-vitals";

onINP((metric) => {
  console.log("INP:", metric.value);
  reportMetric("INP", metric.value);
});
```

## Performance API

浏览器原生的 Performance API 提供了高精度的性能度量能力。

### performance.timing（已废弃，推荐 Navigation Timing Level 2）

```js
// Navigation Timing Level 2
const [navigationEntry] = performance.getEntriesByType("navigation");

console.log("DNS 查询:", navigationEntry.domainLookupEnd - navigationEntry.domainLookupStart);
console.log("TCP 连接:", navigationEntry.connectEnd - navigationEntry.connectStart);
console.log("TTFB:", navigationEntry.responseStart - navigationEntry.requestStart);
console.log("DOM 解析:", navigationEntry.domInteractive - navigationEntry.responseEnd);
console.log("页面完全加载:", navigationEntry.loadEventEnd - navigationEntry.startTime);
```

### performance.mark / performance.measure

用于自定义性能埋点：

```js
// 标记开始
performance.mark("fetchData:start");

const response = await fetch("/api/articles");
const data = await response.json();

// 标记结束
performance.mark("fetchData:end");

// 测量两个标记之间的耗时
performance.measure("fetchData", "fetchData:start", "fetchData:end");

// 读取测量结果
const measure = performance.getEntriesByName("fetchData")[0];
console.log("接口耗时:", measure.duration, "ms");
```

### Resource Timing

获取所有资源加载的详细耗时：

```js
const resources = performance.getEntriesByType("resource");
resources.forEach((entry) => {
  if (entry.duration > 1000) {
    console.warn(`慢资源: ${entry.name} 耗时 ${entry.duration.toFixed(0)}ms`);
  }
});
```

## 错误捕获

### window.onerror

捕获全局 JS 运行时错误：

```js
window.onerror = (message, source, lineno, colno, error) => {
  reportError({
    type: "js-runtime",
    message,
    source,
    lineno,
    colno,
    stack: error?.stack,
  });
  return true; // 阻止默认行为
};
```

### unhandledrejection

捕获未处理的 Promise 异常：

```js
window.addEventListener("unhandledrejection", (event) => {
  reportError({
    type: "unhandled-rejection",
    reason: event.reason?.message || String(event.reason),
    stack: event.reason?.stack,
  });
});
```

### Vue errorHandler

```js
app.config.errorHandler = (err, instance, info) => {
  reportError({
    type: "vue-error",
    message: err.message,
    stack: err.stack,
    component: instance?.$options?.name,
    info,
  });
};
```

### React ErrorBoundary

```jsx
class ErrorBoundary extends React.Component {
  state = { hasError: false };

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    reportError({
      type: "react-error",
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack,
    });
  }

  render() {
    if (this.state.hasError) {
      return <h1>页面出错了</h1>;
    }
    return this.props.children;
  }
}
```

### 资源加载错误

通过 `addEventListener` 监听资源加载失败：

```js
window.addEventListener(
  "error",
  (event) => {
    if (event.target && (event.target.src || event.target.href)) {
      reportError({
        type: "resource-error",
        tag: event.target.tagName,
        url: event.target.src || event.target.href,
      });
    }
  },
  true // 捕获阶段，冒泡阶段拿不到资源加载错误
);
```

## Source Map

生产环境的代码经过压缩和混淆，错误堆栈无法直接定位到源码。Source Map 文件记录了编译前后的代码映射关系。

构建时生成 Source Map：

```js
// vite.config.js
export default defineConfig({
  build: {
    sourcemap: "hidden", // 生成但不暴露给浏览器
  },
});
```

### 生产环境上传 Source Map

将 `.map` 文件上传到监控平台后删除，避免源码泄露：

```bash
# Sentry CLI 上传
sentry-cli releases files "v1.2.0" upload-sourcemaps ./dist --url-prefix "~/static/js"

# 删除 .map 文件
find ./dist -name "*.map" -delete
```

Sentry 的 `@sentry/vite-plugin` 可以在构建时自动上传：

```js
import { sentryVitePlugin } from "@sentry/vite-plugin";

export default defineConfig({
  plugins: [
    sentryVitePlugin({
      org: "my-org",
      project: "my-project",
      authToken: process.env.SENTRY_AUTH_TOKEN,
    }),
  ],
});
```

## Sentry 集成

Sentry 是最流行的前端错误监控平台。

### 初始化

```js
import * as Sentry from "@sentry/vue";

Sentry.init({
  app,
  dsn: "https://xxx@sentry.io/123",
  environment: import.meta.env.MODE,
  release: __APP_VERSION__, // 由构建工具注入
  tracesSampleRate: 0.2, // 20% 的性能采样
  replaysSessionSampleRate: 0.1, // 10% 的 Session Replay
  replaysOnErrorSampleRate: 1.0, // 错误时 100% 录制
  integrations: [
    Sentry.browserTracingIntegration(),
    Sentry.replayIntegration(),
  ],
  beforeSend(event) {
    // 过滤无关错误
    if (event.exception?.values?.[0]?.type === "ChunkLoadError") {
      return null;
    }
    return event;
  },
});
```

### 面包屑（Breadcrumbs）

Sentry 自动记录用户操作路径，也可以手动添加：

```js
Sentry.addBreadcrumb({
  category: "navigation",
  message: "用户进入文章详情页",
  level: "info",
  data: { articleId: 123 },
});
```

### 用户反馈

捕获错误后展示反馈对话框：

```js
Sentry.showReportDialog({
  eventId: lastEventId,
  user: { email: user.email, name: user.name },
});
```

## 自定义埋点

### PV / UV 统计

```js
// 路由守卫中记录 PV
router.afterEach((to) => {
  reportEvent("page_view", {
    path: to.fullPath,
    title: document.title,
    referrer: document.referrer,
    timestamp: Date.now(),
  });
});
```

UV 统计需要用户标识，通常使用 Cookie 存储唯一 ID：

```js
function getVisitorId() {
  let id = localStorage.getItem("visitor_id");
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem("visitor_id", id);
  }
  return id;
}
```

### 点击事件埋点

通过 `data-track` 属性声明式埋点：

```html
<button data-track="article-publish" data-track-param-id="123">发布</button>
```

全局监听：

```js
document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-track]");
  if (!el) return;
  reportEvent(el.dataset.track, { ...el.dataset });
});
```

### 曝光检测

使用 IntersectionObserver 实现元素曝光埋点：

```js
function trackExposure(elements) {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const { trackId } = entry.target.dataset;
          reportEvent("exposure", { id: trackId });
          observer.unobserve(entry.target); // 只曝光一次
        }
      });
    },
    { threshold: 0.5 }
  );

  elements.forEach((el) => observer.observe(el));
}
```

## 前端日志

### 用户标识

使用 FingerprintJS 生成浏览器指纹，无需登录即可识别用户：

```js
import FingerprintJS from "@fingerprintjs/fingerprintjs";

async function getDeviceId() {
  const fp = await FingerprintJS.load();
  const result = await fp.get();
  return result.visitorId;
}
```

### 日志采集与上报

封装统一的日志工具：

```js
class Logger {
  constructor(config) {
    this.buffer = [];
    this.maxSize = config.maxSize || 50;
    this.flushInterval = config.flushInterval || 5000;

    setInterval(() => this.flush(), this.flushInterval);
  }

  log(level, message, extra = {}) {
    this.buffer.push({
      level,
      message,
      extra,
      timestamp: Date.now(),
      url: location.href,
    });

    if (this.buffer.length >= this.maxSize) {
      this.flush();
    }
  }

  flush() {
    if (this.buffer.length === 0) return;

    const logs = [...this.buffer];
    this.buffer = [];

    // 使用 sendBeacon，页面关闭时也能发送
    navigator.sendBeacon(
      "/api/logs",
      new Blob([JSON.stringify(logs)], { type: "application/json" })
    );
  }
}

const logger = new Logger({});
export default logger;
```

`navigator.sendBeacon` 比 `fetch` 更可靠——即使页面正在卸载，浏览器也会尽力发送请求。

## A/B 测试

### Feature Flag

通过服务端下发的配置控制功能的开启/关闭：

```js
class FeatureFlags {
  constructor() {
    this.flags = {};
  }

  async load() {
    const res = await fetch("/api/feature-flags");
    this.flags = await res.json();
  }

  isEnabled(flagName) {
    return this.flags[flagName] === true;
  }

  // 支持按用户 ID 分桶
  getVariant(flagName, userId) {
    const config = this.flags[flagName];
    if (!config) return "control";

    const hash = this.hashCode(userId + flagName);
    const bucket = hash % 100;

    if (bucket < config.percentage) return config.variant;
    return "control";
  }

  hashCode(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i);
      hash |= 0;
    }
    return Math.abs(hash);
  }
}

const flags = new FeatureFlags();
await flags.load();

if (flags.isEnabled("new-editor")) {
  // 使用新版编辑器
}
```

### 灰度发布

通过 Nginx 或网关按比例分流：

```nginx
split_clients "${request_id}" $variant {
  10%  canary;
  *    stable;
}

upstream canary_backend {
  server 10.0.0.1:3000;
}
upstream stable_backend {
  server 10.0.0.2:3000;
}
```

## 监控告警

### 错误率阈值告警

常见的告警规则设计：

| 指标 | 告警阈值 | 窗口期 | 说明 |
|------|---------|--------|------|
| JS 错误率 | > 1% | 5 分钟 | 错误数 / PV |
| 接口 5xx 率 | > 5% | 3 分钟 | 5xx 数 / 总请求数 |
| 白屏率 | > 0.5% | 10 分钟 | 白屏数 / PV |
| LCP P95 | > 4s | 15 分钟 | 95 分位 LCP |
| CLS P95 | > 0.25 | 15 分钟 | 95 分位 CLS |

### 性能劣化检测

对比最近两个版本的核心指标，检测性能是否劣化：

```js
async function detectRegression(currentVersion, previousVersion) {
  const current = await fetchMetrics(currentVersion);
  const previous = await fetchMetrics(previousVersion);

  const metrics = ["LCP", "FID", "CLS", "INP"];
  const regressions = [];

  for (const metric of metrics) {
    const increase =
      (current[metric].p95 - previous[metric].p95) / previous[metric].p95;
    if (increase > 0.1) {
      regressions.push({
        metric,
        previous: previous[metric].p95,
        current: current[metric].p95,
        change: `+${(increase * 100).toFixed(1)}%`,
      });
    }
  }

  if (regressions.length > 0) {
    sendAlert("性能劣化告警", regressions);
  }
}
```

告警通知可以通过企业微信、钉钉、Slack Webhook 推送，确保团队能第一时间感知线上问题。
