<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <div class="stat-card stat-card--blue">
          <div class="stat-card__body">
            <div class="stat-card__info">
              <div class="stat-card__label">用户总数</div>
              <div class="stat-card__value">{{ statCounts.users }}</div>
            </div>
            <div class="stat-card__icon"><el-icon :size="48"><User /></el-icon></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card--green">
          <div class="stat-card__body">
            <div class="stat-card__info">
              <div class="stat-card__label">角色总数</div>
              <div class="stat-card__value">{{ statCounts.roles }}</div>
            </div>
            <div class="stat-card__icon"><el-icon :size="48"><Avatar /></el-icon></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card--orange">
          <div class="stat-card__body">
            <div class="stat-card__info">
              <div class="stat-card__label">权限总数</div>
              <div class="stat-card__value">{{ statCounts.permissions }}</div>
            </div>
            <div class="stat-card__icon"><el-icon :size="48"><Key /></el-icon></div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card--purple">
          <div class="stat-card__body">
            <div class="stat-card__info">
              <div class="stat-card__label">推理任务</div>
              <div class="stat-card__value">{{ statCounts.tasks }}</div>
            </div>
            <div class="stat-card__icon"><el-icon :size="48"><Document /></el-icon></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never" class="glass-card">
          <template #header><span class="card-title">密度等级分布</span></template>
          <v-chart :option="densityPieOption" style="height: 320px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="glass-card">
          <template #header><span class="card-title">近7天任务趋势</span></template>
          <v-chart :option="weeklyTrendOption" style="height: 320px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近任务 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span class="card-title">最近推理任务</span>
          <el-button text type="primary" @click="$router.push('/inference_tasks')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentTasks" style="width: 100%" v-loading="taskLoading" border size="small">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="图片" width="80" align="center">
          <template #default="scope">
            <el-image v-if="scope.row.imagePath" :src="'/api/files/images/' + scope.row.imagePath" style="width:40px;height:40px;border-radius:4px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column label="密度等级" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.densityLevel" :type="levelTagType(scope.row.densityLevel)" effect="dark" size="small">{{ scope.row.densityLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inferenceTime" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : scope.row.status === 'FAILED' ? 'danger' : 'warning'" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserList } from '../api/user'
import { getRoleList } from '../api/role'
import { getPermissionList } from '../api/permission'
import { getInferenceTaskList } from '../api/inferenceTask'

const taskLoading = ref(false)
const recentTasks = ref([])

const statCounts = reactive({
  users: 0, roles: 0, permissions: 0, tasks: 0
})

const densityPieOption = reactive({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { bottom: 0, textStyle: { fontSize: 12 } },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    center: ['50%', '45%'],
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
    label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
    data: []
  }]
})

const weeklyTrendOption = reactive({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, bottom: 30, top: 20 },
  xAxis: { type: 'category', data: [], axisLabel: { rotate: 30, fontSize: 11 } },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'line',
    data: [],
    smooth: true,
    areaStyle: { opacity: 0.3 },
    lineStyle: { width: 3, color: '#409EFF' },
    itemStyle: { color: '#409EFF' }
  }]
})

const levelTagType = (level) => {
  if (!level) return 'info'
  if (level.includes('Low') || level.includes('低')) return 'success'
  if (level.includes('Normal') || level.includes('正常')) return 'warning'
  if (level.includes('Dense') || level.includes('密集')) return 'danger'
  if (level.includes('Crowded') || level.includes('拥挤') || level.includes('极度')) return 'danger'
  return 'info'
}
const densityChartColor = (level) => {
  if (!level) return '#909399'
  if (level.includes('Low') || level.includes('低')) return '#67c23a'
  if (level.includes('Normal') || level.includes('正常')) return '#e6a23c'
  if (level.includes('Dense') || level.includes('密集')) return '#f56c6c'
  if (level.includes('Crowded') || level.includes('拥挤') || level.includes('极度')) return '#e74c3c'
  return '#909399'
}

const fetchCounts = async () => {
  try {
    const [users, roles, perms, tasks] = await Promise.all([
      getUserList({ current: 1, size: 1 }),
      getRoleList({ current: 1, size: 1 }),
      getPermissionList({ current: 1, size: 1 }),
      getInferenceTaskList({ current: 1, size: 1 })
    ])
    statCounts.users = users.total || users.data?.total || 0
    statCounts.roles = roles.total || roles.data?.total || 0
    statCounts.permissions = perms.total || perms.data?.total || 0
    statCounts.tasks = tasks.total || tasks.data?.total || 0
  } catch { /* silent */ }
}

const fetchCharts = async () => {
  try {
    const all = await getInferenceTaskList({ current: 1, size: 1000 })
    const records = all.records || all.data?.records || []

    // Density pie
    const densityMap = {}
    records.forEach(r => {
      const level = r.densityLevel || '未知'
      densityMap[level] = (densityMap[level] || 0) + 1
    })
    densityPieOption.series[0].data = Object.entries(densityMap).map(([name, value]) => ({
      name, value, itemStyle: { color: densityChartColor(name) }
    }))

    // Weekly trend
    const days = []
    const counts = []
    const now = new Date()
    for (let i = 6; i >= 0; i--) {
      const d = new Date(now)
      d.setDate(d.getDate() - i)
      const key = `${d.getMonth() + 1}/${d.getDate()}`
      days.push(key)
      counts.push(records.filter(r => {
        if (!r.createTime) return false
        const rt = new Date(r.createTime)
        return rt.getFullYear() === d.getFullYear() && rt.getMonth() === d.getMonth() && rt.getDate() === d.getDate()
      }).length)
    }
    weeklyTrendOption.xAxis.data = days
    weeklyTrendOption.series[0].data = counts
  } catch { /* silent */ }
}

const fetchRecentTasks = async () => {
  taskLoading.value = true
  try {
    const res = await getInferenceTaskList({ current: 1, size: 5 })
    recentTasks.value = res.records || res.data?.records || []
  } catch { /* silent */ }
  finally { taskLoading.value = false }
}

onMounted(() => {
  fetchCounts()
  fetchCharts()
  fetchRecentTasks()
})
</script>

<style scoped>
.dashboard { padding: 4px; }

/* 统计卡片 — 玻璃态渐变 */
.stat-card {
  border-radius: 14px;
  color: #fff;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  box-shadow: 0 6px 24px rgba(0,0,0,0.12);
}
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.18); }
.stat-card--blue { background: linear-gradient(135deg, rgba(64,158,255,0.88), rgba(51,126,204,0.78)); backdrop-filter: blur(8px); }
.stat-card--green { background: linear-gradient(135deg, rgba(103,194,58,0.88), rgba(82,155,46,0.78)); backdrop-filter: blur(8px); }
.stat-card--orange { background: linear-gradient(135deg, rgba(230,162,60,0.88), rgba(207,142,47,0.78)); backdrop-filter: blur(8px); }
.stat-card--purple { background: linear-gradient(135deg, rgba(155,89,182,0.88), rgba(125,60,152,0.78)); backdrop-filter: blur(8px); }

.stat-card__body {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 20px;
}
.stat-card__label { font-size: 14px; opacity: 0.88; margin-bottom: 8px; }
.stat-card__value { font-size: 36px; font-weight: 700; letter-spacing: 1px; }
.stat-card__icon { opacity: 0.3; }

.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
