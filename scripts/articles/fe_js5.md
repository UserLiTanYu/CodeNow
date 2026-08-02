# 事件循环、宏任务与微任务

## JavaScript 单线程模型

JavaScript 被设计为单线程语言，同一时间只能执行一个任务。这个设计决策源于其最初的应用场景——操作 DOM。

### 为什么是单线程

```js
// 假设 JavaScript 是多线程
// 线程1：添加元素
document.body.appendChild(element);

// 线程2：删除元素
document.body.removeChild(element);

// 问题：两个线程同时操作 DOM，结果不可预测
```

单线程的优势：
- **简单性**：无需处理复杂的线程同步问题
- **安全性**：避免竞态条件和死锁
- **一致性**：DOM 操作结果可预测

单线程的挑战：
- **阻塞问题**：一个任务阻塞会导致整个程序卡顿
- **解决方案**：异步编程、事件循环

## 同步任务与异步任务

### 同步任务

同步任务在主线程上排队执行，前一个任务完成后才会执行下一个：

```js
console.log('1');
console.log('2');
console.log('3');

// 输出顺序：1, 2, 3
// 按代码书写顺序依次执行
```

### 异步任务

异步任务不会阻塞主线程，而是在条件满足时被放入任务队列：

```js
console.log('开始');

setTimeout(() => {
  console.log('异步任务');
}, 0);

console.log('结束');

// 输出顺序：开始, 结束, 异步任务
// 即使延迟为 0，异步任务也会在同步任务之后执行
```

异步任务的类型：
- **定时器**：setTimeout、setInterval
- **网络请求**：fetch、XMLHttpRequest
- **事件处理**：click、scroll、input
- **Promise**：then、catch、finally

## 事件循环（Event Loop）

事件循环是 JavaScript 处理异步任务的核心机制，它不断检查调用栈和任务队列。

### 事件循环的基本流程

```
1. 执行同步代码（调用栈）
2. 调用栈为空时，检查微任务队列
3. 执行所有微任务（直到微任务队列为空）
4. 执行一个宏任务
5. 重复步骤 2-4
```

### 简化的事件循环模型

```js
// 伪代码描述事件循环
while (true) {
  // 1. 从调用栈执行同步代码
  if (callStack.length > 0) {
    execute(callStack.pop());
    continue;
  }
  
  // 2. 执行所有微任务
  while (microtaskQueue.length > 0) {
    execute(microtaskQueue.shift());
  }
  
  // 3. 可选：执行渲染
  
  // 4. 从宏任务队列取一个任务执行
  if (macrotaskQueue.length > 0) {
    execute(macrotaskQueue.shift());
  }
}
```

## 宏任务（Macrotask）

宏任务是事件循环中每次迭代执行的任务，也称为 Task。

### 常见的宏任务来源

```js
// 1. setTimeout / setInterval
setTimeout(() => {
  console.log('setTimeout');  // 宏任务
}, 0);

setInterval(() => {
  console.log('setInterval');  // 宏任务
}, 1000);

// 2. I/O 操作
fs.readFile('file.txt', (err, data) => {
  console.log('文件读取完成');  // 宏任务
});

// 3. UI 渲染事件
button.addEventListener('click', () => {
  console.log('点击事件');  // 宏任务
});

// 4. MessageChannel
const channel = new MessageChannel();
channel.port1.onmessage = (event) => {
  console.log('消息接收');  // 宏任务
};
channel.port2.postMessage('hello');

// 5. requestAnimationFrame（特殊，见下文）
requestAnimationFrame(() => {
  console.log('动画帧');  // 渲染前回调
});
```

### setTimeout 的最小延迟

```js
// 即使设置 0ms 延迟，也不会立即执行
const start = Date.now();
setTimeout(() => {
  console.log(`实际延迟: ${Date.now() - start}ms`);
}, 0);

// 浏览器最小延迟通常为 4ms（嵌套超过 5 层后）
// HTML5 标准规定：
// - 嵌套层级 ≤ 5：最小延迟 0ms
// - 嵌套层级 > 5：最小延迟 4ms

// 递归 setTimeout 的最小延迟
let count = 0;
function recursive() {
  count++;
  if (count < 10) {
    setTimeout(recursive, 0);  // 第 6 次后变为 4ms
  }
}
recursive();
```

## 微任务（Microtask）

微任务在当前宏任务结束后、下一个宏任务开始前执行，优先级高于宏任务。

### 常见的微任务来源

```js
// 1. Promise.then / catch / finally
Promise.resolve().then(() => {
  console.log('Promise.then');  // 微任务
});

Promise.reject().catch(() => {
  console.log('Promise.catch');  // 微任务
});

Promise.resolve().finally(() => {
  console.log('Promise.finally');  // 微任务
});

// 2. MutationObserver
const observer = new MutationObserver(() => {
  console.log('DOM 变化');  // 微任务
});
observer.observe(document.body, { childList: true });

// 3. queueMicrotask
queueMicrotask(() => {
  console.log('queueMicrotask');  // 微任务
});

// 4. await 之后的代码
async function example() {
  await Promise.resolve();
  console.log('await 之后');  // 微任务
}
```

### 微任务的执行时机

```js
console.log('1');  // 同步

setTimeout(() => {
  console.log('2');  // 宏任务
}, 0);

Promise.resolve().then(() => {
  console.log('3');  // 微任务
});

console.log('4');  // 同步

// 输出顺序：1, 4, 3, 2
// 1. 执行同步代码：1, 4
// 2. 执行所有微任务：3
// 3. 执行宏任务：2
```

## 执行顺序详解

### 完整的执行流程

```js
console.log('script start');  // 1. 同步

setTimeout(() => {
  console.log('setTimeout');  // 6. 宏任务
}, 0);

Promise.resolve().then(() => {
  console.log('promise1');    // 4. 微任务
}).then(() => {
  console.log('promise2');    // 5. 微任务（链式调用）
});

console.log('script end');    // 2. 同步

// 输出顺序：
// script start
// script end
// promise1
// promise2
// setTimeout
```

### 复杂示例分析

```js
console.log('1');

setTimeout(() => {
  console.log('2');
  Promise.resolve().then(() => {
    console.log('3');
  });
}, 0);

Promise.resolve().then(() => {
  console.log('4');
  setTimeout(() => {
    console.log('5');
  }, 0);
});

console.log('6');

// 分析过程：
// 1. 同步代码：1, 6
// 2. 微任务队列：4（第一个 Promise.then）
// 3. 执行微任务：4
//    - 微任务中注册了 setTimeout，加入宏任务队列
// 4. 宏任务队列：[第一个 setTimeout, 第二个 setTimeout]
// 5. 执行第一个宏任务：
//    - 同步：2
//    - 微任务：3
// 6. 执行第二个宏任务：
//    - 同步：5

// 最终输出：1, 6, 4, 2, 3, 5
```

### async/await 的执行顺序

```js
async function async1() {
  console.log('async1 start');  // 2. 同步
  await async2();
  // 以下代码相当于：async2().then(() => { ... })
  console.log('async1 end');    // 6. 微任务
}

async function async2() {
  console.log('async2');        // 3. 同步
}

console.log('script start');    // 1. 同步

setTimeout(() => {
  console.log('setTimeout');    // 8. 宏任务
}, 0);

async1();

new Promise((resolve) => {
  console.log('promise1');      // 4. 同步
  resolve();
}).then(() => {
  console.log('promise2');      // 7. 微任务
});

console.log('script end');      // 5. 同步

// 输出顺序：
// script start
// async1 start
// async2
// promise1
// script end
// async1 end
// promise2
// setTimeout
```

## requestAnimationFrame

`requestAnimationFrame` 在浏览器重绘之前调用，通常用于动画。

### 基本用法

```js
function animate() {
  // 更新动画状态
  element.style.transform = `translateX(${position}px)`;
  position += 2;
  
  // 继续下一帧
  if (position < 500) {
    requestAnimationFrame(animate);
  }
}

// 启动动画
requestAnimationFrame(animate);
```

### 与 setTimeout 的区别

```js
// 使用 setTimeout 实现动画（不推荐）
function animateWithTimeout() {
  element.style.transform = `translateX(${position}px)`;
  position += 2;
  
  if (position < 500) {
    setTimeout(animateWithTimeout, 16);  // 约 60fps
  }
}

// 使用 requestAnimationFrame（推荐）
function animateWithRAF() {
  element.style.transform = `translateX(${position}px)`;
  position += 2;
  
  if (position < 500) {
    requestAnimationFrame(animateWithRAF);
  }
}
```

| 特性 | requestAnimationFrame | setTimeout |
|------|----------------------|------------|
| 执行时机 | 浏览器重绘前 | 宏任务队列 |
| 帧率同步 | 自动同步显示器刷新率 | 需要手动计算 |
| 后台标签页 | 暂停执行 | 继续执行 |
| 精确度 | 高（16.67ms @60Hz） | 低（受其他任务影响） |
| 电池消耗 | 低 | 高 |

### 时间戳参数

```js
let startTime = null;

function animate(timestamp) {
  if (!startTime) {
    startTime = timestamp;
  }
  
  const elapsed = timestamp - startTime;
  const progress = Math.min(elapsed / 1000, 1);  // 1秒动画
  
  element.style.opacity = progress;
  
  if (progress < 1) {
    requestAnimationFrame(animate);
  }
}

requestAnimationFrame(animate);
```

### 取消动画

```js
let animationId = null;

function startAnimation() {
  function animate() {
    // 动画逻辑
    animationId = requestAnimationFrame(animate);
  }
  
  animationId = requestAnimationFrame(animate);
}

function stopAnimation() {
  if (animationId) {
    cancelAnimationFrame(animationId);
    animationId = null;
  }
}
```

## requestIdleCallback

`requestIdleCallback` 在浏览器空闲时执行低优先级任务。

### 基本用法

```js
function lowPriorityTask(deadline) {
  // deadline.timeRemaining() 返回当前帧剩余时间
  while (deadline.timeRemaining() > 0 && tasks.length > 0) {
    const task = tasks.shift();
    processTask(task);
  }
  
  // 如果还有任务，继续请求空闲回调
  if (tasks.length > 0) {
    requestIdleCallback(lowPriorityTask);
  }
}

requestIdleCallback(lowPriorityTask);
```

### 超时设置

```js
requestIdleCallback((deadline) => {
  // 即使没有空闲时间，也会在超时后执行
  console.log('任务执行');
  console.log('剩余时间:', deadline.timeRemaining());
  console.log('是否超时:', deadline.didTimeout);
}, { timeout: 1000 });  // 1秒超时
```

### 使用场景

```js
// 1. 数据预加载
function preloadData() {
  requestIdleCallback((deadline) => {
    while (deadline.timeRemaining() > 0 && urls.length > 0) {
      const url = urls.shift();
      fetch(url).then(cacheResponse);
    }
  });
}

// 2. 日志上报
function reportLogs() {
  requestIdleCallback((deadline) => {
    while (deadline.timeRemaining() > 0 && logs.length > 0) {
      const log = logs.shift();
      sendToServer(log);
    }
  });
}

// 3. DOM 批量更新
function batchUpdate() {
  requestIdleCallback((deadline) => {
    while (deadline.timeRemaining() > 0 && updates.length > 0) {
      const update = updates.shift();
      applyUpdate(update);
    }
  });
}
```

### requestIdleCallback vs requestAnimationFrame

```js
// 高优先级：用户可见的动画
requestAnimationFrame(() => {
  element.style.transform = `translateX(${x}px)`;
});

// 低优先级：后台计算
requestIdleCallback(() => {
  const result = heavyComputation();
  cacheResult(result);
});
```

## Node.js 事件循环

Node.js 的事件循环基于 libuv，与浏览器有所不同。

### Node.js 事件循环阶段

```
   ┌───────────────────────────┐
┌─>│           timers          │
│  │     setTimeout/setInterval │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │     pending callbacks     │
│  │   I/O 回调、网络回调     │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │       idle, prepare       │
│  │       内部使用            │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │           poll            │
│  │     检索新的 I/O 事件     │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │           check           │
│  │       setImmediate        │
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │      close callbacks      │
│  │    关闭事件回调           │
│  └─────────────┴─────────────┘
```

### process.nextTick

`process.nextTick` 在当前操作完成后、事件循环继续前执行：

```js
console.log('1');

setTimeout(() => {
  console.log('2');  // 宏任务（timers 阶段）
}, 0);

setImmediate(() => {
  console.log('3');  // 宏任务（check 阶段）
});

process.nextTick(() => {
  console.log('4');  // 微任务（优先级最高）
});

Promise.resolve().then(() => {
  console.log('5');  // 微任务
});

console.log('6');

// 输出顺序：1, 6, 4, 5, 2 或 3, 3 或 2
// nextTick 优先级高于 Promise
// setTimeout 和 setImmediate 的顺序不确定
```

### process.nextTick vs Promise.then

```js
process.nextTick(() => {
  console.log('nextTick 1');
});

Promise.resolve().then(() => {
  console.log('Promise 1');
});

process.nextTick(() => {
  console.log('nextTick 2');
});

Promise.resolve().then(() => {
  console.log('Promise 2');
});

// 输出顺序：
// nextTick 1
// nextTick 2
// Promise 1
// Promise 2
// nextTick 队列优先于微任务队列
```

### setTimeout vs setImmediate

```js
// 在 I/O 回调中，setImmediate 总是先执行
const fs = require('fs');

fs.readFile(__filename, () => {
  setTimeout(() => {
    console.log('setTimeout');  // 后执行
  }, 0);
  
  setImmediate(() => {
    console.log('setImmediate');  // 先执行
  });
});

// 在主模块中，顺序不确定
setTimeout(() => {
  console.log('setTimeout');
}, 0);

setImmediate(() => {
  console.log('setImmediate');
});
// 可能是 setTimeout 先，也可能是 setImmediate 先
```

## 常见异步面试题解析

### 题目 1：基础输出顺序

```js
async function async1() {
  console.log('async1 start');
  await async2();
  console.log('async1 end');
}

async function async2() {
  console.log('async2');
}

console.log('script start');

setTimeout(function() {
  console.log('setTimeout');
}, 0);

async1();

new Promise(function(resolve) {
  console.log('promise1');
  resolve();
}).then(function() {
  console.log('promise2');
});

console.log('script end');

// 答案：
// script start
// async1 start
// async2
// promise1
// script end
// async1 end
// promise2
// setTimeout
```

### 题目 2：嵌套微任务

```js
console.log('1');

setTimeout(() => {
  console.log('2');
}, 0);

Promise.resolve().then(() => {
  console.log('3');
  Promise.resolve().then(() => {
    console.log('4');
  });
}).then(() => {
  console.log('5');
});

console.log('6');

// 分析：
// 同步：1, 6
// 微任务队列：[第一个 then]
// 执行第一个 then：3
//   - 新增微任务：[第二个 then, 内部 then]
// 执行内部 then：4
// 执行第二个 then：5
// 宏任务：2

// 答案：1, 6, 3, 4, 5, 2
```

### 题目 3：async/await 与 Promise 结合

```js
async function foo() {
  console.log('foo start');
  await bar();
  console.log('foo end');
}

async function bar() {
  console.log('bar');
}

console.log('script start');

setTimeout(() => {
  console.log('setTimeout');
}, 0);

foo();

new Promise((resolve) => {
  console.log('promise1');
  resolve();
}).then(() => {
  console.log('promise2');
});

console.log('script end');

// 答案：
// script start
// foo start
// bar
// promise1
// script end
// foo end
// promise2
// setTimeout
```

### 题目 4：综合分析

```js
const promise = new Promise((resolve, reject) => {
  console.log(1);
  resolve();
  console.log(2);
});

promise.then(() => {
  console.log(3);
});

console.log(4);

// 分析：
// Promise 构造函数是同步执行的
// resolve() 后面的代码仍会执行
// then 是微任务

// 答案：1, 2, 4, 3
```

### 题目 5：Node.js 环境

```js
setImmediate(() => {
  console.log('setImmediate');
});

setTimeout(() => {
  console.log('setTimeout');
}, 0);

process.nextTick(() => {
  console.log('nextTick');
});

Promise.resolve().then(() => {
  console.log('promise');
});

console.log('main');

// 答案：
// main
// nextTick
// promise
// setTimeout 或 setImmediate（顺序不确定）
```

## 异步编程最佳实践

### 避免阻塞主线程

```js
// 差：长时间同步操作阻塞主线程
function processData(data) {
  for (let i = 0; i < data.length; i++) {
    // 复杂计算
    heavyComputation(data[i]);
  }
}

// 好：分片处理，让出主线程
async function processDataChunked(data, chunkSize = 100) {
  for (let i = 0; i < data.length; i += chunkSize) {
    const chunk = data.slice(i, i + chunkSize);
    
    // 处理一块数据
    chunk.forEach(item => heavyComputation(item));
    
    // 让出主线程，允许其他任务执行
    await new Promise(resolve => setTimeout(resolve, 0));
  }
}

// 更好：使用 requestIdleCallback
function processDataIdle(data) {
  let index = 0;
  
  function processChunk(deadline) {
    while (deadline.timeRemaining() > 0 && index < data.length) {
      heavyComputation(data[index]);
      index++;
    }
    
    if (index < data.length) {
      requestIdleCallback(processChunk);
    }
  }
  
  requestIdleCallback(processChunk);
}
```

### Web Worker 使用场景

```js
// 主线程
const worker = new Worker('worker.js');

worker.postMessage({ data: largeArray });

worker.onmessage = (event) => {
  const result = event.data;
  console.log('处理结果:', result);
};

// worker.js
self.onmessage = (event) => {
  const { data } = event;
  
  // 在后台线程执行耗时计算
  const result = heavyComputation(data);
  
  self.postMessage(result);
};
```

Web Worker 适用场景：
- **大数据处理**：排序、过滤、聚合
- **图像处理**：像素操作、滤镜
- **加密解密**：哈希计算、加密算法
- **数据压缩**：gzip、deflate
- **复杂算法**：路径规划、物理模拟

### 任务调度策略

```js
// 使用 MessageChannel 实现任务调度
function scheduleTask(task) {
  return new Promise((resolve) => {
    const channel = new MessageChannel();
    
    channel.port1.onmessage = () => {
      const result = task();
      resolve(result);
    };
    
    channel.port2.postMessage(null);
  });
}

// 使用 requestAnimationFrame 调度动画任务
function scheduleAnimationTask(task) {
  return new Promise((resolve) => {
    requestAnimationFrame(() => {
      const result = task();
      resolve(result);
    });
  });
}

// 使用 requestIdleCallback 调度低优先级任务
function scheduleIdleTask(task, timeout = 2000) {
  return new Promise((resolve) => {
    requestIdleCallback((deadline) => {
      const result = task();
      resolve(result);
    }, { timeout });
  });
}
```

### 避免微任务队列过长

```js
// 差：创建过长的微任务队列
async function processAll(items) {
  for (const item of items) {
    await processItem(item);  // 每次 await 创建新的微任务
  }
}

// 好：批量处理
async function processBatch(items, batchSize = 100) {
  for (let i = 0; i < items.length; i += batchSize) {
    const batch = items.slice(i, i + batchSize);
    await Promise.all(batch.map(item => processItem(item)));
    
    // 让出主线程
    await new Promise(resolve => setTimeout(resolve, 0));
  }
}
```

理解事件循环机制是掌握 JavaScript 异步编程的基础。合理使用宏任务、微任务、requestAnimationFrame 和 requestIdleCallback，可以优化应用性能，避免阻塞主线程。在 Node.js 环境中，还需要了解 process.nextTick 和 setImmediate 的特性，以便做出正确的任务调度决策。