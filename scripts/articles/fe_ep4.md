# Element Plus 表格进阶与大数据处理

`el-table` 是 Element Plus 中使用频率最高的组件之一。后台管理系统中几乎每个页面都离不开表格，但很多开发者只停留在基础的列表展示层面。本文将从高级用法、性能优化、实战封装三个维度，系统讲解 `el-table` 的进阶技巧。

## 多级表头与合并行列

### 多级表头

当数据结构复杂时，单层表头无法满足需求。`el-table` 通过嵌套 `el-table-column` 实现多级表头：

```vue
<el-table :data="tableData">
  <el-table-column prop="date" label="日期" width="180" />
  <el-table-column label="配送信息">
    <el-table-column prop="name" label="姓名" width="120" />
    <el-table-column prop="address" label="地址" />
  </el-table-column>
  <el-table-column label="财务">
    <el-table-column prop="amount" label="金额" width="100" />
    <el-table-column prop="status" label="状态" width="100" />
  </el-table-column>
</el-table>
```

嵌套的 `el-table-column` 会自动渲染为带 `colspan` 的表头，层级深度没有硬性限制，但建议不超过三层以保证可读性。

### 合并行列（span-method）

`span-method` 接收一个函数，返回 `[rowspan, colspan]` 来控制单元格的合并：

```vue
<el-table :data="tableData" :span-method="spanMethod">
  <el-table-column prop="area" label="区域" />
  <el-table-column prop="shop" label="门店" />
  <el-table-column prop="sales" label="销售额" />
</el-table>

<script setup>
const spanMethod = ({ row, column, rowIndex, columnIndex }) => {
  if (columnIndex === 0) {
    // 按区域合并第一列
    const sameAreaCount = tableData.filter(
      (item) => item.area === row.area
    ).length
    const firstIndex = tableData.findIndex(
      (item) => item.area === row.area
    )
    if (rowIndex === firstIndex) {
      return [sameAreaCount, 1]
    }
    return [0, 0]
  }
}
</script>
```

对于大数据量的合并场景，建议预处理数据生成 `rowSpanMap`，避免每行渲染时重复计算：

```js
function buildSpanMap(data, key) {
  const map = new Map()
  let prevValue = null
  let span = 0
  let startIndex = 0

  data.forEach((row, index) => {
    if (row[key] === prevValue) {
      span++
    } else {
      if (span > 0) map.set(startIndex, span)
      prevValue = row[key]
      span = 1
      startIndex = index
    }
  })
  if (span > 0) map.set(startIndex, span)
  return map
}
```

### 固定列与表头

通过 `fixed` 属性固定列，`height` 属性固定表头：

```vue
<el-table :data="tableData" height="400" style="width: 100%">
  <el-table-column prop="id" label="ID" width="80" fixed="left" />
  <el-table-column prop="name" label="名称" width="150" />
  <!-- 中间列自适应宽度 -->
  <el-table-column prop="description" label="描述" />
  <el-table-column prop="action" label="操作" width="120" fixed="right">
    <template #default="{ row }">
      <el-button size="small" @click="handleEdit(row)">编辑</el-button>
    </template>
  </el-table-column>
</el-table>
```

当固定列与多级表头混合使用时，注意固定列的 `fixed` 需要设置在最外层父列上。

## 自定义列模板与作用域插槽

`el-table-column` 的 `#default` 插槽提供了行数据的访问能力：

```vue
<el-table-column label="状态">
  <template #default="{ row, $index }">
    <el-tag :type="row.status === 1 ? 'success' : 'danger'">
      {{ row.status === 1 ? '启用' : '禁用' }}
    </el-tag>
  </template>
</el-table-column>
```

通过 `#header` 插槽自定义表头：

```vue
<el-table-column prop="name" label="名称">
  <template #header>
    <span>名称</span>
    <el-tooltip content="这是名称列的说明">
      <el-icon><QuestionFilled /></el-icon>
    </el-tooltip>
  </template>
</el-table-column>
```

对于操作列，推荐使用 `v-for` 动态生成按钮：

```vue
<el-table-column label="操作" width="200">
  <template #default="{ row }">
    <el-button
      v-for="action in getActions(row)"
      :key="action.key"
      :type="action.type"
      size="small"
      @click="action.handler(row)"
    >
      {{ action.label }}
    </el-button>
  </template>
</el-table-column>
```

## 排序、筛选与远程数据

### 排序

通过 `sortable` 属性开启前端排序，或 `sortable: 'custom'` 开启远程排序：

```vue
<el-table :data="tableData" @sort-change="handleSortChange">
  <el-table-column prop="name" label="名称" sortable />
  <el-table-column prop="date" label="日期" sortable="custom" />
  <el-table-column prop="amount" label="金额" sortable="custom" />
</el-table>

<script setup>
const sortParams = ref({ prop: '', order: '' })

function handleSortChange({ prop, order }) {
  sortParams.value = { prop, order }
  fetchData()
}
</script>
```

多列排序需要自行维护排序规则数组，在 `sort-change` 事件中累加：

```js
const sortList = ref([])

function handleSortChange({ prop, order }) {
  const index = sortList.value.findIndex((s) => s.prop === prop)
  if (index > -1) {
    if (order) {
      sortList.value[index].order = order
    } else {
      sortList.value.splice(index, 1)
    }
  } else if (order) {
    sortList.value.push({ prop, order })
  }
  fetchData()
}
```

### 筛选

`filters` 定义筛选选项，`filter-method` 定义筛选逻辑：

```vue
<el-table-column
  prop="status"
  label="状态"
  :filters="[
    { text: '启用', value: 1 },
    { text: '禁用', value: 0 },
  ]"
  :filter-method="filterStatus"
/>
```

对于远程筛选，使用 `filter-change` 事件：

```vue
<el-table-column
  prop="category"
  label="分类"
  :filters="categoryFilters"
  column-key="category"
  filter-placement="bottom-end"
  :filter-multiple="false"
/>

<script setup>
function handleFilterChange(filters) {
  queryFilters.value = { ...queryFilters.value, ...filters }
  fetchData()
}
</script>
```

## 虚拟滚动表格处理万级数据

当数据量超过 500 行时，DOM 节点过多会导致明显卡顿。Element Plus 提供了 `el-table-v2`（Virtualized Table）来解决这个问题：

```vue
<el-auto-resizer>
  <template #default="{ height, width }">
    <el-table-v2
      :columns="columns"
      :data="data"
      :width="width"
      :height="height"
      :row-height="48"
      fixed
    />
  </template>
</el-auto-resizer>

<script setup>
const columns = [
  { key: 'id', title: 'ID', width: 100, dataKey: 'id' },
  { key: 'name', title: '名称', width: 200, dataKey: 'name' },
  { key: 'email', title: '邮箱', width: 300, dataKey: 'email' },
]

const data = Array.from({ length: 100000 }, (_, i) => ({
  id: i + 1,
  name: `用户 ${i + 1}`,
  email: `user${i + 1}@example.com`,
}))
</script>
```

`el-table-v2` 支持固定列、排序、行点击等常用功能，但不支持 `span-method` 和 `filters` 等高级特性。如果需要合并行列，需要自定义单元格渲染。

对于需要保留 `el-table` API 的场景，可以结合虚拟滚动库实现：

```vue
<RecycleScroller
  :items="filteredData"
  :item-size="48"
  key-field="id"
  page-mode
>
  <template #default="{ item }">
    <div class="table-row">
      <span>{{ item.name }}</span>
      <span>{{ item.email }}</span>
    </div>
  </template>
</RecycleScroller>
```

## 表格内编辑

行内编辑是后台管理中常见的交互模式。核心思路是为每行维护一个编辑状态：

```vue
<script setup>
const editingRows = ref(new Set())

function startEdit(row) {
  editingRows.value.add(row.id)
  row._backup = { ...row }
}

function cancelEdit(row) {
  Object.assign(row, row._backup)
  editingRows.value.delete(row.id)
  delete row._backup
}

async function saveEdit(row) {
  await updateApi(row.id, row)
  editingRows.value.delete(row.id)
  delete row._backup
}
</script>
```

在模板中根据编辑状态切换显示：

```vue
<el-table-column prop="name" label="名称">
  <template #default="{ row }">
    <el-input
      v-if="editingRows.has(row.id)"
      v-model="row.name"
      size="small"
    />
    <span v-else>{{ row.name }}</span>
  </template>
</el-table-column>
```

对于可编辑单元格，可以封装一个通用组件：

```vue
<!-- EditableCell.vue -->
<template>
  <div @dblclick="editing = true">
    <el-input
      v-if="editing"
      v-model="localValue"
      size="small"
      @blur="handleBlur"
      @keyup.enter="handleBlur"
      ref="inputRef"
    />
    <span v-else>{{ modelValue }}</span>
  </div>
</template>

<script setup>
const props = defineProps({ modelValue: [String, Number] })
const emit = defineEmits(['update:modelValue'])
const editing = ref(false)
const localValue = ref(props.modelValue)
const inputRef = ref()

watch(editing, (val) => {
  if (val) {
    localValue.value = props.modelValue
    nextTick(() => inputRef.value?.focus())
  }
})

function handleBlur() {
  editing.value = false
  if (localValue.value !== props.modelValue) {
    emit('update:modelValue', localValue.value)
  }
}
</script>
```

## 表格与分页联动

### 远程分页

最常见的模式是前后端分页，后端返回总数和当前页数据：

```vue
<el-table :data="tableData" v-loading="loading">
  <!-- columns -->
</el-table>
<el-pagination
  v-model:current-page="pagination.page"
  v-model:page-size="pagination.size"
  :total="pagination.total"
  :page-sizes="[10, 20, 50, 100]"
  layout="total, sizes, prev, pager, next, jumper"
  @size-change="fetchData"
  @current-change="fetchData"
/>
```

### 前端分页

数据量不大时可以在前端完成分页：

```js
const allData = ref([])
const pagination = ref({ page: 1, size: 10 })

const paginatedData = computed(() => {
  const start = (pagination.value.page - 1) * pagination.value.size
  return allData.value.slice(start, start + pagination.value.size)
})
```

## 表格导出

使用 `xlsx` 库实现前端导出 Excel：

```bash
npm install xlsx file-saver
```

```js
import { utils, write } from 'xlsx'
import { saveAs } from 'file-saver'

function exportToExcel(data, columns, filename = 'export.xlsx') {
  const header = columns.map((col) => col.label)
  const rows = data.map((row) =>
    columns.map((col) => row[col.prop])
  )

  const ws = utils.aoa_to_sheet([header, ...rows])
  const wb = utils.book_new()
  utils.book_append_sheet(wb, ws, 'Sheet1')

  const buffer = write(wb, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  saveAs(blob, filename)
}
```

对于带样式的导出，可以使用 `xlsx-style` 或 `exceljs`：

```js
import ExcelJS from 'exceljs'

async function exportWithStyle(data, columns, filename) {
  const workbook = new ExcelJS.Workbook()
  const sheet = workbook.addWorksheet('Sheet1')

  sheet.columns = columns.map((col) => ({
    header: col.label,
    key: col.prop,
    width: col.width || 20,
  }))

  // 设置表头样式
  sheet.getRow(1).font = { bold: true, size: 12 }
  sheet.getRow(1).fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: 'FF409EFF' },
  }

  data.forEach((row) => sheet.addRow(row))

  const buffer = await workbook.xlsx.writeBuffer()
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  saveAs(blob, filename)
}
```

CSV 导出更简单，可以直接拼接字符串：

```js
function exportToCSV(data, columns, filename = 'export.csv') {
  const BOM = '\uFEFF'
  const header = columns.map((col) => `"${col.label}"`).join(',')
  const rows = data.map((row) =>
    columns
      .map((col) => {
        const val = row[col.prop] ?? ''
        return `"${String(val).replace(/"/g, '""')}"`
      })
      .join(',')
  )

  const csv = BOM + [header, ...rows].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  saveAs(blob, filename)
}
```

## 表格性能优化

### 避免不必要的重渲染

`el-table` 内部会对行做浅比较。确保 `data` 数组的引用只在数据真正变化时更新：

```js
// 避免：每次请求都创建新数组引用
// tableData.value = response.data

// 推荐：对比后决定是否更新
if (!isEqual(tableData.value, response.data)) {
  tableData.value = response.data
}
```

对于不需要响应式的大数据，使用 `shallowRef`：

```js
const tableData = shallowRef([])
```

### 懒加载行数据

树形表格支持 `lazy` 模式，按需加载子节点：

```vue
<el-table :data="treeData" row-key="id" lazy :load="loadChildren">
  <el-table-column prop="name" label="名称" />
</el-table>

<script setup>
async function loadChildren(row, treeNode, resolve) {
  const children = await fetchChildrenApi(row.id)
  resolve(children)
}
</script>
```

### 虚拟化与分页选择

当数据量在 500-5000 行之间，不想用分页但又需要流畅体验时，可以考虑：

1. 使用 `el-table-v2` 替代 `el-table`
2. 减少不必要的列（超过 10 列考虑用可展开行）
3. 关闭不需要的功能（`:show-overflow-tooltip="false"`）
4. 图片使用懒加载，避免一次性加载所有缩略图

## 实战：通用表格组件封装

一个成熟的后台管理系统通常需要封装通用表格组件，将表格、分页、搜索、导出整合在一起：

```vue
<!-- ProTable.vue -->
<template>
  <div class="pro-table">
    <!-- 搜索区域 -->
    <el-form v-if="searchFields.length" :inline="true" @submit.prevent="handleSearch">
      <el-form-item v-for="field in searchFields" :key="field.prop" :label="field.label">
        <el-input
          v-if="field.type === 'input'"
          v-model="searchForm[field.prop]"
          :placeholder="field.placeholder"
          clearable
        />
        <el-select
          v-else-if="field.type === 'select'"
          v-model="searchForm[field.prop]"
          clearable
        >
          <el-option
            v-for="opt in field.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <div class="toolbar">
      <slot name="toolbar" />
      <el-button v-if="exportable" @click="handleExport">导出</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      v-bind="$attrs"
      @sort-change="handleSortChange"
    >
      <slot />
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="pagination"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="pageSizes"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchData"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  request: { type: Function, required: true },
  columns: { type: Array, default: () => [] },
  searchFields: { type: Array, default: () => [] },
  pageSizes: { type: Array, default: () => [10, 20, 50] },
  exportable: { type: Boolean, default: false },
  defaultParams: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['export'])
const loading = ref(false)
const tableData = ref([])
const pagination = ref({ page: 1, size: 10, total: 0 })
const searchForm = ref({})
const sortParams = ref({})

async function fetchData() {
  loading.value = true
  try {
    const params = {
      ...searchForm.value,
      ...sortParams.value,
      ...props.defaultParams,
      page: pagination.value.page,
      size: pagination.value.size,
    }
    const res = await props.request(params)
    tableData.value = res.data.list
    pagination.value.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function handleReset() {
  searchForm.value = {}
  pagination.value.page = 1
  fetchData()
}

function handleSortChange({ prop, order }) {
  sortParams.value = order ? { prop, order } : {}
  fetchData()
}

function handleExport() {
  emit('export', tableData.value)
}

onMounted(() => fetchData())

defineExpose({ fetchData, handleSearch, handleReset })
</script>
```

使用时只需传入请求函数和列定义：

```vue
<ProTable
  :request="getUserList"
  :search-fields="searchFields"
  row-key="id"
  exportable
  @export="handleExport"
>
  <el-table-column prop="id" label="ID" width="80" />
  <el-table-column prop="name" label="姓名" />
  <el-table-column prop="email" label="邮箱" />
  <el-table-column label="操作" width="150">
    <template #default="{ row }">
      <el-button size="small" @click="edit(row)">编辑</el-button>
      <el-button size="small" type="danger" @click="del(row)">删除</el-button>
    </template>
  </el-table-column>
</ProTable>
```

这种封装方式将请求、搜索、分页、排序、导出统一管理，减少了大量重复代码。核心是通过 `request` prop 传入数据获取函数，由外部控制 API 细节，内部只负责状态管理和 UI 渲染。
