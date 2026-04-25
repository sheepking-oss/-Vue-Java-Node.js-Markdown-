<template>
  <div class="members-page">
    <div class="page-header">
      <h2>
        <el-icon><User /></el-icon>
        成员管理
      </h2>
      <el-button type="primary" @click="showAddMember = true">
        <el-icon><Plus /></el-icon>
        添加成员
      </el-button>
    </div>

    <el-table :data="members" style="width: 100%;" stripe>
      <el-table-column prop="user" label="用户" min-width="200">
        <template #default="{ row }">
          <div class="user-info">
            <el-avatar :size="36">
              {{ row.user?.nickname?.charAt(0) || row.user?.username?.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <span class="username">{{ row.user?.nickname || row.user?.username }}</span>
              <span class="email">{{ row.user?.email }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="150">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-select
            v-model="row.role"
            size="small"
            style="width: 100px; margin-right: 8px;"
            @change="(newRole) => handleChangeRole(row, newRole)"
            :disabled="row.role === 'OWNER'"
          >
            <el-option label="管理员" value="ADMIN" />
            <el-option label="编辑者" value="EDITOR" />
            <el-option label="查看者" value="VIEWER" />
          </el-select>
          <el-button
            type="danger"
            link
            @click="handleRemoveMember(row)"
            :disabled="row.role === 'OWNER'"
          >
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddMember" title="添加成员" width="500px">
      <el-form :model="addMemberForm" label-width="80px">
        <el-form-item label="用户ID" required>
          <el-input
            v-model="addMemberForm.userId"
            placeholder="请输入用户ID"
            type="number"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="addMemberForm.role">
            <el-radio value="VIEWER">查看者</el-radio>
            <el-radio value="EDITOR">编辑者</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMember = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddMember">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSpaceStore } from '@/stores/space'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const spaceStore = useSpaceStore()

const spaceId = computed(() => parseInt(route.params.spaceId))
const members = computed(() => spaceStore.members)

const showAddMember = ref(false)
const adding = ref(false)

const addMemberForm = reactive({
  userId: null,
  role: 'VIEWER'
})

const roleMap = {
  OWNER: { label: '所有者', type: 'danger' },
  ADMIN: { label: '管理员', type: 'warning' },
  EDITOR: { label: '编辑者', type: 'primary' },
  VIEWER: { label: '查看者', type: 'info' }
}

function getRoleLabel(role) {
  return roleMap[role]?.label || role
}

function getRoleType(role) {
  return roleMap[role]?.type || 'info'
}

async function loadMembers() {
  await spaceStore.fetchMembers(spaceId.value)
}

async function handleAddMember() {
  if (!addMemberForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }

  adding.value = true
  try {
    await spaceStore.addMember(spaceId.value, {
      userId: addMemberForm.userId,
      role: addMemberForm.role
    })
    ElMessage.success('成员添加成功')
    showAddMember.value = false
    addMemberForm.userId = null
    addMemberForm.role = 'VIEWER'
    loadMembers()
  } catch (error) {
    ElMessage.error('添加失败，请检查用户ID是否正确')
  } finally {
    adding.value = false
  }
}

async function handleChangeRole(row, newRole) {
  try {
    await spaceStore.updateMemberRole(spaceId.value, row.user.id, newRole)
    ElMessage.success('角色已更新')
  } catch (error) {
    ElMessage.error('更新失败')
    loadMembers()
  }
}

async function handleRemoveMember(row) {
  try {
    await ElMessageBox.confirm(
      `确定要移除成员 "${row.user?.nickname || row.user?.username}" 吗？`,
      '确认移除',
      { type: 'warning' }
    )
    
    await spaceStore.removeMember(spaceId.value, row.user.id)
    ElMessage.success('已移除成员')
    loadMembers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败')
    }
  }
}

onMounted(() => {
  if (spaceId.value) {
    loadMembers()
  }
})
</script>

<style scoped>
.members-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
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

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 500;
  font-size: 14px;
}

.email {
  font-size: 12px;
  color: #909399;
}
</style>
