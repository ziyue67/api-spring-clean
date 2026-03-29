import { request } from '../utils/request';
import type { UserItem } from '../types/api';

type UserResponse = {
  success: boolean;
  data: UserItem | null;
  error: string | null;
};

type UpdateUserResponse = {
  success: boolean;
  data: UserItem;
  message: string;
};

export function getCurrentUser() {
  return request<UserResponse>('/users/me');
}

export function updateCurrentUser(payload: Partial<UserItem>) {
  return request<UpdateUserResponse>('/users/me', {
    method: 'PATCH',
    data: payload
  });
}

export function bindCurrentUserPhone(phone: string) {
  return request<UpdateUserResponse>('/users/me/phone', {
    method: 'PATCH',
    data: { phone }
  });
}
