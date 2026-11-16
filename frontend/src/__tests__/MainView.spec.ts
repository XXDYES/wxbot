import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MainView from '../views/MainView.vue'
import userApi from '../api/user'
import botApi from '../api/bot'

vi.mock('../api/user', () => ({
  default: {
    getUserInfo: vi.fn(), getAiSetting: vi.fn(), getUsage: vi.fn(),
  },
}))
vi.mock('../api/bot', () => ({ default: { connectTest: vi.fn() } }))

const mountPage = () => shallowMount(MainView, {
  global: {
    stubs: ['ElButton', 'ElTooltip', 'ElIcon', 'ElSelect', 'ElOption'],
  },
})

describe('MainView initialization', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    localStorage.clear()
  })

  it('opens login without sending protected requests when no token is saved', async () => {
    const page = mountPage()
    await flushPromises()
    expect(page.findComponent({ name: 'LoginDialog' }).props('modelValue')).toBe(true)
    expect(userApi.getUserInfo).not.toHaveBeenCalled()
    expect(botApi.connectTest).not.toHaveBeenCalled()
    page.unmount()
  })

  it('loads usage and connection status while AI settings are still pending', async () => {
    localStorage.setItem('userToken', 'test-token')
    vi.mocked(userApi.getUserInfo).mockResolvedValue({
      data: { code: '200', data: { id: 1, username: 'test' } },
    } as never)
    let finishSettings!: (value: never) => void
    vi.mocked(userApi.getAiSetting).mockImplementation(() => new Promise((resolve) => {
      finishSettings = resolve
    }))
    vi.mocked(userApi.getUsage).mockResolvedValue({
      data: { code: '200', data: {
        chatTimesToday: 0, chatTimesHistory: 0, tokenToday: 0, tokenHistory: 0,
      } },
    } as never)
    vi.mocked(botApi.connectTest).mockResolvedValue({
      data: { code: '200', data: 'CONNECTED' },
    } as never)
    const page = mountPage()
    await flushPromises()
    expect(userApi.getAiSetting).toHaveBeenCalledOnce()
    expect(userApi.getUsage).toHaveBeenCalledOnce()
    expect(botApi.connectTest).toHaveBeenCalledOnce()
    finishSettings({ data: { code: '200', data: {
      model: 'test', reasoningEffort: 'none', persona: '',
    } } } as never)
    await flushPromises()
    page.unmount()
  })
})
