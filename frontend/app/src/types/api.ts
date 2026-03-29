export type ArticleItem = {
  id: string | number;
  title: string;
  link: string;
  publish_time: string | null;
  create_time: string;
};

export type CourseItem = {
  id: string;
  name: string;
  time: string;
  teacher: string;
  location?: string;
  description?: string;
  coverImage?: string;
  difficulty?: string;
  audience?: string;
  duration?: string;
  fee?: string;
  notice?: string;
  materials?: string;
  tags?: string[];
  status: string;
  signupCount?: number;
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

export type CourseSearchItem = {
  id: string;
  name: string;
  college: string;
  teacher?: string;
  location?: string;
  description?: string;
  coverImage?: string;
  difficulty?: string;
  audience?: string;
  duration?: string;
  fee?: string;
  notice?: string;
  materials?: string;
  tags?: string[];
  month: number;
  time: string;
  status: string;
  signupCount?: number;
  maxSeats?: number | null;
  remainingSeats?: number | null;
  isFull?: boolean;
  signupStatus?: 'confirmed' | 'waitlisted';
  isWaitlisted?: boolean;
  waitlistPosition?: number | null;
  signupStartAt?: string | null;
  signupEndAt?: string | null;
  isSignupOpen?: boolean;
  signedAt?: string;
};

export type UserItem = {
  id?: string | number;
  openid?: string;
  unionid?: string | null;
  nickName?: string | null;
  avatarUrl?: string | null;
  phone?: string | null;
  points?: number;
  totalSigns?: number;
  createdAt?: string;
  lastLoginAt?: string;
};

export type SignStatus = {
  success: boolean;
  points: number;
  totalSigns: number;
  signedToday: boolean;
  signRecord: Array<{
    date: string;
    points: number;
  }>;
};
