import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { documentApi, versionApi, operationApi } from '@/services/api'
import { useWebSocket } from '@/services/websocket'
import { computeDiff, applyOperations, OTAlgorithm, Operation } from '@/utils/ot'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref([])
  const currentDocument = ref(null)
  const currentVersions = ref([])
  const activeEditors = ref([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const autoSaveTimer = ref(null)

  const documentVersion = ref(1)
  const baseContent = ref('')
  const isUsingOT = ref(false)

  const { 
    socket, 
    joinDocument, 
    leaveDocument, 
    emitEvent, 
    isConnected,
    submitOperations,
    requestSync,
    localVersion,
    acknowledgedVersion,
    pendingOperationsCount
  } = useWebSocket()

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
      
      documentVersion.value = response.data.version || 1
      baseContent.value = response.data.content || ''
      
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
      if (isUsingOT.value && data.content !== undefined) {
        const currentContent = currentDocument.value?.content || ''
        const newContent = data.content
        
        if (currentContent !== newContent) {
          const operations = computeDiff(currentContent, newContent)
          
          if (operations.length > 0) {
            try {
              const result = await submitOperations(documentId, operations, currentContent)
              
              if (!result.queued) {
                baseContent.value = newContent
                documentVersion.value = result.newVersion || documentVersion.value + 1
                
                if (currentDocument.value?.id === documentId) {
                  currentDocument.value.content = newContent
                  currentDocument.value.version = documentVersion.value
                }
              }
            } catch (error) {
              console.error('OT operations failed:', error)
              throw error
            }
          }
        }
        
        if (data.title !== undefined && data.title !== currentDocument.value?.title) {
          const response = await documentApi.update(documentId, {
            title: data.title,
            changeNote: data.changeNote
          })
          
          if (currentDocument.value?.id === documentId) {
            currentDocument.value.title = data.title
            currentDocument.value.version = response.data.version
          }
          
          return response.data
        }
        
        return currentDocument.value
      }
      
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

  async function applyOperationsToDocument(documentId, operations, fromVersion) {
    isSaving.value = true
    try {
      const currentContent = currentDocument.value?.content || ''
      const newContent = applyOperations(currentContent, operations)
      
      const result = await submitOperations(documentId, operations, currentContent)
      
      if (!result.queued) {
        if (currentDocument.value?.id === documentId) {
          currentDocument.value.content = newContent
          currentDocument.value.version = result.newVersion || documentVersion.value + 1
        }
        documentVersion.value = result.newVersion || documentVersion.value + 1
        baseContent.value = newContent
      }
      
      return { success: true, newVersion: result.newVersion, content: newContent }
    } catch (error) {
      console.error('Failed to apply operations:', error)
      throw error
    } finally {
      isSaving.value = false
    }
  }

  async function computeDocumentDiff(oldContent, newContent) {
    try {
      const response = await operationApi.computeDiff(oldContent, newContent)
      return response.data.operations
    } catch (error) {
      console.error('Failed to compute diff via API:', error)
      return computeDiff(oldContent, newContent)
    }
  }

  async function autoSave(documentId, draftContent) {
    try {
      if (isUsingOT.value && socket.value && isConnected.value) {
        const currentContent = currentDocument.value?.content || baseContent.value
        
        if (currentContent !== draftContent) {
          const operations = computeDiff(currentContent, draftContent)
          
          if (operations.length > 0) {
            try {
              const result = await submitOperations(documentId, operations, currentContent)
              
              if (!result.queued) {
                if (currentDocument.value?.id === documentId) {
                  currentDocument.value.content = draftContent
                  currentDocument.value.version = result.newVersion || documentVersion.value + 1
                }
                documentVersion.value = result.newVersion || documentVersion.value + 1
                baseContent.value = draftContent
              }
              
              emitEvent('auto-save', {
                documentId,
                content: draftContent,
                version: documentVersion.value
              })
              
              return
            } catch (error) {
              console.error('OT auto-save failed, falling back to regular auto-save:', error)
            }
          }
        }
      }
      
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
        baseContent.value = response.data.content || ''
        documentVersion.value = response.data.version || 1
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
        baseContent.value = response.data.content || ''
        documentVersion.value = response.data.version || version
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

  function joinDocumentSession(documentId, spaceId, content = '', version = 1) {
    if (socket.value && isConnected.value) {
      isUsingOT.value = true
      baseContent.value = content
      documentVersion.value = version
      joinDocument(documentId, spaceId, content, version)
    }
  }

  function leaveDocumentSession(documentId) {
    if (socket.value && isConnected.value) {
      leaveDocument(documentId)
    }
    activeEditors.value = []
    isUsingOT.value = false
  }

  async function syncDocument(documentId) {
    try {
      const result = await requestSync(documentId)
      
      if (currentDocument.value?.id === documentId) {
        currentDocument.value.content = result.content
        currentDocument.value.version = result.version
      }
      documentVersion.value = result.version
      baseContent.value = result.content
      
      return result
    } catch (error) {
      console.error('Failed to sync document:', error)
      throw error
    }
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

  function setUsingOT(enabled) {
    isUsingOT.value = enabled
  }

  return {
    documents,
    currentDocument,
    currentVersions,
    activeEditors,
    isLoading,
    isSaving,
    documentVersion,
    baseContent,
    isUsingOT,
    localVersion,
    acknowledgedVersion,
    pendingOperationsCount,
    
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
    stopAutoSave,
    
    applyOperationsToDocument,
    computeDocumentDiff,
    syncDocument,
    setUsingOT
  }
})
