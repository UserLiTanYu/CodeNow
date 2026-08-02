# 排序算法与二分查找

排序算法是计算机科学中最基础、最重要的算法之一。掌握各种排序算法的原理、时间复杂度和适用场景，是每个开发者的必修课。本文将系统讲解常见的排序算法，并深入探讨二分查找的各种变体。

## 基础排序算法

### 冒泡排序

冒泡排序是最简单的排序算法，通过相邻元素的比较和交换，将最大的元素逐步"冒泡"到数组末尾。

```java
public class BubbleSort {
    public static void sort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            // 如果本轮没有交换，说明已经有序
            if (!swapped) break;
        }
    }
}
```

- **时间复杂度**：最好 O(n)（已有序），最坏 O(n²)，平均 O(n²)
- **空间复杂度**：O(1)
- **稳定性**：稳定

### 选择排序

选择排序每次从未排序部分选择最小的元素，放到已排序部分的末尾。

```java
public class SelectionSort {
    public static void sort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int temp = arr[i];
                arr[i] = arr[minIndex];
                arr[minIndex] = temp;
            }
        }
    }
}
```

- **时间复杂度**：任何情况都是 O(n²)
- **空间复杂度**：O(1)
- **稳定性**：不稳定（例如 [5, 5, 3]，第一个 5 可能会和 3 交换）

### 插入排序

插入排序将数组分为已排序和未排序两部分，每次从未排序部分取一个元素，插入到已排序部分的正确位置。

```java
public class InsertionSort {
    public static void sort(int[] arr) {
        int n = arr.length;
        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;
            // 将大于 key 的元素向后移动
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }
    }
}
```

- **时间复杂度**：最好 O(n)（已有序），最坏 O(n²)，平均 O(n²)
- **空间复杂度**：O(1)
- **稳定性**：稳定

**特点**：插入排序在数据量小或近乎有序的情况下表现很好，很多高级排序算法在小数组时会切换到插入排序。

## 高级排序算法

### 希尔排序

希尔排序是插入排序的改进版，通过设置不同的间隔（gap）对数组进行分组插入排序，逐步缩小间隔直到 1。

```java
public class ShellSort {
    public static void sort(int[] arr) {
        int n = arr.length;
        // 初始间隔为数组长度的一半，逐步缩小
        for (int gap = n / 2; gap > 0; gap /= 2) {
            // 对每个间隔的子序列进行插入排序
            for (int i = gap; i < n; i++) {
                int temp = arr[i];
                int j = i;
                while (j >= gap && arr[j - gap] > temp) {
                    arr[j] = arr[j - gap];
                    j -= gap;
                }
                arr[j] = temp;
            }
        }
    }
}
```

- **时间复杂度**：取决于间隔序列，约为 O(n^1.3)
- **空间复杂度**：O(1)
- **稳定性**：不稳定

### 归并排序

归并排序采用分治思想，将数组分成两半，分别排序后合并。

```java
public class MergeSort {
    public static void sort(int[] arr) {
        int[] temp = new int[arr.length];
        mergeSort(arr, 0, arr.length - 1, temp);
    }
    
    private static void mergeSort(int[] arr, int left, int right, int[] temp) {
        if (left >= right) return;
        
        int mid = left + (right - left) / 2;
        mergeSort(arr, left, mid, temp);
        mergeSort(arr, mid + 1, right, temp);
        merge(arr, left, mid, right, temp);
    }
    
    private static void merge(int[] arr, int left, int mid, int right, int[] temp) {
        int i = left, j = mid + 1, k = left;
        
        while (i <= mid && j <= right) {
            if (arr[i] <= arr[j]) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }
        
        while (i <= mid) temp[k++] = arr[i++];
        while (j <= right) temp[k++] = arr[j++];
        
        // 复制回原数组
        System.arraycopy(temp, left, arr, left, right - left + 1);
    }
}
```

- **时间复杂度**：任何情况都是 O(n log n)
- **空间复杂度**：O(n)
- **稳定性**：稳定

### 快速排序

快速排序是最常用的排序算法之一，通过选择一个基准元素，将数组分为小于和大于基准的两部分，递归排序。

```java
public class QuickSort {
    public static void sort(int[] arr) {
        quickSort(arr, 0, arr.length - 1);
    }
    
    private static void quickSort(int[] arr, int low, int high) {
        if (low >= high) return;
        
        // 三数取中法选择基准，避免最坏情况
        int mid = low + (high - low) / 2;
        if (arr[low] > arr[high]) swap(arr, low, high);
        if (arr[mid] > arr[high]) swap(arr, mid, high);
        if (arr[low] < arr[mid]) swap(arr, low, mid);
        
        int pivot = arr[low];
        int i = low, j = high;
        
        while (i < j) {
            while (i < j && arr[j] >= pivot) j--;
            while (i < j && arr[i] <= pivot) i++;
            if (i < j) swap(arr, i, j);
        }
        
        // 将基准放到正确位置
        swap(arr, low, i);
        
        quickSort(arr, low, i - 1);
        quickSort(arr, i + 1, high);
    }
    
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
```

- **时间复杂度**：最好 O(n log n)，最坏 O(n²)（已排序数组），平均 O(n log n)
- **空间复杂度**：O(log n)（递归栈）
- **稳定性**：不稳定

**优化技巧**：
1. 三数取中法选择基准
2. 小数组切换到插入排序
3. 三路快排处理大量重复元素

### 堆排序

堆排序利用堆这种数据结构进行排序。堆是一个完全二叉树，分为最大堆和最小堆。

```java
public class HeapSort {
    public static void sort(int[] arr) {
        int n = arr.length;
        
        // 建堆（从最后一个非叶子节点开始）
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        
        // 排序：将堆顶元素（最大值）与末尾交换，然后调整堆
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);
            heapify(arr, i, 0);
        }
    }
    
    /**
     * 调整堆：以 i 为根的子树调整为最大堆
     */
    private static void heapify(int[] arr, int n, int i) {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        
        if (left < n && arr[left] > arr[largest]) {
            largest = left;
        }
        if (right < n && arr[right] > arr[largest]) {
            largest = right;
        }
        
        if (largest != i) {
            swap(arr, i, largest);
            heapify(arr, n, largest);
        }
    }
    
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
```

- **时间复杂度**：任何情况都是 O(n log n)
- **空间复杂度**：O(1)
- **稳定性**：不稳定

## 线性时间排序算法

### 计数排序

计数排序适用于数据范围不大的整数排序，通过统计每个值出现的次数来排序。

```java
public class CountingSort {
    public static void sort(int[] arr) {
        if (arr.length <= 1) return;
        
        // 找出最大值和最小值
        int max = arr[0], min = arr[0];
        for (int num : arr) {
            max = Math.max(max, num);
            min = Math.min(min, num);
        }
        
        int range = max - min + 1;
        int[] count = new int[range];
        int[] output = new int[arr.length];
        
        // 统计每个值出现的次数
        for (int num : arr) {
            count[num - min]++;
        }
        
        // 累加计数
        for (int i = 1; i < range; i++) {
            count[i] += count[i - 1];
        }
        
        // 从后往前遍历，保证稳定性
        for (int i = arr.length - 1; i >= 0; i--) {
            output[count[arr[i] - min] - 1] = arr[i];
            count[arr[i] - min]--;
        }
        
        System.arraycopy(output, 0, arr, 0, arr.length);
    }
}
```

- **时间复杂度**：O(n + k)，k 是数据范围
- **空间复杂度**：O(k)
- **稳定性**：稳定

### 桶排序

桶排序将数据分配到若干个桶中，每个桶单独排序，最后合并。

```java
public class BucketSort {
    public static void sort(int[] arr, int bucketSize) {
        if (arr.length <= 1) return;
        
        int min = arr[0], max = arr[0];
        for (int num : arr) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        
        int bucketCount = (max - min) / bucketSize + 1;
        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            buckets.add(new ArrayList<>());
        }
        
        // 分配到桶
        for (int num : arr) {
            int index = (num - min) / bucketSize;
            buckets.get(index).add(num);
        }
        
        // 每个桶排序后合并
        int index = 0;
        for (List<Integer> bucket : buckets) {
            Collections.sort(bucket);
            for (int num : bucket) {
                arr[index++] = num;
            }
        }
    }
}
```

- **时间复杂度**：平均 O(n + k)，最坏 O(n²)
- **空间复杂度**：O(n + k)
- **稳定性**：取决于桶内排序算法

### 基数排序

基数排序按位排序，从最低位到最高位，每位使用稳定的排序算法（通常是计数排序）。

```java
public class RadixSort {
    public static void sort(int[] arr) {
        if (arr.length <= 1) return;
        
        int max = Arrays.stream(arr).max().getAsInt();
        
        // 按每一位排序
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSortByDigit(arr, exp);
        }
    }
    
    private static void countingSortByDigit(int[] arr, int exp) {
        int[] output = new int[arr.length];
        int[] count = new int[10];
        
        for (int num : arr) {
            int digit = (num / exp) % 10;
            count[digit]++;
        }
        
        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }
        
        for (int i = arr.length - 1; i >= 0; i--) {
            int digit = (arr[i] / exp) % 10;
            output[count[digit] - 1] = arr[i];
            count[digit]--;
        }
        
        System.arraycopy(output, 0, arr, 0, arr.length);
    }
}
```

- **时间复杂度**：O(d * (n + k))，d 是位数，k 是基数（10）
- **空间复杂度**：O(n + k)
- **稳定性**：稳定

## 排序算法对比

| 算法 | 平均时间 | 最好时间 | 最坏时间 | 空间 | 稳定性 |
|------|----------|----------|----------|------|--------|
| 冒泡排序 | O(n²) | O(n) | O(n²) | O(1) | 稳定 |
| 选择排序 | O(n²) | O(n²) | O(n²) | O(1) | 不稳定 |
| 插入排序 | O(n²) | O(n) | O(n²) | O(1) | 稳定 |
| 希尔排序 | O(n^1.3) | O(n) | O(n²) | O(1) | 不稳定 |
| 归并排序 | O(n log n) | O(n log n) | O(n log n) | O(n) | 稳定 |
| 快速排序 | O(n log n) | O(n log n) | O(n²) | O(log n) | 不稳定 |
| 堆排序 | O(n log n) | O(n log n) | O(n log n) | O(1) | 不稳定 |
| 计数排序 | O(n + k) | O(n + k) | O(n + k) | O(k) | 稳定 |
| 桶排序 | O(n + k) | O(n + k) | O(n²) | O(n + k) | 取决于桶内排序 |
| 基数排序 | O(d(n + k)) | O(d(n + k)) | O(d(n + k)) | O(n + k) | 稳定 |

### 如何选择排序算法

```
数据量小（n < 50）→ 插入排序
数据量中等（50 < n < 10000）→ 快速排序
数据量大且要求稳定 → 归并排序
数据量大且内存有限 → 堆排序
整数且范围不大 → 计数排序
浮点数且分布均匀 → 桶排序
整数且位数不多 → 基数排序
```

## 二分查找

二分查找是在有序数组中查找特定元素的高效算法，时间复杂度为 O(log n)。

### 基本二分查找

```java
public class BinarySearch {
    /**
     * 基本二分查找
     * @return 目标元素的索引，不存在返回 -1
     */
    public static int search(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return -1;
    }
}
```

### 查找左边界

查找目标元素第一次出现的位置：

```java
public static int searchLeft(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) {
            result = mid;
            right = mid - 1;  // 继续向左查找
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return result;
}
```

### 查找右边界

查找目标元素最后一次出现的位置：

```java
public static int searchRight(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    int result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) {
            result = mid;
            left = mid + 1;  // 继续向右查找
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return result;
}
```

### 查找插入位置

查找目标元素应该插入的位置（保持有序）：

```java
public static int searchInsert(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    
    return left;
}
```

### 旋转数组查找

在旋转排序数组中查找目标元素：

```java
/**
 * 在旋转排序数组中查找目标元素
 * 例如：[4, 5, 6, 7, 0, 1, 2] 中查找 0
 */
public static int searchRotated(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] == target) {
            return mid;
        }
        
        // 判断哪半部分是有序的
        if (arr[left] <= arr[mid]) {
            // 左半部分有序
            if (arr[left] <= target && target < arr[mid]) {
                right = mid - 1;  // 目标在左半部分
            } else {
                left = mid + 1;   // 目标在右半部分
            }
        } else {
            // 右半部分有序
            if (arr[mid] < target && target <= arr[right]) {
                left = mid + 1;   // 目标在右半部分
            } else {
                right = mid - 1;  // 目标在左半部分
            }
        }
    }
    
    return -1;
}
```

### 二分查找的应用场景

1. **查找有序数组中的元素**：最基本的应用
2. **查找边界**：第一个/最后一个等于目标值的位置
3. **查找峰值**：山峰数组的峰值
4. **搜索二维矩阵**：行列都有序的矩阵
5. **求平方根**：整数平方根
6. **最小化最大值**：如分割数组的最大值

### 二分查找的模板

```java
// 模板 1：基本二分查找
int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        else if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}

// 模板 2：查找左边界
int binarySearchLeft(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] >= target) right = mid - 1;
        else left = mid + 1;
    }
    return left < arr.length && arr[left] == target ? left : -1;
}

// 模板 3：查找右边界
int binarySearchRight(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] <= target) left = mid + 1;
        else right = mid - 1;
    }
    return right >= 0 && arr[right] == target ? right : -1;
}
```

掌握这些排序算法和二分查找的变体，不仅能帮助你解决算法题，更能让你在实际开发中选择最合适的数据处理方案。排序和查找是编程的基础，值得反复练习和深入理解。
