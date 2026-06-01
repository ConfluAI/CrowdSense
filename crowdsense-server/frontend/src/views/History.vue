<template>
  <div class="history">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">历史检测记录</span>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="任务类型">
          <el-select v-model="searchForm.taskType" placeholder="全部" clearable style="width: 130px" @change="handleSearch">
            <el-option value="IMAGE" label="图片" />
            <el-option value="VIDEO" label="视频" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 130px">
            <el-option value="SUCCESS" label="成功" />
            <el-option value="FAILED" label="失败" />
            <el-option value="PENDING" label="处理中" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshRight">重置</el-button>
          <el-button
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
            :icon="Delete">
            批量删除 {{ selectedIds.length > 0 ? '(' + selectedIds.length + ')' : '' }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="list"
        style="width: 100%"
        v-loading="loading"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="taskType" label="类型" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.taskType === 'VIDEO' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ scope.row.taskType === 'VIDEO' ? '视频' : scope.row.taskType === 'FRAME' ? '帧' : '图片' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件" width="110" align="center">
          <template #default="scope">
            <template v-if="scope.row.taskType === 'FRAME'">
              <el-image v-if="scope.row.imagePath" :src="'/api/files/frames/' + scope.row.imagePath" style="width:60px;height:60px;border-radius:4px" fit="cover" />
            </template>
            <template v-else-if="scope.row.taskType === 'VIDEO'">
              <div style="font-size:20px">🎬</div>
              <span style="font-size:11px;color:#909399">{{ scope.row.totalFrames || 0 }}帧</span>
            </template>
            <template v-else>
              <el-image v-if="scope.row.imagePath" :src="'/api/files/images/' + scope.row.imagePath" style="width:60px;height:60px;border-radius:4px" fit="cover" />
            </template>
          </template>
        </el-table-column>
        <el-table-column label="平均人数" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.crowdCount != null">{{ scope.row.crowdCount }}</span>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="密度等级" width="160" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.densityLevel" :type="levelTagType(scope.row.densityLevel)" effect="dark" size="small">{{ scope.row.densityLevel }}</el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="inferenceTime" label="耗时(ms)" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.taskType === 'VIDEO'" size="small" type="success" @click="handleViewChart(scope.row)" :icon="View">时间线</el-button>
            <el-button v-else-if="scope.row.taskType === 'IMAGE'" size="small" @click="handleView(scope.row)" :icon="View">查看</el-button>
            <el-button v-else size="small" @click="handleView(scope.row)" :icon="View">详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)" :icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

    <!-- 查看详情 -->
    <el-dialog v-model="viewDialogVisible" title="查看详情" width="600px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ formData.id }}</el-descriptions-item>
        <el-descriptions-item label="原图">
          <el-image v-if="formData.imagePath && formData.taskType === 'IMAGE'" :src="'/api/files/images/' + formData.imagePath" style="max-width:200px" fit="contain" />
          <el-image v-else-if="formData.imagePath" :src="'/api/files/frames/' + formData.imagePath" style="max-width:200px" fit="contain" />
        </el-descriptions-item>
        <el-descriptions-item label="密度图">
          <el-image v-if="formData.densityPath" :src="'/api/files/density/' + formData.densityPath" style="max-width:200px" fit="contain" />
        </el-descriptions-item>
        <el-descriptions-item label="密度等级">
          <el-tag v-if="formData.densityLevel" :type="levelTagType(formData.densityLevel)" effect="dark" size="small">{{ formData.densityLevel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="推理耗时(ms)">{{ formData.inferenceTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ formData.status }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formData.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 视频时间线 -->
    <el-dialog v-model="chartDialogVisible" :title="'视频分析 - ' + videoTask.videoName" width="960px" top="30px" destroy-on-close>
      <el-row :gutter="12" style="margin-bottom:16px">
        <el-col :span="6"><div class="mini-stat"><div class="mini-stat__value">{{ videoTask.totalFrames }}</div><div class="mini-stat__label">抽帧数</div></div></el-col>
        <el-col :span="6"><div class="mini-stat"><div class="mini-stat__value">{{ videoFrames.length ? Math.max(...videoFrames.map(f => f.crowdCount || 0)) : 0 }}</div><div class="mini-stat__label">最高人数</div></div></el-col>
        <el-col :span="6"><div class="mini-stat"><div class="mini-stat__value">{{ videoFrames.length ? Math.min(...videoFrames.map(f => f.crowdCount || 0)) : 0 }}</div><div class="mini-stat__label">最低人数</div></div></el-col>
        <el-col :span="6"><div class="mini-stat"><div class="mini-stat__value">{{ videoFrames.length ? Math.round(videoFrames.reduce((s,f) => s + (f.crowdCount || 0), 0) / videoFrames.length) : 0 }}</div><div class="mini-stat__label">平均人数</div></div></el-col>
      </el-row>
      <div style="height:280px;margin-bottom:16px">
        <v-chart v-if="videoFrames.length" :option="videoChartOption" style="height:100%" autoresize />
      </div>
      <div class="frame-gallery">
        <div v-for="fr in videoFrames" :key="fr.frameIndex" class="frame-item" @click="selectedVideoFrame = fr">
          <el-image :src="'/api/files/frames/' + fr.imagePath" fit="cover" style="width:100%;height:100%" />
          <div class="frame-item__badge">
            <span>{{ fr.crowdCount || 0 }}人</span>
            <span style="font-size:10px;opacity:0.7">{{ fr.timestampSeconds?.toFixed(1) }}s</span>
          </div>
        </div>
      </div>
      <div v-if="selectedVideoFrame" style="margin-top:12px">
        <el-divider />
        <el-row :gutter="16">
          <el-col :span="12">
            <h4>帧 #{{ selectedVideoFrame.frameIndex }} ({{ selectedVideoFrame.timestampSeconds?.toFixed(1) }}s)</h4>
            <el-image :src="'/api/files/frames/' + selectedVideoFrame.imagePath" fit="contain" style="width:100%;max-height:240px" />
          </el-col>
          <el-col :span="12">
            <h4>密度图</h4>
            <el-image v-if="selectedVideoFrame.densityPath" :src="'/api/files/density/' + selectedVideoFrame.densityPath" fit="contain" style="width:100%;max-height:240px" />
          </el-col>
        </el-row>
        <el-descriptions :column="2" border style="margin-top:12px">
          <el-descriptions-item label="密度等级">
            <el-tag v-if="selectedVideoFrame.densityLevel" :type="levelTagType(selectedVideoFrame.densityLevel)" effect="dark">{{ selectedVideoFrame.densityLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="推理耗时">{{ selectedVideoFrame.inferenceTime }} ms</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="chartDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, View, Delete } from '@element-plus/icons-vue'
import { getInferenceTaskList, getInferenceTaskById, deleteInferenceTask } from '../api/inferenceTask'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const selectedIds = ref([])
const viewDialogVisible = ref(false)
const formData = ref({})
const chartDialogVisible = ref(false)
const videoTask = ref({})
const videoFrames = ref([])
const selectedVideoFrame = ref(null)

const pagination = reactive({ current: 1, size: 10, total: 0 })
const searchForm = reactive({ status: '', taskType: '' })

const videoChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    name: '时间 (秒)',
    data: videoFrames.value.map(f => f.timestampSeconds?.toFixed(1) || '0')
  },
  yAxis: { type: 'value', name: '人数', minInterval: 1 },
  series: [{
    type: 'line',
    data: videoFrames.value.map(f => f.crowdCount || 0),
    smooth: true,
    showSymbol: true,
    symbolSize: 6,
    areaStyle: { opacity: 0.08, color: '#409EFF' },
    lineStyle: { width: 2, color: '#409EFF' },
    itemStyle: { color: '#409EFF' },
    markLine: {
      silent: true,
      data: [{ type: 'average', name: '平均', label: { formatter: '平均\n{c}' } }],
      lineStyle: { color: '#e6a23c', type: 'dashed' }
    }
  }]
}))

const levelTagType = (level) => {
  if (!level) return 'info'
  if (level.includes('Low') || level.includes('低')) return 'success'
  if (level.includes('Normal') || level.includes('正常')) return 'warning'
  if (level.includes('Dense') || level.includes('密集')) return 'danger'
  if (level.includes('Crowded') || level.includes('拥挤') || level.includes('极度')) return 'danger'
  return 'info'
}
const statusType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size, ...searchForm }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null) delete params[k] })
    const res = await getInferenceTaskList(params)
    list.value = (res.records || res.data?.records || []).filter(r => r.taskType !== 'FRAME')
    pagination.total = res.total || res.data?.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => { searchForm.status = ''; searchForm.taskType = ''; pagination.current = 1; loadData() }
const handleSizeChange = (size) => { pagination.size = size; loadData() }
const handleCurrentChange = (current) => { pagination.current = current; loadData() }

const handleView = async (row) => {
  try {
    const res = await getInferenceTaskById(row.id)
    formData.value = res.data || res
    viewDialogVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

const handleViewChart = async (row) => {
  try {
    videoTask.value = row
    const res = await request({ url: '/inference_tasks/' + row.id + '/frames', method: 'get' })
    videoFrames.value = res.records || res.data?.records || []
    selectedVideoFrame.value = videoFrames.value[0] || null
    chartDialogVisible.value = true
  } catch {
    ElMessage.error('加载视频帧数据失败')
  }
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条记录吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await request({ url: '/inference_tasks/batch', method: 'delete', data: selectedIds.value })
    ElMessage.success(`已删除 ${selectedIds.value.length} 条记录`)
    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  }
}

const handleDelete = async (row) => {
  try {
    const msg = row.taskType === 'VIDEO'
      ? '删除视频任务将同时删除所有帧记录，确定继续？'
      : '确定要删除该记录吗？'
    await ElMessageBox.confirm(msg, '确认删除', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    // Delete child frames first if it's a video task
    if (row.taskType === 'VIDEO') {
      try {
        await request({ url: '/inference_tasks/batch/' + row.id, method: 'delete' })
      } catch { /* best effort */ }
    }
    await deleteInferenceTask(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.history { padding: 20px; }
.card-title { font-weight: 600; font-size: 15px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 20px; padding: 20px; background-color: #f5f7fa; border-radius: 4px; }
.pagination { margin-top: 20px; justify-content: flex-end; }

.mini-stat {
  background: #fff; padding: 14px 12px; border-radius: 8px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.06); text-align: center;
}
.mini-stat__value { font-size: 22px; font-weight: 700; color: #303133; }
.mini-stat__label { font-size: 12px; color: #909399; margin-top: 2px; }

.frame-gallery {
  display: flex; gap: 10px; overflow-x: auto; padding: 8px 0;
  scrollbar-width: thin;
}
.frame-gallery::-webkit-scrollbar { height: 4px; }
.frame-gallery::-webkit-scrollbar-thumb { background: #dcdfe6; border-radius: 2px; }
.frame-item {
  flex-shrink: 0; width: 120px; height: 80px; border-radius: 6px;
  overflow: hidden; cursor: pointer; position: relative;
  border: 2px solid transparent; transition: border-color 0.2s;
}
.frame-item:hover { border-color: #409EFF; }
.frame-item__badge {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.55); color: #fff; font-size: 11px;
  padding: 3px 6px; display: flex; justify-content: space-between;
}
</style>
