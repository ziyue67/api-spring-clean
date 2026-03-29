import { request } from '../utils/request';
import type { SignStatus } from '../types/api';

type SignResponse = {
  success: boolean;
  points?: number;
  error?: string;
};

export function getSignStatus() {
  return request<SignStatus>('/sign/status');
}

export function doSign() {
  return request<SignResponse>('/sign', {
    method: 'POST',
    data: {}
  });
}
