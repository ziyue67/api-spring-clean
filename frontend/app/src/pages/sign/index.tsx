import { View, Text, Button } from '@tarojs/components';
import Taro, { useDidShow } from '@tarojs/taro';
import { useState } from 'react';
import { doSign, getSignStatus } from '../../services/sign';
import type { SignStatus } from '../../types/api';
import { getUser, isLoggedIn, setStoredUser } from '../../utils/session';
import './index.scss';

const initialStatus: SignStatus = {
  success: true,
  points: 0,
  totalSigns: 0,
  signedToday: false,
  signRecord: []
};

export default function SignPage() {
  const [status, setStatus] = useState<SignStatus>(initialStatus);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const loadStatus = async () => {
    if (!isLoggedIn()) {
      setError('请先登录');
      setStatus(initialStatus);
      return;
    }

    setLoading(true);
    try {
      const result = await getSignStatus();
      setStatus(result);
      setError('');

      const user = getUser();
      if (user) {
        setStoredUser({
          ...user,
          points: result.points,
          totalSigns: result.totalSigns
        });
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSign = async () => {
    if (!isLoggedIn()) {
      setError('请先登录');
      return;
    }

    setLoading(true);
    try {
      const result = await doSign();
      if (!result.success) {
        throw new Error(result.error || '签到失败');
      }
      Taro.showToast({ title: `签到成功 +${result.points || 10}`, icon: 'success' });
      await loadStatus();
    } catch (signError) {
      setError(signError instanceof Error ? signError.message : '签到失败');
    } finally {
      setLoading(false);
    }
  };

  useDidShow(() => {
    void loadStatus();
  });

  return (
    <View className='page'>
      <Text className='title'>每日签到</Text>
      {error ? <Text className='error'>{error}</Text> : null}
      <View className='card'>
        <Text className='item'>当前状态：{isLoggedIn() ? '已登录' : '未登录'}</Text>
        <Text className='item'>积分：{status.points}</Text>
        <Text className='item'>总签到次数：{status.totalSigns}</Text>
        <Text className='item'>今日状态：{status.signedToday ? '已签到' : '未签到'}</Text>
      </View>
      <Button className='button' loading={loading} disabled={status.signedToday} onClick={handleSign}>
        {status.signedToday ? '今日已签到' : '立即签到'}
      </Button>
      <View className='record-list'>
        {status.signRecord.map((item) => (
          <View className='record-item' key={`${item.date}-${item.points}`}>
            <Text>{item.date}</Text>
            <Text>+{item.points}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}
