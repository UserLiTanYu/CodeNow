/**
 * 分类树工具函数
 * 提供分类树的扁平化、查找和级联选择器数据转换功能
 */

/**
 * 将树形分类列表扁平化为一维数组
 * 保留完整路径文本，供下拉列表在同名子分类场景中显示可区分的标签
 * @param {Array} categories - 树形分类列表
 * @param {string} prefix - 路径前缀（递归用）
 * @returns {Array} 扁平化后的分类数组，每项包含 path 属性表示完整路径
 */
export function flattenCategories(categories = [], prefix = '') {
  return categories.flatMap((category) => {
    const path = prefix ? `${prefix} / ${category.name}` : category.name
    return [
      { ...category, path },
      ...flattenCategories(category.children || [], path),
    ]
  })
}

/**
 * 在分类树中查找指定 ID 的分类
 * @param {Array} categories - 树形分类列表
 * @param {number|string} id - 要查找的分类 ID
 * @returns {Object|null} 找到的分类对象，未找到返回 null
 */
export function findCategory(categories = [], id) {
  for (const category of categories) {
    if (Number(category.id) === Number(id)) return category
    const child = findCategory(category.children || [], id)
    if (child) return child
  }
  return null
}

/**
 * 将分类树转换为 Element Plus 级联选择器所需的 options 格式
 * @param {Array} categories - 树形分类列表
 * @param {number|string|null} disabledId - 需要禁用的分类 ID（通常为当前编辑的分类自身）
 * @returns {Array} 级联选择器 options 数组
 */
export function categoryCascaderOptions(categories = [], disabledId = null) {
  return categories.map((category) => ({
    value: category.id,
    label: category.name,
    disabled: Number(category.id) === Number(disabledId),
    children: category.children?.length
      ? categoryCascaderOptions(category.children, disabledId)
      : undefined,
  }))
}
