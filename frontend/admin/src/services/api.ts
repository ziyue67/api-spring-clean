import { request } from '../config';

export type OverviewData = {
  totalUsers: number;
  totalCourses: number;
  totalConfirmedSignups: number;
  totalSignLogs: number;
  waitlistedSignups: number;
  recentUsers: Array<{
    id: number;
    nickName: string | null;
    avatarUrl: string | null;
    phone: string | null;
    points: number;
    createdAt: string;
    lastLoginAt: string;
  }>;
  topCourses: Array<{
    id: number;
    title: string;
    college: string;
    teacher: string;
    month: number;
    maxSeats: number;
    status: string;
    signupCount: number;
  }>;
};

export type CourseStatsData = {
  id: number;
  title: string;
  college: string;
  teacher: string;
  month: number;
  week: string;
  timeStart: string;
  timeEnd: string;
  maxSeats: number;
  status: string;
  difficulty: string;
  audience: string;
  signupCount: number;
  confirmedCount: number;
  waitlistedCount: number;
  fillRate: number;
};

export type SignTrendsData = {
  date: string;
  count: number;
};

export type CollegeStatsData = {
  college: string;
  courseCount: number;
};

export type UserListData = {
  data: Array<{
    id: number;
    openid: string;
    nickName: string | null;
    avatarUrl: string | null;
    phone: string | null;
    points: number;
    roles: string[];
    createdAt: string;
    lastLoginAt: string;
    signupCount: number;
    signCount: number;
  }>;
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
};

export type SignupListData = {
  id: number;
  status: string;
  createdAt: string;
  userId: number | null;
  nickName: string;
  avatarUrl: string;
  phone: string;
};

export function fetchOverview() {
  return request<OverviewData>('/admin/stats/overview');
}

export function fetchCourseStats() {
  return request<CourseStatsData[]>('/admin/stats/courses');
}

export function fetchSignTrends(days = 30) {
  return request<SignTrendsData[]>(`/admin/stats/sign-trends?days=${days}`);
}

export function fetchCollegeStats() {
  return request<CollegeStatsData[]>('/admin/stats/colleges');
}

export function fetchUserList(page = 1, pageSize = 20, keyword?: string) {
  const params = new URLSearchParams();
  params.set('page', page.toString());
  params.set('pageSize', pageSize.toString());
  if (keyword) params.set('keyword', keyword);
  return request<UserListData>(`/admin/users?${params}`);
}

export function fetchCourseSignups(courseId: number) {
  return request<SignupListData[]>(`/admin/courses/${courseId}/signups`);
}
