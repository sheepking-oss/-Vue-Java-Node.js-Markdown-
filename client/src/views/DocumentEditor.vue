<template>
  <div class="document-editor-page">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="documentTitle"
          class="title-input"
          @blur="handleTitleBlur"
          @keyup.enter="handleTitleBlur"
        />
        
        <el-tag v-if="hasUnsavedChanges" type="warning" size="small">
          <el-icon><EditPen /></el-icon>
          未保存
        </el-tag>
        
        <el-tag v-if="hasDraft" type="info" size="small" @click="showRestoreDraft = true" class="clickable-tag">
          <el-icon><Clock /></el-icon>
          有草稿
        </el-tag>

        <div class="version-info">
          <el-icon><DocumentCopy /></el-icon>
          <span>版本 {{ currentVersion }}</span>
        </div>
      </div>

      <div class="toolbar-right">
        <div class="active-editors" v-if="activeEditors.length > 0">
          <el-tooltip
            v-for="editor in activeEditors"
            :key="editor.id"
            :content="editor.username + ' 正在编辑'"
            placement="bottom"
          >
            <el-avatar :size="24" class="editor-avatar">
              {{ editor.username?.charAt(0) }}
            </el-avatar>
          </el-tooltip>
        </div>

        <el-select v-model="saveMode" size="small" style="width: 120px; margin-right: 12px;">
          <el-option label="手动保存" value="manual" />
          <el-option label="自动保存" value="auto" />
        </el-select>

        <el-button type="primary" :loading="isSaving" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存
        </el-button>

        <el-dropdown>
          <el-button>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showVersions = true">
                <el-icon><Clock /></el-icon>
                历史版本
              </el-dropdown-item>
              <el-dropdown-item @click="showShare = true">
                <el-icon><Share /></el-icon>
                分享文档
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleMoveToTrash">
                <el-icon><Delete /></el-icon>
                删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="editor-layout">
      <div class="editor-content">
        <div class="editor-pane">
          <div class="pane-header">
            <span class="pane-title">编辑</span>
          </div>
          <el-input
            v-model="documentContent"
            type="textarea"
            :rows="20"
            placeholder="在此输入 Markdown 内容..."
            class="markdown-editor"
            @input="handleContentChange"
            @keyup="handleKeyUp"
          />
        </div>

        <div class="editor-pane">
          <div class="pane-header">
            <span class="pane-title">预览</span>
          </div>
          <div class="markdown-preview" v-html="renderedContent"></div>
        </div>
      </div>

      <div class="editor-sidebar">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="评论" name="comments">
            <div class="comments-section">
              <div class="comment-input-section">
                <el-input
                  v-model="newComment"
                  type="textarea"
                  :rows="3"
                  placeholder="添加评论..."
                  class="comment-input"
                />
                <el-button type="primary" size="small" @click="handleAddComment" :disabled="!newComment.trim()">
                  发送
                </el-button>
              </div>

              <div class="comments-list" v-if="comments.length > 0">
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <div class="comment-header">
                    <el-avatar :size="24">
                      {{ comment.user?.nickname?.charAt(0) || comment.user?.username?.charAt(0) }}
                    </el-avatar>
                    <span class="comment-author">{{ comment.user?.nickname || comment.user?.username }}</span>
                    <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                  </div>
                  <div class="comment-body">{{ comment.content }}</div>
                </div>
              </div>
              <el-empty v-else description="暂无评论" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="标签" name="tags">
            <div class="tags-section">
              <div class="tag-add-section">
                <el-input
                  v-model="newTagName"
                  placeholder="创建新标签"
                  size="small"
                  style="flex: 1;"
                  @keyup.enter="handleCreateTag"
                />
                <el-color-picker v-model="newTagColor" size="small" />
                <el-button size="small" @click="handleCreateTag">
                  <el-icon><Plus /></el-icon>
                </el-button>
              </div>

              <div class="tags-list">
                <el-tag
                  v-for="tag in tags"
                  :key="tag.id"
                  :style="{ backgroundColor: tag.color, borderColor: tag.color }"
                  closable
                  class="document-tag"
                  @close="handleRemoveTag(tag.id)"
                >
                  {{ tag.name }}
                </el-tag>
                <el-empty v-if="tags.length === 0" description="暂无标签" />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="信息" name="info">
            <div class="info-section">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="创建者">
                  {{ document?.createdBy?.username || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ formatTime(document?.createdAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="最后修改">
                  {{ formatTime(document?.updatedAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="修改者">
                  {{ document?.updatedBy?.username || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="当前版本">
                  {{ document?.version || 1 }}
                </el-descriptions-item>
                <el-descriptions-item label="草稿时间">
                  {{ formatTime(document?.autoSavedAt) || '无' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="showVersions" title="历史版本" width="700px">
      <el-table :data="versions" style="width: 100%;">
        <el-table-column prop="version" label="版本号" width="80" />
        <el-table-column prop="title" label="标题" width="200" />
        <el-table-column prop="changeNote" label="变更说明" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewVersion(row)">
              查看
            </el-button>
            <el-button size="small" type="primary" @click="handleRollbackVersion(row)">
              回滚
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="showVersionPreview" :title="'版本 ' + previewVersion?.version" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">
          {{ previewVersion?.title }}
        </el-descriptions-item>
        <el-descriptions-item label="变更说明">
          {{ previewVersion?.changeNote || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="version-preview" v-html="previewVersionContent"></div>
    </el-dialog>

    <el-dialog v-model="showShare" title="分享文档" width="500px">
      <div v-if="shares.length > 0">
        <h4 style="margin-bottom: 16px;">已创建的分享链接</h4>
        <el-table :data="shares" style="width: 100%;">
          <el-table-column prop="type" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.type === 'edit' ? 'warning' : 'info'" size="small">
                {{ row.type === 'edit' ? '可编辑' : '仅查看' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="shareCode" label="分享码" />
          <el-table-column prop="enabled" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" text @click="copyShareLink(row)">
                复制链接
              </el-button>
              <el-button size="small" text @click="toggleShare(row)">
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-divider>创建新分享</el-divider>
      <el-form :model="shareForm" label-width="80px">
        <el-form-item label="权限类型">
          <el-radio-group v-model="shareForm.type">
            <el-radio value="view">仅查看</el-radio>
            <el-radio value="edit">可编辑</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-select v-model="shareForm.expiresInHours" placeholder="选择过期时间">
            <el-option :value="null" label="永不过期" />
            <el-option :value="1" label="1小时" />
            <el-option :value="24" label="1天" />
            <el-option :value="168" label="7天" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问密码">
          <el-input
            v-model="shareForm.password"
            type="password"
            placeholder="留空则无需密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShare = false">取消</el-button>
        <el-button type="primary" @click="handleCreateShare">创建分享</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRestoreDraft" title="恢复草稿" width="400px">
      <p>检测到未保存的草稿，是否恢复？</p>
      <template #footer>
        <el-button @click="showRestoreDraft = false">取消</el-button>
        <el-button type="primary" @click="handleRestoreDraft">恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentStore } from '@/stores/document'
import { useUIStore } from '@/stores/ui'
import { useSpaceStore } from '@/stores/space'
import { useWebSocket } from '@/services/websocket'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import dompurify from 'dompurify'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const documentStore = useDocumentStore()
const uiStore = useUIStore()
const spaceStore = useSpaceStore()
const { sendEditContent, sendCursorMove, emitEvent, isConnected } = useWebSocket()

const documentId = computed(() => parseInt(route.params.documentId))
const spaceId = computed(() => parseInt(route.params.spaceId))

const document = computed(() => documentStore.currentDocument)
const activeEditors = computed(() => documentStore.activeEditors)
const comments = computed(() => uiStore.comments)
const tags = computed(() => uiStore.tags)
const shares = computed(() => uiStore.shares)
const versions = computed(() => documentStore.currentVersions)
const isSaving = computed(() => documentStore.isSaving)

const documentTitle = ref('')
const documentContent = ref('')
const activeTab = ref('comments')
const saveMode = ref('auto')
const hasUnsavedChanges = ref(false)
const hasDraft = ref(false)
const currentVersion = ref(1)
const originalContent = ref('')
const originalTitle = ref('')

const showVersions = ref(false)
const showShare = ref(false)
const showRestoreDraft = ref(false)
const showVersionPreview = ref(false)
const previewVersion = ref(null)
const previewVersionContent = ref('')

const newComment = ref('')
const newTagName = ref('')
const newTagColor = ref('#409eff')

const shareForm = reactive({
  type: 'view',
  expiresInHours: null,
  password: ''
})

const renderedContent = computed(() => {
  try {
    const html = marked.parse(documentContent.value || '')
    return dompurify.sanitize(html)
  } catch (e) {
    return '<p>渲染错误</p>'
  }
})

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

async function loadDocument() {
  try {
    const doc = await documentStore.fetchDocument(documentId.value)
    documentTitle.value = doc.title
    documentContent.value = doc.content || ''
    originalTitle.value = doc.title
    originalContent.value = doc.content || ''
    currentVersion.value = doc.version || 1
    hasDraft.value = doc.hasUnsavedDraft || false

    await documentStore.joinDocumentSession(documentId.value, spaceId.value)
    
    await uiStore.fetchComments(documentId.value)
    await uiStore.fetchTags(spaceId.value)
    await uiStore.fetchShares(documentId.value)
    await documentStore.fetchVersions(documentId.value)
  } catch (error) {
    ElMessage.error('加载文档失败')
  }
}

function handleContentChange() {
  checkUnsavedChanges()
  
  if (saveMode.value === 'auto' && isConnected.value) {
    sendEditContent(documentId.value, documentContent.value, 0)
  }
}

function handleKeyUp(event) {
  if (saveMode.value === 'auto' && documentStore.startAutoSave) {
    documentStore.startAutoSave(
      documentId.value,
      () => documentContent.value
    )
  }
}

function handleTitleBlur() {
  checkUnsavedChanges()
}

function checkUnsavedChanges() {
  hasUnsavedChanges.value = 
    documentTitle.value !== originalTitle.value ||
    documentContent.value !== originalContent.value
}

async function handleSave() {
  try {
    const changeNote = `更新内容 (${dayjs().format('HH:mm')})`
    await documentStore.updateDocument(documentId.value, {
      title: documentTitle.value,
      content: documentContent.value,
      changeNote
    })
    
    originalTitle.value = documentTitle.value
    originalContent.value = documentContent.value
    hasUnsavedChanges.value = false
    
    if (isConnected.value) {
      emitEvent('document-saved', {
        documentId: documentId.value,
        version: currentVersion.value + 1
      })
    }
    
    currentVersion.value++
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function handleAddComment() {
  if (!newComment.value.trim()) return
  
  try {
    await uiStore.createComment({
      documentId: documentId.value,
      content: newComment.value
    })
    newComment.value = ''
    ElMessage.success('评论已添加')
  } catch (error) {
    ElMessage.error('添加评论失败')
  }
}

async function handleCreateTag() {
  if (!newTagName.value.trim()) return
  
  try {
    await uiStore.createTag(spaceId.value, {
      name: newTagName.value.trim(),
      color: newTagColor.value
    })
    newTagName.value = ''
    ElMessage.success('标签已创建')
  } catch (error) {
    ElMessage.error('创建标签失败')
  }
}

async function handleRemoveTag(tagId) {
  try {
    await documentStore.removeTag(documentId.value, tagId)
    ElMessage.success('已移除标签')
  } catch (error) {
    ElMessage.error('移除标签失败')
  }
}

async function handleViewVersion(version) {
  previewVersion.value = version
  try {
    const html = marked.parse(version.content || '')
    previewVersionContent.value = dompurify.sanitize(html)
    showVersionPreview.value = true
  } catch (e) {
    previewVersionContent.value = '<p>渲染错误</p>'
    showVersionPreview.value = true
  }
}

async function handleRollbackVersion(version) {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到版本 ${version.version} 吗？当前未保存的修改将会丢失。`,
      '确认回滚',
      { type: 'warning' }
    )
    
    await documentStore.rollbackToVersion(documentId.value, version.version)
    
    documentTitle.value = version.title
    documentContent.value = version.content
    originalTitle.value = version.title
    originalContent.value = version.content
    hasUnsavedChanges.value = false
    currentVersion.value++
    
    ElMessage.success('回滚成功')
    showVersions.value = false
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('回滚失败')
    }
  }
}

async function handleCreateShare() {
  try {
    await uiStore.createShare({
      documentId: documentId.value,
      type: shareForm.type,
      password: shareForm.password || null,
      expiresInHours: shareForm.expiresInHours
    })
    ElMessage.success('分享链接已创建')
    shareForm.password = ''
  } catch (error) {
    ElMessage.error('创建分享失败')
  }
}

function copyShareLink(share) {
  const link = `${window.location.origin}/share/${share.shareCode}`
  navigator.clipboard.writeText(link)
  ElMessage.success('链接已复制到剪贴板')
}

async function toggleShare(share) {
  try {
    await uiStore.toggleShare(share.id)
    ElMessage.success(share.enabled ? '已禁用分享' : '已启用分享')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

async function handleRestoreDraft() {
  try {
    const doc = await documentStore.restoreDraft(documentId.value)
    documentContent.value = doc.content
    originalContent.value = doc.content
    hasDraft.value = false
    hasUnsavedChanges.value = false
    showRestoreDraft.value = false
    ElMessage.success('草稿已恢复')
  } catch (error) {
    ElMessage.error('恢复草稿失败')
  }
}

async function handleMoveToTrash() {
  try {
    await ElMessageBox.confirm(
      '确定要将此文档移到回收站吗？',
      '确认删除',
      { type: 'warning' }
    )
    
    await documentStore.moveToTrash(documentId.value)
    router.push(`/spaces/${spaceId.value}`)
    ElMessage.success('已移到回收站')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

function handleTabChange(tabName) {
  uiStore.setActiveTab(tabName)
}

onMounted(() => {
  if (documentId.value) {
    loadDocument()
  }
})

onUnmounted(() => {
  documentStore.leaveDocumentSession(documentId.value)
  documentStore.stopAutoSave()
})

watch(() => route.params.documentId, (newDocId) => {
  if (newDocId && parseInt(newDocId) !== documentId.value) {
    documentStore.leaveDocumentSession(documentId.value)
    loadDocument()
  }
})
</script>

<style scoped>
.document-editor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fafafa;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-input {
  width: 300px;
}

.title-input :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 600;
}

.clickable-tag {
  cursor: pointer;
}

.version-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.active-editors {
  display: flex;
  gap: -8px;
}

.editor-avatar {
  border: 2px solid #fff;
  margin-left: -8px;
}

.editor-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-content {
  flex: 1;
  display: flex;
  border-right: 1px solid #e4e7ed;
}

.editor-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pane-header {
  padding: 8px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.pane-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.markdown-editor {
  flex: 1;
  border: none;
  border-radius: 0;
  resize: none;
}

.markdown-editor :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.markdown-preview {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3),
.markdown-preview :deep(h4),
.markdown-preview :deep(h5),
.markdown-preview :deep(h6) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-preview :deep(h1) { font-size: 2em; }
.markdown-preview :deep(h2) { font-size: 1.5em; }
.markdown-preview :deep(h3) { font-size: 1.25em; }

.markdown-preview :deep(p) {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-preview :deep(pre) {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-preview :deep(code) {
  background-color: rgba(27, 31, 35, 0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 85%;
}

.markdown-preview :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-preview :deep(blockquote) {
  border-left: 0.25em solid #dfe2e5;
  color: #6a737d;
  padding: 0 1em;
  margin: 0;
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  padding-left: 2em;
}

.markdown-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
}

.markdown-preview :deep(table th),
.markdown-preview :deep(table td) {
  border: 1px solid #dfe2e5;
  padding: 6px 13px;
}

.markdown-preview :deep(table th) {
  background-color: #f6f8fa;
  font-weight: 600;
}

.editor-sidebar {
  width: 320px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comments-section,
.tags-section,
.info-section {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

.comment-input-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.comment-input {
  margin-bottom: 8px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  font-size: 13px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-body {
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}

.tag-add-section {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.document-tag {
  color: #fff;
}

.version-preview {
  margin-top: 16px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  max-height: 400px;
  overflow-y: auto;
}

:deep(.el-tabs__header) {
  margin-bottom: 0;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.el-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
