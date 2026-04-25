<template>
  <div class="trash-page">
    <div class="page-header">
      <h2>
        <el-icon><Delete /></el-icon>
        回收站
      </h2>
    </div>

    <el-empty v-if="trashDocuments.length === 0 && !isLoading" description="回收站为空" />

    <el-table v-else :data="trashDocuments" style="width: 100%;" stripe>
      <el-table-column prop="title" label="标题" min-width="300">
        <template #default="{ row }">
          <div class="document-title">
            <el-icon :size="18"><Document /></el-icon>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="deletedBy" label="删除者" width="120">
        <template #default="{ row }">
          {{ row.deletedBy?.username || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="deletedAt" label="删除时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.deletedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleRestore(row)">
            恢复
          </el-button>
          <el-button type="danger" link @click="handleDeletePermanently(row)">
            永久删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="empty-notice" v-if="trashDocuments.length > 0">
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          回收站中的文档可以恢复或永久删除。永久删除后无法恢复。
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useDocumentStore } from '@/stores/document'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const documentStore = useDocumentStore()

const spaceId = computed(() => parseInt(route.params.spaceId))
const isLoading = ref(false)
const trashDocuments = ref([])

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

async function loadTrash() {
  isLoading.value = true
  try {
    trashDocuments.value = await documentStore.fetchTrash(spaceId.value)
  } catch (error) {
    console.error('Failed to load trash:', error)
  } finally {
    isLoading.value = false
  }
}

async function handleRestore(row) {
  try {
    await ElMessageBox.confirm(
      `确定要恢复文档 "${row.title}" 吗？`,
      '确认恢复',
      { type: 'info' }
    )
    
    await documentStore.restoreFromTrash(row.id)
    ElMessage.success('已恢复到原位置')
    loadTrash()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败')
    }
  }
}

async function handleDeletePermanently(row) {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除文档 "${row.title}" 吗？此操作不可撤销！`,
      '确认永久删除',
      { type: 'warning' }
    )
    
    await documentStore.deletePermanently(row.id)
    ElMessage.success('已永久删除')
    loadTrash()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  if (spaceId.value) {
    loadTrash()
  }
})
</script>

<style scoped>
.trash-page {
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

.document-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-notice {
  margin-top: 24px;
}
</style>
