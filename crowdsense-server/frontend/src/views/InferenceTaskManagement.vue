<template>
  <div class="management">
    <!-- 统计卡片行 -->
    <el-card shadow="never" class="stats-card glass-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="mini-stat mini-stat--blue">
            <div class="mini-stat__icon"><el-icon :size="24"><Document /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.total }}</div>
              <div class="mini-stat__label">任务总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--green">
            <div class="mini-stat__icon"><el-icon :size="24"><CircleCheck /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.successRate }}%</div>
              <div class="mini-stat__label">成功率</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--orange">
            <div class="mini-stat__icon"><el-icon :size="24"><Timer /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.avgTime }}ms</div>
              <div class="mini-stat__label">平均耗时</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--purple">
            <div class="mini-stat__icon"><el-icon :size="24"><Calendar /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.today }}</div>
              <div class="mini-stat__label">今日任务</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 密度等级分布 -->
    <el-card shadow="never" class="glass-card" style="margin-top:16px">
      <template #header><span class="card-title">密度等级分布</span></template>
      <v-chart :option="densityPieOption" style="height:240px" autoresize />
    </el-card>

    <!-- 上传推理 -->
    <el-card class="upload-card glass-card" shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:16px">
          <span class="card-title">人群计数推理</span>
          <el-radio-group v-model="uploadTab" size="small">
            <el-radio-button value="image">图片上传</el-radio-button>
            <el-radio-button value="video">视频上传</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 图片上传 -->
      <el-upload
        v-if="uploadTab === 'image'"
        class="upload-area"
        drag
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept="image/jpeg,image/png,image/jpg"
      >
        <div class="upload-inner" v-loading="loadingInfer" element-loading-text="推理中...">
          <svg class="upload-art" viewBox="0 0 80 50" width="140" height="88">
            <rect x="0" y="10" width="50" height="40" rx="3" fill="#409EFF" opacity="0.15"/>
            <rect x="4" y="14" width="42" height="32" rx="2" fill="#409EFF" opacity="0.08"/>
            <circle cx="12" cy="22" r="2" fill="#409EFF" opacity="0.4"/>
            <circle cx="20" cy="22" r="2" fill="#67c23a" opacity="0.4"/>
            <circle cx="28" cy="22" r="2" fill="#e6a23c" opacity="0.4"/>
            <circle cx="16" cy="30" r="2" fill="#f56c6c" opacity="0.4"/>
            <circle cx="24" cy="30" r="2" fill="#9b59b6" opacity="0.4"/>
            <circle cx="12" cy="38" r="2" fill="#e6a23c" opacity="0.4"/>
            <circle cx="20" cy="38" r="2" fill="#409EFF" opacity="0.4"/>
            <rect x="56" y="0" width="24" height="50" rx="3" fill="#67c23a" opacity="0.15"/>
            <rect x="60" y="4" width="16" height="42" rx="2" fill="#67c23a" opacity="0.1"/>
            <circle cx="68" cy="12" r="2.5" fill="#67c23a" opacity="0.35"/>
            <circle cx="68" cy="22" r="3" fill="#e6a23c" opacity="0.35"/>
            <circle cx="68" cy="32" r="4" fill="#f56c6c" opacity="0.35"/>
          </svg>
          <div class="el-upload__text">拖拽图片到此处或 <em>点击上传</em></div>
          <div class="el-upload__tip">支持 JPG/PNG 格式</div>
        </div>
      </el-upload>

      <!-- 视频上传 -->
      <div v-if="uploadTab === 'video'" v-loading="loadingVideo" element-loading-text="视频分析中，请耐心等待...">
        <el-row :gutter="16" style="margin-bottom:16px">
          <el-col :span="8">
            <el-input-number v-model="videoInterval" :min="1" :max="10" :step="1" size="small" />
            <span style="margin-left:8px;color:#909399;font-size:13px">抽帧间隔（秒）</span>
          </el-col>
        </el-row>
        <el-upload
          class="upload-area"
          drag
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleVideoChange"
          accept="video/mp4,video/quicktime,video/x-msvideo,video/webm"
        >
          <div class="upload-inner">
            <svg class="upload-art" viewBox="0 0 80 50" width="140" height="88">
              <rect x="0" y="0" width="80" height="50" rx="4" fill="#e6a23c" opacity="0.12"/>
              <rect x="4" y="4" width="72" height="42" rx="2" fill="#e6a23c" opacity="0.06"/>
              <polygon points="32,16 32,36 48,26" fill="#e6a23c" opacity="0.5"/>
              <rect x="8" y="16" width="20" height="20" rx="2" fill="#e6a23c" opacity="0.15"/>
              <rect x="52" y="16" width="20" height="20" rx="2" fill="#e6a23c" opacity="0.15"/>
              <rect x="30" y="40" width="20" height="4" rx="2" fill="#e6a23c" opacity="0.2"/>
            </svg>
            <div class="el-upload__text">拖拽视频到此处或 <em>点击上传</em></div>
            <div class="el-upload__tip">支持 MP4/MOV/AVI/WebM 格式</div>
          </div>
        </el-upload>
      </div>
    </el-card>

    <!-- 图片推理结果 -->
    <el-dialog v-model="resultVisible" title="推理结果" width="700px">
      <el-row :gutter="20">
        <el-col :span="12">
          <h4>原图</h4>
          <el-image v-if="inferResult.imageUrl" :src="inferResult.imageUrl" fit="contain" style="width:100%;max-height:300px" />
        </el-col>
        <el-col :span="12">
          <h4>密度图</h4>
          <el-image v-if="inferResult.densityUrl" :src="inferResult.densityUrl" fit="contain" style="width:100%;max-height:300px" />
        </el-col>
      </el-row>
      <el-descriptions :column="2" border style="margin-top:20px">
        <el-descriptions-item label="密度等级">
          <el-tag :type="inferResult.levelTag" effect="dark" size="large">
            {{ inferResult.densityLevel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="推理耗时">{{ inferResult.inferenceTime }} ms</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 视频推理结果 -->
    <el-dialog v-model="videoResultVisible" title="视频分析结果" width="960px" top="30px">
      <!-- 统计卡片 -->
      <el-row :gutter="12" style="margin-bottom:16px">
        <el-col :span="6">
          <div class="mini-stat mini-stat--blue">
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ videoResult.totalFrames || 0 }}</div>
              <div class="mini-stat__label">抽帧总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--green">
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ videoResult.summary?.maxCount || 0 }}</div>
              <div class="mini-stat__label">最高人数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--orange">
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ videoResult.summary?.minCount || 0 }}</div>
              <div class="mini-stat__label">最低人数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="mini-stat mini-stat--purple">
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ videoResult.summary?.avgCount || 0 }}</div>
              <div class="mini-stat__label">平均人数</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 折线图 -->
      <div style="height:280px;margin-bottom:16px">
        <v-chart :option="videoChartOption" style="height:100%" autoresize />
      </div>

      <!-- 帧缩略图列表 -->
      <div class="frame-gallery">
        <div
          v-for="fr in videoResult.frames"
          :key="fr.frameIndex"
          class="frame-item"
          @click="selectedFrame = fr"
        >
          <el-image :src="fr.imageUrl" fit="cover" style="width:100%;height:100%" />
          <div class="frame-item__badge">
            <span>{{ fr.crowdCount || 0 }}人</span>
            <span style="font-size:10px;opacity:0.7">{{ fr.timestamp?.toFixed(1) }}s</span>
          </div>
        </div>
      </div>

      <!-- 选中的帧详情 -->
      <div v-if="selectedFrame" style="margin-top:12px">
        <el-divider />
        <el-row :gutter="16">
          <el-col :span="12">
            <h4>帧 #{{ selectedFrame.frameIndex }} ({{ selectedFrame.timestamp?.toFixed(1) }}s)</h4>
            <el-image :src="selectedFrame.imageUrl" fit="contain" style="width:100%;max-height:240px" />
          </el-col>
          <el-col :span="12">
            <h4>密度图</h4>
            <el-image v-if="selectedFrame.densityUrl" :src="selectedFrame.densityUrl" fit="contain" style="width:100%;max-height:240px" />
          </el-col>
        </el-row>
        <el-descriptions :column="2" border style="margin-top:12px">
          <el-descriptions-item label="密度等级">
            <el-tag :type="selectedFrame.levelTag" effect="dark">
              {{ selectedFrame.densityLevel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="推理耗时">{{ selectedFrame.inferenceTime }} ms</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, CircleCheck, Timer, Calendar } from '@element-plus/icons-vue'
import { getInferenceTaskList, uploadInferenceImage, uploadInferenceVideo } from '../api/inferenceTask'

const uploadTab = ref('image')
const resultVisible = ref(false)
const loadingInfer = ref(false)
const loadingVideo = ref(false)
const videoInterval = ref(2)

const stats = reactive({ total: 0, successRate: 0, avgTime: 0, today: 0 })

const inferResult = reactive({
  crowdCount: 0, inferenceTime: 0, imageUrl: '', densityUrl: '',
  densityLevel: '', levelTag: 'info'
})

// Video results
const videoResultVisible = ref(false)
const videoResult = reactive({
  taskId: null, videoName: '', totalFrames: 0, interval: 2,
  summary: { maxCount: 0, minCount: 0, avgCount: 0 },
  frames: []
})
const selectedFrame = ref(null)

const videoChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    name: '时间 (秒)',
    nameLocation: 'center',
    nameGap: 20,
    data: videoResult.frames.map(f => f.timestamp?.toFixed(1) || '0')
  },
  yAxis: {
    type: 'value',
    name: '人数',
    minInterval: 1
  },
  series: [{
    type: 'line',
    data: videoResult.frames.map(f => f.crowdCount || 0),
    smooth: true,
    showSymbol: true,
    symbolSize: 6,
    areaStyle: { opacity: 0.08, color: '#409EFF' },
    lineStyle: { width: 2, color: '#409EFF' },
    itemStyle: { color: '#409EFF' },
    markLine: {
      silent: true,
      data: [
        { type: 'average', name: '平均', label: { formatter: '平均\n{c}' } }
      ],
      lineStyle: { color: '#e6a23c', type: 'dashed' }
    }
  }]
}))

const densityPieOption = reactive({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { bottom: 0, textStyle: { fontSize: 12 } },
  series: [{
    type: 'pie',
    radius: ['50%', '75%'],
    center: ['50%', '45%'],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
    data: []
  }]
})

const densityChartColor = (level) => {
  if (!level) return '#909399'
  if (level.includes('Low') || level.includes('低')) return '#67c23a'
  if (level.includes('Normal') || level.includes('正常')) return '#e6a23c'
  if (level.includes('Dense') || level.includes('密集')) return '#f56c6c'
  if (level.includes('Crowded') || level.includes('拥挤') || level.includes('极度')) return '#e74c3c'
  return '#909399'
}

const loadStats = async () => {
  try {
    const res = await getInferenceTaskList({ current: 1, size: 1000 })
    const records = res.records || res.data?.records || []
    stats.total = res.total || res.data?.total || records.length

    const success = records.filter(r => r.status === 'SUCCESS')
    stats.successRate = stats.total > 0 ? Math.round((success.length / records.length) * 100) : 0
    const times = records.filter(r => r.inferenceTime != null).map(r => r.inferenceTime)
    stats.avgTime = times.length > 0 ? Math.round(times.reduce((a, b) => a + b, 0) / times.length) : 0

    const todayStr = new Date().toISOString().slice(0, 10)
    stats.today = records.filter(r => r.createTime && r.createTime.startsWith(todayStr)).length

    const densityMap = {}
    records.forEach(r => {
      const level = r.densityLevel || '未知'
      densityMap[level] = (densityMap[level] || 0) + 1
    })
    densityPieOption.series[0].data = Object.entries(densityMap).map(([name, value]) => ({
      name, value, itemStyle: { color: densityChartColor(name) }
    }))
  } catch { /* silent */ }
}

const handleFileChange = async (file) => {
  loadingInfer.value = true
  try {
    const res = await uploadInferenceImage(file.raw)
    if (res.code === 200) {
      Object.assign(inferResult, res.data)
      resultVisible.value = true
      ElMessage.success('推理完成')
      loadStats()
    } else {
      ElMessage.error(res.message || '推理失败')
    }
  } catch (error) {
    ElMessage.error('推理失败: ' + (error.message || '未知错误'))
  } finally {
    loadingInfer.value = false
  }
}

const handleVideoChange = async (file) => {
  loadingVideo.value = true
  try {
    const res = await uploadInferenceVideo(file.raw, videoInterval.value)
    if (res.code === 200) {
      Object.assign(videoResult, res.data)
      selectedFrame.value = res.data.frames?.[0] || null
      videoResultVisible.value = true
      ElMessage.success(`视频分析完成，共 ${res.data.totalFrames} 帧`)
      loadStats()
    } else {
      ElMessage.error(res.message || '视频分析失败')
    }
  } catch (error) {
    ElMessage.error('视频分析失败: ' + (error.message || '未知错误'))
  } finally {
    loadingVideo.value = false
  }
}

onMounted(() => { loadStats() })
</script>

<style scoped>
.management { padding: 0; }

.stats-card { margin-bottom: 0; }
.mini-stat {
  display: flex; align-items: center; gap: 14px;
  background: rgba(255,255,255,0.65); padding: 18px 16px; border-radius: 12px;
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.5);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: default;
}
.mini-stat:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
.mini-stat__icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}
.mini-stat--blue .mini-stat__icon { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.mini-stat--green .mini-stat__icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.mini-stat--orange .mini-stat__icon { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.mini-stat--purple .mini-stat__icon { background: linear-gradient(135deg, #9b59b6, #b07cc6); }
.mini-stat__value { font-size: 22px; font-weight: 700; color: #303133; line-height: 1; }
.mini-stat__label { font-size: 12px; color: #909399; margin-top: 4px; }

.upload-card :deep(.el-upload-dragger) {
  border: 2px dashed rgba(0,0,0,0.12); border-radius: 12px;
  background: rgba(255,255,255,0.4);
  transition: border-color 0.3s, box-shadow 0.3s;
  min-height: 280px; display: flex; align-items: center; justify-content: center;
}
.upload-card :deep(.el-upload-dragger):hover {
  border-color: #409EFF;
  background: rgba(255,255,255,0.6);
  box-shadow: 0 0 0 4px rgba(64,158,255,0.08);
}

.upload-inner { display: flex; flex-direction: column; align-items: center; }
.upload-art { margin-bottom: 12px; opacity: 0.7; }

.frame-gallery {
  display: flex; gap: 10px; overflow-x: auto; padding: 8px 0;
  scrollbar-width: thin;
}
.frame-gallery::-webkit-scrollbar { height: 4px; }
.frame-gallery::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.15); border-radius: 2px; }
.frame-item {
  flex-shrink: 0; width: 120px; height: 80px; border-radius: 8px;
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
