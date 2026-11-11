import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  try {
    await apiClient.post('/account/change-password', { currentPassword, newPassword })
  } catch (error) {
    throw new Error(getApiErrorMessage(error))
  }
}
