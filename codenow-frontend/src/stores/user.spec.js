import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from './user'

describe('user store author console access', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it.each([
    ['USER', false],
    ['AUTHOR', true],
    ['author', true],
    ['ADMIN', true],
  ])('reports author console access for role %s', (role, expected) => {
    const store = useUserStore()
    store.userInfo = { role }

    expect(store.canEnterAuthorConsole).toBe(expected)
  })
})
