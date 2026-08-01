# Java 日期时间 API

日期时间处理是编程中的常见需求。Java 8 引入了全新的 `java.time` 包，彻底解决了旧 API 的设计缺陷。本文将对比新旧 API，系统讲解现代 Java 日期时间处理方式。

## 旧 API 的设计缺陷

### java.util.Date 的问题

`Date` 类自 JDK 1.0 就存在，设计上存在诸多问题：

```java
import java.util.Date;

Date date = new Date();
// 问题1：月份从 0 开始（0=一月，11=十二月）
date.setMonth(0);  // 设置为一月，直觉上应该是 1

// 问题2：可变性（线程不安全）
// 多个线程共享同一个 Date 对象会产生竞态条件

// 问题3：职责混乱
// Date 既表示时间点，又承担格式化、解析等职责
```

### Calendar 的问题

`Calendar`（JDK 1.1）试图修复 `Date` 的问题，但引入了新的复杂性：

```java
import java.util.Calendar;

Calendar cal = Calendar.getInstance();
// 仍然月份从 0 开始
cal.set(Calendar.MONTH, Calendar.JANUARY);  // JANUARY = 0

// 设计过于复杂
cal.get(Calendar.YEAR);
cal.get(Calendar.MONTH);
cal.get(Calendar.DAY_OF_MONTH);
cal.get(Calendar.HOUR_OF_DAY);
cal.get(Calendar.MINUTE);
cal.get(Calendar.SECOND);

// 可变性问题依然存在
cal.add(Calendar.DAY_OF_MONTH, 1);  // 直接修改原对象
```

| 问题 | Date | Calendar |
|------|------|----------|
| 月份从 0 开始 | 是 | 是 |
| 可变性 | 可变 | 可变 |
| 线程安全 | 不安全 | 不安全 |
| 时区处理 | 简陋 | 复杂 |
| API 易用性 | 差 | 一般 |

## java.time 包（Java 8+）

Java 8 引入的 `java.time` 包基于 Joda-Time 的设计理念，具有以下特点：

- **不可变**：所有类都是不可变的，线程安全
- **清晰的职责分离**：日期、时间、时区分别由不同类表示
- **流畅的 API**：方法链调用
- **ISO 8601 标准**：默认遵循国际标准

## LocalDate、LocalTime、LocalDateTime

### LocalDate：只含日期

```java
import java.time.LocalDate;

// 创建方式
LocalDate today = LocalDate.now();                    // 当前日期
LocalDate specific = LocalDate.of(2026, 8, 1);        // 指定日期
LocalDate parsed = LocalDate.parse("2026-08-01");     // 从字符串解析

// 获取字段
int year = today.getYear();                           // 2026
int month = today.getMonthValue();                    // 8（从 1 开始！）
int day = today.getDayOfMonth();                      // 1
DayOfWeek dow = today.getDayOfWeek();                 // FRIDAY

// 日期运算（返回新对象，原对象不变）
LocalDate tomorrow = today.plusDays(1);
LocalDate lastMonth = today.minusMonths(1);
LocalDate nextYear = today.withYear(2027);

// 日期比较
boolean isBefore = today.isBefore(tomorrow);          // true
boolean isAfter = tomorrow.isAfter(today);            // true
```

### LocalTime：只含时间

```java
import java.time.LocalTime;

LocalTime now = LocalTime.now();                      // 当前时间
LocalTime specific = LocalTime.of(14, 30, 0);         // 14:30:00
LocalTime parsed = LocalTime.parse("14:30:00");

int hour = now.getHour();
int minute = now.getMinute();
int second = now.getSecond();

LocalTime later = now.plusHours(2).plusMinutes(30);
```

### LocalDateTime：日期 + 时间

```java
import java.time.LocalDateTime;

LocalDateTime now = LocalDateTime.now();
LocalDateTime specific = LocalDateTime.of(2026, 8, 1, 14, 30, 0);
LocalDateTime parsed = LocalDateTime.parse("2026-08-01T14:30:00");

// 拆分
LocalDate date = now.toLocalDate();
LocalTime time = now.toLocalTime();

// 组合
LocalDateTime combined = LocalDateTime.of(date, time);
```

## Instant：时间戳

`Instant` 表示时间线上的一个点，以 Unix 纪元（1970-01-01T00:00:00Z）为基准。

```java
import java.time.Instant;

Instant now = Instant.now();

// 获取秒数和纳秒
long epochSecond = now.getEpochSecond();    // 自纪元以来的秒数
int nano = now.getNano();                   // 当前秒内的纳秒数

// 从秒数创建
Instant specific = Instant.ofEpochSecond(1625184000);

// 时间运算
Instant later = now.plusSeconds(3600);      // 加 1 小时
Instant earlier = now.minusMillis(500);     // 减 500 毫秒

// 与 Date 互转
Date date = Date.from(now);
Instant back = date.toInstant();
```

## Duration 与 Period

### Duration：基于时间的持续时间

`Duration` 表示两个时间点之间的时间量，精确到纳秒。

```java
import java.time.Duration;
import java.time.LocalTime;

LocalTime start = LocalTime.of(9, 0);
LocalTime end = LocalTime.of(17, 30);

Duration duration = Duration.between(start, end);
long hours = duration.toHours();           // 8
long minutes = duration.toMinutes();       // 510
long seconds = duration.getSeconds();      // 30600

// 创建固定时长
Duration twoHours = Duration.ofHours(2);
Duration thirtyMin = Duration.ofMinutes(30);
Duration fiveSec = Duration.ofSeconds(5);

// 运算
Duration total = twoHours.plus(thirtyMin);  // 2小时30分钟
```

### Period：基于日期的持续时间

`Period` 表示日期之间的间隔，以年、月、日为单位。

```java
import java.time.LocalDate;
import java.time.Period;

LocalDate birthday = LocalDate.of(1990, 6, 15);
LocalDate today = LocalDate.now();

Period age = Period.between(birthday, today);
int years = age.getYears();
int months = age.getMonths();
int days = age.getDays();

// 创建固定周期
Period threeMonths = Period.ofMonths(3);
Period twoYears = Period.ofYears(2);
Period tenDays = Period.ofDays(10);

// 运算
Period total = threeMonths.plus(twoYears);  // 2年3个月
```

| 类 | 精度 | 典型用途 |
|----|------|----------|
| `Duration` | 纳秒级 | 计算方法执行时间、超时控制 |
| `Period` | 日级 | 计算年龄、账期、合同有效期 |

## ZonedDateTime 与时区处理

```java
import java.time.ZoneId;
import java.time.ZonedDateTime;

// 获取系统默认时区
ZoneId defaultZone = ZoneId.systemDefault();

// 指定时区
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
ZoneId tokyo = ZoneId.of("Asia/Tokyo");
ZoneId newYork = ZoneId.of("America/New_York");

// 创建带时区的日期时间
ZonedDateTime nowInShanghai = ZonedDateTime.now(shanghai);
ZonedDateTime nowInTokyo = ZonedDateTime.now(tokyo);

// 时区转换
ZonedDateTime tokyoTime = nowInShanghai.withZoneSameInstant(tokyo);

// 获取所有可用时区 ID
Set<String> allZones = ZoneId.getAvailableZoneIds();
```

## DateTimeFormatter：格式化与解析

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

LocalDateTime now = LocalDateTime.now();

// 预定义格式器
DateTimeFormatter isoDate = DateTimeFormatter.ISO_LOCAL_DATE;
System.out.println(now.format(isoDate));  // 2026-08-01

DateTimeFormatter isoDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
System.out.println(now.format(isoDateTime));  // 2026-08-01T14:30:25

// 自定义格式
DateTimeFormatter custom = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
System.out.println(now.format(custom));  // 2026年08月01日 14:30:25

DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern("yy/MM/dd");
System.out.println(now.format(shortFormat));  // 26/08/01

// 解析字符串
LocalDateTime parsed = LocalDateTime.parse("2026-08-01 14:30:25", custom);
```

常用格式符号：

| 符号 | 含义 | 示例 |
|------|------|------|
| `yyyy` | 四位年份 | 2026 |
| `yy` | 两位年份 | 26 |
| `MM` | 月份（补零） | 08 |
| `dd` | 日（补零） | 01 |
| `HH` | 小时（24小时制） | 14 |
| `hh` | 小时（12小时制） | 02 |
| `mm` | 分钟 | 30 |
| `ss` | 秒 | 25 |
| `SSS` | 毫秒 | 123 |
| `a` | 上午/下午 | 下午 |
| `E` | 星期几 | 周五 |

## 与旧 API 的互转

```java
import java.util.Date;
import java.time.*;

// Date → Instant → LocalDateTime
Date date = new Date();
Instant instant = date.toInstant();
LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

// LocalDateTime → Instant → Date
LocalDateTime now = LocalDateTime.now();
Instant instant2 = now.atZone(ZoneId.systemDefault()).toInstant();
Date date2 = Date.from(instant2);

// Calendar → ZonedDateTime
Calendar cal = Calendar.getInstance();
ZonedDateTime zdt = cal.toInstant().atZone(ZoneId.systemDefault());

// ZonedDateTime → Calendar
Calendar cal2 = Calendar.getInstance();
cal2.setTime(Date.from(zdt.toInstant()));
```

## 实际项目中的日期处理建议

### 数据库存储

```java
// 数据库 DATETIME/TIMESTAMP 字段使用 LocalDateTime
@Entity
public class Article {
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// MyBatis 类型处理器自动处理 LocalDateTime ↔ DATETIME
```

### API 返回值

```java
// REST API 返回 ISO 8601 格式
// 前端可直接用 new Date() 解析
{
    "createdAt": "2026-08-01T14:30:25",
    "updatedAt": "2026-08-01T15:00:00"
}

// Spring Boot 配置 Jackson 序列化
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
```

### 日志与调试

```java
// 使用 Instant 计算方法执行时间
Instant start = Instant.now();
// ... 执行业务逻辑
Instant end = Instant.now();
Duration elapsed = Duration.between(start, end);
log.info("操作耗时：{}ms", elapsed.toMillis());
```

### 最佳实践

| 建议 | 说明 |
|------|------|
| 存储用 `LocalDateTime` | 数据库 DATETIME 类型 |
| 传输用 ISO 8601 | `2026-08-01T14:30:25` 格式 |
| 显示用 `DateTimeFormatter` | 根据用户 Locale 格式化 |
| 计算用 `Instant` / `Duration` | 时间戳运算、耗时统计 |
| 时区用 `ZonedDateTime` | 需要跨时区的业务逻辑 |
| 避免 `Date` 和 `Calendar` | 新项目统一使用 `java.time` |
