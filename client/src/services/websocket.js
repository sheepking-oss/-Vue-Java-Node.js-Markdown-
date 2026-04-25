import { io } from 'socket.io-client'
import { ref, computed } from 'vue'
import { useDocumentStore } from '@/stores/document'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:3001'

const socket = ref(null)
const isConnected = ref(false)
const currentDocumentId = ref(null)
const remoteCursors = ref(new Map())
const notifications = ref([])

export function useWebSocket() {
  const documentStore = useDocumentStore()

  function connect(token) {
    if (socket.value) {
      return
    }

    socket.value = io(WS_URL, {
      auth: { token },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: 10
    })

    socket.value.on('connect', () => {
      console.log('WebSocket connected')
      isConnected.value = true
    })

    socket.value.on('disconnect', () => {
      console.log('WebSocket disconnected')
      isConnected.value = false
    })

    socket.value.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error)
    })

    socket.value.on('user-joined', (user) => {
      console.log('User joined:', user)
      const editors = [...documentStore.activeEditors]
      if (!editors.find(e => e.id === user.id)) {
        documentStore.setActiveEditors([...editors, user])
      }
    })

    socket.value.on('user-left', (user) => {
      console.log('User left:', user)
      const editors = documentStore.activeEditors.filter(e => e.id !== user.id)
      documentStore.setActiveEditors(editors)
      remoteCursors.value.delete(user.id)
    })

    socket.value.on('active-editors', (editors) => {
      console.log('Active editors:', editors)
      documentStore.setActiveEditors(editors)
    })

    socket.value.on('content-edited', (data) => {
      console.log('Content edited by:', data.username)
    })

    socket.value.on('cursor-moved', (data) => {
      remoteCursors.value.set(data.userId, {
        username: data.username,
        cursorPosition: data.cursorPosition,
        selection: data.selection
      })
    })

    socket.value.on('auto-save-notification', (data) => {
      console.log('Auto-save by:', data.username)
      addNotification({
        type: 'info',
        message: `${data.username} 自动保存了修改`
      })
    })

    socket.value.on('comment-added', (data) => {
      console.log('Comment added:', data)
      addNotification({
        type: 'info',
        message: `${data.username} 添加了评论`
      })
    })

    socket.value.on('comment-updated', (data) => {
      console.log('Comment updated:', data)
    })

    socket.value.on('comment-deleted', (data) => {
      console.log('Comment deleted:', data)
    })

    socket.value.on('document-saved', (data) => {
      console.log('Document saved by:', data.username)
      addNotification({
        type: 'success',
        message: `${data.username} 保存了文档 (版本 ${data.version})`
      })
    })

    socket.value.on('document-renamed', (data) => {
      console.log('Document renamed:', data)
      addNotification({
        type: 'info',
        message: `${data.username} 重命名了文档`
      })
    })

    socket.value.on('notification', (data) => {
      addNotification(data)
    })

    socket.value.on('direct-message', (data) => {
      addNotification({
        type: 'message',
        title: `来自 ${data.fromUsername} 的消息`,
        message: data.message
      })
    })

    socket.value.on('user-typing', (data) => {
      console.log('User typing:', data)
    })
  }

  function disconnect() {
    if (socket.value) {
      socket.value.disconnect()
      socket.value = null
      isConnected.value = false
    }
  }

  function joinDocument(documentId, spaceId) {
    if (socket.value && isConnected.value) {
      currentDocumentId.value = documentId
      socket.value.emit('join-document', { documentId, spaceId })
    }
  }

  function leaveDocument(documentId) {
    if (socket.value && isConnected.value) {
      socket.value.emit('leave-document', { documentId })
      currentDocumentId.value = null
      remoteCursors.value.clear()
    }
  }

  function emitEvent(event, data) {
    if (socket.value && isConnected.value) {
      socket.value.emit(event, data)
    }
  }

  function sendEditContent(documentId, content, cursorPosition) {
    emitEvent('edit-content', { documentId, content, cursorPosition })
  }

  function sendCursorMove(documentId, cursorPosition, selection) {
    emitEvent('cursor-move', { documentId, cursorPosition, selection })
  }

  function sendTypingStart(documentId) {
    emitEvent('typing-start', { documentId })
  }

  function sendTypingStop(documentId) {
    emitEvent('typing-stop', { documentId })
  }

  function addNotification(notification) {
    const id = Date.now()
    notifications.value.push({
      id,
      ...notification,
      timestamp: new Date()
    })

    setTimeout(() => {
      removeNotification(id)
    }, 5000)
  }

  function removeNotification(id) {
    notifications.value = notifications.value.filter(n => n.id !== id)
  }

  return {
    socket,
    isConnected: computed(() => isConnected.value),
    currentDocumentId: computed(() => currentDocumentId.value),
    remoteCursors: computed(() => remoteCursors.value),
    notifications: computed(() => notifications.value),
    connect,
    disconnect,
    joinDocument,
    leaveDocument,
    emitEvent,
    sendEditContent,
    sendCursorMove,
    sendTypingStart,
    sendTypingStop,
    addNotification,
    removeNotification
  }
}
