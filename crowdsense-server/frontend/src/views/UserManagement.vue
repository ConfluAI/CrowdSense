<template>
  <div class="management">
    <!-- 统计卡片行 -->
    <el-card shadow="hover" class="stats-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="mini-stat mini-stat--blue">
            <div class="mini-stat__icon"><el-icon :size="24"><User /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.total }}</div>
              <div class="mini-stat__label">用户总数</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="mini-stat mini-stat--green">
            <div class="mini-stat__icon"><el-icon :size="24"><CircleCheck /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.active }}</div>
              <div class="mini-stat__label">启用用户</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="mini-stat mini-stat--orange">
            <div class="mini-stat__icon"><el-icon :size="24"><WarningFilled /></el-icon></div>
            <div class="mini-stat__info">
              <div class="mini-stat__value">{{ stats.disabled }}</div>
              <div class="mini-stat__label">禁用用户</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never" class="glass-card">
          <template #header><span class="card-title">用户状态分布</span></template>
          <v-chart :option="statusBarOption" style="height:200px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户列表 -->
    <el-card style="margin-top:16px" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="searchForm.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="searchForm.phone" placeholder="请输入电话" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="handleReset" :icon="RefreshRight">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" show-overflow-tooltip />
        <el-table-column label="检测次数" width="100" align="center">
          <template #default="scope">{{ getStat(scope.row.id).total }}</template>
        </el-table-column>
        <el-table-column label="成功率" width="90" align="center">
          <template #default="scope">{{ getStat(scope.row.id).rate }}%</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" @click="handleView(scope.row)" :icon="View">查看</el-button>
            <el-button size="small" type="primary" @click="handleEdit(scope.row)" :icon="Edit">编辑</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="formData" ref="formRef" label-width="100px" :rules="formRules">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="formData.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <el-option value="admin" label="admin（超级管理员）" />
            <el-option value="user" label="user（普通用户）" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情 -->
    <el-dialog v-model="viewDialogVisible" title="查看详情" width="600px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ formData.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ formData.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ formData.realName }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ formData.email }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ formData.phone }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ formData.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formData.updateTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshRight, View, Edit, Delete, User, CircleCheck, WarningFilled } from '@element-plus/icons-vue'
import { getUserList, getUserById, createUser, updateUser, deleteUser, getDetectionStats } from '../api/user'

const loading = ref(false)
const list = ref([])
const detectionStats = ref({})
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('新增')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const stats = reactive({ total: 0, active: 0, disabled: 0 })
const statusBarOption = reactive({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, bottom: 20, top: 20 },
  xAxis: { type: 'category', data: ['启用', '禁用'] },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'bar', barWidth: '40%',
    data: [0, 0],
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: (params) => params.dataIndex === 0 ? '#67c23a' : '#f56c6c'
    },
    label: { show: true, position: 'top', fontSize: 14, fontWeight: 'bold' }
  }]
})

const pagination = reactive({ current: 1, size: 10, total: 0 })
const searchForm = reactive({ username: '', realName: '', email: '', phone: '', status: '' })
const formData = ref({ username: '', realName: '', email: '', phone: '', password: '', role: 'user', status: 1 })
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: pagination.current, size: pagination.size, ...searchForm }
    Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
    const res = await getUserList(params)
    list.value = res.records || res.data?.records || []
    pagination.total = res.total || res.data?.total || 0
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const [activeRes, disabledRes] = await Promise.all([
      getUserList({ current: 1, size: 1, status: 1 }),
      getUserList({ current: 1, size: 1, status: 0 })
    ])
    stats.active = activeRes.total || activeRes.data?.total || 0
    stats.disabled = disabledRes.total || disabledRes.data?.total || 0
    stats.total = stats.active + stats.disabled
    statusBarOption.series[0].data = [stats.active, stats.disabled]
  } catch { /* silent */ }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => {
  searchForm.username = ''; searchForm.realName = ''; searchForm.email = ''
  searchForm.phone = ''; searchForm.status = ''
  pagination.current = 1; loadData()
}
const handleSizeChange = (size) => { pagination.size = size; loadData() }
const handleCurrentChange = (current) => { pagination.current = current; loadData() }

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增'
  formData.value = { username: '', realName: '', email: '', phone: '', password: '', role: 'user', status: 1 }
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await getUserById(row.id)
    formData.value = res
    viewDialogVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑'
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？删除后不可恢复！', '确认删除', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  try { await formRef.value.validate() } catch { return }
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createUser(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
    loadStats()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const fetchDetectionStats = async () => {
  try {
    const res = await getDetectionStats()
    detectionStats.value = res || {}
  } catch { /* silent */ }
}

const getStat = (userId) => {
  const s = detectionStats.value[userId]
  if (!s) return { total: 0, rate: 0 }
  const total = s.total || 0
  const rate = total > 0 ? Math.round((s.success || 0) / total * 100) : 0
  return { total, rate }
}

onMounted(() => { loadData(); loadStats(); fetchDetectionStats() })
</script>

<style scoped>
.management { padding: 0; }

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
.mini-stat__value { font-size: 22px; font-weight: 700; color: #303133; line-height: 1; }
.mini-stat__label { font-size: 12px; color: #909399; margin-top: 4px; }

.card-title { font-weight: 600; font-size: 15px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.search-form {
  margin-bottom: 20px; padding: 18px 20px 4px;
  background: rgba(255,255,255,0.5);
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.35);
}
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
