# TypeScript 工具类型与高级模式

TypeScript 的类型系统是图灵完备的——这意味着我们可以在类型层面实现几乎任何逻辑。掌握工具类型和高级类型模式，不仅能让你写出更精确的类型定义，还能在编译期捕获更多潜在错误。本文将从内置工具类型出发，逐步深入条件类型、映射类型、模板字面量类型，最后通过类型体操实战展示高级类型模式的实际应用。

## 内置工具类型：基础变换

TypeScript 内置了一系列工具类型，它们是类型编程的基石。这些工具类型本质上都是**泛型类型别名**，接收一个或多个类型参数，返回变换后的新类型。

### Partial、Required、Readonly

这三个工具类型用于修改类型的"可选性"和"只读性"：

```ts
interface User {
  name: string
  age: number
  email: string
}

// Partial<T>：所有属性变为可选
type PartialUser = Partial<User>
// 等价于 { name?: string; age?: number; email?: string }

// Required<T>：所有属性变为必选
type RequiredUser = Required<PartialUser>
// 等价于 { name: string; age: number; email: string }

// Readonly<T>：所有属性变为只读
type ReadonlyUser = Readonly<User>
// 等价于 { readonly name: string; readonly age: number; readonly email: string }
```

实际应用场景：

```ts
// 更新操作通常只需要传入要修改的字段
function updateUser(id: string, updates: Partial<User>) {
  // updates 中的字段都是可选的
  const user = getUser(id)
  Object.assign(user, updates)
}

// 只需要修改 name
updateUser('1', { name: '李四' })
```

### Pick 与 Omit

从类型中"选取"或"排除"指定属性：

```ts
// Pick<T, K>：从 T 中选取 K 指定的属性
type UserBasic = Pick<User, 'name' | 'age'>
// 等价于 { name: string; age: number }

// Omit<T, K>：从 T 中排除 K 指定的属性
type UserWithoutEmail = Omit<User, 'email'>
// 等价于 { name: string; age: number }
```

Pick 和 Omit 是互补的——`Pick<T, K>` 等价于 `Omit<T, Exclude<keyof T, K>>`。

### Record

`Record<K, V>` 构造一个属性键为 K、属性值为 V 的对象类型：

```ts
// 键是字符串，值是 User 对象
type UserMap = Record<string, User>

const users: UserMap = {
  '1': { name: '张三', age: 25, email: 'zhangsan@example.com' },
  '2': { name: '李四', age: 30, email: 'lisi@example.com' },
}

// 用联合类型限制键的范围
type Role = 'admin' | 'editor' | 'viewer'
type RolePermissions = Record<Role, string[]>

const permissions: RolePermissions = {
  admin: ['read', 'write', 'delete'],
  editor: ['read', 'write'],
  viewer: ['read'],
}
```

### Exclude、Extract、NonNullable

这三个工具类型作用于**联合类型**：

```ts
type Status = 'active' | 'inactive' | 'deleted' | null | undefined

// Exclude<T, U>：从 T 中排除可赋值给 U 的类型
type NonNullStatus = Exclude<Status, null | undefined>
// 等价于 'active' | 'inactive' | 'deleted'

// Extract<T, U>：从 T 中提取可赋值给 U 的类型
type ActiveStatus = Extract<Status, 'active' | 'inactive'>
// 等价于 'active' | 'inactive'

// NonNullable<T>：从 T 中排除 null 和 undefined
type SafeStatus = NonNullable<Status>
// 等价于 'active' | 'inactive' | 'deleted'
```

### ReturnType、Parameters、InstanceType、ConstructorParameters

这四个工具类型用于提取函数或构造器的类型信息：

```ts
function createUser(name: string, age: number) {
  return { name, age, createdAt: new Date() }
}

// ReturnType<T>：提取函数返回值类型
type UserResult = ReturnType<typeof createUser>
// 等价于 { name: string; age: number; createdAt: Date }

// Parameters<T>：提取函数参数类型（元组）
type CreateUserParams = Parameters<typeof createUser>
// 等价于 [name: string, age: number]

// 用于构造函数
class Animal {
  constructor(public name: string, public age: number) {}
}

// ConstructorParameters<T>：提取构造函数参数类型
type AnimalParams = ConstructorParameters<typeof Animal>
// 等价于 [name: string, age: number]

// InstanceType<T>：提取构造函数实例类型
type AnimalInstance = InstanceType<typeof Animal>
// 等价于 Animal
```

## 自定义工具类型

内置工具类型不能覆盖所有场景，我们经常需要自定义工具类型。

### DeepPartial：递归可选

`Partial<T>` 只处理第一层属性，嵌套对象不会被展开：

```ts
interface Config {
  database: {
    host: string
    port: number
    credentials: {
      username: string
      password: string
    }
  }
  cache: {
    ttl: number
  }
}

// Partial<Config> 只让 database 和 cache 变为可选
// 但 database.host、database.port 仍然是必选的
type ShallowPartial = Partial<Config>

// DeepPartial：递归地将所有属性变为可选
type DeepPartial<T> = {
  [K in keyof T]?: T[K] extends object ? DeepPartial<T[K]> : T[K]
}

type DeepPartialConfig = DeepPartial<Config>
// database?.host?、database?.port?、database?.credentials?.username? 都变为可选
```

### DeepReadonly：递归只读

```ts
type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K]
}

type FrozenConfig = DeepReadonly<Config>
// 所有层级的属性都变为 readonly
```

### RequiredKeys 与 OptionalKeys

提取类型中所有必选属性的键或可选属性的键：

```ts
// RequiredKeys<T>：提取必选属性的键
type RequiredKeys<T> = {
  [K in keyof T]-?: object extends Pick<T, K> ? never : K
}[keyof T]

// OptionalKeys<T>：提取可选属性的键
type OptionalKeys<T> = {
  [K in keyof T]-?: object extends Pick<T, K> ? K : never
}[keyof T]

interface Example {
  name: string
  age: number
  email?: string
  phone?: string
}

type R = RequiredKeys<Example>  // 'name' | 'age'
type O = OptionalKeys<Example>  // 'email' | 'phone'
```

## 条件类型与 infer 关键字

条件类型是 TypeScript 类型系统中最强大的特性之一，它让类型具备了"if-else"逻辑。

### 条件类型基础

语法：`T extends U ? X : Y`——如果 T 可以赋值给 U，则类型为 X，否则为 Y：

```ts
// 判断是否为字符串类型
type IsString<T> = T extends string ? true : false

type A = IsString<string>   // true
type B = IsString<number>   // false
type C = IsString<'hello'>  // true（字符串字面量是 string 的子类型）
```

条件类型在联合类型上会自动**分发**（Distributive）：

```ts
type ToArray<T> = T extends any ? T[] : never

// 联合类型的每个成员都会独立计算条件类型
type Result = ToArray<string | number>
// 等价于 string[] | number[]，而不是 (string | number)[]
```

如果不想触发分发行为，可以用方括号包裹：

```ts
type ToArrayNonDist<T> = [T] extends [any] ? T[] : never

type Result2 = ToArrayNonDist<string | number>
// 等价于 (string | number)[]
```

### infer 关键字：类型推断

`infer` 关键字用于在条件类型的 `extends` 子句中声明一个待推断的类型变量：

```ts
// 提取函数返回值类型（ReturnType 的简化实现）
type MyReturnType<T> = T extends (...args: any[]) => infer R ? R : never

type R1 = MyReturnType<() => string>           // string
type R2 = MyReturnType<(x: number) => boolean>  // boolean

// 提取函数参数类型
type MyParameters<T> = T extends (...args: infer P) => any ? P : never

type P1 = MyParameters<(a: string, b: number) => void>  // [a: string, b: number]
```

infer 可以出现在任何位置：

```ts
// 提取 Promise 内部类型
type UnwrapPromise<T> = T extends Promise<infer U> ? U : T

type R = UnwrapPromise<Promise<string>>  // string
type S = UnwrapPromise<number>           // number（不是 Promise，直接返回原类型）

// 递归提取嵌套 Promise
type DeepUnwrap<T> = T extends Promise<infer U> ? DeepUnwrap<U> : T

type R2 = DeepUnwrap<Promise<Promise<Promise<string>>>>  // string

// 提取数组元素类型
type ElementType<T> = T extends (infer E)[] ? E : never

type E = ElementType<string[]>   // string
type F = ElementType<number[]>   // number
```

## 映射类型

映射类型基于已有的类型创建新类型，语法是 `{ [K in keyof T]: ... }`。

### 基本映射类型

```ts
// 将所有属性类型变为 boolean
type Flags<T> = {
  [K in keyof T]: boolean
}

type UserFlags = Flags<User>
// 等价于 { name: boolean; age: boolean; email: boolean }
```

映射类型可以配合修饰符使用：

```ts
// 移除所有 readonly 修饰符
type Mutable<T> = {
  -readonly [K in keyof T]: T[K]
}

// 移除所有可选修饰符
type Concrete<T> = {
  [K in keyof T]-?: T[K]
}

// 添加可选修饰符
type Soft<T> = {
  [K in keyof T]?: T[K]
}
```

### 键名重映射（as）

TypeScript 4.1 引入了键名重映射，允许在映射过程中修改键名：

```ts
// 给所有属性名加前缀
type Prefixed<T, P extends string> = {
  [K in keyof T as `${P}${Capitalize<string & K>}`]: T[K]
}

type PrefixedUser = Prefixed<User, 'user'>
// 等价于 { userName: string; userAge: number; userEmail: string }
```

## 模板字面量类型

TypeScript 4.1 引入的模板字面量类型，让我们可以在类型层面进行字符串操作。

### 基本语法

```ts
type World = 'world'
type Greeting = `hello ${World}`  // 'hello world'

// 与联合类型组合会产生笛卡尔积
type Color = 'red' | 'blue'
type Size = 'small' | 'large'
type Variant = `${Color}-${Size}`
// 'red-small' | 'red-large' | 'blue-small' | 'blue-large'
```

### 内置字符串工具类型

```ts
type Upper = Uppercase<'hello'>       // 'HELLO'
type Lower = Lowercase<'HELLO'>       // 'hello'
type Cap = Capitalize<'hello'>        // 'Hello'
type Uncap = Uncapitalize<'Hello'>    // 'hello'

// 实际应用：自动生成事件处理器名称
type EventName<T extends string> = `on${Capitalize<T>}`

type ClickHandler = EventName<'click'>   // 'onClick'
type FocusHandler = EventName<'focus'>   // 'onFocus'
```

## 类型体操实战

### DeepMerge：深度合并两个对象类型

```ts
type DeepMerge<T, U> = {
  [K in keyof T | keyof U]: K extends keyof U
    ? K extends keyof T
      ? T[K] extends object
        ? U[K] extends object
          ? DeepMerge<T[K], U[K]>
          : U[K]
        : U[K]
      : U[K]
    : K extends keyof T
      ? T[K]
      : never
}

interface A {
  x: number
  y: { a: string; b: number }
}

interface B {
  y: { b: boolean; c: string }
  z: string
}

type Merged = DeepMerge<A, B>
// { x: number; y: { a: string; b: boolean; c: string }; z: string }
```

### PathKeys：提取对象所有路径

```ts
type PathKeys<T, Prefix extends string = ''> = T extends object
  ? {
      [K in keyof T & string]: K extends keyof T
        ? T[K] extends object
          ? `${Prefix}${K}` | PathKeys<T[K], `${Prefix}${K}.`>
          : `${Prefix}${K}`
        : never
    }[keyof T & string]
  : never

interface Nested {
  a: string
  b: {
    c: number
    d: {
      e: boolean
    }
  }
}

type Paths = PathKeys<Nested>
// 'a' | 'b' | 'b.c' | 'b.d' | 'b.d.e'
```

### 类型安全的路由参数提取

```ts
// 从路由模板中提取参数名称
type ExtractRouteParams<T extends string> =
  T extends `${string}:${infer Param}/${infer Rest}`
    ? Param | ExtractRouteParams<Rest>
    : T extends `${string}:${infer Param}`
      ? Param
      : never

type Params = ExtractRouteParams<'/users/:userId/posts/:postId'>
// 'userId' | 'postId'

// 构建参数对象类型
type RouteParams<T extends string> = {
  [K in ExtractRouteParams<T>]: string
}

type UserPostParams = RouteParams<'/users/:userId/posts/:postId'>
// { userId: string; postId: string }

// 类型安全的路由导航函数
function navigate<T extends string>(
  route: T,
  params: RouteParams<T>
) {
  let url: string = route
  for (const [key, value] of Object.entries(params)) {
    url = url.replace(`:${key}`, value as string)
  }
  window.location.href = url
}

// 正确调用
navigate('/users/:userId/posts/:postId', {
  userId: '123',
  postId: '456',
})

// 编译错误：缺少 postId
navigate('/users/:userId/posts/:postId', {
  userId: '123',
})
```

## 类型设计原则

编写类型时，遵循一些基本原则可以避免常见的陷阱。

### 宁严勿宽

类型应该尽可能精确，而不是用 `any` 或宽泛的类型来"通过编译"：

```ts
// 不好：返回类型过于宽泛
function getConfig(key: string): any {
  return config[key]
}

// 好：精确的返回类型
function getConfig<K extends keyof AppConfig>(key: K): AppConfig[K] {
  return config[key]
}
```

### 优先使用联合类型

联合类型比交叉类型更安全，因为它表达了"其中之一"而非"全部都是"：

```ts
// 危险：交叉类型可能导致不合理的类型
type Bad = { kind: 'circle'; radius: number } & { kind: 'square'; side: number }
// kind 的类型是 'circle' & 'square'，即 never

// 正确：联合类型
type Shape = { kind: 'circle'; radius: number } | { kind: 'square'; side: number }
```

### 避免 any，慎用类型断言

```ts
// 不好：any 关闭了类型检查
function process(data: any) { ... }

// 好：用 unknown 代替 any，强制进行类型检查
function process(data: unknown) {
  if (typeof data === 'string') {
    // 这里 data 被收窄为 string
    console.log(data.toUpperCase())
  }
}

// 类型断言应该是最后的手段，而非第一选择
const value = someValue as string  // 危险：编译器不会验证这个断言

// 更安全的方式是使用类型守卫
function isString(value: unknown): value is string {
  return typeof value === 'string'
}

if (isString(someValue)) {
  // 这里 someValue 的类型是 string，完全安全
  console.log(someValue.toUpperCase())
}
```

TypeScript 的类型系统提供了强大的表达能力，但能力越大责任越大。合理使用工具类型和高级模式，让类型成为你的盟友而非负担。记住：类型的目标是在编译期捕获错误，而不是展示你能在类型层面做多复杂的运算。