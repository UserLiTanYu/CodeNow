# 闭包、作用域与执行上下文

## 执行上下文（Execution Context）

执行上下文是 JavaScript 代码被解析和执行时所在环境的抽象概念。每当 JavaScript 引擎遇到可执行代码时，就会创建一个执行上下文。

### 执行上下文类型

JavaScript 中有三种执行上下文：

**全局执行上下文**：
- 代码首次执行时创建
- 创建全局对象（浏览器中是 `window`，Node.js 中是 `global`）
- 将 `this` 绑定到全局对象
- 一个程序只有一个全局上下文

```js
// 全局上下文创建
console.log(this === window); // true（浏览器环境）
var globalVar = '全局变量';
console.log(window.globalVar); // '全局变量'
```

**函数执行上下文**：
- 每次调用函数时创建
- 每个函数都有自己的执行上下文
- 函数执行完毕后销毁

```js
function outer() {
  var outerVar = '外层变量';
  
  function inner() {
    var innerVar = '内层变量';
    console.log(outerVar); // 可以访问外层变量
  }
  
  inner(); // 创建 inner 的执行上下文
}
outer(); // 创建 outer 的执行上下文
```

**eval 执行上下文**：
- 执行 `eval()` 函数时创建
- 不推荐使用，存在安全和性能问题

```js
eval('var evalVar = "eval变量"');
console.log(evalVar); // 'eval变量'（严格模式下会报错）
```

### 执行上下文的组成

每个执行上下文包含三个核心组件：

```js
ExecutionContext = {
  // 1. 词法环境（LexicalEnvironment）
  LexicalEnvironment: {
    EnvironmentRecord: { /* 变量和函数声明 */ },
    OuterEnvironmentReference: /* 外部环境引用 */
  },
  
  // 2. 变量环境（VariableEnvironment）
  VariableEnvironment: {
    EnvironmentRecord: { /* var 声明的变量 */ },
    OuterEnvironmentReference: /* 外部环境引用 */
  },
  
  // 3. this 绑定（ThisBinding）
  ThisBinding: /* this 的值 */
}
```

## 执行栈（Call Stack）

执行栈（调用栈）是一个 LIFO（后进先出）结构，用于跟踪执行上下文的创建和销毁。

### 执行栈的工作流程

```js
function first() {
  console.log('first');
  second();
}

function second() {
  console.log('second');
  third();
}

function third() {
  console.log('third');
}

first();
```

执行栈变化过程：

```
1. 程序开始 → [全局上下文]
2. 调用 first() → [全局上下文, first 上下文]
3. 调用 second() → [全局上下文, first 上下文, second 上下文]
4. 调用 third() → [全局上下文, first 上下文, second 上下文, third 上下文]
5. third() 执行完毕 → [全局上下文, first 上下文, second 上下文]
6. second() 执行完毕 → [全局上下文, first 上下文]
7. first() 执行完毕 → [全局上下文]
8. 程序结束 → []
```

### 栈溢出（Stack Overflow）

当递归深度超过栈容量时，会发生栈溢出：

```js
// 无限递归导致栈溢出
function recursive() {
  recursive();
}

recursive();
// Uncaught RangeError: Maximum call stack size exceeded

// 尾递归优化（ES6 严格模式，但浏览器支持有限）
function factorial(n, acc = 1) {
  if (n <= 1) return acc;
  return factorial(n - 1, n * acc); // 尾调用位置
}
```

## 变量环境与词法环境

### 变量环境（VariableEnvironment）

变量环境专门用于存储 `var` 声明的变量和函数声明：

```js
function example() {
  var a = 1;      // 存储在变量环境
  var b = 2;      // 存储在变量环境
  function fn() {} // 存储在变量环境
}
```

### 词法环境（LexicalEnvironment）

词法环境用于存储 `let`、`const` 声明的变量和代码块内的声明：

```js
function example() {
  let x = 10;    // 存储在词法环境
  const y = 20;  // 存储在词法环境
  
  if (true) {
    let z = 30;  // 新的词法环境
    const w = 40;
  }
}
```

### 两者的区别

```js
function demo() {
  console.log(a); // undefined（变量环境中的声明已提升）
  console.log(b); // ReferenceError（词法环境中的声明未初始化）
  
  var a = 1;
  let b = 2;
}
```

## var 的变量提升与 let/const 的暂时性死区

### var 的变量提升（Hoisting）

`var` 声明的变量会被提升到作用域顶部，但赋值不会提升：

```js
// 等价于：
// var name;
// console.log(name);
// name = '张三';

console.log(name); // undefined
var name = '张三';

// 函数声明也会提升
greet(); // 'Hello!'
function greet() {
  console.log('Hello!');
}

// 函数表达式不会提升函数体
sayHi(); // TypeError: sayHi is not a function
var sayHi = function() {
  console.log('Hi!');
};
```

### let/const 的暂时性死区（TDZ）

`let` 和 `const` 声明的变量也会被提升，但不会初始化，从块开始到声明语句之间是暂时性死区：

```js
{
  // TDZ 开始
  console.log(x); // ReferenceError: Cannot access 'x' before initialization
  let x = 10;     // TDZ 结束
}

{
  // TDZ 也会发生在函数参数默认值中
  function foo(a = b, b = 1) {
    console.log(a, b);
  }
  foo(); // ReferenceError: Cannot access 'b' before initialization
}

// typeof 也会触发 TDZ
console.log(typeof y); // ReferenceError
let y;
```

### const 的特殊性

```js
// const 必须在声明时初始化
const a; // SyntaxError: Missing initializer in const declaration

// const 声明的基本类型不可修改
const PI = 3.14;
PI = 3.15; // TypeError: Assignment to constant variable

// const 声明的引用类型，引用不可修改，内容可修改
const arr = [1, 2, 3];
arr.push(4); // 可以
arr = [5, 6]; // TypeError

const obj = { name: '张三' };
obj.name = '李四'; // 可以
obj = {}; // TypeError
```

## 作用域链（Scope Chain）

作用域链是由当前执行上下文的词法环境及其所有外部环境引用组成的链式结构。

### 作用域链的形成

```js
var global = '全局';

function outer() {
  var outerVar = '外层';
  
  function middle() {
    var middleVar = '中层';
    
    function inner() {
      var innerVar = '内层';
      console.log(innerVar);    // '内层' - 当前作用域
      console.log(middleVar);   // '中层' - middle 作用域
      console.log(outerVar);    // '外层' - outer 作用域
      console.log(global);      // '全局' - 全局作用域
    }
    
    inner();
  }
  
  middle();
}

outer();
```

### 变量查找机制

```js
var name = '全局名称';

function foo() {
  var name = 'foo 名称';
  
  function bar() {
    // 从当前作用域开始查找
    // bar → foo → 全局
    console.log(name); // 'foo 名称'
  }
  
  bar();
}

foo();
```

### 作用域链的静态特性

作用域链在函数定义时确定，而非调用时：

```js
var value = 1;

function foo() {
  console.log(value);
}

function bar() {
  var value = 2;
  foo(); // 输出 1，因为 foo 的作用域链在定义时确定
}

bar();
```

## 闭包（Closure）

### 闭包的定义

闭包是指函数能够记住并访问它的词法作用域，即使函数在词法作用域之外执行。

```js
function createCounter() {
  let count = 0;  // 局部变量
  
  return function() {
    count++;  // 访问外部函数的变量
    return count;
  };
}

const counter = createCounter();
console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3
```

### 闭包产生的条件

1. 函数嵌套
2. 内部函数引用外部函数的变量
3. 内部函数被返回或传递到外部

```js
function multiplier(factor) {
  // factor 被内部函数引用，形成闭包
  return function(number) {
    return number * factor;
  };
}

const double = multiplier(2);
const triple = multiplier(3);

console.log(double(5));  // 10
console.log(triple(5));  // 15
```

### 闭包的内存模型

```js
function outer() {
  var a = 1;
  var b = 2;
  
  function inner() {
    return a + b;  // 引用 a 和 b
  }
  
  return inner;
}

var fn = outer();
// inner 函数闭包持有 a 和 b 的引用
// a 和 b 不会被垃圾回收
console.log(fn()); // 3
```

## 闭包的经典应用

### 数据私有化

```js
function createPerson(name, age) {
  // 私有变量
  var _name = name;
  var _age = age;
  
  return {
    getName: function() {
      return _name;
    },
    getAge: function() {
      return _age;
    },
    setName: function(newName) {
      if (typeof newName === 'string' && newName.length > 0) {
        _name = newName;
      }
    }
  };
}

var person = createPerson('张三', 25);
console.log(person.getName()); // '张三'
person.setName('李四');
console.log(person.getName()); // '李四'
console.log(person._name);     // undefined（无法直接访问）
```

### 函数工厂

```js
function createLogger(prefix) {
  return function(message) {
    console.log(`[${prefix}] ${new Date().toISOString()}: ${message}`);
  };
}

const errorLogger = createLogger('ERROR');
const infoLogger = createLogger('INFO');

errorLogger('连接失败');  // [ERROR] 2024-01-01T00:00:00.000Z: 连接失败
infoLogger('服务启动');   // [INFO] 2024-01-01T00:00:00.000Z: 服务启动
```

### 柯里化（Currying）

```js
function curry(fn) {
  return function curried(...args) {
    if (args.length >= fn.length) {
      return fn.apply(this, args);
    }
    return function(...args2) {
      return curried.apply(this, args.concat(args2));
    };
  };
}

function add(a, b, c) {
  return a + b + c;
}

const curriedAdd = curry(add);

console.log(curriedAdd(1)(2)(3));     // 6
console.log(curriedAdd(1, 2)(3));     // 6
console.log(curriedAdd(1)(2, 3));     // 6
console.log(curriedAdd(1, 2, 3));     // 6
```

### 防抖（Debounce）

```js
function debounce(fn, delay) {
  let timer = null;
  
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}

const handleSearch = debounce(function(keyword) {
  console.log('搜索:', keyword);
}, 300);

input.addEventListener('input', (e) => {
  handleSearch(e.target.value);
});
```

### 节流（Throttle）

```js
function throttle(fn, interval) {
  let lastTime = 0;
  
  return function(...args) {
    const now = Date.now();
    if (now - lastTime >= interval) {
      lastTime = now;
      fn.apply(this, args);
    }
  };
}

const handleScroll = throttle(function() {
  console.log('处理滚动');
}, 100);

window.addEventListener('scroll', handleScroll);
```

## 闭包的内存泄漏风险

### 意外持有大对象引用

```js
function processData() {
  const largeData = new Array(1000000).fill('数据');
  
  // 问题：返回的闭包持有 largeData 的引用
  return function() {
    return largeData.length;
  };
}

// 即使不需要 largeData，它也不会被回收
const getLength = processData();

// 解决方案：用完后解除引用
function processDataFixed() {
  let largeData = new Array(1000000).fill('数据');
  const length = largeData.length;
  
  // 不再引用 largeData
  largeData = null;
  
  return function() {
    return length;
  };
}
```

### DOM 引用导致的泄漏

```js
// 问题
function setupHandler() {
  const element = document.getElementById('large-element');
  
  element.addEventListener('click', function handler() {
    // handler 闭包持有 element 引用
    // 即使移除了 DOM，闭包仍持有引用
    console.log(element.innerHTML);
  });
  
  // 移除 DOM
  element.parentNode.removeChild(element);
  // element 仍然被 handler 引用，无法回收
}

// 解决方案
function setupHandlerFixed() {
  const element = document.getElementById('large-element');
  
  element.addEventListener('click', function handler() {
    console.log(this.innerHTML); // 使用 this 代替 element
  });
  
  element.parentNode.removeChild(element);
}
```

## IIFE（立即执行函数表达式）

IIFE 是定义后立即执行的函数，常用于创建独立作用域。

### 基本语法

```js
// 标准 IIFE
(function() {
  var private = '私有变量';
  console.log(private);
})();

// 箭头函数 IIFE
(() => {
  console.log('箭头函数 IIFE');
})();

// 带参数的 IIFE
(function(name) {
  console.log('Hello, ' + name);
})('张三');

// 返回值的 IIFE
var result = (function() {
  return 42;
})();
console.log(result); // 42
```

### IIFE 与闭包的关系

```js
// 使用 IIFE 创建闭包，避免变量污染全局
for (var i = 0; i < 5; i++) {
  (function(j) {
    setTimeout(function() {
      console.log(j); // 0, 1, 2, 3, 4
    }, 1000);
  })(i);
}

// 不使用 IIFE 的问题
for (var i = 0; i < 5; i++) {
  setTimeout(function() {
    console.log(i); // 5, 5, 5, 5, 5
  }, 1000);
}

// 使用 let 的解决方案（块级作用域）
for (let i = 0; i < 5; i++) {
  setTimeout(function() {
    console.log(i); // 0, 1, 2, 3, 4
  }, 1000);
}
```

## 模块模式

### 闭包实现私有变量

```js
const Calculator = (function() {
  // 私有变量
  let result = 0;
  
  // 私有方法
  function validate(num) {
    return typeof num === 'number' && !isNaN(num);
  }
  
  // 返回公共接口
  return {
    add(num) {
      if (validate(num)) result += num;
      return this; // 链式调用
    },
    subtract(num) {
      if (validate(num)) result -= num;
      return this;
    },
    getResult() {
      return result;
    },
    reset() {
      result = 0;
      return this;
    }
  };
})();

Calculator.add(10).subtract(3).add(5);
console.log(Calculator.getResult()); // 12
console.log(Calculator.result);       // undefined
```

### ES Module 对比

```js
// math.js - ES Module
const _private = new WeakMap();

export class Calculator {
  constructor() {
    _private.set(this, { result: 0 });
  }
  
  add(num) {
    const state = _private.get(this);
    state.result += num;
    return this;
  }
  
  getResult() {
    return _private.get(this).result;
  }
}

// 使用
import { Calculator } from './math.js';
const calc = new Calculator();
```

ES Module 的优势：
- 语法原生支持，无需 IIFE 包裹
- 静态分析，支持 tree-shaking
- 严格模式默认开启
- 异步加载

## this 绑定规则

### 默认绑定

独立函数调用时，`this` 指向全局对象（严格模式下为 `undefined`）：

```js
function standalone() {
  console.log(this);
}

standalone(); // window（非严格模式）
standalone(); // undefined（严格模式）

var name = '全局';
function greet() {
  console.log(this.name);
}

greet(); // '全局'（非严格模式）
```

### 隐式绑定

作为对象方法调用时，`this` 指向调用对象：

```js
const user = {
  name: '张三',
  greet() {
    console.log(this.name);
  }
};

user.greet(); // '张三'（this = user）

// 隐式丢失
const greetFunc = user.greet;
greetFunc(); // undefined（this = window）

// 回调函数中的隐式丢失
setTimeout(user.greet, 100); // undefined
```

### 显式绑定（call/apply/bind）

```js
function greet(greeting) {
  console.log(`${greeting}, ${this.name}`);
}

const user = { name: '张三' };

// call：逐个传参
greet.call(user, 'Hello');   // 'Hello, 张三'

// apply：数组传参
greet.apply(user, ['Hi']);   // 'Hi, 张三'

// bind：返回新函数
const boundGreet = greet.bind(user);
boundGreet('Hey');           // 'Hey, 张三'

// 硬绑定
const obj = {
  name: '李四',
  greet: greet.bind(user)
};
obj.greet('你好'); // '你好, 张三'（bind 优先级高于隐式绑定）
```

### new 绑定

使用 `new` 调用构造函数时，`this` 指向新创建的对象：

```js
function Person(name) {
  this.name = name;
  this.greet = function() {
    console.log(`我是 ${this.name}`);
  };
}

const person = new Person('王五');
person.greet(); // '我是 王五'（this = person）
```

### 绑定优先级

```
new 绑定 > 显式绑定 > 隐式绑定 > 默认绑定
```

```js
function foo() {
  console.log(this.a);
}

const obj1 = { a: 1, foo };
const obj2 = { a: 2, foo };

// 隐式绑定
obj1.foo(); // 1
obj2.foo(); // 2

// 显式绑定
obj1.foo.call(obj2); // 2（显式 > 隐式）

// new 绑定
const bar = foo.bind(obj1);
new bar(); // undefined（new > 显式）
```

## 箭头函数的 this

箭头函数没有自己的 `this`，它继承外层作用域的 `this`：

```js
const user = {
  name: '张三',
  greet() {
    // 普通函数的 this 指向 user
    console.log('普通函数:', this.name);
    
    // 箭头函数继承 greet 的 this
    const arrow = () => {
      console.log('箭头函数:', this.name);
    };
    arrow();
  }
};

user.greet();
// 普通函数: 张三
// 箭头函数: 张三

// 箭头函数不能通过 call/apply/bind 修改 this
const other = { name: '李四' };
user.greet.call(other);
// 普通函数: 李四
// 箭头函数: 张三（仍指向 user）
```

### 箭头函数的适用场景

```js
// 1. 回调函数中保持 this
class Timer {
  constructor() {
    this.seconds = 0;
    
    setInterval(() => {
      this.seconds++;  // this 指向 Timer 实例
      console.log(this.seconds);
    }, 1000);
  }
}

// 2. 数组方法的回调
const obj = {
  numbers: [1, 2, 3, 4],
  
  double() {
    return this.numbers.map(n => n * 2);  // this 指向 obj
  }
};

// 3. 不适合作为对象方法（this 不指向对象）
const bad = {
  value: 42,
  getValue: () => {
    return this.value;  // this 指向外层（可能是 window）
  }
};
```

### 箭头函数的限制

```js
// 1. 不能作为构造函数
const Foo = () => {};
new Foo(); // TypeError: Foo is not a constructor

// 2. 没有 arguments 对象
const bar = () => {
  console.log(arguments); // ReferenceError
};

// 3. 没有 prototype 属性
const Baz = () => {};
console.log(Baz.prototype); // undefined
```

理解执行上下文、作用域链和 this 绑定是掌握 JavaScript 核心机制的关键。闭包作为这些概念的综合应用，在模块化、数据封装、函数式编程中发挥着重要作用。正确使用闭包需要注意内存管理，避免意外的内存泄漏。