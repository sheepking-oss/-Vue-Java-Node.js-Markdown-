import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { documentApi, versionApi } from '@/services/api'
import { useWebSocket } from '@/services/websocket'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref([])
  const currentDocument = ref(null)
  const currentVersions = ref([])
  const activeEditors = ref([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const autoSaveTimer = ref(null)

  const { socket, joinDocument, leaveDocument, emitEvent, isConnected } = useWebSocket()

  async function fetchDocuments(spaceId) {
    isLoading.value = true
    try {
      const response = await documentApi.getBySpace(spaceId)
      documents.value = response.data
    } catch (error) {
      console.error('Failed to fetch documents:', error)
    } finally {
      isLoading.value = false
    }
  }

  async function fetchDocument(documentId) {
    isLoading.value = true
    try {
      const response = await documentApi.getById(documentId)
      currentDocument.value = response.data
      
      if (response.data.autoSavedAt && response.data.draftContent) {
        currentDocument.value.hasUnsavedDraft = true
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to fetch document:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function createDocument(data) {
    try {
      const response = await documentApi.create(data)
      documents.value.unshift(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to create document:', error)
      throw error
    }
  }

  async function updateDocument(documentId, data) {
    isSaving.value = true
    try {
      const response = await documentApi.update(documentId, data)
      
      if (currentDocument.value?.id === documentId) {
        currentDocument.value = { ...currentDocument.value, ...response.data }
      }
      
      const index = documents.value.findIndex(d => d.id === documentId)
      if (index !== -1) {
        documents.value[index] = { ...documents.value[index], ...response.data }
      }

      if (socket.value && isConnected.value) {
        emitEvent('document-saved', {
          documentId,
          version: response.data.version
        })
      }

      return response.data
    } catch (error) {
      console.error('Failed to update document:', error)
      throw error
    } finally {
      isSaving.value = false
    }
  }

  async function autoSave(documentId, draftContent) {
    try {
      await documentApi.autoSave(documentId, { draftContent })
      
      if (socket.value && isConnected.value) {
        emitEvent('auto-save', {
          documentId,
          draftContent
        })
      }
      
      if (currentDocument.value?.id === documentId) {
        currentDocument.value.draftContent = draftContent
        currentDocument.value.autoSavedAt = new Date().toISOString()
      }
    } catch (error) {
      console.error('Failed to auto-save:', error)
    }
  }

  async function restoreDraft(documentId) {
    try {
      const response = await documentApi.restoreDraft(documentId)
      if (currentDocument.value?.id === documentId) {
        currentDocument.value = response.data
        currentDocument.value.hasUnsavedDraft = false
      }
      return response.data
    } catch (error) {
      console.error('Failed to restore draft:', error)
      throw error
    }
  }

  async function moveToTrash(documentId) {
    try {
      await documentApi.moveToTrash(documentId)
      documents.value = documents.value.filter(d => d.id !== documentId)
      
      if (currentDocument.value?.id === documentId) {
        currentDocument.value = null
      }
    } catch (error) {
      console.error('Failed to move to trash:', error)
      throw error
    }
  }

  async function restoreFromTrash(documentId) {
    try {
      await documentApi.restoreFromTrash(documentId)
    } catch (error) {
      console.error('Failed to restore from trash:', error)
      throw error
    }
  }

  async function deletePermanently(documentId) {
    try {
      await documentApi.deletePermanently(documentId)
    } catch (error) {
      console.error('Failed to delete permanently:', error)
      throw error
    }
  }

  async function fetchTrash(spaceId) {
    isLoading.value = true
    try {
      const response = await documentApi.getTrash(spaceId)
      return response.data
    } catch (error) {
      console.error('Failed to fetch trash:', error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function fetchVersions(documentId) {
    try {
      const response = await versionApi.getByDocument(documentId)
      currentVersions.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch versions:', error)
      return []
    }
  }

  async function rollbackToVersion(documentId, version) {
    try {
      const response = await documentApi.rollback(documentId, version)
      if (currentDocument.value?.id === documentId) {
        currentDocument.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('Failed to rollback:', error)
      throw error
    }
  }

  async function searchDocuments(spaceId, keyword) {
    try {
      const response = await documentApi.search(spaceId, keyword)
      return response.data
    } catch (error) {
      console.error('Failed to search documents:', error)
      return []
    }
  }

  function joinDocumentSession(documentId, spaceId) {
    if (socket.value && isConnected.value) {
      joinDocument(documentId, spaceId)
    }
  }

  function leaveDocumentSession(documentId) {
    if (socket.value && isConnected.value) {
      leaveDocument(documentId)
    }
    activeEditors.value = []
  }

  function setActiveEditors(editors) {
    activeEditors.value = editors
  }

  function startAutoSave(documentId, getContentFn, interval = 30000) {
    if (autoSaveTimer.value) {
      clearInterval(autoSaveTimer.value)
    }
    
    autoSaveTimer.value = setInterval(() => {
      const content = getContentFn()
      if (content) {
        autoSave(documentId, content)
      }
    }, interval)
  }

  function stopAutoSave() {
    if (autoSaveTimer.value) {
      clearInterval(autoSaveTimer.value)
      autoSaveTimer.value = null
    }
  }

  return {
    documents,
    currentDocument,
    currentVersions,
    activeEditors,
    isLoading,
    isSaving,
    fetchDocuments,
    fetchDocument,
    createDocument,
    updateDocument,
    autoSave,
    restoreDraft,
    moveToTrash,
    restoreFromTrash,
    deletePermanently,
    fetchTrash,
    fetchVersions,
    rollbackToVersion,
    searchDocuments,
    joinDocumentSession,
    leaveDocumentSession,
    setActiveEditors,
    startAutoSave,
    stopAutoSave
  }
})
