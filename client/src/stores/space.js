import { defineStore } from 'pinia'
import { ref } from 'vue'
import { spaceApi } from '@/services/api'

export const useSpaceStore = defineStore('space', () => {
  const spaces = ref([])
  const currentSpace = ref(null)
  const members = ref([])
  const isLoading = ref(false)

  async function fetchSpaces() {
    isLoading.value = true
    try {
      const response = await spaceApi.getAll()
      spaces.value = response.data
    } catch (error) {
      console.error('Failed to fetch spaces:', error)
    } finally {
      isLoading.value = false
    }
  }

  async function fetchSpace(spaceId) {
    isLoading.value = true
    try {
      const response = await spaceApi.getById(spaceId)
      currentSpace.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch space:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function createSpace(data) {
    try {
      const response = await spaceApi.create(data)
      spaces.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to create space:', error)
      throw error
    }
  }

  async function updateSpace(spaceId, data) {
    try {
      const response = await spaceApi.update(spaceId, data)
      
      if (currentSpace.value?.id === spaceId) {
        currentSpace.value = response.data
      }
      
      const index = spaces.value.findIndex(s => s.id === spaceId)
      if (index !== -1) {
        spaces.value[index] = response.data
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to update space:', error)
      throw error
    }
  }

  async function deleteSpace(spaceId) {
    try {
      await spaceApi.delete(spaceId)
      spaces.value = spaces.value.filter(s => s.id !== spaceId)
      
      if (currentSpace.value?.id === spaceId) {
        currentSpace.value = null
      }
    } catch (error) {
      console.error('Failed to delete space:', error)
      throw error
    }
  }

  async function fetchMembers(spaceId) {
    try {
      const response = await spaceApi.getMembers(spaceId)
      members.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch members:', error)
      return []
    }
  }

  async function addMember(spaceId, data) {
    try {
      const response = await spaceApi.addMember(spaceId, data)
      members.value.push(response.data)
      return response.data
    } catch (error) {
      console.error('Failed to add member:', error)
      throw error
    }
  }

  async function removeMember(spaceId, userId) {
    try {
      await spaceApi.removeMember(spaceId, userId)
      members.value = members.value.filter(m => m.user?.id !== userId)
    } catch (error) {
      console.error('Failed to remove member:', error)
      throw error
    }
  }

  async function updateMemberRole(spaceId, userId, role) {
    try {
      const response = await spaceApi.updateMemberRole(spaceId, userId, role)
      
      const index = members.value.findIndex(m => m.user?.id === userId)
      if (index !== -1) {
        members.value[index] = response.data
      }
      
      return response.data
    } catch (error) {
      console.error('Failed to update member role:', error)
      throw error
    }
  }

  return {
    spaces,
    currentSpace,
    members,
    isLoading,
    fetchSpaces,
    fetchSpace,
    createSpace,
    updateSpace,
    deleteSpace,
    fetchMembers,
    addMember,
    removeMember,
    updateMemberRole
  }
})
