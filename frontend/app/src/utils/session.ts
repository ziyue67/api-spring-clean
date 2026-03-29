import Taro from '@tarojs/taro';
import type { UserItem } from '../types/api';

const LEGACY_OPENID_KEY = 'youth-openid';
const TOKEN_KEY = 'youth-token';
const USER_KEY = 'youth-user';

function sanitizeOpenid(value: unknown) {
  return String(value || '').trim().slice(0, 64);
}

function sanitizeToken(value: unknown) {
  return String(value || '').trim().slice(0, 2048);
}

function sanitizeUser(user: UserItem | null | undefined): UserItem | null {
  if (!user || typeof user !== 'object') {
    return null;
  }

  return {
    ...user,
    openid: user.openid ? sanitizeOpenid(user.openid) : undefined,
    unionid: user.unionid ? String(user.unionid).trim().slice(0, 64) : user.unionid,
    nickName: user.nickName ? String(user.nickName).replace(/[<>]/g, '').trim().slice(0, 100) : user.nickName,
    avatarUrl: user.avatarUrl ? String(user.avatarUrl).trim().slice(0, 500) : user.avatarUrl,
    phone: user.phone ? String(user.phone).trim().slice(0, 32) : user.phone
  };
}

export function getToken() {
  return sanitizeToken(Taro.getStorageSync<string>(TOKEN_KEY));
}

export function isLoggedIn() {
  return !!getToken();
}

export function getUser() {
  return sanitizeUser(Taro.getStorageSync<UserItem>(USER_KEY) || null);
}

export function setSession(token: string, user: UserItem) {
  Taro.setStorageSync(TOKEN_KEY, sanitizeToken(token));
  Taro.setStorageSync(USER_KEY, sanitizeUser(user));
}

export function setStoredUser(user: UserItem) {
  Taro.setStorageSync(USER_KEY, sanitizeUser(user));
}

export function clearSession() {
  Taro.removeStorageSync(LEGACY_OPENID_KEY);
  Taro.removeStorageSync(TOKEN_KEY);
  Taro.removeStorageSync(USER_KEY);
}
