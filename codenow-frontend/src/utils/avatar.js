export const DEFAULT_AVATAR = '/images/default-avatar.svg'

export function avatarUrl(url) {
  return url || DEFAULT_AVATAR
}

export function useDefaultAvatar(event) {
  const image = event.currentTarget
  image.onerror = null
  image.src = DEFAULT_AVATAR
}
