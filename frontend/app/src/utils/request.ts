import Taro from '@tarojs/taro';
import { buildApiUrl } from '../config/env';
import { clearSession, getToken } from './session';

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH';
  data?: Record<string, unknown> | unknown[];
};

function getRequestErrorMessage(error: unknown, fallback = '请求失败') {
  const errorMessage = error instanceof Error ? error.message : String(error || '');

  if (!errorMessage) {
    return fallback;
  }

  if (errorMessage.includes('request:fail timeout')) {
    return '请求超时，请稍后重试';
  }

  if (errorMessage.includes('request:fail')) {
    return '网络异常，请检查连接后重试';
  }

  return errorMessage;
}

export async function request<T>(path: string, options: RequestOptions = {}) {
  const token = getToken();
  let response;

  try {
    response = await Taro.request<T>({
      url: buildApiUrl(path),
      method: options.method || 'GET',
      data: options.data,
      header: {
        'content-type': 'application/json',
        'X-API-Version': '1',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      timeout: 30000
    });
  } catch (error) {
    throw new Error(getRequestErrorMessage(error));
  }

  if (response.statusCode < 200 || response.statusCode >= 300) {
    if (response.statusCode === 401) {
      clearSession();
      Taro.showToast({
        title: '登录已过期，请重新登录',
        icon: 'none',
        duration: 2000
      });
      setTimeout(() => {
        void Taro.reLaunch({ url: '/pages/login/index' });
      }, 800);
    }

    if (response.statusCode === 403) {
      throw new Error('当前账号暂无权限执行该操作');
    }

    const errorMessage = (response.data as { message?: string; error?: string } | undefined)?.message
      || (response.data as { message?: string; error?: string } | undefined)?.error
      || '请求失败';

    throw new Error(errorMessage);
  }

  return response.data;
}
