<template>
  <div class="share-access-page">
    <div class="access-container">
      <div class="access-header">
        <el-icon class="logo-icon"><Document /></el-icon>
        <h1>共享文档</h1>
        <p>您正在访问一个共享文档</p>
      </div>

      <el-form v-if="needsPassword" :model="accessForm" class="access-form">
        <el-alert
          title="此文档需要访问密码"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
        />
        
        <el-form-item>
          <el-input
            v-model="accessForm.password"
            type="password"
            placeholder="请输入访问密码"
            size="large"
            show-password
            @keyup.enter="handleAccess"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="accessing"
            class="access-button"
            @click="handleAccess"
          >
            访问文档
          </el-button>
        </el-form-item>
      </el-form>

      <div v-else-if="document" class="document-preview">
        <div class="preview-header">
          <h3>{{ document.title }}</h3>
          <el-tag :type="shareType === 'edit' ? 'warning' : 'info'" size="small">
            {{ shareType === 'edit' ? '可编辑' : '仅查看' }}
          </el-tag>
        </div>

        <div class="preview-content" v-html="renderedContent"></div>

        <div v-if="shareType === 'view'" class="preview-footer">
          <el-alert
            title="这是只读预览"
            type="info"
            :closable="false"
          >
            <template #default>
              此共享链接仅提供查看权限。如需编辑，请联系文档所有者。
            </template>
          </el-alert>
        </div>
      </div>

      <el-empty v-else-if="!accessing && !needsPassword" description="链接无效或已过期">
        <el-button type="primary" @click="goToHome">返回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { shareApi } from '@/services/api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import dompurify from 'dompurify'

const route = useRoute()
const router = useRouter()

const shareCode = computed(() => route.params.shareCode)

const accessing = ref(false)
const needsPassword = ref(false)
const document = ref(null)
const shareType = ref('view')

const accessForm = ref({
  password: ''
})

const renderedContent = computed(() => {
  if (!document.value) return ''
  try {
    const html = marked.parse(document.value.content || '')
    return dompurify.sanitize(html)
  } catch (e) {
    return '<p>渲染错误</p>'
  }
})

async function accessShare(password = null) {
  accessing.value = true
  try {
    const response = await shareApi.access({
      shareCode: shareCode.value,
      password
    })
    document.value = response.data
    needsPassword.value = false
  } catch (error) {
    if (error.response?.status === 401) {
      needsPassword.value = true
      ElMessage.warning('请输入访问密码')
    } else if (error.response?.status === 400) {
      ElMessage.error(error.response.data || '链接无效或已过期')
    }
  } finally {
    accessing.value = false
  }
}

function handleAccess() {
  if (!accessForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }
  accessShare(accessForm.value.password)
}

function goToHome() {
  router.push('/')
}

onMounted(() => {
  accessShare()
})
</script>

<style scoped>
.share-access-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.access-container {
  width: 100%;
  max-width: 800px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  margin: 20px;
}

.access-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 16px;
}

.access-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #303133;
}

.access-header p {
  margin: 0;
  color: #909399;
}

.access-form {
  max-width: 400px;
  margin: 0 auto;
}

.access-button {
  width: 100%;
}

.document-preview {
  max-height: 70vh;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.preview-header h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.preview-content {
  font-size: 14px;
  line-height: 1.8;
  padding: 0 8px;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4),
.preview-content :deep(h5),
.preview-content :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.preview-content :deep(h1) { font-size: 2em; }
.preview-content :deep(h2) { font-size: 1.5em; }
.preview-content :deep(h3) { font-size: 1.25em; }

.preview-content :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.preview-content :deep(pre) {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.preview-content :deep(code) {
  background-color: rgba(27, 31, 35, 0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
}

.preview-content :deep(pre code) {
  background: none;
  padding: 0;
}

.preview-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}
</style>
