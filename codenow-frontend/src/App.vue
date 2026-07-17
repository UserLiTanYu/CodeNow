<template>
  <router-view />
</template>

<style>
:root {
  --blog-content-max-width: 1440px;
  --blog-sidebar-width: 320px;
  --blog-layout-gap: 24px;
  --blog-space-1: 4px;
  --blog-space-2: 8px;
  --blog-space-3: 12px;
  --blog-space-4: 16px;
  --blog-space-5: 24px;
  --blog-space-6: 32px;
  --blog-color-primary: #337ecc;
  --blog-color-primary-strong: #2b6fb7;
  --blog-color-primary-soft: #ecf5ff;
  --blog-color-primary-disabled: #a0cfff;
  --blog-color-text: #252b33;
  --blog-color-text-secondary: #606873;
  --blog-color-text-muted: #9098a3;
  --blog-color-border: #e4e7ed;
  --blog-color-border-hover: #b3d8ff;
  --blog-color-background: #f5f7fa;
  --blog-color-surface: #fff;
  --blog-radius-card: 10px;
  --blog-radius-button: 6px;
  --blog-radius-tag: 4px;
  --blog-radius-avatar: 50%;
  --blog-shadow-hover: 0 6px 18px rgba(31, 45, 61, 0.08);
  --blog-focus-ring: 0 0 0 3px rgba(51, 126, 204, 0.24);
}

body {
  margin: 0;
  padding: 0;
}

#app .el-pagination {
  justify-content: center;
}
</style>
