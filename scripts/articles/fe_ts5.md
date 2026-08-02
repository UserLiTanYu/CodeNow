# 装饰器与 TypeScript 编译配置

装饰器（Decorators）是一种特殊的声明，可以附加到类声明、方法、属性或参数上，用于修改它们的行为。TypeScript 从早期版本就支持实验性装饰器语法，而 TC39 的装饰器提案（Stage 3）带来了全新的标准语法。本文将全面介绍装饰器的用法、TypeScript 的编译配置，以及大型项目的工程化实践。

## 装饰器概述

装饰器本质上是一个**函数**，它接收被装饰的目标作为参数，在不修改原始代码的前提下，为目标添加额外的行为。装饰器通过 `@expression` 语法使用，其中 `expression` 必须求值为一个函数。

### 装饰器的种类

TypeScript 支持五种装饰器：

| 装饰器类型 | 装饰目标 | 参数 |
|-----------|---------|------|
| 类装饰器 | 类 | 类的构造函数 |
| 方法装饰器 | 方法 | (target, propertyKey, descriptor) |
| 属性装饰器 | 属性 | (target, propertyKey) |
| 参数装饰器 | 方法参数 | (target, propertyKey, parameterIndex) |
| 访问器装饰器 | getter/setter | (target, propertyKey, descriptor) |

## 类装饰器

类装饰器应用于类的构造函数之前，可以用来观察、修改或替换类的定义。

### 基本用法

```ts
// 最简单的类装饰器：添加额外属性
function Timestamped<T extends new (...args: any[]) => any>(constructor: T) {
  return class extends constructor {
    createdAt = new Date()
  }
}

@Timestamped
class Article {
  title: string
  constructor(title: string) {
    this.title = title
  }
}

const article = new Article('TypeScript 装饰器')
console.log(article.createdAt)  // 当前时间
```

### 装饰器工厂

装饰器工厂是一个返回装饰器函数的函数，允许我们传入配置参数：

```ts
function Sealed(constructor: Function) {
  Object.seal(constructor)
  Object.seal(constructor.prototype)
}

// 装饰器工厂：带参数
function Deprecated(message: string) {
  return function (constructor: Function) {
    console.warn(`${constructor.name} 已废弃：${message}`)
  }
}

@Deprecated('请使用 NewApi 代替')
class OldApi {
  // ...
}
```

## 方法装饰器

方法装饰器在方法声明之前被声明，用于拦截方法调用、修改方法行为。

### 基本签名

```ts
function Log(
  target: any,           // 对于静态成员是类的构造函数，对于实例成员是类的原型
  propertyKey: string,   // 方法名
  descriptor: PropertyDescriptor  // 属性描述符
) {
  const originalMethod = descriptor.value
  
  descriptor.value = function (...args: any[]) {
    console.log(`调用 ${propertyKey}，参数：`, args)
    const result = originalMethod.apply(this, args)
    console.log(`${propertyKey} 返回：`, result)
    return result
  }
}
```

### 日志装饰器

```ts
class Calculator {
  @Log
  add(a: number, b: number): number {
    return a + b
  }

  @Log
  multiply(a: number, b: number): number {
    return a * b
  }
}

const calc = new Calculator()
calc.add(1, 2)
// 输出：
// 调用 add，参数：[1, 2]
// add 返回：3
```

### 缓存装饰器

```ts
function Memoize(
  target: any,
  propertyKey: string,
  descriptor: PropertyDescriptor
) {
  const originalMethod = descriptor.value
  const cache = new Map<string, any>()

  descriptor.value = function (...args: any[]) {
    const key = JSON.stringify(args)
    if (cache.has(key)) {
      return cache.get(key)
    }
    const result = originalMethod.apply(this, args)
    cache.set(key, result)
    return result
  }
}

class MathUtils {
  @Memoize
  fibonacci(n: number): number {
    if (n <= 1) return n
    return this.fibonacci(n - 1) + this.fibonacci(n - 2)
  }
}
```

### 权限检查装饰器

```ts
function RequireAuth(role: string) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value

    descriptor.value = function (...args: any[]) {
      const user = getCurrentUser()
      if (!user || user.role !== role) {
        throw new Error(`需要 ${role} 权限才能访问 ${propertyKey}`)
      }
      return originalMethod.apply(this, args)
    }
  }
}

class AdminPanel {
  @RequireAuth('admin')
  deleteUser(userId: string) {
    // 删除用户逻辑
  }

  @RequireAuth('editor')
  editArticle(articleId: string, content: string) {
    // 编辑文章逻辑
  }
}
```

## 属性装饰器与参数装饰器

### 属性装饰器

属性装饰器接收两个参数：target 和 propertyKey。它不能直接修改属性定义，但可以用来定义元数据：

```ts
function Range(min: number, max: number) {
  return function (target: any, propertyKey: string) {
    let value: number
    
    const getter = () => value
    const setter = (newVal: number) => {
      if (newVal < min || newVal > max) {
        throw new Error(`${propertyKey} 必须在 ${min} 到 ${max} 之间`)
      }
      value = newVal
    }

    Object.defineProperty(target, propertyKey, {
      get: getter,
      set: setter,
      enumerable: true,
      configurable: true,
    })
  }
}

class Student {
  @Range(0, 150)
  age: number

  @Range(0, 100)
  score: number
}

const student = new Student()
student.age = 25     // 正常
student.age = -5     // 抛出错误：age 必须在 0 到 150 之间
```

### 参数装饰器

参数装饰器用于为方法参数添加元数据，通常与方法装饰器配合使用：

```ts
function Validate(
  target: any,
  propertyKey: string,
  parameterIndex: number
) {
  const existingParams: number[] = Reflect.getOwnMetadata('validate', target, propertyKey) || []
  existingParams.push(parameterIndex)
  Reflect.defineMetadata('validate', existingParams, target, propertyKey)
}

class UserService {
  createUser(@Validate name: string, @Validate email: string, age?: number) {
    // 创建用户逻辑
  }
}
```

## 装饰器执行顺序

装饰器的执行顺序遵循明确的规则：**由外向内求值，由内向外执行**。

```ts
function First() {
  console.log('First 求值')
  return function (target: any) {
    console.log('First 执行')
  }
}

function Second() {
  console.log('Second 求值')
  return function (target: any) {
    console.log('Second 执行')
  }
}

@First()
@Second()
class MyClass {}
// 输出顺序：
// First 求值
// Second 求值
// Second 执行
// First 执行
```

对于类中不同成员的装饰器，执行顺序为：

1. 实例方法/属性的参数装饰器（按参数顺序）
2. 实例方法/属性的方法装饰器
3. 静态方法/属性的参数装饰器
4. 静态方法/属性的方法装饰器
5. 类装饰器

```ts
class Example {
  @MethodDecorator
  method(@ParamDecorator param: string) {}
  
  @StaticDecorator
  static staticMethod() {}
  
  @PropertyDecorator
  prop: string
}
// 执行顺序：ParamDecorator → MethodDecorator → PropertyDecorator → StaticDecorator
```

## reflect-metadata

`reflect-metadata` 是一个实验性的库，提供了一套元数据反射 API，让装饰器能够存储和读取元数据。

### 安装与配置

```bash
npm install reflect-metadata
```

```json
// tsconfig.json
{
  "compilerOptions": {
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true
  }
}
```

### 使用元数据

```ts
import 'reflect-metadata'

// 定义元数据
function Inject(token: string) {
  return function (target: any, propertyKey: string) {
    const type = Reflect.getMetadata('design:type', target, propertyKey)
    console.log(`${propertyKey} 的类型：`, type.name)
    Reflect.defineMetadata('inject', token, target, propertyKey)
  }
}

class Container {
  private services = new Map<string, any>()

  register<T>(token: string, instance: T) {
    this.services.set(token, instance)
  }

  resolve<T>(token: string): T {
    return this.services.get(token)
  }
}

// 自动注入示例
class UserController {
  @Inject('UserService')
  private userService: any
}
```

## TypeScript 装饰器 vs ES Decorator 提案

TC39 的装饰器提案（Stage 3）与 TypeScript 的实验性装饰器有显著差异：

| 特性 | TypeScript 实验性装饰器 | ES Decorator (Stage 3) |
|------|----------------------|----------------------|
| 标准状态 | 非标准，实验性 | TC39 Stage 3 标准 |
| 参数数量 | 2-3 个（取决于装饰目标） | 统一 2 个（target, context） |
| 类装饰器参数 | (constructor) | (target, context) |
| 访问器 | 不支持独立装饰器 | 支持 |
| 自动访问器 | 不支持 | 支持 `accessor` 关键字 |
| 元数据 API | 需要 reflect-metadata | 内置 `Symbol.metadata` |

### ES Decorator 语法示例

```ts
// ES Decorator 的类装饰器
function logged<T extends new (...args: any[]) => any>(
  target: T,
  context: ClassDecoratorContext
) {
  return class extends target {
    constructor(...args: any[]) {
      super(...args)
      console.log(`创建了 ${context.name} 的实例`)
    }
  }
}

@logged
class Person {
  name = '张三'
}

// ES Decorator 的方法装饰器
function autobind(
  target: Function,
  context: ClassMethodDecoratorContext
) {
  return function (this: any, ...args: any[]) {
    return target.apply(this, args)
  }
}

class Button {
  @autobind
  handleClick() {
    console.log(this)  // 正确绑定
  }
}
```

## tsconfig.json 核心配置详解

### target

指定编译输出的 ECMAScript 版本：

```json
{
  "compilerOptions": {
    "target": "ES2022"
  }
}
```

| target 值 | 输出特性 | 适用场景 |
|-----------|---------|---------|
| ES5 | 无 Promise、无 async/await | IE11 兼容 |
| ES2015 | 支持 Promise、let/const、class | 现代浏览器基础支持 |
| ES2020 | 支持 optional chaining、nullish coalescing | 现代浏览器 |
| ES2022 | 支持 top-level await、Array.at() | 最新浏览器 |
| ESNext | 最新特性 | 仅限最新运行时 |

### module

指定模块系统：

```json
{
  "compilerOptions": {
    "module": "ESNext"
  }
}
```

| module 值 | 输出格式 | 适用场景 |
|-----------|---------|---------|
| CommonJS | require/module.exports | Node.js 传统项目 |
| ESNext | import/export | 现代打包工具（Vite、Webpack） |
| NodeNext | Node.js 原生 ESM | Node.js ESM 项目 |

### lib

指定项目中可用的类型声明库：

```json
{
  "compilerOptions": {
    "lib": ["ES2022", "DOM", "DOM.Iterable"]
  }
}
```

- `ES2022`：ECMAScript 内置类型（Array、Promise 等）
- `DOM`：浏览器 DOM API 类型
- `DOM.Iterable`：可迭代 DOM 集合的类型
- `WebWorker`：Web Worker API 类型

### strict 模式家族

```json
{
  "compilerOptions": {
    "strict": true
  }
}
```

`strict: true` 等于同时开启以下所有严格检查：

| 选项 | 作用 |
|------|------|
| strictNullChecks | null 和 undefined 是独立类型 |
| strictFunctionTypes | 函数参数类型严格检查 |
| strictBindCallApply | bind/call/apply 参数类型检查 |
| noImplicitAny | 禁止隐式 any |
| noImplicitThis | 禁止隐式 any 类型的 this |
| alwaysStrict | 输出文件添加 "use strict" |
| useUnknownInCatchVariables | catch 变量默认为 unknown |

### 路径映射

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@components/*": ["src/components/*"],
      "@utils/*": ["src/utils/*"]
    }
  }
}
```

## 编译输出：模块格式选择

不同的打包目标需要不同的模块格式：

```json
// ESM 输出（推荐用于现代项目）
{
  "compilerOptions": {
    "module": "ESNext",
    "moduleResolution": "bundler",
    "declaration": true,
    "declarationMap": true,
    "outDir": "./dist"
  }
}

// CJS 输出（Node.js 兼容）
{
  "compilerOptions": {
    "module": "CommonJS",
    "moduleResolution": "node",
    "declaration": true,
    "outDir": "./lib"
  }
}
```

### declaration 文件生成

```json
{
  "compilerOptions": {
    "declaration": true,        // 生成 .d.ts 文件
    "declarationMap": true,     // 生成 .d.ts.map 文件（支持跳转到源码）
    "sourceMap": true           // 生成 .js.map 文件（支持调试源码）
  }
}
```

## 项目引用（Project References）

对于 monorepo 或大型项目，项目引用允许将代码库拆分为多个独立的 TypeScript 项目，实现增量编译。

### 配置项目引用

```json
// tsconfig.json（根配置）
{
  "files": [],
  "references": [
    { "path": "./packages/core" },
    { "path": "./packages/utils" },
    { "path": "./packages/ui" }
  ]
}
```

```json
// packages/core/tsconfig.json
{
  "compilerOptions": {
    "composite": true,         // 必须开启，表示这是一个可引用的项目
    "declaration": true,
    "outDir": "./dist",
    "rootDir": "./src"
  },
  "include": ["src/**/*"],
  "references": [
    { "path": "../utils" }     // core 依赖 utils
  ]
}
```

### 增量编译

```json
{
  "compilerOptions": {
    "incremental": true,       // 启用增量编译
    "tsBuildInfoFile": "./.tsbuildinfo"
  }
}
```

使用 `tsc --build` 命令进行增量编译：

```bash
# 首次编译
tsc --build

# 增量编译（只编译修改过的项目）
tsc --build --incremental

# 清除编译缓存
tsc --build --clean
```

## TypeScript 与 Babel/SWC 的配合

### TypeScript + Babel

Babel 可以处理 TypeScript 的转译，但不做类型检查：

```json
// babel.config.json
{
  "presets": [
    "@babel/preset-typescript",
   ["@babel/preset-env", { "targets": { "node": "current" } }]
  ]
}
```

**优势**：编译速度快，支持与其他 Babel 插件组合
**劣势**：不进行类型检查，需要单独运行 `tsc --noEmit` 检查类型

### TypeScript + SWC

SWC 是 Rust 编写的超快编译器：

```json
// .swcrc
{
  "jsc": {
    "parser": {
      "syntax": "typescript",
      "decorators": true
    },
    "transform": {
      "legacyDecorator": true,
      "decoratorMetadata": true
    }
  }
}
```

**优势**：比 Babel 快 20-70 倍，支持装饰器
**劣势**：不进行类型检查，生态不如 Babel 丰富

### 最佳实践

```json
// package.json
{
  "scripts": {
    "typecheck": "tsc --noEmit",
    "build": "swc src -d dist",
    "ci": "npm run typecheck && npm run build"
  }
}
```

在 CI 流程中，先用 `tsc --noEmit` 进行类型检查，再用 SWC/Babel 进行快速编译。这样既保证了类型安全，又获得了最佳的编译性能。