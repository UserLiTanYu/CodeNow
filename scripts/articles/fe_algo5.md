# 图算法与高级数据结构

图是一种强大的数据结构，能够表示复杂的关系网络。本文将介绍图的基本表示方法、遍历算法、最短路径算法，以及一些高级数据结构如并查集、字典树、线段树和布隆过滤器。

## 图的表示

### 邻接矩阵

使用二维数组表示图中顶点之间的连接关系：

```java
public class AdjacencyMatrix {
    private int[][] matrix;
    private int vertices;
    
    public AdjacencyMatrix(int vertices) {
        this.vertices = vertices;
        this.matrix = new int[vertices][vertices];
    }
    
    // 添加边（无向图）
    public void addEdge(int from, int to, int weight) {
        matrix[from][to] = weight;
        matrix[to][from] = weight;
    }
    
    // 检查两个顶点是否相邻
    public boolean isAdjacent(int from, int to) {
        return matrix[from][to] != 0;
    }
    
    // 获取某个顶点的所有邻居
    public List<Integer> getNeighbors(int vertex) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            if (matrix[vertex][i] != 0) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }
}
```

**优点**：查询两个顶点是否相邻的时间复杂度为 O(1)
**缺点**：空间复杂度为 O(V²)，对于稀疏图浪费空间

### 邻接表

使用数组 + 链表（或 List）表示图：

```java
public class AdjacencyList {
    private List<List<int[]>> adjList;  // int[] 存储 [邻居顶点, 权重]
    private int vertices;
    
    public AdjacencyList(int vertices) {
        this.vertices = vertices;
        this.adjList = new ArrayList<>();
        for (int i = 0; i < vertices; i++) {
            this.adjList.add(new ArrayList<>());
        }
    }
    
    // 添加边（无向图）
    public void addEdge(int from, int to, int weight) {
        adjList.get(from).add(new int[]{to, weight});
        adjList.get(to).add(new int[]{from, weight});
    }
    
    // 获取某个顶点的所有邻居
    public List<int[]> getNeighbors(int vertex) {
        return adjList.get(vertex);
    }
}
```

**优点**：空间复杂度为 O(V + E)，适合稀疏图
**缺点**：查询两个顶点是否相邻需要遍历邻居列表

### 表示方式对比

| 特性 | 邻接矩阵 | 邻接表 |
|------|----------|--------|
| 空间复杂度 | O(V²) | O(V + E) |
| 查询相邻 | O(1) | O(degree) |
| 添加边 | O(1) | O(1) |
| 遍历邻居 | O(V) | O(degree) |
| 适用场景 | 稠密图 | 稀疏图 |

## 图的遍历

### 广度优先搜索（BFS）

BFS 从起始顶点开始，逐层访问所有邻居，使用队列实现：

```java
public class GraphBFS {
    public List<Integer> bfs(List<List<Integer>> adj, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[adj.size()];
        Queue<Integer> queue = new LinkedList<>();
        
        queue.offer(start);
        visited[start] = true;
        
        while (!queue.isEmpty()) {
            int vertex = queue.poll();
            result.add(vertex);
            
            for (int neighbor : adj.get(vertex)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
        
        return result;
    }
}
```

**应用场景**：
- 最短路径（无权图）
- 层序遍历
- 连通性检测
- 拓扑排序

### 深度优先搜索（DFS）

DFS 从起始顶点开始，沿着一条路径尽可能深地探索，使用栈或递归实现：

```java
public class GraphDFS {
    // 递归实现
    public List<Integer> dfsRecursive(List<List<Integer>> adj, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[adj.size()];
        dfsHelper(adj, start, visited, result);
        return result;
    }
    
    private void dfsHelper(List<List<Integer>> adj, int vertex, 
                           boolean[] visited, List<Integer> result) {
        visited[vertex] = true;
        result.add(vertex);
        
        for (int neighbor : adj.get(vertex)) {
            if (!visited[neighbor]) {
                dfsHelper(adj, neighbor, visited, result);
            }
        }
    }
    
    // 迭代实现（使用栈）
    public List<Integer> dfsIterative(List<List<Integer>> adj, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[adj.size()];
        Stack<Integer> stack = new Stack<>();
        
        stack.push(start);
        
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            if (visited[vertex]) continue;
            
            visited[vertex] = true;
            result.add(vertex);
            
            // 注意：需要逆序入栈，才能保证正确的遍历顺序
            List<Integer> neighbors = adj.get(vertex);
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                if (!visited[neighbors.get(i)]) {
                    stack.push(neighbors.get(i));
                }
            }
        }
        
        return result;
    }
}
```

**应用场景**：
- 路径查找
- 环检测
- 连通分量
- 拓扑排序

## 最短路径算法

### Dijkstra 算法

适用于非负权重的单源最短路径：

```java
public class Dijkstra {
    /**
     * Dijkstra 算法：求从源点到所有其他顶点的最短路径
     * 时间复杂度：O((V + E) log V)，使用优先队列
     */
    public int[] dijkstra(List<List<int[]>> adj, int source) {
        int n = adj.size();
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[source] = 0;
        
        // 优先队列：[距离, 顶点]
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[]{0, source});
        
        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int d = curr[0];
            int u = curr[1];
            
            // 如果已经有更短的路径，跳过
            if (d > dist[u]) continue;
            
            // 遍历所有邻居
            for (int[] edge : adj.get(u)) {
                int v = edge[0];
                int weight = edge[1];
                
                if (dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    pq.offer(new int[]{dist[v], v});
                }
            }
        }
        
        return dist;
    }
}
```

### Floyd-Warshall 算法

适用于所有顶点对之间的最短路径：

```java
public class FloydWarshall {
    /**
     * Floyd-Warshall 算法：求所有顶点对之间的最短路径
     * 时间复杂度：O(V³)
     * 空间复杂度：O(V²)
     */
    public int[][] floyd(int[][] graph) {
        int n = graph.length;
        int[][] dist = new int[n][n];
        
        // 初始化距离矩阵
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = graph[i][j];
            }
        }
        
        // k 作为中间顶点
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != Integer.MAX_VALUE && 
                        dist[k][j] != Integer.MAX_VALUE &&
                        dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }
        
        return dist;
    }
}
```

### 算法对比

| 算法 | 时间复杂度 | 适用场景 | 限制 |
|------|------------|----------|------|
| Dijkstra | O((V+E)logV) | 单源最短路径 | 不能有负权边 |
| Bellman-Ford | O(VE) | 单源最短路径 | 可以检测负权环 |
| Floyd-Warshall | O(V³) | 所有顶点对 | 图不能太大 |

## 拓扑排序

拓扑排序是对有向无环图（DAG）的顶点进行线性排序，使得对于每条边 (u, v)，u 在排序中出现在 v 之前。

```java
public class TopologicalSort {
    /**
     * Kahn 算法（BFS 方式）
     * 核心思想：每次选择入度为 0 的顶点
     */
    public List<Integer> kahn(List<List<Integer>> adj) {
        int n = adj.size();
        int[] inDegree = new int[n];
        
        // 计算每个顶点的入度
        for (int u = 0; u < n; u++) {
            for (int v : adj.get(u)) {
                inDegree[v]++;
            }
        }
        
        // 将所有入度为 0 的顶点加入队列
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        List<Integer> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            result.add(u);
            
            for (int v : adj.get(u)) {
                inDegree[v]--;
                if (inDegree[v] == 0) {
                    queue.offer(v);
                }
            }
        }
        
        // 如果结果数量不等于顶点数量，说明有环
        if (result.size() != n) {
            throw new RuntimeException("图中存在环，无法进行拓扑排序");
        }
        
        return result;
    }
    
    /**
     * DFS 方式
     * 核心思想：后序遍历的逆序
     */
    public List<Integer> dfs(List<List<Integer>> adj) {
        int n = adj.size();
        boolean[] visited = new boolean[n];
        boolean[] onPath = new boolean[n];  // 用于检测环
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (hasCycle(adj, i, visited, onPath, stack)) {
                    throw new RuntimeException("图中存在环");
                }
            }
        }
        
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }
    
    private boolean hasCycle(List<List<Integer>> adj, int u, 
                            boolean[] visited, boolean[] onPath, Stack<Integer> stack) {
        visited[u] = true;
        onPath[u] = true;
        
        for (int v : adj.get(u)) {
            if (onPath[v]) return true;  // 发现环
            if (!visited[v] && hasCycle(adj, v, visited, onPath, stack)) {
                return true;
            }
        }
        
        onPath[u] = false;
        stack.push(u);
        return false;
    }
}
```

**应用场景**：
- 课程安排
- 任务调度
- 编译顺序
- 依赖解析

## 并查集（Union-Find）

并查集用于处理不相交集合的合并与查询问题：

```java
public class UnionFind {
    private int[] parent;
    private int[] rank;
    private int count;  // 连通分量数量
    
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        count = n;
        
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
    }
    
    // 查找根节点（路径压缩）
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);  // 路径压缩
        }
        return parent[x];
    }
    
    // 合并两个集合（按秩合并）
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        
        if (rootX == rootY) return false;  // 已经在同一集合
        
        // 按秩合并：将秩小的树合并到秩大的树
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        
        count--;
        return true;
    }
    
    // 检查两个元素是否在同一集合
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }
    
    // 获取连通分量数量
    public int getCount() {
        return count;
    }
}
```

**应用场景**：
- 连通性检测
- 最小生成树（Kruskal 算法）
- 朋友圈/社交网络
- 图像处理中的连通区域

## 字典树（Trie）

字典树是一种树形数据结构，用于高效地存储和检索字符串：

```java
public class Trie {
    private TrieNode root;
    
    public Trie() {
        root = new TrieNode();
    }
    
    // 插入一个单词
    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                node.children[index] = new TrieNode();
            }
            node = node.children[index];
        }
        node.isEnd = true;
    }
    
    // 搜索单词是否存在
    public boolean search(String word) {
        TrieNode node = searchPrefix(word);
        return node != null && node.isEnd;
    }
    
    // 检查是否有以 prefix 为前缀的单词
    public boolean startsWith(String prefix) {
        return searchPrefix(prefix) != null;
    }
    
    private TrieNode searchPrefix(String prefix) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            int index = c - 'a';
            if (node.children[index] == null) {
                return null;
            }
            node = node.children[index];
        }
        return node;
    }
    
    private static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEnd;
    }
}
```

**应用场景**：
- 自动补全
- 拼写检查
- IP 路由（最长前缀匹配）
- 单词搜索

## 线段树

线段树用于处理区间查询和区间更新：

```java
public class SegmentTree {
    private int[] tree;
    private int n;
    
    public SegmentTree(int[] arr) {
        n = arr.length;
        tree = new int[4 * n];
        build(arr, 1, 0, n - 1);
    }
    
    // 建树
    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2 * node, start, mid);
            build(arr, 2 * node + 1, mid + 1, end);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }
    }
    
    // 区间查询
    public int query(int left, int right) {
        return query(1, 0, n - 1, left, right);
    }
    
    private int query(int node, int start, int end, int left, int right) {
        if (right < start || end < left) {
            return 0;  // 无交集
        }
        if (left <= start && end <= right) {
            return tree[node];  // 完全包含
        }
        int mid = (start + end) / 2;
        int leftSum = query(2 * node, start, mid, left, right);
        int rightSum = query(2 * node + 1, mid + 1, end, left, right);
        return leftSum + rightSum;
    }
    
    // 单点更新
    public void update(int index, int value) {
        update(1, 0, n - 1, index, value);
    }
    
    private void update(int node, int start, int end, int index, int value) {
        if (start == end) {
            tree[node] = value;
        } else {
            int mid = (start + end) / 2;
            if (index <= mid) {
                update(2 * node, start, mid, index, value);
            } else {
                update(2 * node + 1, mid + 1, end, index, value);
            }
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }
    }
}
```

**应用场景**：
- 区间求和
- 区间最值
- 区间更新
- 统计问题

## 布隆过滤器

布隆过滤器是一种概率型数据结构，用于判断一个元素是否在集合中：

```java
public class BloomFilter {
    private BitSet bitSet;
    private int size;
    private int hashCount;
    
    public BloomFilter(int size, int hashCount) {
        this.size = size;
        this.hashCount = hashCount;
        this.bitSet = new BitSet(size);
    }
    
    // 添加元素
    public void add(String value) {
        for (int i = 0; i < hashCount; i++) {
            int hash = hash(value, i);
            bitSet.set(hash);
        }
    }
    
    // 检查元素是否可能存在
    public boolean mightContain(String value) {
        for (int i = 0; i < hashCount; i++) {
            int hash = hash(value, i);
            if (!bitSet.get(hash)) {
                return false;  // 一定不存在
            }
        }
        return true;  // 可能存在（有误判率）
    }
    
    // 使用不同的种子生成多个哈希值
    private int hash(String value, int seed) {
        int hash = 0;
        for (char c : value.toCharArray()) {
            hash = seed * hash + c;
        }
        return Math.abs(hash) % size;
    }
}
```

**特性**：
- **优点**：空间效率高、查询时间 O(k)
- **缺点**：有误判率（可能误报存在）、不支持删除
- **适用场景**：缓存穿透防护、爬虫 URL 去重、垃圾邮件过滤

**误判率计算**：

```
p ≈ (1 - e^(-kn/m))^k

其中：
- m：位数组大小
- k：哈希函数数量
- n：已插入元素数量
```

## 高级数据结构对比

| 数据结构 | 查询 | 插入 | 删除 | 空间 | 适用场景 |
|----------|------|------|------|------|----------|
| 并查集 | O(α(n)) | O(α(n)) | - | O(n) | 连通性 |
| 字典树 | O(m) | O(m) | O(m) | O(ALPHABET * m * n) | 字符串前缀 |
| 线段树 | O(log n) | O(log n) | O(log n) | O(4n) | 区间操作 |
| 布隆过滤器 | O(k) | O(k) | - | O(m) | 存在性判断 |

其中 α 是阿克曼函数的反函数，实际可以认为是常数。

掌握这些图算法和高级数据结构，能够帮助你解决更复杂的问题，在算法面试中脱颖而出。关键是要理解每种数据结构的特性、适用场景和实现原理。
