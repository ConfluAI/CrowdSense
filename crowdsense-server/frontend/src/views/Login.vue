<template>
  <div class="login-container">
    <div class="login-bg">
      <div v-for="i in 8" :key="i" class="login-bubble" :style="bubbleStyle(i)" />
    </div>

    <div class="login-card-wrapper">
      <div class="login-logo">
        <svg viewBox="0 0 32 32" width="42" height="42">
          <circle cx="10" cy="10" r="4" fill="#409EFF" opacity="0.9"/>
          <circle cx="22" cy="10" r="4" fill="#67c23a" opacity="0.9"/>
          <circle cx="10" cy="22" r="4" fill="#e6a23c" opacity="0.9"/>
          <circle cx="22" cy="22" r="4" fill="#f56c6c" opacity="0.9"/>
          <circle cx="16" cy="16" r="4" fill="#9b59b6" opacity="0.85"/>
        </svg>
        <h1 class="login-brand">CrowdSense</h1>
        <p class="login-subtitle">人群计数管理系统</p>
      </div>

      <el-card class="login-box glass-card" shadow="never">
        <!-- 登录模式 -->
        <template v-if="!isRegister">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" class="login-input" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" class="login-input" @keyup.enter="handleLogin" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">登 录</el-button>
            </el-form-item>
          </el-form>
          <div class="login-switch">
            还没有账号？<el-button link type="primary" @click="switchToRegister">立即注册</el-button>
          </div>
        </template>

        <!-- 注册模式 -->
        <template v-else>
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" :prefix-icon="User" size="large" class="login-input" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" class="login-input" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" size="large" class="login-input" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="真实姓名（选填）" :prefix-icon="UserFilled" size="large" class="login-input" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱（选填）" :prefix-icon="Message" size="large" class="login-input" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" size="large" :loading="loading" @click="handleRegister" class="login-btn register-btn">注 册</el-button>
            </el-form-item>
          </el-form>
          <div class="login-switch">
            已有账号？<el-button link type="primary" @click="switchToLogin">返回登录</el-button>
          </div>
        </template>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled, Message } from '@element-plus/icons-vue'
import { login, register } from '../api/user'

const router = useRouter()
const isRegister = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '', realName: '', email: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) callback(new Error('两次密码不一致'))
  else callback()
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const bubbleStyle = (i) => {
  const sizes = [60, 90, 40, 120, 70, 50, 100, 80]
  const lefts = [8, 22, 38, 55, 72, 85, 15, 65]
  const delays = [0, 2, 4, 6, 1, 3, 5, 7]
  const durations = [12, 18, 14, 20, 16, 22, 15, 19]
  const idx = i - 1
  return {
    width: sizes[idx] + 'px', height: sizes[idx] + 'px', left: lefts[idx] + '%',
    animationDelay: delays[idx] + 's', animationDuration: durations[idx] + 's'
  }
}

const switchToRegister = () => {
  isRegister.value = true
  Object.assign(registerForm, { username: '', password: '', confirmPassword: '', realName: '', email: '' })
}
const switchToLogin = () => {
  isRegister.value = false
  loginForm.username = ''; loginForm.password = ''
}

const handleLogin = async () => {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data))
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch {
    ElMessage.error('登录请求失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    const res = await register({ username: registerForm.username, password: registerForm.password, realName: registerForm.realName, email: registerForm.email })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      switchToLogin()
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch {
    ElMessage.error('注册请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh; display: flex; justify-content: center; align-items: center;
  position: relative; overflow: hidden;
}

.login-bg { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.login-bubble {
  position: absolute; bottom: -150px;
  background: rgba(255,255,255,0.12); border-radius: 50%;
  animation: float-up linear infinite;
}
@keyframes float-up {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 0.4; }
  100% { transform: translateY(-110vh) scale(1.3); opacity: 0; }
}

.login-card-wrapper { position: relative; z-index: 1; text-align: center; }

.login-logo { margin-bottom: 28px; }
.login-brand { color: #fff; font-size: 32px; font-weight: 800; margin: 14px 0 6px; letter-spacing: 3px; text-shadow: 0 2px 12px rgba(0,0,0,0.5); }
.login-subtitle { color: rgba(255,255,255,0.85); font-size: 15px; margin: 0; text-shadow: 0 1px 6px rgba(0,0,0,0.4); letter-spacing: 2px; }

.login-box {
  width: 420px;
  background: rgba(255,255,255,0.85) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.5) !important;
  border-radius: 18px !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2), 0 4px 16px rgba(0,0,0,0.1) !important;
  overflow: hidden;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 10px; transition: all 0.3s; background: rgba(255,255,255,0.7);
}
.login-input :deep(.el-input__wrapper):hover { box-shadow: 0 0 0 1px #409EFF inset; background: rgba(255,255,255,0.95); }

.login-btn {
  width: 100%; border-radius: 10px !important; letter-spacing: 6px; font-size: 16px;
  height: 44px;
  transition: transform 0.2s, box-shadow 0.2s;
  background: linear-gradient(135deg, #409EFF, #66b1ff) !important;
  border: none !important;
  box-shadow: 0 6px 20px rgba(64,158,255,0.35) !important;
}
.login-btn:hover { transform: translateY(-2px); box-shadow: 0 10px 28px rgba(64,158,255,0.45) !important; }
.register-btn {
  background: linear-gradient(135deg, #67c23a, #85ce61) !important;
  box-shadow: 0 6px 20px rgba(103,194,58,0.35) !important;
}
.register-btn:hover { box-shadow: 0 10px 28px rgba(103,194,58,0.45) !important; }

.login-switch { padding-top: 18px; border-top: 1px solid rgba(0,0,0,0.06); color: #909399; font-size: 13px; }
</style>
