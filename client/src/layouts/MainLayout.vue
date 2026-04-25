<template>
  <el-container class="main-layout">
    <el-aside :width="sidebarCollapsed ? '64px' : '240px'" class="sidebar">
      <div class="sidebar-header">
        <el-icon v-if="sidebarCollapsed" class="logo-icon"><Document /></el-icon>
        <span v-else class="logo-text">知识文库</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/spaces">
          <el-icon><FolderOpened /></el-icon>
          <template #title>我的空间</template>
        </el-menu-item>

        <el-sub-menu v-if="currentSpaceId" :index="'space-' + currentSpaceId">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>{{ currentSpace?.name || '当前空间' }}</span>
          </template>
          
          <el-menu-item :index="`/spaces/${currentSpaceId}`">
            <el-icon><Document /></el-icon>
            <template #title>文档列表</template>
          </el-menu-item>
          
          <el-menu-item :index="`/spaces/${currentSpaceId}/trash`">
            <el-icon><Delete /></el-icon>
            <template #title>回收站</template>
          </el-menu-item>
          
          <el-menu-item :index="`/spaces/${currentSpaceId}/members`">
            <el-icon><User /></el-icon>
            <template #title>成员管理</template>
          </el-menu-item>
          
          <el-menu-item :index="`/spaces/${currentSpaceId}/settings`">
            <el-icon><Setting /></el-icon>
            <template #title>空间设置</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="sidebar-footer">
        <el-divider v-if="!sidebarCollapsed" />
        <el-dropdown placement="top">
          <div class="user-info">
            <el-avatar :size="32">
              {{ user?.nickname?.charAt(0) || user?.username?.charAt(0) }}
            </el-avatar>
            <span v-if="!sidebarCollapsed" class="username">{{ user?.nickname || user?.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container>
      <el-header class="main-header">
        <div class="header-left">
          <el-button text @click="toggleSidebar">
            <el-icon :size="20"><Menu /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              <router-link v-if="item.path" :to="item.path">{{ item.name }}</router-link>
              <span v-else>{{ item.name }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-input
            v-if="currentSpaceId"
            v-model="searchKeyword"
            placeholder="搜索文档..."
            style="width: 200px; margin-right: 16px;"
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
            <el-button text>
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-tooltip :content="wsConnected ? '已连接' : '未连接'" placement="bottom">
            <el-icon :size="18" :color="wsConnected ? '#67c23a' : '#909399'" class="ws-status">
              <Connection />
            </el-icon>
          </el-tooltip>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSpaceStore } from '@/stores/space'
import { useUIStore } from '@/stores/ui'
import { useDocumentStore } from '@/stores/document'
import { useWebSocket } from '@/services/websocket'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const spaceStore = useSpaceStore()
const uiStore = useUIStore()
const documentStore = useDocumentStore()
const { isConnected, notifications, connect, disconnect } = useWebSocket()

const sidebarCollapsed = computed(() => uiStore.sidebarCollapsed)
const user = computed(() => authStore.user)
const spaces = computed(() => spaceStore.spaces)
const currentSpace = computed(() => spaceStore.currentSpace)
const wsConnected = computed(() => isConnected.value)
const unreadCount = computed(() => notifications.value.length)

const searchKeyword = ref('')
const searchTimer = ref(null)

const currentSpaceId = computed(() => {
  const spaceId = route.params.spaceId
  return spaceId ? parseInt(spaceId) : null
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/spaces/') && route.params.spaceId) {
    const basePath = `/spaces/${route.params.spaceId}`
    if (route.path === basePath) return route.path
    if (route.path.includes('/document/')) return `${basePath}/documents`
    return route.path
  }
  return '/spaces'
})

const breadcrumbs = computed(() => {
  const crumbs = [{ name: '首页', path: '/spaces' }]
  
  if (currentSpace.value) {
    crumbs.push({ 
      name: currentSpace.value.name, 
      path: `/spaces/${currentSpace.value.id}` 
    })
    
    if (route.name === 'DocumentEditor' && documentStore.currentDocument) {
      crumbs.push({ 
        name: documentStore.currentDocument.title,
        path: null 
      })
    } else if (route.name === 'Trash') {
      crumbs.push({ name: '回收站', path: null })
    } else if (route.name === 'SpaceMembers') {
      crumbs.push({ name: '成员管理', path: null })
    } else if (route.name === 'SpaceSettings') {
      crumbs.push({ name: '空间设置', path: null })
    }
  }
  
  return crumbs
})

function toggleSidebar() {
  uiStore.toggleSidebar()
}

function handleLogout() {
  disconnect()
  authStore.logout()
  ElMessage.success('已退出登录')
}

function handleSearch() {
  if (!searchKeyword.value.trim() || !currentSpaceId.value) return
  
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
  
  searchTimer.value = setTimeout(async () => {
    try {
      const results = await documentStore.searchDocuments(currentSpaceId.value, searchKeyword.value)
      if (results.length === 0) {
        ElMessage.info('未找到匹配的文档')
      }
    } catch (error) {
      console.error('Search error:', error)
    }
  }, 300)
}

function clearSearch() {
  searchKeyword.value = ''
}

onMounted(() => {
  spaceStore.fetchSpaces()
  
  if (authStore.token) {
    connect(authStore.token)
  }
})

onUnmounted(() => {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
})

watch(() => currentSpaceId.value, (newSpaceId) => {
  if (newSpaceId) {
    spaceStore.fetchSpace(newSpaceId)
  }
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  margin-left: 8px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.username {
  margin-left: 12px;
  color: #bfcbd9;
  font-size: 14px;
}

.main-header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-badge {
  cursor: pointer;
}

.ws-status {
  cursor: pointer;
}

.main-content {
  background-color: #f0f2f5;
  padding: 24px;
  overflow: auto;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-sub-menu__title) {
  padding-left: 20px !important;
}
</style>
