export function flattenCategories(categories = [], prefix = '') {
  return categories.flatMap((category) => {
    const path = prefix ? `${prefix} / ${category.name}` : category.name
    return [
      { ...category, path },
      ...flattenCategories(category.children || [], path),
    ]
  })
}

export function findCategory(categories = [], id) {
  for (const category of categories) {
    if (Number(category.id) === Number(id)) return category
    const child = findCategory(category.children || [], id)
    if (child) return child
  }
  return null
}

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
