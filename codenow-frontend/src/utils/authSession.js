export const AUTH_INVALIDATED_EVENT = 'codenow:auth-invalidated'

export function invalidateAuthSession() {
  localStorage.removeItem('token')
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_INVALIDATED_EVENT))
  }
}
