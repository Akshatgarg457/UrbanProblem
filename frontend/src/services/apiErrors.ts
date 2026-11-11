import axios from 'axios'

interface ApiErrorBody {
  message?: unknown
  error?: unknown
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const responseMessage = error.response?.data?.message ?? error.response?.data?.error
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
    if (error.code === 'ECONNABORTED') {
      return 'The request timed out. Please try again.'
    }
    if (error.response) {
      return `The request failed with status ${error.response.status}.`
    }
    return 'The service could not be reached. Check your connection and try again.'
  }

  if (error instanceof Error) {
    return error.message
  }
  return 'An unexpected error occurred.'
}
