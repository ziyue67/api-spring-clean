import { request } from '../utils/request';
import type { ArticleItem } from '../types/api';

type ArticlesResponse = {
  success: boolean;
  data: ArticleItem[];
};

function validateLimit(limit: number): number {
  return Math.max(1, Math.min(limit, 100)); // 限制在 1-100 之间
}

export function getRecentArticles() {
  return request<ArticlesResponse>('/articles/recent');
}

export function getArticles(limit = 20) {
  const validatedLimit = validateLimit(limit);
  return request<ArticlesResponse>(`/articles?limit=${validatedLimit}`);
}
