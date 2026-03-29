declare const process: {
  env?: {
    TARO_APP_API_BASE_URL?: string;
    TARO_APP_ENV?: string;
    NODE_ENV?: string;
  };
};

export type AppEnv = 'development' | 'staging' | 'production';

export const APP_ENV: AppEnv = (process.env?.TARO_APP_ENV as AppEnv)
  || (process.env?.NODE_ENV === 'production' ? 'production' : 'development');

export const API_BASE_URL = process.env?.TARO_APP_API_BASE_URL || 'http://127.0.0.1:3000/api/v1';

export const isDev = APP_ENV === 'development';
export const isStaging = APP_ENV === 'staging';
export const isProd = APP_ENV === 'production';

export function buildApiUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}
