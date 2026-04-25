<template>
  <div class="space-detail-page">
    <div class="sidebar-container">
      <div class="sidebar-header">
        <h3>{{ currentSpace?.name }}</h3>
        <el-button text @click="showCreateDocument = true">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      
      <div class="folder-tree">
        <el-tree
          :data="treeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <el-icon v-if="data.type === 'folder'"><FolderOpened /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <span class="node-label">{{ node.label }}</span>
            </div>
          </template>
        </el-tree>
      </div>
    </div>

    <div class="content-container">
      <router-view />
    </div>

    <el-dialog v-model="showCreateDocument" title="新建文档" width="500px">
      <el-form :model="documentForm" label-width="80px">
        <el-form-item label="文档标题" required>
          <el-input v-model="documentForm.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="父级文档">
          <el-tree-select
            v-model="documentForm.parentId"
            :data="treeData"
            :props="{ label: 'label', value: 'id' }"
            placeholder="选择父级文档（可选）"
            clearable
            check-strictly
          />
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
import { useSpaceStore } from '@/stores/space'
import { useDocumentStore } from '@/stores/document'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const documentStore = useDocumentStore()

const spaceId = computed(() => parseInt(route.params.spaceId))
const currentSpace = computed(() => spaceStore.currentSpace)
const documents = computed(() => documentStore.documents)

const showCreateDocument = ref(false)
const creating = ref(false)

const documentForm = reactive({
  title: '',
  parentId: null
})

const treeProps = {
  children: 'children',
  label: 'label'
}

const treeData = computed(() => {
  const buildTree = (docs, parentId = null) => {
    return docs
      .filter(d => d.parentId === parentId)
      .map(d => ({
        id: d.id,
        label: d.title,
        type: 'document',
        children: buildTree(docs, d.id)
      }))
  }
  
  return buildTree(documents.value)
})

function handleNodeClick(data) {
  router.push(`/spaces/${spaceId.value}/document/${data.id}`)
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
      spaceId: spaceId.value,
      parentId: documentForm.parentId
    })
    
    ElMessage.success('文档创建成功')
    showCreateDocument.value = false
    documentForm.title = ''
    documentForm.parentId = null
    
    router.push(`/spaces/${spaceId.value}/document/${doc.id}`)
  } catch (error) {
    ElMessage.error('创建失败，请稍后重试')
  } finally {
    creating.value = false
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
.space-detail-page {
  display: flex;
  height: 100%;
}

.sidebar-container {
  width: 240px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.folder-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-label {
  font-size: 13px;
}

.content-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.el-tree-node__content) {
  height: 32px;
}

:deep(.el-tree-node__label) {
  flex: 1;
}
</style>
