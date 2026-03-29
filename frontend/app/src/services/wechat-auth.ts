import { request } from '../utils/request';
import type { UserItem } from '../types/api';

type WechatLoginResponse = {
  success: boolean;
  data: UserItem;
  token: string;
  message: string;
};

export function loginWithWechatCode(code: string, nickName?: string, avatarUrl?: string) {
  return request<WechatLoginResponse>('/auth/wechat-login', {
    method: 'POST',
    data: {
      code,
      nickName,
      avatarUrl
    }
  });
}
