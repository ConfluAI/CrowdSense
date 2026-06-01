<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <svg class="logo-icon" viewBox="0 0 32 32" width="28" height="28">
          <circle cx="10" cy="10" r="4" fill="#409EFF" opacity="0.9"/>
          <circle cx="22" cy="10" r="4" fill="#67c23a" opacity="0.9"/>
          <circle cx="10" cy="22" r="4" fill="#e6a23c" opacity="0.9"/>
          <circle cx="22" cy="22" r="4" fill="#f56c6c" opacity="0.9"/>
          <circle cx="16" cy="16" r="4" fill="#9b59b6" opacity="0.85"/>
        </svg>
        <span class="logo-text">CrowdSense</span>
      </div>
      <el-menu :default-active="$route.path" router class="el-menu-vertical"
        background-color="transparent" text-color="#bfcbd9" active-text-color="#fff">
        <!-- 管理员菜单 -->
        <template v-if="isAdmin">
          <el-menu-item index="/inference_tasks">
            <el-icon><Document /></el-icon><span>人群密度预测</span>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon><span>历史记录</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><Avatar /></el-icon><span>角色管理</span>
          </el-menu-item>
        </template>
        <!-- 普通用户菜单 -->
        <template v-else>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/inference_tasks">
            <el-icon><Document /></el-icon><span>人群密度预测</span>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon><span>历史记录</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="right-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/inference_tasks' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="header-time">{{ currentTime }}</span>
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="28" class="user-avatar">{{ (userInfo.username || 'U')[0].toUpperCase() }}</el-avatar>
              <span class="user-name">{{ userInfo.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-item v-if="!isAdmin" @click="$router.push('/profile')">
                <el-icon><User /></el-icon>个人信息
              </el-dropdown-item>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userInfo = ref({})
const currentTime = ref('')
let timer = null

const currentTitle = computed(() => route.meta?.title || '')

const isAdmin = computed(() => userInfo.value.role === 'admin')

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
}

onMounted(() => {
  const user = localStorage.getItem('user')
  if (user) userInfo.value = JSON.parse(user)
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => { if (timer) clearInterval(timer) })

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  ElMessage.success('退出成功')
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }

.aside {
  background: rgba(10, 12, 20, 0.92) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.logo {
  height: 64px; display: flex; align-items: center; justify-content: center; gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.15);
}
.logo-icon { flex-shrink: 0; filter: drop-shadow(0 2px 6px rgba(0,0,0,0.3)); }
.logo-text { color: #fff; font-size: 18px; font-weight: 800; letter-spacing: 2px; text-shadow: 0 2px 8px rgba(0,0,0,0.3); }

.el-menu-vertical {
  border-right: none;
  background: transparent !important;
}
.el-menu-vertical .el-menu-item {
  margin: 5px 10px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #bfcbd9;
}
.el-menu-vertical .el-menu-item:hover {
  background: rgba(255,255,255,0.1) !important;
  color: #fff;
}
.el-menu-vertical .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(64,158,255,0.85), rgba(102,177,255,0.7)) !important;
  box-shadow: 0 4px 16px rgba(64,158,255,0.35);
  color: #fff;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.right-container { background: transparent; }

.header {
  background: rgba(255,255,255,0.72) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 2px 20px rgba(0,21,41,0.06);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px;
  z-index: 10;
  border-bottom: 1px solid rgba(255,255,255,0.4);
  height: 60px;
}

.header-left { display: flex; align-items: center; }

.header-right { display: flex; align-items: center; gap: 18px; }

.header-time { color: #606266; font-size: 13px; font-family: 'Courier New', monospace; font-weight: 500; }

.user-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; color: #303133; font-size: 14px;
  padding: 6px 12px; border-radius: 20px;
  transition: background 0.25s;
  background: rgba(0,0,0,0.03);
}
.user-info:hover { background: rgba(0,0,0,0.06); }
.user-avatar { background: linear-gradient(135deg, #409EFF, #66b1ff); color: #fff; font-weight: 700; }
.user-name { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }

.main {
  background: transparent;
  padding: 20px 24px;
  min-height: calc(100vh - 60px);
}
</style>
