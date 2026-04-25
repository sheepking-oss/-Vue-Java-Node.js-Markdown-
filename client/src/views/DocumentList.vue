<template>
  <div class="document-list-page">
    <div class="list-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文档..."
        style="width: 300px;"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="header-actions">
        <el-select v-model="sortBy" size="small" style="width: 150px;" @change="handleSort">
          <el-option label="最近修改" value="updatedAt" />
          <el-option label="创建时间" value="createdAt" />
          <el-option label="名称" value="title" />
        </el-select>

        <el-button type="primary" @click="showCreateDocument = true">
          <el-icon><Plus /></el-icon>
          新建文档
        </el-button>
      </div>
    </div>

    <el-empty v-if="filteredDocuments.length === 0 && !isLoading" description="暂无文档，创建一个开始吧">
      <el-button type="primary" @click="showCreateDocument = true">创建文档</el-button>
    </el-empty>

    <el-table v-else :data="filteredDocuments" style="width: 100%;" @row-click="handleRowClick" stripe>
      <el-table-column prop="title" label="标题" min-width="300">
        <template #default="{ row }">
          <div class="document-title">
            <el-icon :size="18"><Document /></el-icon>
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建者" width="120">
        <template #default="{ row }">
          {{ row.createdBy?.username || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="80">
        <template #default="{ row }">
          <el-tag size="small" type="info">v{{ row.version || 1 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="goToDocument(row)">
            编辑
          </el-button>
          <el-button type="danger" link @click.stop="handleMoveToTrash(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showCreateDocument" title="新建文档" width="500px">
      <el-form :model="documentForm" label-width="80px">
        <el-form-item label="文档标题" required>
          <el-input v-model="documentForm.title" placeholder="请输入文档标题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDocument = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateDocument">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentStore } from '@/stores/document'
import { useSpaceStore } from '@/stores/space'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const documentStore = useDocumentStore()
const spaceStore = useSpaceStore()

const spaceId = computed(() => parseInt(route.params.spaceId))
const documents = computed(() => documentStore.documents)
const isLoading = computed(() => documentStore.isLoading)

const searchKeyword = ref('')
const sortBy = ref('updatedAt')
const showCreateDocument = ref(false)
const creating = ref(false)

const documentForm = reactive({
  title: ''
})

const filteredDocuments = computed(() => {
  let docs = [...documents.value]
  
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    docs = docs.filter(d => 
      d.title.toLowerCase().includes(keyword)
    )
  }
  
  docs.sort((a, b) => {
    switch (sortBy.value) {
      case 'title':
        return a.title.localeCompare(b.title)
      case 'createdAt':
        return new Date(b.createdAt) - new Date(a.createdAt)
      case 'updatedAt':
      default:
        return new Date(b.updatedAt) - new Date(a.updatedAt)
    }
  })
  
  return docs
})

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

function handleRowClick(row) {
  goToDocument(row)
}

function goToDocument(row) {
  router.push(`/spaces/${spaceId.value}/document/${row.id}`)
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    documentStore.searchDocuments(spaceId.value, searchKeyword.value)
  }
}

function handleSort() {
}

async function handleCreateDocument() {
  if (!documentForm.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }

  creating.value = true
  try {
    const doc = await documentStore.createDocument({
      title: documentForm.title.trim(),
      content: '',
      spaceId: spaceId.value
    })
    
    ElMessage.success('文档创建成功')
    showCreateDocument.value = false
    documentForm.title = ''
    
    router.push(`/spaces/${spaceId.value}/document/${doc.id}`)
  } catch (error) {
    ElMessage.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
  }
}

async function handleMoveToTrash(row) {
  try {
    await ElMessageBox.confirm(
      `确定要将文档 "${row.title}" 移到回收站吗？`,
      '确认删除',
      { type: 'warning' }
    )
    
    await documentStore.moveToTrash(row.id)
    ElMessage.success('已移到回收站')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  if (spaceId.value) {
    documentStore.fetchDocuments(spaceId.value)
  }
})

watch(() => route.params.spaceId, (newSpaceId) => {
  if (newSpaceId && parseInt(newSpaceId) !== spaceId.value) {
    documentStore.fetchDocuments(parseInt(newSpaceId))
  }
})
</script>

<style scoped>
.document-list-page {
  padding: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.document-title {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.document-title span {
  font-weight: 500;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
