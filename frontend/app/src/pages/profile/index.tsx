import { View, Text, Button, Input, Image } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { useState } from 'react';
import { cancelSignupCourse, getSignedCourses } from '../../services/courses';
import { bindCurrentUserPhone, getCurrentUser, updateCurrentUser } from '../../services/users';
import type { CourseSearchItem, UserItem } from '../../types/api';
import { clearSession, getUser, isLoggedIn, setStoredUser } from '../../utils/session';
import { sanitizePhoneInput, sanitizeTextInput } from '../../utils/sanitize';
import './index.scss';

const COURSE_STATUS_LABEL_MAP: Record<string, string> = {
  available: '报名中',
  closed: '已关闭',
  draft: '未发布'
};

export default function ProfilePage() {
  const [user, setUser] = useState<UserItem | null>(getUser());
  const [error, setError] = useState('');
  const [nickName, setNickName] = useState(getUser()?.nickName || '');
  const [phone, setPhone] = useState(getUser()?.phone || '');
  const [signedCourses, setSignedCourses] = useState<CourseSearchItem[]>([]);
  const [saving, setSaving] = useState(false);
  const [cancelingCourseId, setCancelingCourseId] = useState('');

  const isNickNameChanged = nickName !== (user?.nickName || '');
  const isPhoneChanged = phone !== (user?.phone || '');

  const getMaskedPhone = (value?: string | null) => {
    const normalized = String(value || '').trim();
    return /^1[3-9]\d{9}$/.test(normalized)
      ? `${normalized.slice(0, 3)}****${normalized.slice(-4)}`
      : (normalized || '未绑定');
  };

  const confirmPhoneBinding = async (phoneNumber: string) => {
    const result = await Taro.showModal({
      title: '确认绑定手机号',
      content: `确认绑定手机号 ${getMaskedPhone(phoneNumber)} 吗？`,
      confirmText: '确认绑定',
      confirmColor: '#2563eb'
    });

    return !!result.confirm;
  };

  useDidShow(() => {
    if (!isLoggedIn()) {
      setError('当前未登录');
      setUser(null);
      return;
    }

      const loadUser = async () => {
        try {
          const result = await getCurrentUser();
          if (!result.success || !result.data) {
            clearSession();
            setUser(null);
            setNickName('');
            setPhone('');
            setError(result.error || '用户不存在，请重新登录');
            return;
          }

          setUser(result.data);
          setStoredUser(result.data);
          setNickName(result.data?.nickName || '');
          setPhone(result.data?.phone || '');

          const signupResult = await getSignedCourses();
          setSignedCourses(signupResult.courses || []);
          setError('');
        } catch (loadError) {
          setUser(null);
          setSignedCourses([]);
          setError(loadError instanceof Error ? loadError.message : '加载失败');
        }
      };

    void loadUser();
  });

  return (
    <View className='page'>
      <Text className='title'>我的信息</Text>
      {error ? <Text className='error'>{error}</Text> : null}
      {user ? (
        <View className='card'>
          <View className='section profile-summary'>
            <Text className='section-title'>账户信息</Text>
            <View className='profile-info-grid'>
              <View className='profile-info-card'>
                <Text className='profile-info-label'>账号状态</Text>
                <Text className='profile-info-value'>已登录</Text>
              </View>
              <View className='profile-info-card'>
                <Text className='profile-info-label'>昵称</Text>
                <Text className='profile-info-value'>{user.nickName || '未设置'}</Text>
              </View>
              <View className='profile-info-card'>
                <Text className='profile-info-label'>手机号</Text>
                <Text className='profile-info-value'>{getMaskedPhone(user.phone)}</Text>
              </View>
              <View className='profile-info-card'>
                <Text className='profile-info-label'>积分</Text>
                <Text className='profile-info-value'>{user.points || 0}</Text>
              </View>
            </View>
          </View>

          <View className='section'>
            <Text className='section-title'>我的报名</Text>
            {signedCourses.length === 0 ? <Text className='muted'>暂未报名课程</Text> : null}
            {signedCourses.map((course) => (
              <View className='course-card' key={`${course.id}-${course.signedAt || course.month}`}>
                {course.coverImage ? <Image className='course-cover' src={course.coverImage} mode='aspectFill' /> : null}
                <View className='card-top-row'>
                  <View className='card-badges'>
                    <Text className={`course-status status-${course.status === 'available' ? 'active' : 'default'}`}>
                      {COURSE_STATUS_LABEL_MAP[course.status || 'available'] || '报名中'}
                    </Text>
                    <Text className={`signed-state ${course.signupStatus === 'waitlisted' ? 'waitlisted' : ''}`}>
                      {course.signupStatus === 'waitlisted' ? '候补中' : '已报名'}
                    </Text>
                  </View>
                  <Text className='capacity-pill'>余位：{typeof course.remainingSeats === 'number' ? `${course.remainingSeats}` : '不限'}</Text>
                </View>
                <Text className='course-title'>{course.name}</Text>
                <View className='headline-meta stacked'>
                  <Text className='course-meta headline'>{course.college}</Text>
                  <Text className='course-meta headline'>{course.month} 月 · {course.time}</Text>
                  <Text className='course-meta headline'>{course.signupCount || 0} 人已报名</Text>
                </View>
                <View className='facts-row'>
                  <Text className='fact-pill'>状态：{COURSE_STATUS_LABEL_MAP[course.status || 'available'] || '报名中'}</Text>
                  <Text className='fact-pill'>名额：{typeof course.remainingSeats === 'number' ? `剩余 ${course.remainingSeats} 个` : '名额充足'}</Text>
                  {course.signupStatus === 'waitlisted' && course.waitlistPosition ? <Text className='fact-pill queue'>候补顺位：第 {course.waitlistPosition} 位</Text> : null}
                </View>
                <View className='meta-grid'>
                  {course.teacher ? <Text className='course-meta meta-chip'>讲师：{course.teacher}</Text> : null}
                  {course.location ? <Text className='course-meta meta-chip'>地点：{course.location}</Text> : null}
                  {course.difficulty ? <Text className='course-meta meta-chip difficulty'>难度：{course.difficulty}</Text> : null}
                  {course.audience ? <Text className='course-meta meta-chip audience'>适合人群：{course.audience}</Text> : null}
                  {course.duration ? <Text className='course-meta meta-chip duration'>时长：{course.duration}</Text> : null}
                  {course.fee ? <Text className='course-meta meta-chip fee'>费用：{course.fee}</Text> : null}
                </View>
                {(course.notice || course.materials) ? <Text className='detail-section-title'>报名提醒</Text> : null}
                {course.notice ? <Text className='course-meta detail-block notice'>报名须知：{course.notice}</Text> : null}
                {course.materials ? <Text className='course-meta detail-block materials'>需自备：{course.materials}</Text> : null}
                {course.tags?.length ? (
                  <View className='tag-list'>
                    {course.tags.map((tag) => (
                      <Text className='tag-chip' key={`${course.id}-${tag}`}>{tag}</Text>
                    ))}
                  </View>
                ) : null}
                {course.description ? <Text className='detail-section-title'>课程简介</Text> : null}
                {course.description ? <Text className='course-meta detail-block'>{course.description}</Text> : null}
                {course.signupStartAt || course.signupEndAt ? (
                  <Text className='course-meta signup-window'>
                    报名时间：{course.signupStartAt ? new Date(course.signupStartAt).toLocaleString('zh-CN', { hour12: false }) : '即刻'} - {course.signupEndAt ? new Date(course.signupEndAt).toLocaleString('zh-CN', { hour12: false }) : '长期开放'}
                  </Text>
                ) : null}
                <Button
                  className='button cancel-btn'
                  loading={cancelingCourseId === String(course.id)}
                  onClick={async () => {
                      if (!user) {
                        return;
                      }

                    const confirmResult = await Taro.showModal({
                      title: '提示',
                      content: course.signupStatus === 'waitlisted' ? '确定取消候补吗？' : '确定取消报名吗？',
                      confirmColor: '#ef4444'
                    });

                    if (!confirmResult.confirm) {
                      return;
                    }

                    setCancelingCourseId(String(course.id));
                    try {
                      const result = await cancelSignupCourse(undefined, course.id);
                      if (!result.success) {
                        throw new Error(result.message || '取消报名失败');
                      }

                      const signupResult = await getSignedCourses();
                      setSignedCourses(signupResult.courses || []);
                      Taro.showToast({ title: '已取消报名', icon: 'success' });
                    } catch (cancelError) {
                      setError(cancelError instanceof Error ? cancelError.message : '取消报名失败');
                    } finally {
                      setCancelingCourseId('');
                    }
                  }}
                >
                  {course.signupStatus === 'waitlisted' ? '取消候补' : '取消报名'}
                </Button>
                <Text className='course-action-hint'>
                  {course.signupStatus === 'waitlisted' ? '取消后将移出当前候补名单' : '取消后该课程名额将重新释放'}
                </Text>
              </View>
            ))}
          </View>

          <View className='section action-section'>
            <Text className='section-title'>资料操作</Text>
            <View className='action-card'>
              <Text className='action-title'>更新昵称</Text>
              <Input
                className='input'
                placeholder='更新昵称'
                value={nickName || ''}
                disabled={saving}
                onInput={(event) => setNickName(sanitizeTextInput(event.detail.value, 100))}
              />
              <Button
                className='button secondary'
                loading={saving}
                disabled={saving || !isNickNameChanged}
                onClick={async () => {
                  if (!user) {
                    return;
                  }

                  setSaving(true);
                  try {
                    const result = await updateCurrentUser({ nickName: sanitizeTextInput(nickName, 100) });
                    setUser(result.data);
                    setStoredUser(result.data);
                    Taro.showToast({ title: '昵称已更新', icon: 'success' });
                    setError('');
                  } catch (updateError) {
                    setError(updateError instanceof Error ? updateError.message : '更新失败');
                  } finally {
                    setSaving(false);
                  }
                }}
              >
                更新昵称
              </Button>
            </View>
            <View className='action-card'>
              <Text className='action-title'>绑定手机号</Text>
              <Input
                className='input'
                placeholder='绑定手机号'
                value={phone || ''}
                disabled={saving}
                onInput={(event) => setPhone(sanitizePhoneInput(event.detail.value))}
              />
              <Button
                className='button secondary'
                loading={saving}
                disabled={saving || !phone.trim() || !isPhoneChanged}
                onClick={async () => {
                  const normalizedPhone = sanitizePhoneInput(phone);
                  if (!user || !normalizedPhone) {
                    setError('请输入手机号');
                    return;
                  }

                  const confirmed = await confirmPhoneBinding(normalizedPhone);
                  if (!confirmed) {
                    return;
                  }

                  setSaving(true);
                  try {
                    const result = await bindCurrentUserPhone(normalizedPhone);
                    setUser(result.data);
                    setStoredUser(result.data);
                    setPhone(normalizedPhone);
                    Taro.showToast({ title: '手机号已绑定', icon: 'success' });
                    setError('');
                  } catch (bindError) {
                    setError(bindError instanceof Error ? bindError.message : '绑定失败');
                  } finally {
                    setSaving(false);
                  }
                }}
              >
                绑定手机号
              </Button>
            </View>
          </View>
        </View>
      ) : null}
      <Button
        className='button'
        onClick={() => {
          clearSession();
          setUser(null);
          setError('已退出登录');
          Taro.showToast({ title: '已退出', icon: 'success' });
          void Taro.switchTab({ url: '/pages/home/index' }).catch(() => {
            void Taro.navigateTo({ url: '/pages/home/index' });
          });
        }}
      >
        退出登录
      </Button>
    </View>
  );
}
