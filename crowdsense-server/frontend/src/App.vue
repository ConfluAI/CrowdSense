<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
}

/* ====== 全局玻璃态卡片 ====== */
.glass-card {
  background: rgba(255, 255, 255, 0.82) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  border-radius: 14px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04) !important;
}
.glass-card .el-card__header {
  background: rgba(0, 0, 0, 0.02);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px 14px 0 0;
  padding: 16px 20px;
  font-weight: 600;
}
.glass-card .el-card__body {
  border-radius: 0 0 14px 14px;
}

/* 玻璃态表格包裹 */
.glass-table {
  background: rgba(255, 255, 255, 0.78) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  border-radius: 12px !important;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06) !important;
}

/* 全局卡片标题 */
.card-title {
  font-size: 16px;
  font-weight: 650;
  color: #1a1a2e;
  letter-spacing: 0.5px;
}

/* 全局按钮圆角 */
.el-button { border-radius: 8px !important; }
.el-button--primary {
  background: linear-gradient(135deg, #409EFF, #66b1ff) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.3) !important;
  transition: transform 0.15s, box-shadow 0.15s;
}
.el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4) !important;
}
.el-button--danger {
  box-shadow: 0 4px 14px rgba(245, 108, 108, 0.3) !important;
  transition: transform 0.15s, box-shadow 0.15s;
}
.el-button--danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(245, 108, 108, 0.4) !important;
}

/* 全局输入框圆角 */
.el-input__wrapper { border-radius: 8px !important; }
.el-select .el-input__wrapper { border-radius: 8px !important; }
.el-select-dropdown, .el-picker-panel { border-radius: 12px !important; overflow: hidden; }

/* 全局分页 */
.el-pagination { margin-top: 16px; justify-content: flex-end; }
.el-pagination .el-pager li { border-radius: 6px !important; }

/* 全局标签 */
.el-tag { border-radius: 6px !important; font-weight: 500; }

/* 菜单项动画 */
.el-menu-item {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* 搜索表单玻璃 */
.search-form-glass {
  background: rgba(255, 255, 255, 0.55) !important;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 12px;
  padding: 18px 20px 4px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.35);
}
</style>
