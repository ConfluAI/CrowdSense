<template>
  <div class="profile">
    <el-card shadow="never" class="profile-card glass-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">个人信息</span>
          <el-tag :type="isAdmin ? 'danger' : 'success'" effect="dark" size="small">{{ isAdmin ? '超级管理员' : '普通用户' }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ userInfo.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ userInfo.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ userInfo.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ isAdmin ? '超级管理员' : '普通用户' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { getUserInfo } from '../api/user'

const userInfo = reactive({
  username: '', realName: '', email: '', phone: '', role: ''
})

const isAdmin = computed(() => userInfo.role === 'admin')

onMounted(async () => {
  try {
    const res = await getUserInfo()
    if (res.code === 200) Object.assign(userInfo, res.data)
  } catch {
    const stored = localStorage.getItem('user')
    if (stored) Object.assign(userInfo, JSON.parse(stored))
  }
})
</script>

<style scoped>
.profile { padding: 20px; display: flex; justify-content: center; }
.profile-card { width: 500px; }
.card-title { font-weight: 600; font-size: 15px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
