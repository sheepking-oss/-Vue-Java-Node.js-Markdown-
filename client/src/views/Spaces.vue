<template>
  <div class="spaces-page">
    <div class="page-header">
      <h2>我的空间</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建空间
      </el-button>
    </div>

    <el-empty v-if="spaces.length === 0 && !isLoading" description="暂无空间，创建一个开始协作吧">
      <el-button type="primary" @click="showCreateDialog = true">创建空间</el-button>
    </el-empty>

    <el-row :gutter="24" v-else>
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="space in spaces" :key="space.id">
        <el-card class="space-card" shadow="hover" @click="goToSpace(space)">
          <template #header>
            <div class="space-card-header">
              <el-avatar :size="40" :style="{ backgroundColor: getSpaceColor(space.name) }">
                {{ space.name.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="space-info">
                <span class="space-name">{{ space.name }}</span>
                <span class="space-owner">{{ space.owner?.username || '未知' }}</span>
              </div>
            </div>
          </template>
          
          <p class="space-desc">{{ space.description || '暂无描述' }}</p>
          
          <div class="space-meta">
            <span class="meta-item">
              <el-icon><Document /></el-icon>
              文档
            </span>
            <span class="meta-item">
              <el-icon><User /></el-icon>
              成员
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showCreateDialog" title="创建空间" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="空间名称" required>
          <el-input v-model="createForm.name" placeholder="请输入空间名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入空间描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateSpace">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { ElMessage } from 'element-plus'

const router = useRouter()
const spaceStore = useSpaceStore()

const spaces = computed(() => spaceStore.spaces)
const isLoading = computed(() => spaceStore.isLoading)

const showCreateDialog = ref(false)
const creating = ref(false)

const createForm = reactive({
  name: '',
  description: ''
})

const colors = [
  '#409eff',
  '#67c23a',
  '#e6a23c',
  '#f56c6c',
  '#909399',
  '#06b6d4',
  '#8b5cf6',
  '#ec4899'
]

function getSpaceColor(name) {
  const hash = name.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + ((acc << 5) - acc)
  }, 0)
  return colors[Math.abs(hash) % colors.length]
}

function goToSpace(space) {
  router.push(`/spaces/${space.id}`)
}

async function handleCreateSpace() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入空间名称')
    return
  }

  creating.value = true
  try {
    await spaceStore.createSpace({
      name: createForm.name.trim(),
      description: createForm.description
    })
    ElMessage.success('空间创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.description = ''
  } catch (error) {
    ElMessage.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  spaceStore.fetchSpaces()
})
</script>

<style scoped>
.spaces-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.space-card {
  cursor: pointer;
  margin-bottom: 24px;
  transition: transform 0.2s;
}

.space-card:hover {
  transform: translateY(-4px);
}

.space-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.space-info {
  display: flex;
  flex-direction: column;
}

.space-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.space-owner {
  font-size: 12px;
  color: #909399;
}

.space-desc {
  font-size: 13px;
  color: #606266;
  margin: 0 0 16px 0;
  line-height: 1.5;
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.space-meta {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
