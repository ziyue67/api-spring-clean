import { request } from '../utils/request';
import type { CourseItem, CourseSearchItem } from '../types/api';

type MonthsResponse = {
  success: boolean;
  months: number[];
};

type CoursesResponse = {
  success: boolean;
  courses: CourseItem[];
};

type SearchResponse = {
  success: boolean;
  results: CourseSearchItem[];
};

type SignupListResponse = {
  success: boolean;
  courses: CourseSearchItem[];
};

type SignupResponse = {
  success: boolean;
  message: string;
  data?: {
    courseId: number;
    legacyId?: string | null;
    signupCount: number;
    maxSeats?: number | null;
    remainingSeats?: number | null;
    isFull?: boolean;
    signupStatus?: 'confirmed' | 'waitlisted';
    isWaitlisted?: boolean;
    waitlistPosition?: number | null;
    signupStartAt?: string | null;
    signupEndAt?: string | null;
    isSignupOpen?: boolean;
  };
};

export function getCourseMonths(college: string) {
  return request<MonthsResponse>(`/courses/months?college=${encodeURIComponent(college)}`);
}

export function getCourses(college: string, month: number) {
  return request<CoursesResponse>(`/courses?college=${encodeURIComponent(college)}&month=${month}`);
}

export function searchCourses(keyword: string, college?: string) {
  const query = college
    ? `/courses/search?keyword=${encodeURIComponent(keyword)}&college=${encodeURIComponent(college)}`
    : `/courses/search?keyword=${encodeURIComponent(keyword)}`;

  return request<SearchResponse>(query);
}

export function signupCourse(courseId?: number | string, legacyId?: string | number) {
  return request<SignupResponse>('/courses/signup', {
    method: 'POST',
    data: {
      ...(typeof courseId === 'number' ? { courseId } : {}),
      ...(legacyId ? { legacyId: String(legacyId) } : {})
    }
  });
}

export function cancelSignupCourse(courseId?: number | string, legacyId?: string | number) {
  return request<SignupResponse>('/courses/cancel-signup', {
    method: 'POST',
    data: {
      ...(typeof courseId === 'number' ? { courseId } : {}),
      ...(legacyId ? { legacyId: String(legacyId) } : {})
    }
  });
}

export function getSignedCourses() {
  return request<SignupListResponse>('/courses/signup-list');
}
