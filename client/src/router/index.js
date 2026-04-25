import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/spaces'
  },
  {
    path: '/spaces',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Spaces',
        component: () => import('@/views/Spaces.vue')
      },
      {
        path: ':spaceId',
        name: 'SpaceDetail',
        component: () => import('@/views/SpaceDetail.vue'),
        children: [
          {
            path: '',
            name: 'SpaceDocuments',
            component: () => import('@/views/DocumentList.vue')
          },
          {
            path: 'document/:documentId',
            name: 'DocumentEditor',
            component: () => import('@/views/DocumentEditor.vue')
          },
          {
            path: 'trash',
            name: 'Trash',
            component: () => import('@/views/Trash.vue')
          },
          {
            path: 'settings',
            name: 'SpaceSettings',
            component: () => import('@/views/SpaceSettings.vue')
          },
          {
            path: 'members',
            name: 'SpaceMembers',
            component: () => import('@/views/Members.vue')
          }
        ]
      }
    ]
  },
  {
    path: '/share/:shareCode',
    name: 'ShareAccess',
    component: () => import('@/views/ShareAccess.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (!requiresAuth && authStore.isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Spaces' })
  } else {
    next()
  }
})

export default router
