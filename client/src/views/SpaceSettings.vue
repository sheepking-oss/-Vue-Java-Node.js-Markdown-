<template>
  <div class="settings-page">
    <div class="page-header">
      <h2>
        <el-icon><Setting /></el-icon>
        空间设置
      </h2>
    </div>

    <el-card class="settings-card">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="空间名称">
          <el-input v-model="settingsForm.name" placeholder="请输入空间名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="settingsForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入空间描述"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveSettings">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card">
      <template #header>
        <span>危险操作</span>
      </template>
      <el-alert
        title="删除空间将无法恢复"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      >
        <template #default>
          删除此空间将永久删除所有文档、版本、评论和成员数据，此操作无法撤销。
        </template>
      </el-alert>
      <el-button type="danger" @click="handleDeleteSpace">
        删除空间
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()

const spaceId = computed(() => parseInt(route.params.spaceId))
const currentSpace = computed(() => spaceStore.currentSpace)

const settingsForm = reactive({
  name: '',
  description: ''
})

watch(currentSpace, (space) => {
  if (space) {
    settingsForm.name = space.name
    settingsForm.description = space.description || ''
  }
}, { immediate: true })

async function handleSaveSettings() {
  if (!settingsForm.name.trim()) {
    ElMessage.warning('空间名称不能为空')
    return
  }

  try {
    await spaceStore.updateSpace(spaceId.value, {
      name: settingsForm.name.trim(),
      description: settingsForm.description
    })
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function handleDeleteSpace() {
  try {
    await ElMessageBox.confirm(
      `确定要删除空间 "${currentSpace.value?.name}" 吗？此操作将永久删除所有数据且无法恢复！`,
      '确认删除空间',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await spaceStore.deleteSpace(spaceId.value)
    ElMessage.success('空间已删除')
    router.push('/spaces')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  if (spaceId.value) {
    spaceStore.fetchSpace(spaceId.value)
  }
})
</script>

<style scoped>
.settings-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
}

.settings-card {
  margin-bottom: 24px;
  max-width: 600px;
}
</style>
