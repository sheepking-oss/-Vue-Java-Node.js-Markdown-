import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tagApi, commentApi, shareApi } from '@/services/api'
import { useWebSocket } from '@/services/websocket'

export const useUIStore = defineStore('ui', () => {
  const { socket, emitEvent, isConnected } = useWebSocket()

  const sidebarCollapsed = ref(false)
  const activeTab = ref('editor')
  const comments = ref([])
  const tags = ref([])
  const shares = ref([])

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setActiveTab(tab) {
    activeTab.value = tab
  }

  async function fetchTags(spaceId) {
    try {
      const response = await tagApi.getBySpace(spaceId)
      tags.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch tags:', error)
      return []
    }
  }

  async function createTag(spaceId, data) {
    try {
      const response = await tagApi.create(spaceId, data)
      tags.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to create tag:', error)
      throw error
    }
  }

  async function updateTag(tagId, data) {
    try {
      const response = await tagApi.update(tagId, data)
      
      const index = tags.value.findIndex(t => t.id === tagId)
      if (index !== -1) {
        tags.value[index] = response.data
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to update tag:', error)
      throw error
    }
  }

  async function deleteTag(tagId) {
    try {
      await tagApi.delete(tagId)
      tags.value = tags.value.filter(t => t.id !== tagId)
    } catch (error) {
      console.error('Failed to delete tag:', error)
      throw error
    }
  }

  async function fetchComments(documentId) {
    try {
      const response = await commentApi.getByDocument(documentId)
      comments.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch comments:', error)
      return []
    }
  }

  async function createComment(data) {
    try {
      const response = await commentApi.create(data)
      comments.value.unshift(response.data)
      
      if (socket.value && isConnected.value) {
        emitEvent('new-comment', {
          documentId: data.documentId,
          comment: response.data
        })
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to create comment:', error)
      throw error
    }
  }

  async function updateComment(commentId, data) {
    try {
      const response = await commentApi.update(commentId, data)
      
      const index = comments.value.findIndex(c => c.id === commentId)
      if (index !== -1) {
        comments.value[index] = response.data
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to update comment:', error)
      throw error
    }
  }

  async function deleteComment(commentId, documentId) {
    try {
      await commentApi.delete(commentId)
      comments.value = comments.value.filter(c => c.id !== commentId)
      
      if (socket.value && isConnected.value) {
        emitEvent('delete-comment', {
          documentId,
          commentId
        })
      }
    } catch (error) {
      console.error('Failed to delete comment:', error)
      throw error
    }
  }

  async function fetchShares(documentId) {
    try {
      const response = await shareApi.getByDocument(documentId)
      shares.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch shares:', error)
      return []
    }
  }

  async function createShare(data) {
    try {
      const response = await shareApi.create(data)
      shares.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to create share:', error)
      throw error
    }
  }

  async function toggleShare(shareId) {
    try {
      const response = await shareApi.toggle(shareId)
      
      const index = shares.value.findIndex(s => s.id === shareId)
      if (index !== -1) {
        shares.value[index] = response.data
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to toggle share:', error)
      throw error
    }
  }

  async function deleteShare(shareId) {
    try {
      await shareApi.delete(shareId)
      shares.value = shares.value.filter(s => s.id !== shareId)
    } catch (error) {
      console.error('Failed to delete share:', error)
      throw error
    }
  }

  return {
    sidebarCollapsed,
    activeTab,
    comments,
    tags,
    shares,
    toggleSidebar,
    setActiveTab,
    fetchTags,
    createTag,
    updateTag,
    deleteTag,
    fetchComments,
    createComment,
    updateComment,
    deleteComment,
    fetchShares,
    createShare,
    toggleShare,
    deleteShare
  }
})
