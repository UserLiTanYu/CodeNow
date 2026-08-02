# 动态规划与贪心算法

动态规划（Dynamic Programming）和贪心算法是解决最优化问题的两大核心方法。它们看似相似，实则有着本质的区别。本文将深入讲解这两种算法的思想、适用场景和经典问题。

## 动态规划的核心思想

动态规划适用于具有以下两个特性的问题：

1. **最优子结构**：问题的最优解包含子问题的最优解
2. **重叠子问题**：子问题会被重复计算

### 动态规划的解题步骤

```
1. 定义状态：明确 dp[i] 或 dp[i][j] 代表什么
2. 写出状态转移方程：dp[i] 与 dp[i-1] 等的关系
3. 确定初始条件：dp[0]、dp[1] 等的值
4. 确定遍历顺序：确保计算 dp[i] 时，依赖的状态已经计算
5. 推导验证：用小例子验证状态转移方程的正确性
```

## 经典动态规划问题

### 1. 斐波那契数列

最简单的动态规划入门问题：

```java
public class Fibonacci {
    // 递归（有重叠子问题）
    public static int fibRecursive(int n) {
        if (n <= 1) return n;
        return fibRecursive(n - 1) + fibRecursive(n - 2);
    }
    
    // 动态规划
    public static int fibDP(int n) {
        if (n <= 1) return n;
        
        int[] dp = new int[n + 1];
        dp[0] = 0;
        dp[1] = 1;
        
        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        
        return dp[n];
    }
    
    // 空间优化：只用两个变量
    public static int fibOptimized(int n) {
        if (n <= 1) return n;
        
        int prev2 = 0, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### 2. 爬楼梯

每次可以爬 1 或 2 级台阶，问有多少种方法爬到第 n 级：

```java
public class ClimbingStairs {
    /**
     * 状态转移方程：dp[i] = dp[i-1] + dp[i-2]
     * 含义：到达第 i 级的方法数 = 从第 i-1 级爬 1 步 + 从第 i-2 级爬 2 步
     */
    public static int climbStairs(int n) {
        if (n <= 2) return n;
        
        int prev2 = 1, prev1 = 2;
        for (int i = 3; i <= n; i++) {
            int curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

### 3. 最长递增子序列（LIS）

给定一个数组，找到最长的严格递增子序列的长度：

```java
public class LongestIncreasingSubsequence {
    /**
     * 状态定义：dp[i] 表示以 nums[i] 结尾的最长递增子序列长度
     * 状态转移：dp[i] = max(dp[j] + 1)，其中 j < i 且 nums[j] < nums[i]
     */
    public static int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1);  // 每个元素自身就是一个长度为 1 的子序列
        
        int maxLen = 1;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        
        return maxLen;
    }
    
    /**
     * 二分查找优化：O(n log n)
     * 维护一个数组 tails，tails[i] 表示长度为 i+1 的递增子序列的最小末尾
     */
    public static int lengthOfLISBinarySearch(int[] nums) {
        int[] tails = new int[nums.length];
        int size = 0;
        
        for (int num : nums) {
            int left = 0, right = size;
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < num) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
            tails[left] = num;
            if (left == size) size++;
        }
        
        return size;
    }
}
```

### 4. 最长公共子序列（LCS）

给定两个字符串，找到它们的最长公共子序列：

```java
public class LongestCommonSubsequence {
    /**
     * 状态定义：dp[i][j] 表示 text1[0..i-1] 和 text2[0..j-1] 的 LCS 长度
     * 状态转移：
     *   如果 text1[i-1] == text2[j-1]：dp[i][j] = dp[i-1][j-1] + 1
     *   否则：dp[i][j] = max(dp[i-1][j], dp[i][j-1])
     */
    public static int lcs(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 如果需要输出具体的 LCS 字符串
     */
    public static String lcsString(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // 回溯构造 LCS
        StringBuilder sb = new StringBuilder();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                sb.append(text1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        
        return sb.reverse().toString();
    }
}
```

### 5. 编辑距离

将一个字符串转换为另一个字符串所需的最少操作数（插入、删除、替换）：

```java
public class EditDistance {
    /**
     * 状态定义：dp[i][j] 表示 word1[0..i-1] 转换为 word2[0..j-1] 的最少操作数
     * 状态转移：
     *   如果 word1[i-1] == word2[j-1]：dp[i][j] = dp[i-1][j-1]
     *   否则：dp[i][j] = 1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
     *         分别对应删除、插入、替换
     */
    public static int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // 初始化边界
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j - 1],  // 替换
                        Math.min(
                            dp[i - 1][j],   // 删除
                            dp[i][j - 1]    // 插入
                        )
                    );
                }
            }
        }
        
        return dp[m][n];
    }
}
```

### 6. 背包问题

**0-1 背包**：每个物品只能选一次

```java
public class Knapsack01 {
    /**
     * 状态定义：dp[i][w] 表示前 i 个物品、容量为 w 时的最大价值
     * 状态转移：
     *   不选第 i 个物品：dp[i][w] = dp[i-1][w]
     *   选第 i 个物品：dp[i][w] = dp[i-1][w-weight[i]] + value[i]
     */
    public static int knapsack(int[] weight, int[] value, int capacity) {
        int n = weight.length;
        int[] dp = new int[capacity + 1];
        
        for (int i = 0; i < n; i++) {
            // 倒序遍历，保证每个物品只选一次
            for (int w = capacity; w >= weight[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weight[i]] + value[i]);
            }
        }
        
        return dp[capacity];
    }
}
```

**完全背包**：每个物品可以选无限次

```java
public class KnapsackComplete {
    public static int knapsack(int[] weight, int[] value, int capacity) {
        int n = weight.length;
        int[] dp = new int[capacity + 1];
        
        for (int i = 0; i < n; i++) {
            // 正序遍历，允许重复选择
            for (int w = weight[i]; w <= capacity; w++) {
                dp[w] = Math.max(dp[w], dp[w - weight[i]] + value[i]);
            }
        }
        
        return dp[capacity];
    }
}
```

### 7. 零钱兑换

给定不同面额的硬币和一个总金额，计算凑出该金额的最少硬币数：

```java
public class CoinChange {
    /**
     * 状态定义：dp[i] 表示凑出金额 i 需要的最少硬币数
     * 状态转移：dp[i] = min(dp[i - coin] + 1)，coin 为每个硬币面额
     */
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);  // 初始化为一个不可能的大值
        dp[0] = 0;
        
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        
        return dp[amount] > amount ? -1 : dp[amount];
    }
}
```

### 8. 股票买卖问题

**只能买卖一次**：

```java
public class Stock买卖I {
    /**
     * 维护一个最低买入价格，计算每天卖出的利润
     */
    public static int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
        
        return maxProfit;
    }
}
```

**可以买卖多次**：

```java
public class Stock买卖II {
    /**
     * 贪心：只要今天比昨天贵，就昨天买今天卖
     */
    public static int maxProfit(int[] prices) {
        int profit = 0;
        for (int i = 1; i < prices.length; i++) {
            if (prices[i] > prices[i - 1]) {
                profit += prices[i] - prices[i - 1];
            }
        }
        return profit;
    }
}
```

**最多买卖两次**：

```java
public class Stock买卖III {
    /**
     * 状态定义：
     * buy1：第一次买入后的最大利润
     * sell1：第一次卖出后的最大利润
     * buy2：第二次买入后的最大利润
     * sell2：第二次卖出后的最大利润
     */
    public static int maxProfit(int[] prices) {
        int buy1 = Integer.MIN_VALUE, sell1 = 0;
        int buy2 = Integer.MIN_VALUE, sell2 = 0;
        
        for (int price : prices) {
            buy1 = Math.max(buy1, -price);
            sell1 = Math.max(sell1, buy1 + price);
            buy2 = Math.max(buy2, sell1 - price);
            sell2 = Math.max(sell2, buy2 + price);
        }
        
        return sell2;
    }
}
```

## 贪心算法

贪心算法在每一步选择中都采取当前状态下最优的选择，希望导致全局最优解。

### 贪心与动态规划的区别

| 特性 | 贪心算法 | 动态规划 |
|------|----------|----------|
| 选择策略 | 每步选局部最优 | 考虑所有子问题 |
| 最优性 | 不一定全局最优 | 一定全局最优 |
| 适用条件 | 贪心选择性质 + 最优子结构 | 最优子结构 + 重叠子问题 |
| 时间效率 | 通常更快 | 可能较慢 |

### 区间调度问题

给定一组区间，找到最多不重叠区间的数量：

```java
public class IntervalScheduling {
    /**
     * 贪心策略：按结束时间排序，每次选结束最早的区间
     */
    public static int maxNonOverlapping(int[][] intervals) {
        if (intervals.length == 0) return 0;
        
        // 按结束时间排序
        Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
        
        int count = 1;
        int end = intervals[0][1];
        
        for (int i = 1; i < intervals.length; i++) {
            if (intervals[i][0] >= end) {
                count++;
                end = intervals[i][1];
            }
        }
        
        return count;
    }
}
```

### 分发糖果

每个孩子至少一个糖果，评分更高的孩子比邻居获得更多糖果：

```java
public class Candy {
    /**
     * 贪心策略：两次遍历，先从左到右，再从右到左
     */
    public static int candy(int[] ratings) {
        int n = ratings.length;
        int[] candies = new int[n];
        Arrays.fill(candies, 1);
        
        // 从左到右：右边评分高，则糖果数 = 左边 + 1
        for (int i = 1; i < n; i++) {
            if (ratings[i] > ratings[i - 1]) {
                candies[i] = candies[i - 1] + 1;
            }
        }
        
        // 从右到左：左边评分高，则取 max(当前, 右边 + 1)
        for (int i = n - 2; i >= 0; i--) {
            if (ratings[i] > ratings[i + 1]) {
                candies[i] = Math.max(candies[i], candies[i + 1] + 1);
            }
        }
        
        int total = 0;
        for (int candy : candies) {
            total += candy;
        }
        return total;
    }
}
```

### 跳跃游戏

判断能否到达数组末尾：

```java
public class JumpGame {
    /**
     * 贪心策略：维护能到达的最远位置
     */
    public static boolean canJump(int[] nums) {
        int maxReach = 0;
        
        for (int i = 0; i < nums.length; i++) {
            if (i > maxReach) {
                return false;  // 当前位置无法到达
            }
            maxReach = Math.max(maxReach, i + nums[i]);
            if (maxReach >= nums.length - 1) {
                return true;
            }
        }
        
        return false;
    }
}
```

## 动态规划 vs 贪心：如何选择

```
问题是否具有贪心选择性质？
  → 是：每步选局部最优是否能导致全局最优？
    → 是：使用贪心算法
    → 否：使用动态规划
  → 否：使用动态规划
```

**判断贪心选择性质的方法**：
1. 尝试举反例：找一个贪心策略不能得到最优解的例子
2. 证明贪心选择性质：证明存在一个最优解包含贪心选择

**经典贪心问题**：
- 区间调度（按结束时间排序）
- 分发糖果（两次遍历）
- 跳跃游戏（维护最远位置）
- 股票买卖 II（只要涨就买卖）

**经典动态规划问题**：
- 背包问题
- 最长公共子序列
- 编辑距离
- 股票买卖 III/IV（限制交易次数）

掌握动态规划和贪心算法，需要大量的练习和总结。建议从简单问题开始，逐步理解状态转移方程的设计思路，培养对问题特征的敏感度。
