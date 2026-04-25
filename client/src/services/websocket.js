import { io } from 'socket.io-client'
import { ref, computed } from 'vue'
import { useDocumentStore } from '@/stores/document'
import { applyOperations, transformOperations } from '@/utils/ot'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:3001'

const socket = ref(null)
const isConnected = ref(false)
const currentDocumentId = ref(null)
const remoteCursors = ref(new Map())
const notifications = ref([])

const pendingOperations = ref([])
const acknowledgedVersion = ref(0)
const localVersion = ref(0)
const isSendingOperations = ref(false)

const operationCallbacks = ref(new Map())

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

    socket.value.on('operation-ack', (data) => {
      console.log('[OT] Operation acknowledged:', data)
      
      if (data.success) {
        acknowledgedVersion.value = data.newVersion
        localVersion.value = data.newVersion
        
        const callback = operationCallbacks.value.get('operation')
        if (callback) {
          callback(null, data)
          operationCallbacks.value.delete('operation')
        }
        
        processPendingOperations()
      } else {
        console.error('[OT] Operation error:', data.error)
        addNotification({
          type: 'error',
          message: `操作失败: ${data.error}`
        })
        
        const callback = operationCallbacks.value.get('operation')
        if (callback) {
          callback(new Error(data.error), data)
          operationCallbacks.value.delete('operation')
        }
      }
    })

    socket.value.on('operation-broadcast', (data) => {
      console.log('[OT] Received broadcast operation from:', data.username)
      
      if (pendingOperations.value.length > 0) {
        console.log('[OT] Need to transform pending operations against broadcast')
        
        for (let i = 0; i < pendingOperations.value.length; i++) {
          const transformed = transformOperations(
            pendingOperations.value[i].operations,
            data.operations
          )
          pendingOperations.value[i].operations = transformed.first
        }
      }
      
      try {
        const currentContent = documentStore.currentDocument?.content || ''
        const newContent = applyOperations(currentContent, data.operations)
        
        if (documentStore.currentDocument) {
          documentStore.currentDocument.content = newContent
        }
        
        localVersion.value = data.newVersion
        
        console.log('[OT] Applied broadcast operations, new version:', data.newVersion)
        
        addNotification({
          type: 'info',
          message: `${data.username} 更新了文档`
        })
      } catch (error) {
        console.error('[OT] Failed to apply broadcast operations:', error)
        requestSync(currentDocumentId.value)
      }
    })

    socket.value.on('operation-error', (data) => {
      console.error('[OT] Operation error:', data.error)
      addNotification({
        type: 'error',
        message: `操作失败: ${data.error}`
      })
      
      if (data.currentContent) {
        console.log('[OT] Server content version:', data.currentVersion)
        if (documentStore.currentDocument) {
          documentStore.currentDocument.content = data.currentContent
        }
        localVersion.value = data.currentVersion
        pendingOperations.value = []
      }
    })

    socket.value.on('sync-response', (data) => {
      console.log('[Sync] Received sync response, version:', data.version)
      
      if (documentStore.currentDocument) {
        documentStore.currentDocument.content = data.content
      }
      localVersion.value = data.version
      acknowledgedVersion.value = data.version
      pendingOperations.value = []
      
      const callback = operationCallbacks.value.get('sync')
      if (callback) {
        callback(null, data)
        operationCallbacks.value.delete('sync')
      }
      
      addNotification({
        type: 'info',
        message: '文档已同步到最新版本'
      })
    })

    socket.value.on('sync-request-failed', (data) => {
      console.error('[Sync] Sync request failed:', data.error)
      
      const callback = operationCallbacks.value.get('sync')
      if (callback) {
        callback(new Error(data.error), data)
        operationCallbacks.value.delete('sync')
      }
    })

    socket.value.on('document-state-initialized', (data) => {
      console.log('[Init] Document state initialized, version:', data.version)
      localVersion.value = data.version
      acknowledgedVersion.value = data.version
    })

    socket.value.on('diff-result', (data) => {
      console.log('[Diff] Computed', data.operations.length, 'operations')
      const callback = operationCallbacks.value.get('diff')
      if (callback) {
        callback(null, data)
        operationCallbacks.value.delete('diff')
      }
    })

    socket.value.on('diff-error', (data) => {
      console.error('[Diff] Error:', data.error)
      const callback = operationCallbacks.value.get('diff')
      if (callback) {
        callback(new Error(data.error), data)
        operationCallbacks.value.delete('diff')
      }
    })

    socket.value.on('apply-diff-result', (data) => {
      console.log('[Apply Diff] Success:', data.success)
      const callback = operationCallbacks.value.get('applyDiff')
      if (callback) {
        callback(null, data)
        operationCallbacks.value.delete('applyDiff')
      }
    })

    socket.value.on('apply-diff-error', (data) => {
      console.error('[Apply Diff] Error:', data.error)
      const callback = operationCallbacks.value.get('applyDiff')
      if (callback) {
        callback(new Error(data.error), data)
        operationCallbacks.value.delete('applyDiff')
      }
    })
  }

  function disconnect() {
    if (socket.value) {
      socket.value.disconnect()
      socket.value = null
      isConnected.value = false
    }
  }

  function joinDocument(documentId, spaceId, initialContent = '', initialVersion = 1) {
    if (socket.value && isConnected.value) {
      currentDocumentId.value = documentId
      localVersion.value = initialVersion
      acknowledgedVersion.value = initialVersion
      pendingOperations.value = []
      
      socket.value.emit('initialize-document-state', {
        documentId,
        content: initialContent,
        version: initialVersion
      })
      
      socket.value.emit('join-document', { documentId, spaceId })
    }
  }

  function leaveDocument(documentId) {
    if (socket.value && isConnected.value) {
      socket.value.emit('leave-document', { documentId })
      currentDocumentId.value = null
      remoteCursors.value.clear()
      pendingOperations.value = []
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

  async function submitOperations(documentId, operations, baseContent) {
    return new Promise((resolve, reject) => {
      if (!socket.value || !isConnected.value) {
        reject(new Error('WebSocket not connected'))
        return
      }

      const pendingOp = {
        documentId,
        operations,
        fromVersion: localVersion.value,
        baseContent,
        timestamp: Date.now()
      }

      if (pendingOperations.value.length > 0 || isSendingOperations.value) {
        console.log('[OT] Queuing operation, pending count:', pendingOperations.value.length + 1)
        pendingOperations.value.push(pendingOp)
        resolve({ queued: true })
        return
      }

      isSendingOperations.value = true
      
      operationCallbacks.value.set('operation', (error, data) => {
        isSendingOperations.value = false
        if (error) {
          reject(error)
        } else {
          resolve(data)
        }
      })

      console.log('[OT] Submitting', operations.length, 'operations from version', localVersion.value)
      
      socket.value.emit('operation-submit', {
        documentId,
        operations,
        fromVersion: localVersion.value,
        baseContent
      })
    })
  }

  function processPendingOperations() {
    if (pendingOperations.value.length === 0 || isSendingOperations.value) {
      return
    }

    const pending = pendingOperations.value.shift()
    
    console.log('[OT] Processing queued operation, remaining:', pendingOperations.value.length)
    
    submitOperations(
      pending.documentId,
      pending.operations,
      pending.baseContent
    ).catch(error => {
      console.error('[OT] Failed to process queued operation:', error)
      pendingOperations.value.unshift(pending)
    })
  }

  function requestSync(documentId) {
    return new Promise((resolve, reject) => {
      if (!socket.value || !isConnected.value) {
        reject(new Error('WebSocket not connected'))
        return
      }

      operationCallbacks.value.set('sync', (error, data) => {
        if (error) {
          reject(error)
        } else {
          resolve(data)
        }
      })

      console.log('[Sync] Requesting sync for document:', documentId)
      socket.value.emit('sync-request', { documentId })
    })
  }

  function computeDiff(oldContent, newContent) {
    return new Promise((resolve, reject) => {
      if (!socket.value || !isConnected.value) {
        reject(new Error('WebSocket not connected'))
        return
      }

      operationCallbacks.value.set('diff', (error, data) => {
        if (error) {
          reject(error)
        } else {
          resolve(data)
        }
      })

      socket.value.emit('compute-diff', { oldContent, newContent })
    })
  }

  function applyDiff(content, operations) {
    return new Promise((resolve, reject) => {
      if (!socket.value || !isConnected.value) {
        reject(new Error('WebSocket not connected'))
        return
      }

      operationCallbacks.value.set('applyDiff', (error, data) => {
        if (error) {
          reject(error)
        } else {
          resolve(data)
        }
      })

      socket.value.emit('apply-diff', { content, operations })
    })
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
    localVersion: computed(() => localVersion.value),
    acknowledgedVersion: computed(() => acknowledgedVersion.value),
    pendingOperationsCount: computed(() => pendingOperations.value.length),
    
    connect,
    disconnect,
    joinDocument,
    leaveDocument,
    emitEvent,
    sendEditContent,
    sendCursorMove,
    sendTypingStart,
    sendTypingStop,
    
    submitOperations,
    requestSync,
    computeDiff,
    applyDiff,
    processPendingOperations,
    
    addNotification,
    removeNotification
  }
}
